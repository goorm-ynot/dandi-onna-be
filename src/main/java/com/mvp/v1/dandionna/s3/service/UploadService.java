package com.mvp.v1.dandionna.s3.service;

import java.time.Duration;
import java.util.UUID;

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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class UploadService {

	private final S3Presigner presigner;
	private final S3Client s3Client;

	@Value("${app.s3.bucket}")
	private String bucket;

	@Value("${app.s3.presign-ttl-seconds:300}")
	private long presignTtlSeconds;

	public PresignedUrlResponse presign(UploadTarget target, String referenceId, PresignedUrlRequest request) {
		validateReference(referenceId);
		validateFileName(request.fileName());

		String key = buildObjectKey(target, referenceId, request.fileName());

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

		return new PresignedUrlResponse(
			presigned.url().toString(),
			key,
			presignTtlSeconds
		);
	}

	public PresignedUrlResponse presignDownload(String key) {
		if (!StringUtils.hasText(key)) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "등록된 이미지가 없습니다.");
		}
		GetObjectRequest objectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(presignTtlSeconds))
			.getObjectRequest(objectRequest)
			.build();

		PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

		return new PresignedUrlResponse(
			presigned.url().toString(),
			key,
			presignTtlSeconds
		);
	}

	public S3Metadata confirm(UploadTarget target, String referenceId, UploadConfirmRequest request) {
		// 1. 경로 검증
		String expectedPrefix = folderFor(target) + "/" + sanitize(referenceId);
		if (!request.key().startsWith(expectedPrefix)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "경로 불일치");
		}
		// 2. ✅ S3에서 직접 메타데이터 조회 (핵심!)
		try {
			HeadObjectResponse metadata = s3Client.headObject(r ->
				r.bucket(bucket).key(request.key())
			);

			// 3. ✅ ETag 검증
			String s3Etag = metadata.eTag().replace("\"", "");
			if (!s3Etag.equals(request.etag())) {
				throw new BusinessException(ErrorCode.BAD_REQUEST, "ETag 불일치");
			}

			// 4. 파일 크기 검증
			if (metadata.contentLength() == 0) {
				throw new BusinessException(ErrorCode.BAD_REQUEST, "빈 파일");
			}
			return new S3Metadata(request.key(), s3Etag, metadata.contentType());
		} catch (NoSuchKeyException e) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "S3에 파일 없음");
		}
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

	private String buildObjectKey(UploadTarget target, String referenceId, String fileName) {
		return target.generateKey(referenceId, fileName);
	}

	private String folderFor(UploadTarget target) {
		return switch (target) {
			case STORE_IMAGE -> "stores";
			case MENU_IMAGE -> "menus";
		};
	}

	private String sanitize(String input) {
		return input.replaceAll("[^a-zA-Z0-9_-]", "");
	}

	private String extractExtension(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot == -1 || lastDot == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(lastDot);
	}
}
