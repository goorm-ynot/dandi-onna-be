package com.mvp.v1.dandionna.store.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.store.dto.StoreCreateRequest;
import com.mvp.v1.dandionna.store.dto.StoreResponse;
import com.mvp.v1.dandionna.store.dto.StoreUpdateRequest;
import com.mvp.v1.dandionna.store.service.StorePermissionService;
import com.mvp.v1.dandionna.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/api/stores")
@Validated
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final StorePermissionService storePermissionService;

    @Operation(summary = "가게 정보 등록")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> create(@Valid @RequestBody StoreCreateRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        StoreResponse response = storeService.createStore(ownerId, request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "가게 정보 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponse>> update(
        @PathVariable UUID storeId,
        @Valid @RequestBody StoreUpdateRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        storePermissionService.verifyOwner(ownerId, storeId);
        StoreResponse response = storeService.updateStore(ownerId, storeId, request);
        return ApiResponse.ok(response);
    }

	//관리자용
    @Operation(summary = "가게 정보 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID storeId) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        storePermissionService.verifyOwner(ownerId, storeId);
        storeService.deleteStore(ownerId, storeId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "가게 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponse>> get(@PathVariable UUID storeId) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        StoreResponse response = storeService.getStoreForOwner(ownerId, storeId);
        return ApiResponse.ok(response);
    }
}
