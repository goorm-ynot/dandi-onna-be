package com.mvp.v1.dandionna.s3.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempConfirmRequest;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempConfirmResponse;
import com.mvp.v1.dandionna.s3.dto.MenuImageTempPresignResponse;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlRequest;
import com.mvp.v1.dandionna.s3.service.MenuImageTempUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/owner/menu-images/temp")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class MenuTempUploadController {

	private final MenuImageTempUploadService menuImageTempUploadService;

	@Operation(summary = "메뉴 이미지 임시 업로드 Presign URL 발급")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/presign")
	public ResponseEntity<ApiResponse<MenuImageTempPresignResponse>> presign(
		@Valid @RequestBody PresignedUrlRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(menuImageTempUploadService.presign(ownerId, request));
	}

	@Operation(summary = "메뉴 이미지 임시 업로드 Confirm")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<MenuImageTempConfirmResponse>> confirm(
		@Valid @RequestBody MenuImageTempConfirmRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(menuImageTempUploadService.confirm(ownerId, request));
	}
}
