package com.mvp.v1.dandionna.noshow_post.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostStatus;

import jakarta.persistence.LockModeType;

public interface NoShowPostRepository extends JpaRepository<NoShowPost, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from NoShowPost p where p.storeId = :storeId and p.menuId = :menuId and p.expireAt = :expireAt")
	Optional<NoShowPost> findForUpdate(
		@Param("storeId") UUID storeId,
		@Param("menuId") UUID menuId,
		@Param("expireAt") OffsetDateTime expireAt
	);

	Page<NoShowPost> findByStoreIdAndExpireAtBetween(UUID storeId, OffsetDateTime start, OffsetDateTime end,
		Pageable pageable);

	Page<NoShowPost> findByStoreIdAndStatusAndExpireAtAfterAndDeletedAtIsNullOrderByExpireAtAsc(
		UUID storeId,
		NoShowPostStatus status,
		OffsetDateTime now,
		Pageable pageable
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from NoShowPost p where p.id in :ids and p.deletedAt is null")
	List<NoShowPost> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
