package com.mvp.v1.dandionna.home.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 가능한 노쇼 가게 조회 응답")
public record HomeStoreResponse(
	@Schema(description = "가게 목록")
	List<StoreItem> stores,
	PageInfo pagination
) {

	@Schema(description = "가게 단일 정보")
	public record StoreItem(
		@Schema(description = "매장 ID")
		UUID storeId,
		@Schema(description = "매장 이름")
		String name,
		@Schema(description = "매장 이미지 Presigned URL")
		String imageUrl,
		@Schema(description = "영업 시작 시간", example = "09:00")
		String openTime,
		@Schema(description = "영업 종료 시간", example = "20:30")
		String closeTime,
		@Schema(description = "사용자로부터의 거리 (m 단위)")
		double distanceMeters
	) {}

	public record PageInfo(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
	) {}
}
