package com.mvp.v1.dandionna.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.menu.dto.MenuComponentRequest;
import com.mvp.v1.dandionna.menu.dto.MenuCreateRequest;
import com.mvp.v1.dandionna.menu.dto.MenuDetailResponse;
import com.mvp.v1.dandionna.menu.dto.MenuStatusResponse;
import com.mvp.v1.dandionna.menu.dto.MenuSummaryResponse;
import com.mvp.v1.dandionna.menu.dto.MenuUpdateRequest;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.entity.MenuSetItem;
import com.mvp.v1.dandionna.menu.entity.MenuStatus;
import com.mvp.v1.dandionna.menu.entity.MenuType;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.menu.repository.MenuSetItemRepository;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostStatus;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostRepository;
import com.mvp.v1.dandionna.s3.dto.PresignedUrlResponse;
import com.mvp.v1.dandionna.s3.dto.S3Metadata;
import com.mvp.v1.dandionna.s3.service.MenuImageTempUploadService;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.entity.ImageStatus;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	@Mock
	private MenuRepository menuRepository;
	@Mock
	private MenuSetItemRepository menuSetItemRepository;
	@Mock
	private StoreRepository storeRepository;
	@Mock
	private NoShowPostRepository noShowPostRepository;
	@Mock
	private MenuImageTempUploadService menuImageTempUploadService;
	@Mock
	private UploadService uploadService;

	@InjectMocks
	private MenuService menuService;

	@Test
	void createSingleMenu_defaultsToSoldOut() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID savedMenuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> {
			Menu menu = invocation.getArgument(0);
			ReflectionTestUtils.setField(menu, "id", savedMenuId);
			return menu;
		});

		MenuCreateRequest request = new MenuCreateRequest(
			"아메리카노",
			"기본 메뉴",
			4500,
			MenuType.single,
			null,
			null
		);

		MenuDetailResponse response = menuService.create(ownerId, request);

		ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
		verify(menuRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(MenuStatus.sold_out);
		assertThat(captor.getValue().getType()).isEqualTo(MenuType.single);
		assertThat(response.status()).isEqualTo(MenuStatus.sold_out);
		assertThat(response.effectiveStatus()).isEqualTo(MenuStatus.sold_out);
		assertThat(response.components()).isEmpty();
	}

	@Test
	void createSetMenu_rejectsNonSingleComponent() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID componentId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu nestedSet = createMenu(componentId, storeId, "세트A", 12000, MenuType.set, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByStoreIdAndIdIn(eq(storeId), any())).thenReturn(List.of(nestedSet));

		MenuCreateRequest request = new MenuCreateRequest(
			"점심세트",
			"세트 설명",
			15000,
			MenuType.set,
			List.of(new MenuComponentRequest(componentId, 1)),
			null
		);

		assertThatThrownBy(() -> menuService.create(ownerId, request))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.MENU_SET_COMPONENT_INVALID));
	}

	@Test
	void changeStatus_singleMenuToSoldOut_closesOwnAndDependentSetPosts() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID singleId = UUID.randomUUID();
		UUID setId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu singleMenu = createMenu(singleId, storeId, "단품", 8000, MenuType.single, MenuStatus.on_sale);
		NoShowPost singlePost = createOpenPost(singleId);
		NoShowPost setPost = createOpenPost(setId);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(singleId, storeId)).thenReturn(Optional.of(singleMenu));
		when(menuSetItemRepository.findByComponentMenuIdIn(List.of(singleId)))
			.thenReturn(List.of(MenuSetItem.create(setId, singleId, 1)));
		when(noShowPostRepository.findByMenuIdInAndStatusAndDeletedAtIsNull(any(), eq(NoShowPostStatus.open)))
			.thenReturn(List.of(singlePost, setPost));

		MenuStatusResponse response = menuService.changeStatus(ownerId, singleId, false);

		assertThat(response.status()).isEqualTo(MenuStatus.sold_out);
		assertThat(response.effectiveStatus()).isEqualTo(MenuStatus.sold_out);
		assertThat(singlePost.getStatus()).isEqualTo(NoShowPostStatus.closed);
		assertThat(setPost.getStatus()).isEqualTo(NoShowPostStatus.closed);
	}

	@Test
	void changeStatus_setMenuToOnSale_returnsSoldOutEffectiveStatusWhenComponentIsSoldOut() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID setId = UUID.randomUUID();
		UUID componentId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu setMenu = createMenu(setId, storeId, "세트", 16000, MenuType.set, MenuStatus.sold_out);
		Menu component = createMenu(componentId, storeId, "단품", 6000, MenuType.single, MenuStatus.sold_out);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(setId, storeId)).thenReturn(Optional.of(setMenu));
		when(menuSetItemRepository.findBySetMenuIdIn(Set.of(setId)))
			.thenReturn(List.of(MenuSetItem.create(setId, componentId, 2)));
		when(menuRepository.findByIdIn(Set.of(componentId))).thenReturn(List.of(component));

		MenuStatusResponse response = menuService.changeStatus(ownerId, setId, true);

		verify(noShowPostRepository, never()).findByMenuIdInAndStatusAndDeletedAtIsNull(any(), any());
		assertThat(response.status()).isEqualTo(MenuStatus.on_sale);
		assertThat(response.effectiveStatus()).isEqualTo(MenuStatus.sold_out);
	}

	@Test
	void delete_rejectsSingleMenuUsedBySet() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));
		when(menuSetItemRepository.existsByComponentMenuId(menuId)).thenReturn(true);

		assertThatThrownBy(() -> menuService.delete(ownerId, menuId))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.MENU_COMPONENT_IN_USE));
	}

	@Test
	void list_filtersByEffectiveStatus() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID singleId = UUID.randomUUID();
		UUID setId = UUID.randomUUID();
		UUID componentId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu singleMenu = createMenu(singleId, storeId, "김밥", 4000, MenuType.single, MenuStatus.on_sale);
		Menu setMenu = createMenu(setId, storeId, "세트", 9000, MenuType.set, MenuStatus.on_sale);
		Menu soldOutComponent = createMenu(componentId, storeId, "튀김", 3000, MenuType.single, MenuStatus.sold_out);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.search(storeId, null, null)).thenReturn(List.of(singleMenu, setMenu));
		when(menuSetItemRepository.findBySetMenuIdIn(Set.of(setId)))
			.thenReturn(List.of(MenuSetItem.create(setId, componentId, 1)));
		when(menuRepository.findByIdIn(Set.of(componentId))).thenReturn(List.of(soldOutComponent));

		Page<MenuSummaryResponse> response = menuService.list(ownerId, 0, 10, null, null, "ON_SALE");

		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getContent().get(0).id()).isEqualTo(singleId);
	}

	@Test
	void create_blankDescription_normalizesToNull() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID savedMenuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> {
			Menu menu = invocation.getArgument(0);
			ReflectionTestUtils.setField(menu, "id", savedMenuId);
			return menu;
		});

		MenuCreateRequest request = new MenuCreateRequest(
			"아메리카노",
			"   ",
			4500,
			MenuType.single,
			null,
			null
		);

		MenuDetailResponse response = menuService.create(ownerId, request);

		assertThat(response.description()).isNull();
	}

	@Test
	void update_nullDescription_keepsExistingDescription() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));

		MenuUpdateRequest request = new MenuUpdateRequest(
			null,
			null,
			null,
			null,
			null,
			null
		);

		MenuDetailResponse response = menuService.update(ownerId, menuId, request);

		assertThat(response.description()).isEqualTo("설명");
	}

	@Test
	void update_blankDescription_keepsExistingDescription() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));

		MenuUpdateRequest request = new MenuUpdateRequest(
			null,
			"   ",
			null,
			null,
			null,
			null
		);

		MenuDetailResponse response = menuService.update(ownerId, menuId, request);

		assertThat(response.description()).isEqualTo("설명");
	}

	@Test
	void update_description_updatesWhenTextProvided() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));

		MenuUpdateRequest request = new MenuUpdateRequest(
			null,
			"  새 설명  ",
			null,
			null,
			null,
			null
		);

		MenuDetailResponse response = menuService.update(ownerId, menuId, request);

		assertThat(response.description()).isEqualTo("새 설명");
	}

	@Test
	void get_singleMenu_returnsOwnDescriptionAndEmptyComponents() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));

		MenuDetailResponse response = menuService.get(ownerId, menuId);

		assertThat(response.description()).isEqualTo("설명");
		assertThat(response.components()).isEmpty();
	}

	@Test
	void get_setMenu_returnsSetDescriptionAndCurrentComponentShape() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID setId = UUID.randomUUID();
		UUID componentId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu setMenu = createMenu(setId, storeId, "세트", 12000, MenuType.set, MenuStatus.on_sale);
		Menu componentMenu = createMenu(componentId, storeId, "단품", 5000, MenuType.single, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(setId, storeId)).thenReturn(Optional.of(setMenu));
		when(menuSetItemRepository.findBySetMenuIdIn(Set.of(setId)))
			.thenReturn(List.of(MenuSetItem.create(setId, componentId, 2)));
		when(menuRepository.findByIdIn(Set.of(componentId))).thenReturn(List.of(componentMenu));

		MenuDetailResponse response = menuService.get(ownerId, setId);

		assertThat(response.description()).isEqualTo("설명");
		assertThat(response.components()).hasSize(1);
		assertThat(response.components().get(0).menuId()).isEqualTo(componentId);
		assertThat(response.components().get(0).name()).isEqualTo("단품");
		assertThat(response.components().get(0).quantity()).isEqualTo(2);
	}

	@Test
	void list_withSingleTypeQueriesOnlySingleMenus() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu singleMenu = createMenu(UUID.randomUUID(), storeId, "김밥", 4000, MenuType.single, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.search(storeId, null, MenuType.single)).thenReturn(List.of(singleMenu));

		Page<MenuSummaryResponse> response = menuService.list(ownerId, 0, 10, null, "SINGLE", null);

		assertThat(response.getContent()).hasSize(1);
		verify(menuRepository).search(storeId, null, MenuType.single);
	}

	@Test
	void list_withSetTypeQueriesOnlySetMenus() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu setMenu = createMenu(UUID.randomUUID(), storeId, "세트", 9000, MenuType.set, MenuStatus.on_sale);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.search(storeId, null, MenuType.set)).thenReturn(List.of(setMenu));

		Page<MenuSummaryResponse> response = menuService.list(ownerId, 0, 10, null, "SET", null);

		assertThat(response.getContent()).hasSize(1);
		verify(menuRepository).search(storeId, null, MenuType.set);
	}

	@Test
	void create_withImageUploadToken_updatesImageMetadata() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID savedMenuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> {
			Menu menu = invocation.getArgument(0);
			ReflectionTestUtils.setField(menu, "id", savedMenuId);
			return menu;
		});
		when(menuImageTempUploadService.consumeForMenu(ownerId, "upload-token", savedMenuId))
			.thenReturn(new S3Metadata("menus/" + savedMenuId + "/image.png", "etag-1", "image/png"));
		when(uploadService.presignDownload("menus/" + savedMenuId + "/image.png"))
			.thenReturn(new PresignedUrlResponse("https://example.com/menu-" + savedMenuId, "menus/" + savedMenuId + "/image.png", 300));

		MenuCreateRequest request = new MenuCreateRequest(
			"아메리카노",
			"기본 메뉴",
			4500,
			MenuType.single,
			null,
			"upload-token"
		);

		MenuDetailResponse response = menuService.create(ownerId, request);

		assertThat(response.imageStatus()).isEqualTo(ImageStatus.uploaded);
		assertThat(response.imageUrl()).isEqualTo("https://example.com/menu-" + savedMenuId);
		assertThat(response.imageUrlExpiresInSeconds()).isEqualTo(300L);
		verify(menuImageTempUploadService).consumeForMenu(ownerId, "upload-token", savedMenuId);
	}

	@Test
	void update_withoutImageUploadToken_keepsExistingImage() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);
		menu.updateImage("menus/original.png", "image/png", "etag-old", ImageStatus.uploaded);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));
		when(uploadService.presignDownload("menus/original.png"))
			.thenReturn(new PresignedUrlResponse("https://example.com/original", "menus/original.png", 300));

		MenuUpdateRequest request = new MenuUpdateRequest(
			"수정된 단품",
			"설명 수정",
			8000,
			null,
			null,
			null
		);

		MenuDetailResponse response = menuService.update(ownerId, menuId, request);

		assertThat(response.imageUrl()).isEqualTo("https://example.com/original");
		assertThat(response.imageUrlExpiresInSeconds()).isEqualTo(300L);
		verify(menuImageTempUploadService, never()).consumeForMenu(any(), any(), any());
	}

	@Test
	void update_withImageUploadToken_replacesImageMetadata() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);
		menu.updateImage("menus/original.png", "image/png", "etag-old", ImageStatus.uploaded);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));
		when(menuImageTempUploadService.consumeForMenu(ownerId, "new-upload-token", menuId))
			.thenReturn(new S3Metadata("menus/" + menuId + "/new.png", "etag-new", "image/jpeg"));
		when(uploadService.presignDownload("menus/" + menuId + "/new.png"))
			.thenReturn(new PresignedUrlResponse("https://example.com/new-" + menuId, "menus/" + menuId + "/new.png", 300));

		MenuUpdateRequest request = new MenuUpdateRequest(
			null,
			null,
			null,
			null,
			null,
			"new-upload-token"
		);

		MenuDetailResponse response = menuService.update(ownerId, menuId, request);

		assertThat(response.imageStatus()).isEqualTo(ImageStatus.uploaded);
		assertThat(response.imageUrl()).isEqualTo("https://example.com/new-" + menuId);
		assertThat(response.imageUrlExpiresInSeconds()).isEqualTo(300L);
		verify(menuImageTempUploadService).consumeForMenu(ownerId, "new-upload-token", menuId);
	}

	@Test
	void list_uploadedImage_includesPresignedImageUrl() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);
		menu.updateImage("menus/list.png", "image/png", "etag-list", ImageStatus.uploaded);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.search(storeId, null, null)).thenReturn(List.of(menu));
		when(uploadService.presignDownload("menus/list.png"))
			.thenReturn(new PresignedUrlResponse("https://example.com/list", "menus/list.png", 300));

		Page<MenuSummaryResponse> response = menuService.list(ownerId, 0, 10, null, null, null);

		assertThat(response.getContent()).hasSize(1);
		assertThat(response.getContent().get(0).imageUrl()).isEqualTo("https://example.com/list");
		assertThat(response.getContent().get(0).imageUrlExpiresInSeconds()).isEqualTo(300L);
	}

	@Test
	void get_imageUrlGenerationFails_returnsDetailWithNullImageUrl() {
		UUID ownerId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		UUID menuId = UUID.randomUUID();
		Store store = createStore(storeId, ownerId);
		Menu menu = createMenu(menuId, storeId, "단품", 7000, MenuType.single, MenuStatus.on_sale);
		menu.updateImage("menus/fail.png", "image/png", "etag-fail", ImageStatus.uploaded);

		when(storeRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(store));
		when(menuRepository.findByIdAndStoreId(menuId, storeId)).thenReturn(Optional.of(menu));
		when(uploadService.presignDownload("menus/fail.png")).thenThrow(new RuntimeException("presign failed"));

		MenuDetailResponse response = menuService.get(ownerId, menuId);

		assertThat(response.imageStatus()).isEqualTo(ImageStatus.uploaded);
		assertThat(response.imageUrl()).isNull();
		assertThat(response.imageUrlExpiresInSeconds()).isNull();
	}

	private Store createStore(UUID storeId, UUID ownerId) {
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
			LocalTime.of(10, 0),
			LocalTime.of(22, 0),
			"설명",
			null,
			null,
			null,
			ImageStatus.pending
		);
		ReflectionTestUtils.setField(store, "id", storeId);
		return store;
	}

	private Menu createMenu(UUID menuId, UUID storeId, String name, int price, MenuType type, MenuStatus status) {
		Menu menu = Menu.create(storeId, name, "설명", price, status, type, null, null, null, ImageStatus.pending);
		ReflectionTestUtils.setField(menu, "id", menuId);
		return menu;
	}

	private NoShowPost createOpenPost(UUID menuId) {
		NoShowPost post = NoShowPost.create(
			UUID.randomUUID(),
			menuId,
			30,
			7000,
			10000,
			2,
			java.time.OffsetDateTime.now().minusMinutes(10),
			java.time.OffsetDateTime.now().plusMinutes(20)
		);
		ReflectionTestUtils.setField(post, "status", NoShowPostStatus.open);
		return post;
	}
}
