package com.mvp.v1.dandionna.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
	@NotBlank String name,
	String description,
	@NotNull @Min(0) Integer priceKrw,
	String imageKey,
	String imageMime,
	String imageEtag
) {}
