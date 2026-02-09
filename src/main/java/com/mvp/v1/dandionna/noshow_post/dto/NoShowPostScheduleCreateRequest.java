package com.mvp.v1.dandionna.noshow_post.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoShowPostScheduleCreateRequest(
	UUID presetId,

	@NotNull
	@Size(min = 1, max = 30)
	List<@Valid Item> items
) {
	public record Item(
		@NotNull UUID menuId,
		@Min(1) int quantity
	) {
	}
}

