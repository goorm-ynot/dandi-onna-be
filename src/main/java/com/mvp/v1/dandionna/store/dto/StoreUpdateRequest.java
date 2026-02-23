package com.mvp.v1.dandionna.store.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record StoreUpdateRequest(
	@Size(max = 100, message = "매장명은 100자 이내로 작성해주세요.")
	String name,
	String category,
	String phone,
	String addressRoad,
	@DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
	BigDecimal lat,
	@DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
	BigDecimal lon,
	LocalTime openTime,
	LocalTime closeTime,
	@Size(max = 1000, message = "설명은 1000자 이내로 작성해주세요.")
	String description,
	String imageKey,
	String imageMime,
	String imageEtag
) {}
