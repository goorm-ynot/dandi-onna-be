package com.mvp.v1.dandionna.noshow_post.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowScheduleStatus;

public record NoShowPostScheduleListResponse(
	List<ScheduleSummary> schedules,
	PageInfo pagination
) {
	public record ScheduleSummary(
		UUID scheduleId,
		NoShowScheduleStatus status,
		int discountPercent,
		int visitAvailableMinutes,
		int saleDelayMinutes,
		int itemCount,
		Integer publishedPostCount,
		OffsetDateTime startAt,
		OffsetDateTime expireAt,
		OffsetDateTime requestedAt,
		String errorMessage
	) {
	}

	public record PageInfo(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
	) {
	}
}
