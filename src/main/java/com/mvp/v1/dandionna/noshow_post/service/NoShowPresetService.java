package com.mvp.v1.dandionna.noshow_post.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPresetResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPresetUpsertRequest;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPreset;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPresetRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoShowPresetService {

	private static final String DEFAULT_PRESET_NAME = "기본";
	private static final int DEFAULT_DISCOUNT_PERCENT = 30;
	private static final int DEFAULT_VISIT_AVAILABLE_MINUTES = 30;
	private static final int DEFAULT_SALE_DELAY_MINUTES = 30;

	private final StoreRepository storeRepository;
	private final NoShowPresetRepository noShowPresetRepository;

	@Transactional
	public NoShowPresetResponse getDefaultPreset(UUID ownerId) {
		Store store = loadStore(ownerId);
		NoShowPreset preset = resolveOrCreateDefault(store.getId());
		return toResponse(preset);
	}

	@Transactional
	public NoShowPresetResponse upsertDefaultPreset(UUID ownerId, NoShowPresetUpsertRequest request) {
		Store store = loadStore(ownerId);
		NoShowPreset preset = resolveOrCreateDefault(store.getId());
		preset.update(
			request.name(),
			request.discountPercent(),
			request.visitAvailableMinutes(),
			request.saleDelayMinutes()
		);
		NoShowPreset saved = noShowPresetRepository.save(preset);
		return toResponse(saved);
	}

	NoShowPreset resolvePreset(UUID storeId, UUID presetId) {
		if (presetId == null) {
			return resolveOrCreateDefault(storeId);
		}
		return noShowPresetRepository.findByIdAndStoreIdAndActiveTrueAndDeletedAtIsNull(presetId, storeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프리셋을 찾을 수 없습니다."));
	}

	private NoShowPreset resolveOrCreateDefault(UUID storeId) {
		return noShowPresetRepository.findByStoreIdAndDefaultPresetTrueAndActiveTrueAndDeletedAtIsNull(storeId)
			.orElseGet(() -> noShowPresetRepository.save(
				NoShowPreset.createDefault(
					storeId,
					DEFAULT_PRESET_NAME,
					DEFAULT_DISCOUNT_PERCENT,
					DEFAULT_VISIT_AVAILABLE_MINUTES,
					DEFAULT_SALE_DELAY_MINUTES
				)));
	}

	private Store loadStore(UUID ownerId) {
		return storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
	}

	private NoShowPresetResponse toResponse(NoShowPreset preset) {
		return new NoShowPresetResponse(
			preset.getId(),
			preset.getName(),
			preset.getDiscountPercent(),
			preset.getVisitAvailableMinutes(),
			preset.getSaleDelayMinutes(),
			preset.getUpdatedAt()
		);
	}
}
