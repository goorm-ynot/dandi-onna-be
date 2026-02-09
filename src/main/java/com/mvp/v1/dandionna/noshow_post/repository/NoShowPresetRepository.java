package com.mvp.v1.dandionna.noshow_post.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowPreset;

public interface NoShowPresetRepository extends JpaRepository<NoShowPreset, UUID> {

	Optional<NoShowPreset> findByStoreIdAndDefaultPresetTrueAndActiveTrueAndDeletedAtIsNull(UUID storeId);

	Optional<NoShowPreset> findByIdAndStoreIdAndActiveTrueAndDeletedAtIsNull(UUID id, UUID storeId);
}

