package com.mvp.v1.dandionna.noshow_order.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowPaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record NoShowOrderCreateResponse(
	@Schema(description = "주문 ID")
	UUID orderId,
	@Schema(description = "주문 번호", example = "NS-20251202-9K3A1F")
	String orderNo,
	@Schema(description = "주문 상태")
	NoShowOrderStatus status,
	@Schema(description = "결제 상태")
	NoShowPaymentStatus paymentStatus,
	@Schema(description = "결제 금액")
	int paidAmount,
	@Schema(description = "방문 시간")
	OffsetDateTime visitTime,
	@Schema(description = "주문 요약")
	String menuSummary
) {}
