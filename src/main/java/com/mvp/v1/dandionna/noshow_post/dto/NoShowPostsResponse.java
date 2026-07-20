package com.mvp.v1.dandionna.noshow_post.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record NoShowPostsResponse(
	List<PostItem> posts,
	PageInfo pagination
) {

	public record PostItem(
		Long postId,
		UUID menuId,
		String name,
		OffsetDateTime visitTime,
		int quantity,
		int discountPercent
	) {}

	public record PageInfo(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
	) {}
}
