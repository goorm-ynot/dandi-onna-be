package com.mvp.v1.dandionna.noshow_post.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoShowBatchCreateRequest(
	@NotNull
	@Size(min = 1, max = 30)
	List<Item> items,

	@Min(30)
	@Max(90)
	int discountPercent,

	@NotNull
	OffsetDateTime expireAt
) {
	public record Item(
		@NotNull UUID menuId,
		@Min(1) int quantity
	) {}
}
