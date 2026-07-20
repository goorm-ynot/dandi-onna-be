package com.mvp.v1.dandionna.export_job.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.common.util.RequestDateParser;
import com.mvp.v1.dandionna.export_job.dto.ExportJobCreateRequest;
import com.mvp.v1.dandionna.export_job.dto.ExportJobCreateResponse;
import com.mvp.v1.dandionna.export_job.dto.ExportJobStatusResponse;
import com.mvp.v1.dandionna.export_job.entity.ExportJob;
import com.mvp.v1.dandionna.export_job.entity.ExportJobStatus;
import com.mvp.v1.dandionna.export_job.producer.ExportJobProducer;
import com.mvp.v1.dandionna.export_job.repository.ExportJobRepository;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportJobService {

	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
	private static final String REPORT_TYPE_OWNER_SALES = "OWNER_SALES";
	private static final short FORMAT_VERSION = 1;

	private static final String REDIS_JOB_PREFIX = "export:job:";
	private static final String REDIS_LOCK_PREFIX = "export:lock:";

	private final ExportJobRepository exportJobRepository;
	private final StoreRepository storeRepository;
	private final ExportJobProducer exportJobProducer;
	private final UploadService uploadService;
	private final StringRedisTemplate redisTemplate;

	@Value("${app.export.presign-ttl-seconds:600}")
	private long presignTtlSeconds;

	@Value("${app.export.redis-ttl-seconds:900}")
	private long redisTtlSeconds;

	@Value("${app.export.lock-ttl-seconds:60}")
	private long lockTtlSeconds;

	@Transactional
	public ExportJobCreateResponse requestOwnerSalesExport(UUID ownerId, ExportJobCreateRequest request) {
		Store store = loadStore(ownerId);
		LocalDate start = parseDate(request.startDate(), "startDate");
		LocalDate end = parseDate(request.endDate(), "endDate");
		if (end.isBefore(start)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "종료 날짜는 시작 날짜 이후여야 합니다.");
		}

		boolean includeDetail = request.includeDetail() == null || request.includeDetail();
		String requestHash = computeRequestHash(store.getId(), start, end, includeDetail);
		String lockKey = REDIS_LOCK_PREFIX + requestHash;
		boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
			.setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtlSeconds)));

		try {
			ExportJob existing = exportJobRepository.findByRequestHashAndActiveTrue(requestHash).orElse(null);
			if (existing != null) {
				if (existing.getStatus() == ExportJobStatus.QUEUED || existing.getStatus() == ExportJobStatus.PROCESSING) {
					return toCreateResponse(existing);
				}
				if (existing.getStatus() == ExportJobStatus.DONE && !isFileExpired(existing)) {
					return toCreateResponse(existing);
				}
				existing.setStatus(ExportJobStatus.EXPIRED);
				existing.deactivate();
				exportJobRepository.save(existing);
			}

			ExportJob job = ExportJob.create(store.getId(), ownerId, requestHash, start, end);
			job.setIncludeDetail(includeDetail);
			job.setReportType(REPORT_TYPE_OWNER_SALES);
			job.setFormatVersion(FORMAT_VERSION);
			ExportJob saved = exportJobRepository.save(job);
			enqueueAfterCommit(saved.getId());
			return toCreateResponse(saved);
		} finally {
			if (locked) {
				redisTemplate.delete(lockKey);
			}
		}
	}

	@Transactional(readOnly = true)
	public ExportJobStatusResponse getOwnerSalesExportStatus(UUID ownerId, UUID jobId) {
		ExportJob job = loadJobForOwner(ownerId, jobId);
		if (job.getStatus() != ExportJobStatus.DONE) {
			return new ExportJobStatusResponse(job.getId(), job.getStatus(), null, null, null, job.getErrorMessage());
		}

		return buildDoneResponse(job);
	}

	@Transactional
	public ExportJobStatusResponse refreshOwnerSalesExport(UUID ownerId, UUID jobId) {
		ExportJob job = loadJobForOwner(ownerId, jobId);
		if (job.getStatus() != ExportJobStatus.DONE) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "완료된 엑셀만 재발급할 수 있습니다.");
		}
		if (isFileExpired(job)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "파일 보관 기간이 만료되었습니다.");
		}
		if (job.getFileKey() == null || job.getFileKey().isBlank()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "다운로드 가능한 파일이 없습니다.");
		}

		return issueAndCacheUrl(job);
	}

	private ExportJobStatusResponse buildDoneResponse(ExportJob job) {
		Map<Object, Object> cached = redisTemplate.opsForHash().entries(redisKey(job.getId()));
		String cachedUrl = cached.get("downloadUrl") != null ? cached.get("downloadUrl").toString() : null;
		OffsetDateTime cachedExpiresAt = parseOffsetDateTime(cached.get("expiresAt"));

		if (cachedUrl != null && cachedExpiresAt != null && cachedExpiresAt.isAfter(OffsetDateTime.now())) {
			return new ExportJobStatusResponse(job.getId(), job.getStatus(), null, cachedUrl, cachedExpiresAt,
				job.getErrorMessage());
		}

		if (job.getFileKey() == null || job.getFileKey().isBlank() || isFileExpired(job)) {
			return new ExportJobStatusResponse(job.getId(), job.getStatus(), null, null, null, job.getErrorMessage());
		}

		return issueAndCacheUrl(job);
	}

	private ExportJobStatusResponse issueAndCacheUrl(ExportJob job) {
		PresignedUrlResponse presigned = uploadService.presignDownloadWithTtl(job.getFileKey(), presignTtlSeconds);
		OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(presignTtlSeconds);
		cacheDownload(job.getId(), presigned.url(), expiresAt);
		return new ExportJobStatusResponse(job.getId(), job.getStatus(), null, presigned.url(), expiresAt,
			job.getErrorMessage());
	}

	private void cacheDownload(UUID jobId, String url, OffsetDateTime expiresAt) {
		Map<String, String> payload = Map.of(
			"downloadUrl", url,
			"expiresAt", expiresAt.toString()
		);
		String key = redisKey(jobId);
		redisTemplate.opsForHash().putAll(key, payload);
		redisTemplate.expire(key, Duration.ofSeconds(redisTtlSeconds));
	}

	private ExportJob loadJobForOwner(UUID ownerId, UUID jobId) {
		Store store = loadStore(ownerId);
		ExportJob job = exportJobRepository.findById(jobId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "엑셀 작업을 찾을 수 없습니다."));
		if (!job.getStoreId().equals(store.getId())) {
			throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "본인의 엑셀 작업만 조회할 수 있습니다.");
		}
		return job;
	}

	private Store loadStore(UUID ownerId) {
		return storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
	}

	private String computeRequestHash(UUID storeId, LocalDate startDate, LocalDate endDate, boolean includeDetail) {
		String payload = storeId + "|" + startDate + "|" + endDate + "|" + includeDetail + "|" + REPORT_TYPE_OWNER_SALES
			+ "|" + FORMAT_VERSION;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "요청 해시 생성에 실패했습니다.");
		}
	}

	private boolean isFileExpired(ExportJob job) {
		if (job.getFileKey() == null || job.getFileKey().isBlank()) {
			return true;
		}
		OffsetDateTime expiresAt = job.getFileExpiresAt();
		return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
	}

	private LocalDate parseDate(String value, String fieldName) {
		return RequestDateParser.parseIsoDate(value, fieldName);
	}

	private ExportJobCreateResponse toCreateResponse(ExportJob job) {
		OffsetDateTime requestedAt = job.getCreatedAt();
		if (requestedAt == null) {
			requestedAt = OffsetDateTime.now(ZONE_KST);
		}
		return new ExportJobCreateResponse(job.getId(), job.getStatus(), requestedAt);
	}

	private void enqueueAfterCommit(UUID jobId) {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					exportJobProducer.enqueue(jobId);
				}
			});
			return;
		}
		exportJobProducer.enqueue(jobId);
	}

	private String redisKey(UUID jobId) {
		return REDIS_JOB_PREFIX + jobId;
	}

	private OffsetDateTime parseOffsetDateTime(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(value.toString());
		} catch (Exception ignored) {
			return null;
		}
	}
}
