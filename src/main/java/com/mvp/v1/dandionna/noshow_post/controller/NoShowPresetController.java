package com.mvp.v1.dandionna.noshow_post.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPresetResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPresetUpsertRequest;
import com.mvp.v1.dandionna.noshow_post.service.NoShowPresetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/owner/no-show-presets")
@Validated
@RequiredArgsConstructor
public class NoShowPresetController {

	private final NoShowPresetService noShowPresetService;

	@Operation(summary = "기본 노쇼 프리셋 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/default")
	public ResponseEntity<ApiResponse<NoShowPresetResponse>> getDefaultPreset() {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(noShowPresetService.getDefaultPreset(ownerId));
	}

	@Operation(summary = "기본 노쇼 프리셋 저장")
	@SecurityRequirement(name = "bearerAuth")
	@PutMapping("/default")
	public ResponseEntity<ApiResponse<NoShowPresetResponse>> upsertDefaultPreset(
		@Valid @RequestBody NoShowPresetUpsertRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(noShowPresetService.upsertDefaultPreset(ownerId, request));
	}
}
