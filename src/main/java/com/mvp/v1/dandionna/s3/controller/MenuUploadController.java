package com.mvp.v1.dandionna.s3.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.service.MenuPermissionService;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.service.UploadService;

import org.springframework.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/menus/{menuId}/uploads")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class MenuUploadController {

	private final UploadService uploadService;
	private final MenuPermissionService menuPermissionService;

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
