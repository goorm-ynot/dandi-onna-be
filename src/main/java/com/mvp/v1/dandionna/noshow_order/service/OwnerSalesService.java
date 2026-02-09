package com.mvp.v1.dandionna.noshow_order.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.noshow_order.dto.OwnerSalesResponse;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrder;
import com.mvp.v1.dandionna.noshow_order.repository.NoShowOrderRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerSalesService {

	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
		.withZone(ZONE_KST);

	private final StoreRepository storeRepository;
	private final NoShowOrderRepository noShowOrderRepository;

	@Transactional(readOnly = true)
	public OwnerSalesResponse getSales(UUID ownerId, String startDate, String endDate, int page, int size) {
		Store store = storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		LocalDate start = parseDate(startDate, "startDate");
		LocalDate end = parseDate(endDate, "endDate");
		if (end.isBefore(start)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "종료 날짜는 시작 날짜 이후여야 합니다.");
		}

		OffsetDateTime startAt = start.atStartOfDay(ZONE_KST).toOffsetDateTime();
		OffsetDateTime endAt = end.plusDays(1).atStartOfDay(ZONE_KST).toOffsetDateTime();

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<NoShowOrder> ordersPage = noShowOrderRepository.findByStoreIdAndCreatedAtBetween(
			store.getId(), startAt, endAt, pageable);

		List<OwnerSalesResponse.SaleItem> items = ordersPage.getContent().stream()
			.map(order -> new OwnerSalesResponse.SaleItem(
				DATETIME_FORMAT.format(order.getCreatedAt()),
				order.getOrderNo(),
				"NO_SHOW",
				order.getMenuNames() != null ? order.getMenuNames() : "",
				order.getPaidAmount(),
				order.getPaymentMethod(),
				order.getStatus()
			))
			.toList();

		OwnerSalesResponse.PageInfo pageInfo = new OwnerSalesResponse.PageInfo(
			ordersPage.getNumber(),
			ordersPage.getSize(),
			ordersPage.getTotalElements(),
			ordersPage.getTotalPages(),
			ordersPage.hasNext()
		);

		return new OwnerSalesResponse(items, pageInfo);
	}

	private LocalDate parseDate(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " 값을 입력하세요.");
		}
		try {
			return LocalDate.parse(value, DATE_FORMAT);
		} catch (DateTimeParseException ex) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " 형식은 YYYY.MM.DD 입니다.");
		}
	}
}
