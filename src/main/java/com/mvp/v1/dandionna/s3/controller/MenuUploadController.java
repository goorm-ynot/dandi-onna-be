package com.mvp.v1.dandionna.s3.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.service.MenuPermissionService;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlRequest;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.dto.S3Metadata;
import com.mvp.v1.dandionna.s3.dto.UploadConfirmRequest;
import com.mvp.v1.dandionna.s3.dto.UploadTarget;
import com.mvp.v1.dandionna.s3.service.UploadService;

import org.springframework.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/menus/{menuId}/uploads")
@Validated
@RequiredArgsConstructor
public class MenuUploadController {

	private final UploadService uploadService;
	private final MenuPermissionService menuPermissionService;

	@Operation(summary = "메뉴 이미지 Presign URL 발급")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/presign")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> presign(
		@PathVariable UUID menuId,
		@Valid @RequestBody PresignedUrlRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		menuPermissionService.verifyOwner(ownerId, menuId);
		PresignedUrlResponse response = uploadService.presign(UploadTarget.MENU_IMAGE, menuId.toString(), request);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "메뉴 이미지 업로드 확인")
  @SecurityRequirement(name = "bearerAuth")
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<S3Metadata>> confirm(
		@PathVariable UUID menuId,
		@Valid @RequestBody UploadConfirmRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		menuPermissionService.verifyOwner(ownerId, menuId);
		S3Metadata metadata = uploadService.confirm(UploadTarget.MENU_IMAGE, menuId.toString(), request);
		return ApiResponse.ok(metadata);
	}

	@Operation(summary = "메뉴 이미지 열람 URL 발급")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/view")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> view(
		@PathVariable UUID menuId
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		Menu menu = menuPermissionService.verifyOwner(ownerId, menuId);
		String key = menu.getImageKey();
		if (!StringUtils.hasText(key)) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "등록된 이미지가 없습니다.");
		}
		PresignedUrlResponse response = uploadService.presignDownload(key);
		return ApiResponse.ok(response);
	}
}
