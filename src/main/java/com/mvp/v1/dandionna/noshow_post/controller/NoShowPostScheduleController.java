package com.mvp.v1.dandionna.noshow_post.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleCreateRequest;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleCreateResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleDetailResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostScheduleListResponse;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowScheduleStatus;
import com.mvp.v1.dandionna.noshow_post.service.NoShowPostScheduleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/owner/no-show-post-schedules")
@Validated
@RequiredArgsConstructor
public class NoShowPostScheduleController {

	private final NoShowPostScheduleService noShowPostScheduleService;

	@Operation(summary = "노쇼 예약 등록 생성")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping
	public ResponseEntity<ApiResponse<NoShowPostScheduleCreateResponse>> create(
		@Valid @RequestBody NoShowPostScheduleCreateRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		NoShowPostScheduleCreateResponse response = noShowPostScheduleService.createSchedule(ownerId, request);
		return ApiResponse.created(response);
	}

	@Operation(summary = "노쇼 예약 등록 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public ResponseEntity<ApiResponse<NoShowPostScheduleListResponse>> list(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) NoShowScheduleStatus status
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		NoShowPostScheduleListResponse response = noShowPostScheduleService.listSchedules(ownerId, page, size, status);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "노쇼 예약 등록 상세 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<NoShowPostScheduleDetailResponse>> detail(
		@PathVariable UUID scheduleId
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		NoShowPostScheduleDetailResponse response = noShowPostScheduleService.getScheduleDetail(ownerId, scheduleId);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "노쇼 예약 등록 즉시 게시")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/{scheduleId}/publish-now")
	public ResponseEntity<ApiResponse<NoShowPostScheduleDetailResponse>> publishNow(
		@PathVariable UUID scheduleId
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		NoShowPostScheduleDetailResponse response = noShowPostScheduleService.publishNow(ownerId, scheduleId);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "노쇼 예약 등록 취소")
	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<NoShowPostScheduleDetailResponse>> cancel(
		@PathVariable UUID scheduleId
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		NoShowPostScheduleDetailResponse response = noShowPostScheduleService.cancelSchedule(ownerId, scheduleId);
		return ApiResponse.ok(response);
	}
}

