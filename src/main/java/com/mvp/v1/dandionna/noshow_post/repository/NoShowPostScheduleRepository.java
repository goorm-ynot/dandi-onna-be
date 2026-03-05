package com.mvp.v1.dandionna.noshow_post.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostSchedule;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowScheduleStatus;

import jakarta.persistence.LockModeType;

public interface NoShowPostScheduleRepository extends JpaRepository<NoShowPostSchedule, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from NoShowPostSchedule s where s.id = :id and s.deletedAt is null")
	Optional<NoShowPostSchedule> findByIdForUpdate(@Param("id") UUID id);

	Page<NoShowPostSchedule> findByStoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

	Page<NoShowPostSchedule> findByStoreIdAndStatusAndDeletedAtIsNull(UUID storeId, NoShowScheduleStatus status,
		Pageable pageable);
}

