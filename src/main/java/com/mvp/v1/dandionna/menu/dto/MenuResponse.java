package com.mvp.v1.dandionna.menu.dto;

import java.util.UUID;

import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.store.entity.ImageStatus;

public record MenuResponse(
	UUID id,
	String name,
	String description,
	int priceKrw,
	String imageKey,
	String imageMime,
	String imageEtag,
	ImageStatus imageStatus
) {
	public static MenuResponse from(Menu menu) {
		return new MenuResponse(
			menu.getId(),
			menu.getName(),
			menu.getDescription(),
			menu.getPriceKrw(),
			menu.getImageKey(),
			menu.getImageMime(),
			menu.getImageEtag(),
			menu.getImageStatus()
		);
	}
}
