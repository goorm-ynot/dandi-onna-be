package com.mvp.v1.dandionna.home.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record HomeResponse(
	List<MyOrder> myOrders
) {

	@Schema(description = "홈 - 내가 주문한 가게 정보")
	public record MyOrder(
		@Schema(description = "주문 ID", example = "123")
		Long orderId,
		@Schema(description = "매장 ID")
		UUID storeId,
		@Schema(description = "매장 이름", example = "청자 소반 백현동점")
		String storeName,
		@Schema(description = "매장 이미지 Presigned URL")
		String storeImageKey,
		@Schema(description = "주문 메뉴 요약 (예: 메뉴명1(수량1), 메뉴명2(수량2))")
		String menuSummary,
		@Schema(description = "주문 총액(정가)", example = "45000")
		int totalPrice,
		@Schema(description = "결제 금액", example = "27000")
		int paidAmount,
		@Schema(description = "주문 상태")
		NoShowOrderStatus status,
		@Schema(description = "방문 예정 시각", example = "2025-11-17T18:00:00+09:00")
		OffsetDateTime visitTime
	) {}
}
