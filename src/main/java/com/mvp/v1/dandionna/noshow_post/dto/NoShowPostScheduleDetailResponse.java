package com.mvp.v1.dandionna.noshow_post.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowScheduleStatus;

public record NoShowPostScheduleDetailResponse(
	UUID scheduleId,
	NoShowScheduleStatus status,
	int discountPercent,
	int visitAvailableMinutes,
	int saleDelayMinutes,
	Integer publishedPostCount,
	OffsetDateTime startAt,
	OffsetDateTime expireAt,
	OffsetDateTime requestedAt,
	OffsetDateTime publishedAt,
	OffsetDateTime cancelledAt,
	String errorMessage,
	List<Item> items
) {
	public record Item(
		UUID menuId,
		int quantity
	) {
	}
}
