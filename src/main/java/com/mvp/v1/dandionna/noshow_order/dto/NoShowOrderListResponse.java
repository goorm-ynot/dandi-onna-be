package com.mvp.v1.dandionna.noshow_order.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowPaymentStatus;

public record NoShowOrderListResponse(
	List<OrderSummary> orders,
	PageInfo pagination
) {

	public record OrderSummary(
		UUID orderId,
		String orderNo,
		OffsetDateTime visitTime,
		NoShowOrderStatus status,
		String menuNames,
		String consumerPhone
	) {}

	public record PageInfo(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
	) {}
}
