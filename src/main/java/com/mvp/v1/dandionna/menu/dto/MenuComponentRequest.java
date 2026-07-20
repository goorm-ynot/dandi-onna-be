package com.mvp.v1.dandionna.menu.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MenuComponentRequest(
	@NotNull UUID menuId,
	@Min(1) int quantity
) {
}
