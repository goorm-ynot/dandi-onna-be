package com.mvp.v1.dandionna.noshow_order.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.export_job.dto.ExportJobCreateRequest;
import com.mvp.v1.dandionna.export_job.dto.ExportJobCreateResponse;
import com.mvp.v1.dandionna.export_job.dto.ExportJobStatusResponse;
import com.mvp.v1.dandionna.export_job.service.ExportJobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/owner/sales/export")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerSalesExportController {

	private final ExportJobService exportJobService;

	@Operation(summary = "사장님 매출 엑셀 생성 요청 (비동기)")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping
	public ResponseEntity<ApiResponse<ExportJobCreateResponse>> create(
		@RequestBody ExportJobCreateRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		ExportJobCreateResponse response = exportJobService.requestOwnerSalesExport(ownerId, request);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "사장님 매출 엑셀 상태 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/{jobId}")
	public ResponseEntity<ApiResponse<ExportJobStatusResponse>> status(
		@PathVariable UUID jobId
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		ExportJobStatusResponse response = exportJobService.getOwnerSalesExportStatus(ownerId, jobId);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "사장님 매출 엑셀 다운로드 URL 재발급")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/{jobId}/refresh")
	public ResponseEntity<ApiResponse<ExportJobStatusResponse>> refresh(
		@PathVariable UUID jobId
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		ExportJobStatusResponse response = exportJobService.refreshOwnerSalesExport(ownerId, jobId);
		return ApiResponse.ok(response);
	}
}
