package com.mvp.v1.dandionna.noshow_post.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record NoShowPresetUpsertRequest(
	@NotBlank
	String name,

	@Min(30)
	@Max(90)
	int discountPercent,

	@Min(1)
	@Max(300)
	int visitAvailableMinutes,

	@Min(0)
	@Max(300)
	int saleDelayMinutes
) {
}
