package com.mvp.v1.dandionna.admin.dto;

import java.util.UUID;

import com.mvp.v1.dandionna.store.entity.Store;

public record AdminStoreResponse(
	UUID id,
	UUID ownerUserId,
	String name,
	String category,
	String phone,
	String addressRoad
) {
	public static AdminStoreResponse from(Store store) {
		return new AdminStoreResponse(
			store.getId(),
			store.getOwnerUserId(),
			store.getName(),
			store.getCategory(),
			store.getPhone(),
			store.getAddressRoad()
		);
	}
}
