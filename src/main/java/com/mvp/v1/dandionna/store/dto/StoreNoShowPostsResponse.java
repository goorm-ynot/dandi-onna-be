package com.mvp.v1.dandionna.store.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record StoreNoShowPostsResponse(
	StoreInfo store,
	List<PostSummary> posts,
	PageInfo page
) {

	@Schema(description = "매장 기본 정보")
	public record StoreInfo(
		UUID storeId,
		String name,
		String description,
		String addressRoad,
		String imageUrl
	) {}

	@Schema(description = "노쇼 글 요약 정보")
	public record PostSummary(
		Long postId,
		OffsetDateTime expireAt,
		String menuName,
		String menuDescription,
		Integer originalPrice,
		Integer discountPercent,
		Integer discountedPrice,
		Integer qtyRemaining,
		String menuImageUrl
	) {}

	public record PageInfo(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
	) {}
}
