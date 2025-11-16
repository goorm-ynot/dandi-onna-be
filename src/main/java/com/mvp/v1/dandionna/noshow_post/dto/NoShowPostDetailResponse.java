package com.mvp.v1.dandionna.noshow_post.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoShowPostDetailResponse(
	Long noshowPostsId,
	UUID menuId,
	OffsetDateTime visitTime,
	String name,
	int quantity,
	int price,
	int discountPercent
) {}
