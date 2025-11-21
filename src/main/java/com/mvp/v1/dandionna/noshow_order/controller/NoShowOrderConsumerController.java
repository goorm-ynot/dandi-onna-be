package com.mvp.v1.dandionna.noshow_order.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderCreateRequest;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderCreateResponse;
import com.mvp.v1.dandionna.noshow_order.service.NoShowOrderConsumerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@Validated
@RequiredArgsConstructor
public class NoShowOrderConsumerController {

	private final NoShowOrderConsumerService consumerService;

	@Operation(summary = "노쇼 주문 생성")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping
	public ResponseEntity<ApiResponse<NoShowOrderCreateResponse>> createOrder(
		@Valid @RequestBody NoShowOrderCreateRequest request
	) {
		UUID consumerId = SecurityUtils.getCurrentUserId();
		NoShowOrderCreateResponse response = consumerService.createOrder(consumerId, request);
		return ApiResponse.ok(response);
	}
}
