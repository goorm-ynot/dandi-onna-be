package com.mvp.v1.dandionna.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.admin.dto.AdminStoreResponse;
import com.mvp.v1.dandionna.admin.dto.AdminUserResponse;
import com.mvp.v1.dandionna.admin.service.AdminService;
import com.mvp.v1.dandionna.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminService adminService;

	@Operation(summary = "전체 사용자 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/users")
	public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> listUsers(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.ok(adminService.listUsers(page, size));
	}

	@Operation(summary = "전체 매장 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/stores")
	public ResponseEntity<ApiResponse<Page<AdminStoreResponse>>> listStores(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.ok(adminService.listStores(page, size));
	}
}
