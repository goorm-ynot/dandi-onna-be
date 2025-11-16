package com.mvp.v1.dandionna.menu.dto;

import jakarta.validation.constraints.Min;

public record MenuUpdateRequest(
	String name,
	String description,
	@Min(0) Integer priceKrw,
	String imageKey,
	String imageMime,
	String imageEtag
) {}
