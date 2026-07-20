package com.mvp.v1.dandionna.fcm.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.fcm.dto.PushTokenRegisterRequest;
import com.mvp.v1.dandionna.fcm.dto.PushTokenRemoveRequest;
import com.mvp.v1.dandionna.fcm.service.PushTokenService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Validated
@Tag(name = "Push API", description = "푸시 토큰 등록 API")
@PreAuthorize("isAuthenticated()")
public class PushTokenController {

	private final PushTokenService pushTokenService;

	@Operation(summary = "푸시 토큰 등록", description = "디바이스 별 FCM 토큰을 등록/갱신합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/tokens")
	public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody PushTokenRegisterRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId();
		pushTokenService.register(userId, request);
		return ApiResponse.created("푸시 토큰이 등록되었습니다.");
	}

	@Operation(summary = "푸시 토큰 삭제", description = "로그아웃 시 디바이스 토큰을 삭제합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping("/tokens")
	public ResponseEntity<ApiResponse<Void>> remove(@Valid @RequestBody PushTokenRemoveRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId();
		pushTokenService.unregister(userId, request);
		return ApiResponse.ok(null);
	}
}
