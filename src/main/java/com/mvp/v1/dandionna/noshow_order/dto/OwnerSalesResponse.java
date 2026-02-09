package com.mvp.v1.dandionna.noshow_order.dto;

import java.util.List;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;

public record OwnerSalesResponse(
	List<SaleItem> items,
	PageInfo pageInfo
) {

	public record SaleItem(
		String saleDateTime,
		String orderNo,
		String orderType,
		String menuNames,
		int paidAmount,
		String paymentMethod,
		NoShowOrderStatus status
	) {}

	public record PageInfo(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
	) {}
}
