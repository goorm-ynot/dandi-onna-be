package com.mvp.v1.dandionna.noshow_post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.mvp.v1.dandionna.favorite.service.FavoriteNotificationService;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowBatchCreateRequest;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostsResponse;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostHistory;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostHistoryRepository;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.entity.ImageStatus;
import com.mvp.v1.dandionna.store.repository.StoreRepository;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@ExtendWith(MockitoExtension.class)
class NoShowPostServiceTest {

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
	@Mock
	private StoreRepository storeRepository;
	@Mock
	private MenuRepository menuRepository;
	@Mock
	private NoShowPostRepository noShowPostRepository;
	@Mock
	private NoShowPostHistoryRepository historyRepository;
	@Mock
	private FavoriteNotificationService favoriteNotificationService;

	@InjectMocks
	private NoShowPostService noShowPostService;

	@Test
	void createBatch_insertsNewPostWhenNoneExists() {
		UUID userId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, userId, LocalTime.of(10, 0), LocalTime.of(22, 0));
		Menu menu = createMenu(menuId, storeId, "모둠회", 20000);

		when(storeRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(store));
		when(menuRepository.findByStoreIdAndIdIn(eq(storeId), any())).thenReturn(List.of(menu));
		when(noShowPostRepository.findForUpdate(eq(storeId), eq(menuId), any())).thenReturn(Optional.empty());

		OffsetDateTime expireAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
		NoShowBatchCreateRequest request = new NoShowBatchCreateRequest(
			List.of(new NoShowBatchCreateRequest.Item(menuId, 2)),
			40,
			expireAt
		);

		noShowPostService.createBatch(userId, request);

		ArgumentCaptor<NoShowPost> captor = ArgumentCaptor.forClass(NoShowPost.class);
		verify(noShowPostRepository).save(captor.capture());
		verify(historyRepository, never()).save(any(NoShowPostHistory.class));

		NoShowPost saved = captor.getValue();
		assertThat(saved.getMenuId()).isEqualTo(menuId);
		assertThat(saved.getQtyTotal()).isEqualTo(2);
		assertThat(saved.getPricePercent()).isEqualTo(40);
		assertThat(saved.getOriginalUnitPrice()).isEqualTo(20000);
		assertThat(saved.getDiscountedUnitPrice()).isEqualTo(12000);
	}

	@Test
	void createBatch_overridesExistingPostAndStoresHistory() {
		UUID userId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, userId, LocalTime.of(10, 0), LocalTime.of(22, 0));
		Menu menu = createMenu(menuId, storeId, "모둠회", 15000);

		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		NoShowPost existing = NoShowPost.create(
			storeId,
			menuId,
			30,
			10500,
			15000,
			1,
			now,
			now.plusMinutes(20)
		);
		ReflectionTestUtils.setField(existing, "qtyRemaining", 1);

		when(storeRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(store));
		when(menuRepository.findByStoreIdAndIdIn(eq(storeId), any())).thenReturn(List.of(menu));
		when(noShowPostRepository.findForUpdate(eq(storeId), eq(menuId), any())).thenReturn(Optional.of(existing));

		NoShowBatchCreateRequest request = new NoShowBatchCreateRequest(
			List.of(new NoShowBatchCreateRequest.Item(menuId, 3)),
			50,
			now.plusMinutes(25)
		);

		noShowPostService.createBatch(userId, request);

		verify(noShowPostRepository, never()).save(any(NoShowPost.class));
		verify(historyRepository).save(any(NoShowPostHistory.class));
		assertThat(existing.getPricePercent()).isEqualTo(50);
		assertThat(existing.getOriginalUnitPrice()).isEqualTo(15000);
		assertThat(existing.getDiscountedUnitPrice()).isEqualTo(7500);
		assertThat(existing.getQtyTotal()).isEqualTo(4);
		assertThat(existing.getQtyRemaining()).isEqualTo(4);
	}

	@Test
	void listPosts_groupsByVisitTimeDescending() {
		UUID userId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuA = UUID.randomUUID();
		UUID menuB = UUID.randomUUID();
		Store store = createStore(storeId, userId, LocalTime.of(10, 0), LocalTime.of(22, 0));
		Menu menuEntityA = createMenu(menuA, storeId, "A세트", 12000);
		Menu menuEntityB = createMenu(menuB, storeId, "B세트", 15000);

		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		NoShowPost first = createPost(storeId, menuA, 40, 7200, 12000, 2, now.plusMinutes(30));
		NoShowPost second = createPost(storeId, menuB, 40, 9000, 15000, 0, now.plusMinutes(30));
		NoShowPost third = createPost(storeId, menuA, 50, 6000, 12000, 5, now.plusMinutes(10));

		when(storeRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(store));
		when(noShowPostRepository.findByStoreIdAndExpireAtBetween(eq(storeId), any(), any(), any()))
			.thenReturn(new PageImpl<>(List.of(first, second, third), PageRequest.of(0, 10, Sort.by("expireAt").descending()), 3));
		when(menuRepository.findByStoreIdAndIdIn(eq(storeId), any()))
			.thenReturn(List.of(menuEntityA, menuEntityB));

		NoShowPostsResponse response = noShowPostService.listPosts(userId, 0, 10, LocalDate.now());

		assertThat(response.posts()).hasSize(3);
		assertThat(response.posts().get(0).visitTime()).isEqualTo(first.getExpireAt());
		assertThat(response.posts().get(1).quantity()).isZero();
		assertThat(response.posts().get(2).visitTime()).isEqualTo(third.getExpireAt());
		assertThat(response.pagination().totalElements()).isEqualTo(3);
		assertThat(response.pagination().totalPages()).isEqualTo(1);
	}

	private Store createStore(UUID storeId, UUID ownerId, LocalTime open, LocalTime close) {
		Point geom = GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(1.0, 1.0));
		Store store = Store.create(
			ownerId,
			"테스트매장",
			"요식업",
			"010-1111-2222",
			"서울시 어딘가",
			BigDecimal.ONE,
			BigDecimal.ONE,
			geom,
			open,
			close,
			"설명",
			null,
			null,
			null,
			ImageStatus.pending
		);
		ReflectionTestUtils.setField(store, "id", storeId);
		return store;
	}

	private Menu createMenu(UUID menuId, UUID storeId, String name, int price) {
		Menu menu = Menu.create(storeId, name, "설명", price, null, null, null, ImageStatus.pending);
		ReflectionTestUtils.setField(menu, "id", menuId);
		return menu;
	}

	private NoShowPost createPost(UUID storeId, UUID menuId, int discount, int discountedUnitPrice,
		int originalUnitPrice, int qtyRemaining, OffsetDateTime expireAt) {
		NoShowPost post = NoShowPost.create(
			storeId,
			menuId,
			discount,
			discountedUnitPrice,
			originalUnitPrice,
			qtyRemaining,
			expireAt.minusMinutes(10),
			expireAt
		);
		ReflectionTestUtils.setField(post, "id", System.nanoTime());
		ReflectionTestUtils.setField(post, "qtyRemaining", qtyRemaining);
		return post;
	}
}
