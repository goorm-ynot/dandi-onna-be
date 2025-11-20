package com.mvp.v1.dandionna.s3.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlRequest;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.dto.UploadConfirmRequest;
import com.mvp.v1.dandionna.s3.dto.UploadTarget;
import com.mvp.v1.dandionna.s3.service.UploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/api/menus/{menuId}/uploads")
@Validated
@RequiredArgsConstructor
public class MenuUploadController {

	private final UploadService uploadService;

	@Operation(summary = "메뉴 이미지 Presign URL 발급")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/presign")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> presign(
		@PathVariable UUID menuId,
		@Valid @RequestBody PresignedUrlRequest request
	) {
		PresignedUrlResponse response = uploadService.presign(UploadTarget.MENU_IMAGE, menuId.toString(), request);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "메뉴 이미지 업로드 확인")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<Void>> confirm(
		@PathVariable UUID menuId,
		@Valid @RequestBody UploadConfirmRequest request
	) {
		uploadService.confirm(UploadTarget.MENU_IMAGE, menuId.toString(), request);
		return ApiResponse.ok(null);
	}
}
