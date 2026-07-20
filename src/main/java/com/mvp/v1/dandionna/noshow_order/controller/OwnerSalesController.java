package com.mvp.v1.dandionna.noshow_order.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.noshow_order.dto.OwnerSalesResponse;
import com.mvp.v1.dandionna.noshow_order.service.OwnerSalesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/owner/sales")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerSalesController {

	private final OwnerSalesService ownerSalesService;

	@Operation(summary = "사장님 매출 조회 (기간/페이지)")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public ResponseEntity<ApiResponse<OwnerSalesResponse>> list(
		@RequestParam String startDate,
		@RequestParam String endDate,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		OwnerSalesResponse response = ownerSalesService.getSales(ownerId, startDate, endDate, page, size);
		return ApiResponse.ok(response);
	}
}
