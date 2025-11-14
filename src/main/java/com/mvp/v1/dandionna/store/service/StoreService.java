package com.mvp.v1.dandionna.store.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.s3.dto.S3Metadata;
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

	private final StoreRepository storeRepository;
	private final StorePermissionService permissionService; // ✅ 추가

	@Transactional
	public StoreResponse createStore(UUID ownerId, StoreCreateRequest request) {
		Store store = Store.create(
			ownerId,
			request.name(),
			request.category(),
			request.phone(),
			request.addressRoad(),
			request.lat(),
			request.lon(),
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
	public StoreResponse getStoreForOwner(UUID ownerId, UUID storeId) {
		permissionService.verifyOwner(ownerId, storeId);
		return StoreResponse.from(getStore(storeId));
	}

	@Transactional
	public StoreResponse updateStore(UUID ownerId, UUID storeId, StoreUpdateRequest request) {
		permissionService.verifyOwner(ownerId, storeId); // ✅ 권한 검증 분리

		Store store = getStore(storeId); // ✅ 단순 조회
		store.update(
			defaultIfNull(request.name(), store.getName()),
			defaultIfNull(request.category(), store.getCategory()),
			request.phone() != null ? request.phone() : store.getPhone(),
			defaultIfNull(request.addressRoad(), store.getAddressRoad()),
			request.lat() != null ? request.lat() : store.getLat(),
			request.lon() != null ? request.lon() : store.getLon(),
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
	public void deleteStore(UUID ownerId, UUID storeId) {
		permissionService.verifyOwner(ownerId, storeId); // ✅ 권한 검증 분리

		Store store = getStore(storeId); // ✅ 단순 조회
		storeRepository.delete(store);
	}

	private <T> T defaultIfNull(T value, T defaultValue) {
		return value != null ? value : defaultValue;
	}
}
