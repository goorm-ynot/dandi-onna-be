package com.mvp.v1.dandionna.noshow_order.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record NoShowOrderCreateRequest(
	@NotNull UUID storeId,
	@NotNull OffsetDateTime visitTime,
	@NotBlank String paymentMethod,
	@NotNull @Min(0) Integer totalAmount,
	@NotNull @Min(0) Integer appliedDiscountAmount,
	@NotEmpty List<@Valid Item> items
) {
	public record Item(
		@NotNull Long noShowPostId,
		@NotBlank String menuName,
		@NotNull @Min(1) Integer quantity,
		@NotNull @Min(0) Integer originalPrice,
		@NotNull @Min(0) Integer discountRate
	) {}
}
