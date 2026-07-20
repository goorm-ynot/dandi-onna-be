package com.mvp.v1.dandionna.noshow_post.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoShowPresetResponse(
	UUID presetId,
	String name,
	int discountPercent,
	int visitAvailableMinutes,
	int saleDelayMinutes,
	OffsetDateTime updatedAt
) {
}
