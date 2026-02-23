package com.mvp.v1.dandionna.notification.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.notification.dto.NotificationHistoryResponse;
import com.mvp.v1.dandionna.notification.service.NotificationHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

	private final NotificationHistoryService notificationHistoryService;

	@Operation(summary = "알림 이력 조회", description = "로그인 사용자의 알림 이력을 페이지네이션하여 조회합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<NotificationHistoryResponse>>> getHistory(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		UUID userId = SecurityUtils.getCurrentUserId();
		Page<NotificationHistoryResponse> history = notificationHistoryService.getHistory(userId, page, size);
		return ApiResponse.ok(history);
	}
}
