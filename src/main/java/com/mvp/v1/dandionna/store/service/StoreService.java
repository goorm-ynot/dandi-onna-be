package com.mvp.v1.dandionna.store.service;

import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.store.dto.StoreCreateRequest;
import com.mvp.v1.dandionna.store.dto.StoreResponse;
import com.mvp.v1.dandionna.store.dto.StoreUpdateRequest;
import com.mvp.v1.dandionna.store.entity.ImageStatus;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreService {

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	private final StoreRepository storeRepository;

	@Transactional
	public StoreResponse createStore(UUID ownerId, StoreCreateRequest request) {
		Point geom = GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(
			request.lon().doubleValue(), request.lat().doubleValue()));
		Store store = Store.create(
			ownerId,
			request.name(),
			request.category(),
			request.phone(),
			request.addressRoad(),
			request.lat(),
			request.lon(),
			geom,
			request.openTime(),
			request.closeTime(),
			request.description(),
			request.imageKey(),
			request.imageMime(),
			request.imageEtag(),
			request.imageKey() != null ? ImageStatus.uploaded : ImageStatus.pending
		);

		Store saved = storeRepository.save(store);
		return StoreResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public Store getStore(UUID storeId) {  // ✅ 간단한 조회 메서드로 변경
		return storeRepository.findById(storeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
	}

	@Transactional(readOnly = true)
	public StoreResponse getStoreForOwner(UUID ownerId) {
		return StoreResponse.from(getStoreByOwner(ownerId));
	}

	@Transactional
	public StoreResponse updateStore(UUID ownerId, StoreUpdateRequest request) {
		Store store = getStoreByOwner(ownerId);
		Point geom = store.getGeom();
		if (request.lat() != null && request.lon() != null) {
			geom = GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(
				request.lon().doubleValue(), request.lat().doubleValue()));
		}
		store.update(
			defaultIfNull(request.name(), store.getName()),
			defaultIfNull(request.category(), store.getCategory()),
			request.phone() != null ? request.phone() : store.getPhone(),
			defaultIfNull(request.addressRoad(), store.getAddressRoad()),
			request.lat() != null ? request.lat() : store.getLat(),
			request.lon() != null ? request.lon() : store.getLon(),
			geom,
			request.openTime() != null ? request.openTime() : store.getOpenTime(),
			request.closeTime() != null ? request.closeTime() : store.getCloseTime(),
			defaultIfNull(request.description(), store.getDescription())
		);

		if (request.imageKey() != null) {
			store.updateImage(request.imageKey(), request.imageMime(), request.imageEtag(), ImageStatus.uploaded);
		}

		return StoreResponse.from(store);
	}

	@Transactional
	public void deleteStore(UUID ownerId) {
		Store store = getStoreByOwner(ownerId);
		storeRepository.delete(store);
	}

	@Transactional(readOnly = true)
	public UUID getOwnedStoreId(UUID ownerId) {
		return getStoreByOwner(ownerId).getId();
	}

	private Store getStoreByOwner(UUID ownerId) {
		return storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
	}

	private <T> T defaultIfNull(T value, T defaultValue) {
		return value != null ? value : defaultValue;
	}
}
