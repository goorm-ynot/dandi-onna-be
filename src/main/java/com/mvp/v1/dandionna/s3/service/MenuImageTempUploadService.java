package com.mvp.v1.dandionna.s3.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempConfirmRequest;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempConfirmResponse;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempPresignResponse;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlRequest;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.dto.S3Metadata;
import com.mvp.v1.dandionna.s3.dto.UploadTarget;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuImageTempUploadService {

	private static final Logger log = LoggerFactory.getLogger(MenuImageTempUploadService.class);

	private static final String REDIS_KEY_PREFIX = "menu:image:temp:";
	private static final String REDIS_LOCK_PREFIX = "menu:image:temp:lock:";
	private static final String FIELD_OWNER_USER_ID = "ownerUserId";
	private static final String FIELD_TEMP_KEY = "tempKey";
	private static final String FIELD_REQUESTED_CONTENT_TYPE = "requestedContentType";
	private static final String FIELD_CONFIRMED_CONTENT_TYPE = "confirmedContentType";
	private static final String FIELD_ETAG = "etag";
	private static final String FIELD_STATUS = "status";
	private static final String FIELD_CREATED_AT = "createdAtEpochSecond";
	private static final String FIELD_CONFIRMED_AT = "confirmedAtEpochSecond";
	private static final String FIELD_RESERVED_AT = "reservedAtEpochSecond";
	private static final String FIELD_USED_AT = "usedAtEpochSecond";

	private final StringRedisTemplate redisTemplate;
	private final UploadService uploadService;

	@Value("${app.s3.menu-image-upload-token-ttl-seconds:1800}")
	private long tokenTtlSeconds;

	@Value("${app.s3.menu-image-upload-lock-ttl-seconds:60}")
	private long lockTtlSeconds;

	public MenuImageTempPresignResponse presign(UUID ownerId, PresignedUrlRequest request) {
		String uploadToken = UUID.randomUUID().toString();
		PresignedUrlResponse presigned = uploadService.presign(UploadTarget.TEMP_MENU_IMAGE, ownerId.toString(), request);

		Map<String, String> payload = new LinkedHashMap<>();
		payload.put(FIELD_OWNER_USER_ID, ownerId.toString());
		payload.put(FIELD_TEMP_KEY, presigned.key());
		payload.put(FIELD_REQUESTED_CONTENT_TYPE, request.contentType());
		payload.put(FIELD_STATUS, TempUploadStatus.PRESIGNED.name());
		payload.put(FIELD_CREATED_AT, String.valueOf(Instant.now().getEpochSecond()));

		String redisKey = redisKey(uploadToken);
		redisTemplate.opsForHash().putAll(redisKey, payload);
		redisTemplate.expire(redisKey, Duration.ofSeconds(tokenTtlSeconds));

		return new MenuImageTempPresignResponse(
			uploadToken,
			presigned.url(),
			presigned.expiresInSeconds()
		);
	}

	public MenuImageTempConfirmResponse confirm(UUID ownerId, MenuImageTempConfirmRequest request) {
		TokenMetadata metadata = loadMetadata(request.uploadToken());
		validateOwner(metadata, ownerId);
		validateNotExpired(metadata);

		if (metadata.status() == TempUploadStatus.IN_USE || metadata.status() == TempUploadStatus.CONSUMED) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_IN_USE, "이미 사용 중인 이미지 업로드 토큰입니다.");
		}
		if (metadata.status() == TempUploadStatus.CONFIRMED) {
			return new MenuImageTempConfirmResponse(true);
		}

		S3Metadata objectMetadata = uploadService.head(metadata.tempKey());
		if (!objectMetadata.getEtag().equals(request.etag())) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_INVALID, "업로드된 이미지의 ETag 가 일치하지 않습니다.");
		}

		Map<String, String> updates = new LinkedHashMap<>();
		updates.put(FIELD_STATUS, TempUploadStatus.CONFIRMED.name());
		updates.put(FIELD_ETAG, objectMetadata.getEtag());
		updates.put(FIELD_CONFIRMED_CONTENT_TYPE, objectMetadata.getContentType());
		updates.put(FIELD_CONFIRMED_AT, String.valueOf(Instant.now().getEpochSecond()));
		redisTemplate.opsForHash().putAll(redisKey(request.uploadToken()), updates);

		return new MenuImageTempConfirmResponse(true);
	}

	public S3Metadata consumeForMenu(UUID ownerId, String uploadToken, UUID menuId) {
		if (!StringUtils.hasText(uploadToken)) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_INVALID, "이미지 업로드 토큰이 필요합니다.");
		}

		String redisKey = redisKey(uploadToken);
		String lockKey = lockKey(uploadToken);
		Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtlSeconds));
		if (!Boolean.TRUE.equals(locked)) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_IN_USE, "동시에 같은 이미지 업로드 토큰을 사용할 수 없습니다.");
		}

		boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
		TokenMetadata metadata = loadMetadata(uploadToken);
		validateOwner(metadata, ownerId);
		validateNotExpired(metadata);
		validateConsumable(metadata);

		markInUse(redisKey);
		try {
			String finalKey = UploadTarget.MENU_IMAGE.generateKey(menuId.toString(), metadata.tempKey());
			S3Metadata copied = uploadService.copy(metadata.tempKey(), finalKey);
			if (transactionActive) {
				registerTransactionHooks(redisKey, lockKey, metadata.tempKey());
			} else {
				markConsumed(redisKey);
				redisTemplate.delete(redisKey);
				uploadService.deleteQuietly(metadata.tempKey());
			}
			return copied;
		} catch (RuntimeException ex) {
			restoreConfirmed(redisKey);
			throw ex;
		} finally {
			if (!transactionActive) {
				redisTemplate.delete(lockKey);
			}
		}
	}

	private void registerTransactionHooks(String redisKey, String lockKey, String tempKey) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					markConsumed(redisKey);
					redisTemplate.delete(redisKey);
				} catch (Exception e) {
					log.warn("메뉴 이미지 업로드 토큰 정리 실패: {}", redisKey, e);
				}
				uploadService.deleteQuietly(tempKey);
			}

			@Override
			public void afterCompletion(int status) {
				if (status != TransactionSynchronization.STATUS_COMMITTED) {
					restoreConfirmed(redisKey);
				}
				redisTemplate.delete(lockKey);
			}
		});
	}

	private void validateConsumable(TokenMetadata metadata) {
		if (metadata.status() == TempUploadStatus.IN_USE || metadata.status() == TempUploadStatus.CONSUMED) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_IN_USE, "이미 사용 중이거나 재사용할 수 없는 이미지 업로드 토큰입니다.");
		}
		if (metadata.status() != TempUploadStatus.CONFIRMED) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_NOT_CONFIRMED, "이미지 업로드 확인이 완료되지 않았습니다.");
		}
	}

	private void validateOwner(TokenMetadata metadata, UUID ownerId) {
		if (!ownerId.toString().equals(metadata.ownerUserId())) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_INVALID, "현재 사용자에게 발급된 이미지 업로드 토큰이 아닙니다.");
		}
	}

	private void validateNotExpired(TokenMetadata metadata) {
		if (metadata.createdAtEpochSecond() <= 0) {
			return;
		}
		long expiresAt = metadata.createdAtEpochSecond() + tokenTtlSeconds;
		if (Instant.now().getEpochSecond() > expiresAt) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_EXPIRED, "메뉴 이미지 업로드 토큰이 만료되었습니다.");
		}
	}

	private TokenMetadata loadMetadata(String uploadToken) {
		Map<Object, Object> values = redisTemplate.opsForHash().entries(redisKey(uploadToken));
		if (values == null || values.isEmpty()) {
			throw new BusinessException(ErrorCode.MENU_IMAGE_UPLOAD_INVALID, "유효하지 않은 메뉴 이미지 업로드 토큰입니다.");
		}
		return TokenMetadata.from(values);
	}

	private void markInUse(String redisKey) {
		Map<String, String> updates = new LinkedHashMap<>();
		updates.put(FIELD_STATUS, TempUploadStatus.IN_USE.name());
		updates.put(FIELD_RESERVED_AT, String.valueOf(Instant.now().getEpochSecond()));
		redisTemplate.opsForHash().putAll(redisKey, updates);
	}

	private void restoreConfirmed(String redisKey) {
		if (!Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
			return;
		}
		Map<String, String> updates = new LinkedHashMap<>();
		updates.put(FIELD_STATUS, TempUploadStatus.CONFIRMED.name());
		updates.put(FIELD_RESERVED_AT, "");
		redisTemplate.opsForHash().putAll(redisKey, updates);
	}

	private void markConsumed(String redisKey) {
		if (!Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
			return;
		}
		Map<String, String> updates = new LinkedHashMap<>();
		updates.put(FIELD_STATUS, TempUploadStatus.CONSUMED.name());
		updates.put(FIELD_USED_AT, String.valueOf(Instant.now().getEpochSecond()));
		redisTemplate.opsForHash().putAll(redisKey, updates);
	}

	private String redisKey(String uploadToken) {
		return REDIS_KEY_PREFIX + uploadToken;
	}

	private String lockKey(String uploadToken) {
		return REDIS_LOCK_PREFIX + uploadToken;
	}

	private enum TempUploadStatus {
		PRESIGNED,
		CONFIRMED,
		IN_USE,
		CONSUMED
	}

	private record TokenMetadata(
		String ownerUserId,
		String tempKey,
		String contentType,
		String etag,
		TempUploadStatus status,
		long createdAtEpochSecond
	) {
		private static TokenMetadata from(Map<Object, Object> values) {
			String contentType = value(values, FIELD_CONFIRMED_CONTENT_TYPE);
			if (!StringUtils.hasText(contentType)) {
				contentType = value(values, FIELD_REQUESTED_CONTENT_TYPE);
			}
			return new TokenMetadata(
				value(values, FIELD_OWNER_USER_ID),
				value(values, FIELD_TEMP_KEY),
				contentType,
				value(values, FIELD_ETAG),
				parseStatus(value(values, FIELD_STATUS)),
				parseLong(value(values, FIELD_CREATED_AT))
			);
		}

		private static String value(Map<Object, Object> values, String field) {
			Object raw = values.get(field);
			return raw != null ? raw.toString() : "";
		}

		private static long parseLong(String value) {
			if (!StringUtils.hasText(value)) {
				return 0L;
			}
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException ex) {
				return 0L;
			}
		}

		private static TempUploadStatus parseStatus(String value) {
			if (!StringUtils.hasText(value)) {
				return TempUploadStatus.PRESIGNED;
			}
			try {
				return TempUploadStatus.valueOf(value.trim().toUpperCase());
			} catch (IllegalArgumentException ex) {
				return TempUploadStatus.PRESIGNED;
			}
		}
	}
}
