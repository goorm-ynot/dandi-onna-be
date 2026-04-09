package com.mvp.v1.dandionna.menu.dto;

import java.util.UUID;

import com.mvp.v1.dandionna.menu.entity.MenuStatus;

public record MenuStatusResponse(
	UUID menuId,
	MenuStatus status,
	MenuStatus effectiveStatus
) {
}
