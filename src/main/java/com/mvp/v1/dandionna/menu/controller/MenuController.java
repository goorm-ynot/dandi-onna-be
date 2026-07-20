package com.mvp.v1.dandionna.menu.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.menu.dto.MenuCreateRequest;
import com.mvp.v1.dandionna.menu.dto.MenuDetailResponse;
import com.mvp.v1.dandionna.menu.dto.MenuStatusChangeRequest;
import com.mvp.v1.dandionna.menu.dto.MenuStatusResponse;
import com.mvp.v1.dandionna.menu.dto.MenuSummaryResponse;
import com.mvp.v1.dandionna.menu.dto.MenuUpdateRequest;
import com.mvp.v1.dandionna.menu.service.MenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/owner/menus")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class MenuController {

	private final MenuService menuService;

	@Operation(summary = "메뉴 생성")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping
	public ResponseEntity<ApiResponse<MenuDetailResponse>> create(@Valid @RequestBody MenuCreateRequest request) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.created(menuService.create(ownerId, request));
	}

	@Operation(summary = "메뉴 상세 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/{menuId}")
	public ResponseEntity<ApiResponse<MenuDetailResponse>> get(@PathVariable UUID menuId) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(menuService.get(ownerId, menuId));
	}

	@Operation(summary = "메뉴 목록 조회")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<MenuSummaryResponse>>> list(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) String type,
		@RequestParam(required = false) String effectiveStatus
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(menuService.list(ownerId, page, size, keyword, type, effectiveStatus));
	}

	@Operation(summary = "메뉴 수정")
	@SecurityRequirement(name = "bearerAuth")
	@PatchMapping("/{menuId}")
	public ResponseEntity<ApiResponse<MenuDetailResponse>> update(
		@PathVariable UUID menuId,
		@Valid @RequestBody MenuUpdateRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(menuService.update(ownerId, menuId, request));
	}

	@Operation(summary = "메뉴 판매 상태 변경")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/{menuId}/status")
	public ResponseEntity<ApiResponse<MenuStatusResponse>> changeStatus(
		@PathVariable UUID menuId,
		@Valid @RequestBody MenuStatusChangeRequest request
	) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		return ApiResponse.ok(menuService.changeStatus(ownerId, menuId, request.onSale()));
	}

	@Operation(summary = "메뉴 삭제")
	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping("/{menuId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID menuId) {
		UUID ownerId = SecurityUtils.getCurrentUserId();
		menuService.delete(ownerId, menuId);
		return ApiResponse.ok(null);
	}
}
