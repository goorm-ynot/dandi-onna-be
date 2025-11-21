package com.mvp.v1.dandionna.store.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record StoreUpdateRequest(
	String name,
	String category,
    String phone,
	String addressRoad,
	BigDecimal lat,
	BigDecimal lon,
	LocalTime openTime,
	LocalTime closeTime,
	String description,
	String imageKey,
	String imageMime,
	String imageEtag
) {}
