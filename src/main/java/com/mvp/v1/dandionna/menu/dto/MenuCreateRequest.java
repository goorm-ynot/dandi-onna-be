package com.mvp.v1.dandionna.menu.dto;

import java.util.List;

import com.mvp.v1.dandionna.menu.entity.MenuType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
	@NotBlank String name,
	String description,
	@NotNull @Min(0) Integer priceKrw,
	MenuType type,
	List<@Valid MenuComponentRequest> components,
	String imageUploadToken
) {}
