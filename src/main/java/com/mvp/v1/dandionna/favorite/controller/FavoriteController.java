package com.mvp.v1.dandionna.favorite.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mvp.v1.dandionna.common.dto.ApiResponse;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.favorite.dto.FavoriteRequest;
import com.mvp.v1.dandionna.favorite.dto.FavoriteResponse;
import com.mvp.v1.dandionna.favorite.service.FavoriteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/favorites")
@Validated
@RequiredArgsConstructor
public class FavoriteController {

	private final FavoriteService favoriteService;

	@Operation(summary = "즐겨찾기 추가")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping
	public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(@Valid @RequestBody FavoriteRequest request) {
		UUID consumerId = SecurityUtils.getCurrentUserId();
		FavoriteResponse response = favoriteService.addFavorite(consumerId, request.storeId());
		return ApiResponse.ok(response);
	}

	@Operation(summary = "즐겨찾기 삭제")
	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping
	public ResponseEntity<ApiResponse<FavoriteResponse>> removeFavorite(@Valid @RequestBody FavoriteRequest request) {
		UUID consumerId = SecurityUtils.getCurrentUserId();
		FavoriteResponse response = favoriteService.removeFavorite(consumerId, request.storeId());
		return ApiResponse.ok(response);
	}
}
