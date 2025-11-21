package com.mvp.v1.dandionna.noshow_order.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrder;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;

public interface NoShowOrderRepository extends JpaRepository<NoShowOrder, Long> {

	Page<NoShowOrder> findByStoreId(UUID storeId, Pageable pageable);

	Page<NoShowOrder> findByStoreIdAndStatus(UUID storeId, NoShowOrderStatus status, Pageable pageable);

	Page<NoShowOrder> findByStoreIdAndVisitTimeBetween(UUID storeId, OffsetDateTime start, OffsetDateTime end,
		Pageable pageable);

	Page<NoShowOrder> findByStoreIdAndStatusAndVisitTimeBetween(UUID storeId, NoShowOrderStatus status,
		OffsetDateTime start, OffsetDateTime end, Pageable pageable);

	Page<NoShowOrder> findByConsumerIdAndStatusAndVisitTimeBetween(UUID consumerId, NoShowOrderStatus status,
		OffsetDateTime start, OffsetDateTime end, Pageable pageable);

	Page<NoShowOrder> findByConsumerIdAndStatusNotAndVisitTimeBetween(UUID consumerId, NoShowOrderStatus status,
		OffsetDateTime start, OffsetDateTime end, Pageable pageable);

	@Query("select distinct o from NoShowOrder o left join fetch o.items where o.id = :orderId")
	Optional<NoShowOrder> findByIdWithItems(@Param("orderId") Long orderId);
}
