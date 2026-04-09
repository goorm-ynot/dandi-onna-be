package com.mvp.v1.dandionna.menu.dto;

import java.util.UUID;

import com.mvp.v1.dandionna.menu.entity.MenuStatus;
import com.mvp.v1.dandionna.menu.entity.MenuType;
import com.mvp.v1.dandionna.store.entity.ImageStatus;

public record MenuSummaryResponse(
	UUID id,
	String name,
	String description,
	int priceKrw,
	String imageKey,
	String imageMime,
	String imageEtag,
	ImageStatus imageStatus,
	MenuType type,
	MenuStatus status,
	MenuStatus effectiveStatus,
	int componentCount
) {
}
