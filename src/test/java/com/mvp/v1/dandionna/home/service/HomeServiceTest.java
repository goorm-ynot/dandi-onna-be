package com.mvp.v1.dandionna.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.mvp.v1.dandionna.home.dto.HomeResponse;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrder;
import com.mvp.v1.dandionna.noshow_order.entity.NoShowOrderStatus;
import com.mvp.v1.dandionna.noshow_order.repository.NoShowOrderRepository;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

	@Mock
	private NoShowOrderRepository noShowOrderRepository;
	@Mock
	private StoreRepository storeRepository;
	@Mock
	private UploadService uploadService;

	@InjectMocks
	private HomeService homeService;

	@Test
	void getHome_prioritizesPendingOrdersAndResolvesImageUrl() {
		UUID consumerId = UUID.randomUUID();
		OffsetDateTime base = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));

		NoShowOrder pending = createOrder(1L, consumerId, NoShowOrderStatus.PENDING, base.plusHours(1));
		NoShowOrder completed = createOrder(2L, consumerId, NoShowOrderStatus.COMPLETED, base.plusHours(2));

		when(noShowOrderRepository.findByConsumerIdAndStatusAndVisitTimeBetween(
			org.mockito.ArgumentMatchers.eq(consumerId),
			org.mockito.ArgumentMatchers.eq(NoShowOrderStatus.PENDING),
			org.mockito.ArgumentMatchers.any(OffsetDateTime.class),
			org.mockito.ArgumentMatchers.any(OffsetDateTime.class),
			org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(pending), PageRequest.of(0, 3), 1));

		when(noShowOrderRepository.findByConsumerIdAndStatusNotAndVisitTimeBetween(
			org.mockito.ArgumentMatchers.eq(consumerId),
			org.mockito.ArgumentMatchers.eq(NoShowOrderStatus.PENDING),
			org.mockito.ArgumentMatchers.any(OffsetDateTime.class),
			org.mockito.ArgumentMatchers.any(OffsetDateTime.class),
			org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(completed), PageRequest.of(0, 3), 1));

		Store storeA = mockStore(pending.getStoreId(), "매장A", "image-a");
		Store storeB = mockStore(completed.getStoreId(), "매장B", null);
		when(storeRepository.findAllById(any(Iterable.class)))
			.thenReturn(List.of(storeA, storeB));
		when(uploadService.presignDownload("image-a"))
			.thenReturn(new PresignedUrlResponse("https://example.com/a", "image-a", 300));

		HomeResponse response = homeService.getHome(consumerId);

		assertThat(response.myOrders()).hasSize(2);
		assertThat(response.myOrders().get(0).orderId()).isEqualTo(pending.getId());
		assertThat(response.myOrders().get(0).storeName()).isEqualTo("매장A");
		assertThat(response.myOrders().get(0).storeImageKey()).isEqualTo("https://example.com/a");
		assertThat(response.myOrders().get(1).orderId()).isEqualTo(completed.getId());
		assertThat(response.myOrders().get(1).storeImageKey()).isNull();

		verify(uploadService).presignDownload("image-a");
		verify(uploadService, never()).presignDownload(null);
	}

	private NoShowOrder createOrder(Long id, UUID consumerId, NoShowOrderStatus status, OffsetDateTime visitTime) {
		NoShowOrder order = NoShowOrder.create(consumerId, UUID.randomUUID(), 10000, visitTime, null);
		order.setStatus(status);
		order.setMenuNames("세트(1)");
		order.setPaidAmount(8000);
		ReflectionTestUtils.setField(order, "id", id);
		return order;
	}

	private Store mockStore(UUID storeId, String name, String imageKey) {
		Store store = org.mockito.Mockito.mock(Store.class);
		when(store.getId()).thenReturn(storeId);
		when(store.getName()).thenReturn(name);
		when(store.getImageKey()).thenReturn(imageKey);
		return store;
	}
}
