package com.mvp.v1.dandionna.noshow_order.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

import java.util.UUID;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderItem;

public interface NoShowOrderItemRepository extends JpaRepository<NoShowOrderItem, Long> {

	List<NoShowOrderItem> findByOrderId(UUID orderId);

	List<NoShowOrderItem> findByOrderIdIn(Collection<UUID> orderIds);

	@Query("select i from NoShowOrderItem i join fetch i.order where i.order.id in :orderIds")
	List<NoShowOrderItem> findWithOrderByOrderIdIn(@Param("orderIds") Collection<UUID> orderIds);
}
