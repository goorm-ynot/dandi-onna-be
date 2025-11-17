package com.mvp.v1.dandionna.noshow_order.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderCompleteRequest;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderDetailResponse;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderListResponse;
import com.mvp.v1.dandionna.noshow_order.service.NoShowOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/owner/orders")
@Validated
@RequiredArgsConstructor
public class NoShowOrderController {

	private final NoShowOrderService noShowOrderService;

	@Operation(summary = "노쇼 주문 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public ResponseEntity<ApiResponse<NoShowOrderListResponse>> list(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		NoShowOrderListResponse response = noShowOrderService.listOrders(ownerId, page, size, date);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "노쇼 주문 상세 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/{orderId}")
	public ResponseEntity<ApiResponse<NoShowOrderDetailResponse>> detail(
		@PathVariable Long orderId
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		NoShowOrderDetailResponse response = noShowOrderService.getOrderDetail(ownerId, orderId);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "노쇼 주문 방문 완료 처리")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/{orderId}/complete")
	public ResponseEntity<ApiResponse<Void>> complete(
		@PathVariable Long orderId,
		@RequestBody(required = false) NoShowOrderCompleteRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		noShowOrderService.completeOrder(ownerId, orderId, request);
		return ApiResponse.ok(null);
	}
}
