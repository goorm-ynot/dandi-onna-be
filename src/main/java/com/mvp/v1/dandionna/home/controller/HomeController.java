package com.mvp.v1.dandionna.home.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.home.dto.HomeResponse;
import com.mvp.v1.dandionna.home.dto.HomeStoreRequest;
import com.mvp.v1.dandionna.home.dto.HomeStoreResponse;
import com.mvp.v1.dandionna.home.service.HomeService;
import com.mvp.v1.dandionna.home.service.HomeStoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/home")
@Validated
@RequiredArgsConstructor
public class HomeController {

	private final HomeService homeService;
	private final HomeStoreService homeStoreService;

	@Operation(summary = "소비자 홈 요약 (내 주문, 추천 매장 등)")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public ResponseEntity<ApiResponse<HomeResponse>> getHome() {
		UUID consumerId = SecurityUtils.getCurrentUserId();
		HomeResponse response = homeService.getHome(consumerId);
		return ApiResponse.ok(response);
	}

	@Operation(summary = "노쇼 주문 가능한 가게 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/stores")
	public ResponseEntity<ApiResponse<HomeStoreResponse>> getStores(@Valid HomeStoreRequest request) {
		HomeStoreResponse response = homeStoreService.listHomeStores(request);
		return ApiResponse.ok(response);
	}
}
