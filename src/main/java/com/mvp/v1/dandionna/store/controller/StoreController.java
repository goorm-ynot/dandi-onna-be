package com.mvp.v1.dandionna.store.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.store.dto.StoreCreateRequest;
import com.mvp.v1.dandionna.store.dto.StoreResponse;
import com.mvp.v1.dandionna.store.dto.StoreUpdateRequest;
import com.mvp.v1.dandionna.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stores")
@Validated
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

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
    @PatchMapping
    public ResponseEntity<ApiResponse<StoreResponse>> update(
        @Valid @RequestBody StoreUpdateRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        StoreResponse response = storeService.updateStore(ownerId, request);
        return ApiResponse.ok(response);
    }

	//관리자용
    @Operation(summary = "가게 정보 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete() {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        storeService.deleteStore(ownerId);
        return ApiResponse.ok(null);		
    }

    @Operation(summary = "가게 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<StoreResponse>> get() {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        StoreResponse response = storeService.getStoreForOwner(ownerId);
        return ApiResponse.ok(response);
    }
}
