package com.mvp.v1.dandionna.noshow_order.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderCompleteRequest;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderDetailResponse;
import com.mvp.v1.dandionna.noshow_order.dto.NoShowOrderListResponse;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrder;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowPaymentStatus;
import com.mvp.v1.dandionna.noshow_order.repository.NoShowOrderRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import com.mvp.v1.dandionna.consumer.entity.ConsumerProfile;
import com.mvp.v1.dandionna.consumer.repository.ConsumerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoShowOrderService {

	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

	private final StoreRepository storeRepository;
	private final NoShowOrderRepository noShowOrderRepository;
	private final ConsumerProfileRepository consumerProfileRepository;

	@Transactional(readOnly = true)
	public NoShowOrderListResponse listOrders(UUID ownerId, int page, int size, LocalDate date) {
		Store store = storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<NoShowOrder> ordersPage = queryOrders(store.getId(), date, pageable);

		List<NoShowOrder> orders = ordersPage.getContent();
		Map<UUID, String> phoneMap = loadConsumerPhones(
			orders.stream().map(NoShowOrder::getConsumerId).collect(Collectors.toSet())
		);

		List<NoShowOrderListResponse.OrderSummary> summaries = orders.stream()
			.map(order -> new NoShowOrderListResponse.OrderSummary(
				order.getId(),
				order.getVisitTime(),
				order.getStatus(),
				order.getMenuNames(),
				phoneMap.getOrDefault(order.getConsumerId(), "")
			))
			.toList();

		NoShowOrderListResponse.PageInfo pageInfo = new NoShowOrderListResponse.PageInfo(
			ordersPage.getNumber(),
			ordersPage.getSize(),
			ordersPage.getTotalElements(),
			ordersPage.getTotalPages(),
			ordersPage.hasNext()
		);

		return new NoShowOrderListResponse(summaries, pageInfo);
	}

	@Transactional(readOnly = true)
	public NoShowOrderDetailResponse getOrderDetail(UUID ownerId, Long orderId) {
		Store store = storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		NoShowOrder order = noShowOrderRepository.findByIdWithItems(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

		if (!order.getStoreId().equals(store.getId())) {
			throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "해당 주문을 조회할 권한이 없습니다.");
		}

		List<NoShowOrderDetailResponse.Item> items = mapOrderItems(order);

		return new NoShowOrderDetailResponse(
			order.getId(),
			order.getConsumerId(),
			order.getStoreId(),
			order.getVisitTime(),
			order.getTotalPrice(),
			order.getPaidAmount(),
			order.getStatus(),
			order.getPaymentStatus(),
			order.getPaymentMethod(),
			order.getPaymentTxId(),
			order.getPaymentMemo(),
			order.getPaidAt(),
			order.getFailedAt(),
			order.getRefundedAt(),
			order.getStoreMemo(),
			order.getCreatedAt(),
			items
		);
	}

	@Transactional
	public void completeOrder(UUID ownerId, Long orderId, NoShowOrderCompleteRequest request) {
		Store store = storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		NoShowOrder order = noShowOrderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));

		if (!order.getStoreId().equals(store.getId())) {
			throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "해당 주문을 처리할 권한이 없습니다.");
		}

		if (order.getStatus() == NoShowOrderStatus.CANCELLED) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "취소된 주문입니다.");
		}
		if (order.getStatus() == NoShowOrderStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "이미 완료된 주문입니다.");
		}

		order.setStatus(NoShowOrderStatus.COMPLETED);
		order.setPaymentStatus(NoShowPaymentStatus.PAID);
		if (order.getPaidAt() == null) {
			order.setPaidAt(OffsetDateTime.now());
		}
		if (request != null && request.storeMemo() != null) {
			order.setStoreMemo(request.storeMemo());
		}
	}

	private List<NoShowOrderDetailResponse.Item> mapOrderItems(NoShowOrder order) {
		return order.getItems().stream()
			.map(item -> new NoShowOrderDetailResponse.Item(
				item.getId(),
				item.getMenuName(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getDiscountPercent(),
				item.getVisitTime()
			))
			.toList();
	}

	private Page<NoShowOrder> queryOrders(UUID storeId, LocalDate date, Pageable pageable) {
		if (date != null) {
			OffsetDateTime start = date.atStartOfDay(ZONE_KST).toOffsetDateTime();
			OffsetDateTime end = date.plusDays(1).atStartOfDay(ZONE_KST).toOffsetDateTime();
			return noShowOrderRepository.findByStoreIdAndVisitTimeBetween(storeId, start, end, pageable);
		}
		return noShowOrderRepository.findByStoreId(storeId, pageable);
	}

	private Map<UUID, String> loadConsumerPhones(Collection<UUID> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		List<ConsumerProfile> profiles = consumerProfileRepository.findByUserIdIn(userIds);
		return profiles.stream()
			.collect(Collectors.toMap(
				ConsumerProfile::getUserId,
				ConsumerProfile::getPhone
			));
	}
}
