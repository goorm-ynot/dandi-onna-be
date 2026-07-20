package com.mvp.v1.dandionna.s3.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlRequest;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.dto.S3Metadata;
import com.mvp.v1.dandionna.s3.dto.UploadConfirmRequest;
import com.mvp.v1.dandionna.s3.dto.UploadTarget;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class UploadService {

	private static final Logger log = LoggerFactory.getLogger(UploadService.class);

	private final S3Presigner presigner;
	private final S3Client s3Client;

	@Value("${app.s3.bucket}")
	private String bucket;

	@Value("${app.s3.presign-ttl-seconds:300}")
	private long presignTtlSeconds;

	public PresignedUrlResponse presign(UploadTarget target, String referenceId, PresignedUrlRequest request) {
		validateReference(referenceId);
		validateFileName(request.fileName());

		String key = target.generateKey(referenceId, request.fileName());
		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(request.contentType())
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(presignTtlSeconds))
			.putObjectRequest(objectRequest)
			.build();

		PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
		return new PresignedUrlResponse(presigned.url().toString(), key, presignTtlSeconds);
	}

	public PresignedUrlResponse presignDownload(String key) {
		return presignDownloadWithTtl(key, presignTtlSeconds);
	}

	public PresignedUrlResponse presignDownloadWithTtl(String key, long ttlSeconds) {
		if (!StringUtils.hasText(key)) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "등록된 이미지가 없습니다.");
		}

		GetObjectRequest objectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build();
		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(ttlSeconds))
			.getObjectRequest(objectRequest)
			.build();

		PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
		return new PresignedUrlResponse(presigned.url().toString(), key, ttlSeconds);
	}

	public S3Metadata confirm(UploadTarget target, String referenceId, UploadConfirmRequest request) {
		String expectedPrefix = folderFor(target) + "/" + sanitize(referenceId);
		if (!request.key().startsWith(expectedPrefix)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "경로 불일치");
		}

		S3Metadata metadata = head(request.key());
		if (!metadata.getEtag().equals(request.etag())) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "ETag 불일치");
		}
		return metadata;
	}

	public S3Metadata head(String key) {
		if (!StringUtils.hasText(key)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "key 가 필요합니다.");
		}
		try {
			HeadObjectResponse metadata = s3Client.headObject(r -> r.bucket(bucket).key(key));
			if (metadata.contentLength() == 0) {
				throw new BusinessException(ErrorCode.BAD_REQUEST, "빈 파일");
			}
			String etag = metadata.eTag() != null ? metadata.eTag().replace("\"", "") : "";
			return new S3Metadata(key, etag, metadata.contentType());
		} catch (NoSuchKeyException e) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "S3에 파일 없음");
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				throw new BusinessException(ErrorCode.NOT_FOUND, "S3에 파일 없음");
			}
			throw e;
		}
	}

	public S3Metadata copy(String sourceKey, String targetKey) {
		if (!StringUtils.hasText(sourceKey) || !StringUtils.hasText(targetKey)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "sourceKey 와 targetKey 가 필요합니다.");
		}

		CopyObjectRequest request = CopyObjectRequest.builder()
			.sourceBucket(bucket)
			.sourceKey(sourceKey)
			.destinationBucket(bucket)
			.destinationKey(targetKey)
			.copySource(copySource(sourceKey))
			.build();
		s3Client.copyObject(request);
		return head(targetKey);
	}

	public void deleteQuietly(String key) {
		if (!StringUtils.hasText(key)) {
			return;
		}
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
		} catch (Exception e) {
			log.warn("S3 객체 삭제 실패: {}", key, e);
		}
	}

	public void uploadBytes(String key, byte[] bytes, String contentType) {
		if (!StringUtils.hasText(key)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "key 가 필요합니다.");
		}
		if (bytes == null || bytes.length == 0) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "업로드할 데이터가 없습니다.");
		}
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(contentType)
			.build();
		s3Client.putObject(request, RequestBody.fromBytes(bytes));
	}

	private void validateReference(String referenceId) {
		if (!StringUtils.hasText(referenceId)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "referenceId 가 필요합니다.");
		}
	}

	private void validateFileName(String fileName) {
		if (!StringUtils.hasText(fileName)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "fileName 이 필요합니다.");
		}
	}

	private String folderFor(UploadTarget target) {
		return switch (target) {
			case STORE_IMAGE -> "stores";
			case MENU_IMAGE -> "menus";
			case TEMP_MENU_IMAGE -> "temp/menu-images";
		};
	}

	private String sanitize(String input) {
		return input.replaceAll("[^a-zA-Z0-9_-]", "");
	}

	private String copySource(String key) {
		return bucket + "/" + URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
