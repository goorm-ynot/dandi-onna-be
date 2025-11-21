package com.mvp.v1.dandionna.store.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.store.dto.StoreNoShowPostsResponse;
import com.mvp.v1.dandionna.store.service.StoreConsumerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stores")
@Validated
@RequiredArgsConstructor
public class StoreConsumerController {

	private final StoreConsumerService storeConsumerService;

	@Operation(summary = "특정 매장의 활성 노쇼 글 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/{storeId}/no-show-posts")
	public ResponseEntity<ApiResponse<StoreNoShowPostsResponse>> listNoShowPosts(
		@PathVariable UUID storeId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		StoreNoShowPostsResponse response = storeConsumerService.getNoShowPosts(storeId, page, size);
		return ApiResponse.ok(response);
	}
}
