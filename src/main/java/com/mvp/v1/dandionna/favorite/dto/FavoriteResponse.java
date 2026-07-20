package com.mvp.v1.dandionna.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FavoriteResponse(
	@Schema(description = "즐겨찾기 여부")
	boolean favorited,
	@Schema(description = "상태 메시지")
	String message
) {}
