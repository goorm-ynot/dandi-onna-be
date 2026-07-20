package com.mvp.v1.dandionna.menu.dto;

import jakarta.validation.constraints.NotNull;

public record MenuStatusChangeRequest(
	@NotNull Boolean onSale
) {
}
