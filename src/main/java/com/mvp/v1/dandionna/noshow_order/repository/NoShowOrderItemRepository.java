package com.mvp.v1.dandionna.noshow_order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderItem;

public interface NoShowOrderItemRepository extends JpaRepository<NoShowOrderItem, Long> {

	List<NoShowOrderItem> findByOrderId(Long orderId);
}
