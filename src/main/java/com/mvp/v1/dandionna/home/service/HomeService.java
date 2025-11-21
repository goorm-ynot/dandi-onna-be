package com.mvp.v1.dandionna.home.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.home.dto.HomeResponse;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrder;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;
import com.mvp.v1.dandionna.noshow_order.repository.NoShowOrderRepository;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeService {

	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

	private final NoShowOrderRepository noShowOrderRepository;
	private final StoreRepository storeRepository;
	private final UploadService uploadService;

	@Transactional(readOnly = true)
	public HomeResponse getHome(UUID consumerId) {
		LocalDate today = LocalDate.now(ZONE_KST);
		OffsetDateTime start = today.atStartOfDay(ZONE_KST).toOffsetDateTime();
		OffsetDateTime end = today.plusDays(1).atStartOfDay(ZONE_KST).toOffsetDateTime();

		Pageable limit = PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "visitTime"));
		List<NoShowOrder> pending = noShowOrderRepository
			.findByConsumerIdAndStatusAndVisitTimeBetween(consumerId, NoShowOrderStatus.PENDING, start, end, limit)
			.getContent();

		List<NoShowOrder> result = new ArrayList<>(pending);
		if (result.size() < 3) {
			List<NoShowOrder> nonPending = noShowOrderRepository
				.findByConsumerIdAndStatusNotAndVisitTimeBetween(consumerId, NoShowOrderStatus.PENDING, start, end,
					limit)
				.getContent();
			for (NoShowOrder order : nonPending) {
				if (result.size() >= 3) break;
				result.add(order);
			}
		}

		Set<UUID> storeIds = result.stream()
			.map(NoShowOrder::getStoreId)
			.collect(Collectors.toSet());
		Map<UUID, Store> storeMap = storeRepository.findAllById(storeIds)
			.stream()
			.collect(Collectors.toMap(Store::getId, s -> s));

		List<HomeResponse.MyOrder> myOrders = result.stream()
			.map(order -> {
				Store store = storeMap.get(order.getStoreId());
				String imageUrl = null;
				if (store != null && store.getImageKey() != null) {
					imageUrl = uploadService.presignDownload(store.getImageKey()).url();
				}
				return new HomeResponse.MyOrder(
					order.getId(),
					order.getStoreId(),
					store != null ? store.getName() : "",
					imageUrl,
					order.getMenuNames() != null ? order.getMenuNames() : "",
					order.getTotalPrice(),
					order.getPaidAmount(),
					order.getStatus(),
					order.getVisitTime()
				);
			})
			.toList();

		return new HomeResponse(myOrders);
	}
}
