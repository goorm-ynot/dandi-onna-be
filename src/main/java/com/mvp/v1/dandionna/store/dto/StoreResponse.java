package com.mvp.v1.dandionna.store.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import com.mvp.v1.dandionna.store.entity.ImageStatus;
import com.mvp.v1.dandionna.store.entity.Store;

public record StoreResponse(
    UUID id,
    String name,
    String category,
    String phone,
    String addressRoad,
	String description,
    BigDecimal lat,
    BigDecimal lon,
    LocalTime openTime,
    LocalTime closeTime,
    String imageKey,
    String imageMime,
    String imageEtag,
    ImageStatus imageStatus
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
            store.getId(),
            store.getName(),
            store.getCategory(),
            store.getPhone(),
            store.getAddressRoad(),
			store.getDescription(),
            store.getLat(),
            store.getLon(),
            store.getOpenTime(),
            store.getCloseTime(),
            store.getImageKey(),
            store.getImageMime(),
            store.getImageEtag(),
            store.getImageStatus()
        );
    }
}
