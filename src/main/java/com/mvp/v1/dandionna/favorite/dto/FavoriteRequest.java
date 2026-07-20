package com.mvp.v1.dandionna.favorite.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
	@NotNull
	UUID storeId
) {}
