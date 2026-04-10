package com.mvp.v1.dandionna.menu.dto;

import java.util.List;
import java.util.UUID;

import com.mvp.v1.dandionna.menu.entity.MenuStatus;
import com.mvp.v1.dandionna.menu.entity.MenuType;
import com.mvp.v1.dandionna.store.entity.ImageStatus;

public record MenuDetailResponse(
	UUID id,
	String name,
	String description,
	int priceKrw,
	ImageStatus imageStatus,
	String imageUrl,
	Long imageUrlExpiresInSeconds,
	MenuType type,
	MenuStatus status,
	MenuStatus effectiveStatus,
	List<Component> components
) {
	public record Component(
		UUID menuId,
		String name,
		MenuStatus status,
		MenuStatus effectiveStatus,
		int quantity
	) {
	}
}
