package com.mvp.v1.dandionna.home.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 가능한 가게(위치 기반) 요청")
public record HomeStoreRequest(
	@Schema(description = "위도 (WGS84, 소수점 6자리)", example = "37.481403",format = "double")
	@NotNull Double lat,
	@Schema(description = "경도 (WGS84, 소수점 6자리)", example = "126.995157",format = "double")
	@NotNull Double lon,
	@Schema(description = "페이지 (0부터 시작)", example = "0",format = "int")
	int page,
	@Schema(description = "페이지 크기", example = "10",format = "int")
	int size
) {}
