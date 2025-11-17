package com.mvp.v1.dandionna.noshow_order.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowPaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노쇼 주문 상세 응답")
public record NoShowOrderDetailResponse(
	@Schema(description = "주문 ID", example = "1")
	Long orderId,
	@Schema(description = "소비자 사용자 ID")
	UUID consumerId,
	@Schema(description = "매장 ID")
	UUID storeId,
	@Schema(description = "방문 예정 시각 (KST)", example = "2025-11-17T18:00:00+09:00")
	OffsetDateTime visitTime,
	@Schema(description = "주문 총액(정가 기준)", example = "45000")
	int totalPrice,
	@Schema(description = "실제 결제 금액", example = "27000")
	int paidAmount,
	@Schema(description = "주문 상태")
	NoShowOrderStatus status,
	@Schema(description = "결제 상태")
	NoShowPaymentStatus paymentStatus,
	@Schema(description = "결제 수단", example = "TEST_CARD")
	String paymentMethod,
	@Schema(description = "결제 PG 거래 ID")
	String paymentTxId,
	@Schema(description = "결제 메모")
	String paymentMemo,
	@Schema(description = "결제 완료 시각")
	OffsetDateTime paidAt,
	@Schema(description = "결제 실패 시각")
	OffsetDateTime failedAt,
	@Schema(description = "환불 완료 시각")
	OffsetDateTime refundedAt,
	@Schema(description = "매장 메모")
	String storeMemo,
	@Schema(description = "주문 생성 시각")
	OffsetDateTime createdAt,
	List<Item> items
) {

	@Schema(description = "노쇼 주문 아이템 스냅샷")
	public record Item(
		@Schema(description = "주문 아이템 ID", example = "10")
		Long orderItemId,
		@Schema(description = "주문 당시 메뉴 이름", example = "초밥 A세트")
		String menuName,
		@Schema(description = "주문 수량", example = "2")
		int quantity,
		@Schema(description = "한 개당 결제 단가", example = "13500")
		int unitPrice,
		@Schema(description = "적용된 할인율(%)", example = "40")
		int discountPercent,
		@Schema(description = "방문 예정 시각 (아이템 단위)")
		OffsetDateTime visitTime
	) {}
}
