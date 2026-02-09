package com.mvp.v1.dandionna.store.dto;

import java.time.LocalTime;

import com.mvp.v1.dandionna.store.entity.Store;

public record StoreMyPageResponse(
	String ownerName,
	String storeName,
	String addressRoad,
	LocalTime openTime,
	LocalTime closeTime
) {
	public static StoreMyPageResponse from(Store store, String ownerName) {
		return new StoreMyPageResponse(
			ownerName,
			store.getName(),
			store.getAddressRoad(),
			store.getOpenTime(),
			store.getCloseTime()
		);
	}
}
