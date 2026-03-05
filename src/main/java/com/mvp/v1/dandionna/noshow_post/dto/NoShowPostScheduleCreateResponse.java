package com.mvp.v1.dandionna.noshow_post.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowScheduleStatus;

public record NoShowPostScheduleCreateResponse(
	UUID scheduleId,
	NoShowScheduleStatus status,
	int discountPercent,
	int visitAvailableMinutes,
	int saleDelayMinutes,
	OffsetDateTime startAt,
	OffsetDateTime expireAt,
	OffsetDateTime requestedAt
) {
}
