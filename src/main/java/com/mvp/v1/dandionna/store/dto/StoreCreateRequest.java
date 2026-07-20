package com.mvp.v1.dandionna.store.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StoreCreateRequest(
	@NotBlank String name,
	@NotBlank String category,
	@NotBlank String phone,
	@NotBlank String addressRoad,
	@NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal lat,
	@NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal lon,
	@NotNull LocalTime openTime,
	@NotNull LocalTime closeTime,
	String description,
	String imageKey,
	String imageMime,
	String imageEtag
) {}
