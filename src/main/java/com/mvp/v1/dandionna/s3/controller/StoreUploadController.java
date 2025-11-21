package com.mvp.v1.dandionna.s3.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import org.springframework.util.StringUtils;

import com.mvp.v1.dandionna.s3.dto.PresignedUrlRequest;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.dto.S3Metadata;
import com.mvp.v1.dandionna.s3.dto.UploadConfirmRequest;
import com.mvp.v1.dandionna.s3.dto.UploadTarget;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stores/uploads")
@Validated
@RequiredArgsConstructor
public class StoreUploadController {

	private final UploadService uploadService;
	private final StoreService storeService;

	@Operation(summary = "매장 이미지 Presign URL 발급")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/presign")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> presign(
		@Valid @RequestBody PresignedUrlRequest request
	) {
		UUID storeId = resolveCurrentStoreId();
		PresignedUrlResponse response = uploadService.presign(UploadTarget.STORE_IMAGE, storeId.toString(), request);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "매장 이미지 업로드 확인")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<S3Metadata>> confirm(
		@Valid @RequestBody UploadConfirmRequest request
	) {
		UUID storeId = resolveCurrentStoreId();
		// 1. UploadService는 검증만
		S3Metadata metadata = uploadService.confirm(UploadTarget.STORE_IMAGE, storeId.toString(), request);
		// 2. StoreService는 DB 업데이트만
		return ApiResponse.ok(metadata);
	}

	@Operation(summary = "매장 이미지 열람 URL 발급")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/view")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> view() {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		String key = storeService.getStoreForOwner(ownerId).imageKey();
		if (!StringUtils.hasText(key)) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "등록된 이미지가 없습니다.");
		}
		PresignedUrlResponse response = uploadService.presignDownload(key);
		return ApiResponse.ok(response);
	}

	private UUID resolveCurrentStoreId() {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return storeService.getOwnedStoreId(ownerId);
	}
}
