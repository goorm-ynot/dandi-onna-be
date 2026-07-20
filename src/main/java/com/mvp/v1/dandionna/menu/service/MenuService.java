package com.mvp.v1.dandionna.menu.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private static final Logger log = LoggerFactory.getLogger(MenuService.class);

	private final MenuRepository menuRepository;
	private final MenuSetItemRepository menuSetItemRepository;
	private final StoreRepository storeRepository;
	private final NoShowPostRepository noShowPostRepository;
	private final MenuImageTempUploadService menuImageTempUploadService;
	private final UploadService uploadService;

	@Transactional
	public MenuDetailResponse create(UUID ownerId, MenuCreateRequest request) {
		Store store = getStore(ownerId);
		MenuType type = request.type() != null ? request.type() : MenuType.single;
		validateCreateRequest(store.getId(), type, request.components());
		String normalizedDescription = normalizeDescriptionForCreate(request.description());

		Menu menu = Menu.create(
			store.getId(),
			request.name(),
			normalizedDescription,
			request.priceKrw(),
			MenuStatus.sold_out,
			type,
			null,
			null,
			null,
			ImageStatus.pending
		);
		Menu saved = menuRepository.save(menu);

		if (type.isSet()) {
			replaceSetComponents(store.getId(), saved.getId(), request.components());
		}

		applyImageIfPresent(ownerId, saved, request.imageUploadToken());

		return toDetail(saved, buildViewContext(List.of(saved)));
	}

	@Transactional(readOnly = true)
	public MenuDetailResponse get(UUID ownerId, UUID menuId) {
		Store store = getStore(ownerId);
		Menu menu = getMenu(store.getId(), menuId);
		return toDetail(menu, buildViewContext(List.of(menu)));
	}

	@Transactional(readOnly = true)
	public Page<MenuSummaryResponse> list(UUID ownerId, int page, int size, String keyword, String typeValue,
		String effectiveStatusValue) {
		Store store = getStore(ownerId);
		String normalizedKeyword = normalizeKeyword(keyword);
		MenuType type = parseMenuType(typeValue);
		MenuStatus effectiveStatus = parseMenuStatus(effectiveStatusValue);

		int pageNumber = Math.max(page, 0);
		int pageSize = size > 0 ? size : 10;
		Pageable pageable = PageRequest.of(pageNumber, pageSize);

		List<Menu> menus = menuRepository.search(store.getId(), normalizedKeyword, type);
		MenuViewContext context = buildViewContext(menus);

		List<MenuSummaryResponse> filtered = menus.stream()
			.filter(menu -> effectiveStatus == null || context.effectiveStatuses().getOrDefault(menu.getId(), MenuStatus.sold_out) == effectiveStatus)
			.map(menu -> toSummary(menu, context))
			.toList();

		int total = filtered.size();
		int fromIndex = Math.min(pageNumber * pageSize, total);
		int toIndex = Math.min(fromIndex + pageSize, total);
		return new PageImpl<>(filtered.subList(fromIndex, toIndex), pageable, total);
	}

	@Transactional
	public MenuDetailResponse update(UUID ownerId, UUID menuId, MenuUpdateRequest request) {
		Store store = getStore(ownerId);
		Menu menu = getMenu(store.getId(), menuId);

		if (request.type() != null) {
			throw new BusinessException(ErrorCode.MENU_TYPE_IMMUTABLE, "메뉴 유형은 수정할 수 없습니다.");
		}

		menu.update(request.name(), normalizeDescriptionForUpdate(request.description()), request.priceKrw());
		applyImageIfPresent(ownerId, menu, request.imageUploadToken());

		if (request.components() != null) {
			if (menu.isSingle()) {
				throw new BusinessException(ErrorCode.MENU_SET_COMPONENT_INVALID, "단품 메뉴는 세트 구성을 가질 수 없습니다.");
			}
			replaceSetComponents(store.getId(), menu.getId(), request.components());
		}

		MenuViewContext context = buildViewContext(List.of(menu));
		if (context.effectiveStatuses().getOrDefault(menu.getId(), MenuStatus.sold_out) == MenuStatus.sold_out) {
			closeOpenPosts(Set.of(menu.getId()));
		}
		return toDetail(menu, context);
	}

	@Transactional
	public MenuStatusResponse changeStatus(UUID ownerId, UUID menuId, boolean onSale) {
		Store store = getStore(ownerId);
		Menu menu = getMenu(store.getId(), menuId);
		MenuStatus nextStatus = onSale ? MenuStatus.on_sale : MenuStatus.sold_out;
		menu.changeStatus(nextStatus);

		if (!onSale) {
			Set<UUID> affectedMenuIds = new LinkedHashSet<>();
			affectedMenuIds.add(menu.getId());
			if (menu.isSingle()) {
				affectedMenuIds.addAll(findDependentSetIds(List.of(menu.getId())));
			}
			closeOpenPosts(affectedMenuIds);
		}

		MenuStatus effectiveStatus = buildViewContext(List.of(menu)).effectiveStatuses()
			.getOrDefault(menu.getId(), MenuStatus.sold_out);
		return new MenuStatusResponse(menu.getId(), menu.getStatus(), effectiveStatus);
	}

	@Transactional
	public void delete(UUID ownerId, UUID menuId) {
		Store store = getStore(ownerId);
		Menu menu = getMenu(store.getId(), menuId);
		if (menu.isSingle() && menuSetItemRepository.existsByComponentMenuId(menuId)) {
			throw new BusinessException(ErrorCode.MENU_COMPONENT_IN_USE, "세트 메뉴에서 사용 중인 단품은 삭제할 수 없습니다.");
		}
		menuRepository.delete(menu);
	}

	@Transactional(readOnly = true)
	public Map<UUID, Menu> loadMenusForPosting(UUID storeId, Collection<UUID> menuIds) {
		Set<UUID> ids = menuIds.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		if (ids.isEmpty()) {
			return Map.of();
		}

		List<Menu> menus = menuRepository.findByStoreIdAndIdIn(storeId, ids);
		if (menus.size() != ids.size()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 메뉴가 포함되어 있습니다.");
		}

		MenuViewContext context = buildViewContext(menus);
		boolean unavailable = menus.stream()
			.anyMatch(menu -> context.effectiveStatuses().getOrDefault(menu.getId(), MenuStatus.sold_out) != MenuStatus.on_sale);
		if (unavailable) {
			throw new BusinessException(ErrorCode.MENU_NOT_ON_SALE, "판매중이 아닌 메뉴가 포함되어 있습니다.");
		}

		Map<UUID, Menu> menuMap = new LinkedHashMap<>();
		for (Menu menu : menus) {
			menuMap.put(menu.getId(), menu);
		}
		return menuMap;
	}

	private Store getStore(UUID ownerId) {
		return storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
	}

	private Menu getMenu(UUID storeId, UUID menuId) {
		return menuRepository.findByIdAndStoreId(menuId, storeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메뉴를 찾을 수 없습니다."));
	}

	private void validateCreateRequest(UUID storeId, MenuType type, List<MenuComponentRequest> components) {
		if (type.isSet()) {
			validateSetComponents(storeId, null, components);
			return;
		}
		if (components != null) {
			throw new BusinessException(ErrorCode.MENU_SET_COMPONENT_INVALID, "단품 메뉴는 세트 구성을 가질 수 없습니다.");
		}
	}

	private void replaceSetComponents(UUID storeId, UUID setMenuId, List<MenuComponentRequest> components) {
		validateSetComponents(storeId, setMenuId, components);
		menuSetItemRepository.deleteBySetMenuId(setMenuId);
		List<MenuSetItem> items = components.stream()
			.map(component -> MenuSetItem.create(setMenuId, component.menuId(), component.quantity()))
			.toList();
		menuSetItemRepository.saveAll(items);
	}

	private void validateSetComponents(UUID storeId, UUID setMenuId, List<MenuComponentRequest> components) {
		if (components == null || components.isEmpty()) {
			throw new BusinessException(ErrorCode.MENU_SET_COMPONENTS_REQUIRED, "세트 메뉴는 최소 1개의 단품이 필요합니다.");
		}

		Set<UUID> componentIds = components.stream()
			.map(MenuComponentRequest::menuId)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		if (componentIds.size() != components.size()) {
			throw new BusinessException(ErrorCode.MENU_SET_COMPONENT_INVALID, "동일한 단품을 중복 구성할 수 없습니다.");
		}

		for (MenuComponentRequest component : components) {
			if (component.quantity() <= 0) {
				throw new BusinessException(ErrorCode.MENU_SET_COMPONENT_INVALID, "구성 단품 수량은 1 이상이어야 합니다.");
			}
			if (setMenuId != null && setMenuId.equals(component.menuId())) {
				throw new BusinessException(ErrorCode.MENU_SET_COMPONENT_INVALID, "자기 자신을 세트 구성품으로 넣을 수 없습니다.");
			}
		}

		List<Menu> componentMenus = menuRepository.findByStoreIdAndIdIn(storeId, componentIds);
		if (componentMenus.size() != componentIds.size()) {
			throw new BusinessException(ErrorCode.MENU_SET_COMPONENT_INVALID, "같은 매장의 단품 메뉴만 세트 구성품으로 등록할 수 있습니다.");
		}

		boolean hasNonSingle = componentMenus.stream().anyMatch(menu -> !menu.isSingle());
		if (hasNonSingle) {
			throw new BusinessException(ErrorCode.MENU_SET_COMPONENT_INVALID, "세트 메뉴에는 단품 메뉴만 포함할 수 있습니다.");
		}
	}

	private Set<UUID> findDependentSetIds(Collection<UUID> componentMenuIds) {
		if (componentMenuIds.isEmpty()) {
			return Set.of();
		}
		return menuSetItemRepository.findByComponentMenuIdIn(componentMenuIds).stream()
			.map(MenuSetItem::getSetMenuId)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private void closeOpenPosts(Collection<UUID> menuIds) {
		if (menuIds == null || menuIds.isEmpty()) {
			return;
		}
		List<NoShowPost> openPosts = noShowPostRepository.findByMenuIdInAndStatusAndDeletedAtIsNull(menuIds, NoShowPostStatus.open);
		for (NoShowPost post : openPosts) {
			post.markClosed();
		}
	}

	private void applyImageIfPresent(UUID ownerId, Menu menu, String imageUploadToken) {
		if (!StringUtils.hasText(imageUploadToken)) {
			return;
		}
		S3Metadata imageMetadata = menuImageTempUploadService.consumeForMenu(ownerId, imageUploadToken, menu.getId());
		menu.updateImage(
			imageMetadata.getKey(),
			imageMetadata.getContentType(),
			imageMetadata.getEtag(),
			ImageStatus.uploaded
		);
	}

	private String normalizeDescriptionForCreate(String description) {
		if (!StringUtils.hasText(description)) {
			return null;
		}
		return description.trim();
	}

	private String normalizeDescriptionForUpdate(String description) {
		if (!StringUtils.hasText(description)) {
			return null;
		}
		return description.trim();
	}

	private MenuSummaryResponse toSummary(Menu menu, MenuViewContext context) {
		MenuImageView imageView = resolveMenuImageView(menu);
		return new MenuSummaryResponse(
			menu.getId(),
			menu.getName(),
			menu.getDescription(),
			menu.getPriceKrw(),
			menu.getImageStatus(),
			imageView.imageUrl(),
			imageView.imageUrlExpiresInSeconds(),
			menu.getType(),
			menu.getStatus(),
			context.effectiveStatuses().getOrDefault(menu.getId(), MenuStatus.sold_out),
			context.setItemsBySetId().getOrDefault(menu.getId(), List.of()).size()
		);
	}

	private MenuDetailResponse toDetail(Menu menu, MenuViewContext context) {
		MenuImageView imageView = resolveMenuImageView(menu);
		List<MenuDetailResponse.Component> components = context.setItemsBySetId().getOrDefault(menu.getId(), List.of()).stream()
			.map(item -> {
				Menu componentMenu = context.componentMenusById().get(item.getComponentMenuId());
				MenuStatus componentStatus = componentMenu != null ? componentMenu.getStatus() : MenuStatus.sold_out;
				return new MenuDetailResponse.Component(
					item.getComponentMenuId(),
					componentMenu != null ? componentMenu.getName() : "",
					componentStatus,
					context.effectiveStatuses().getOrDefault(item.getComponentMenuId(), MenuStatus.sold_out),
					item.getQuantity()
				);
			})
			.toList();

		return new MenuDetailResponse(
			menu.getId(),
			menu.getName(),
			menu.getDescription(),
			menu.getPriceKrw(),
			menu.getImageStatus(),
			imageView.imageUrl(),
			imageView.imageUrlExpiresInSeconds(),
			menu.getType(),
			menu.getStatus(),
			context.effectiveStatuses().getOrDefault(menu.getId(), MenuStatus.sold_out),
			components
		);
	}

	private MenuImageView resolveMenuImageView(Menu menu) {
		if (!StringUtils.hasText(menu.getImageKey())) {
			return MenuImageView.empty();
		}

		try {
			PresignedUrlResponse response = uploadService.presignDownload(menu.getImageKey());
			return new MenuImageView(response.url(), response.expiresInSeconds());
		} catch (RuntimeException ex) {
			log.warn("메뉴 이미지 URL 생성 실패: menuId={}, storeId={}, imageKey={}", menu.getId(), menu.getStoreId(), menu.getImageKey(), ex);
			return MenuImageView.empty();
		}
	}

	private MenuViewContext buildViewContext(Collection<Menu> menus) {
		if (menus == null || menus.isEmpty()) {
			return new MenuViewContext(Map.of(), Map.of(), Map.of());
		}

		Map<UUID, Menu> menusById = menus.stream()
			.collect(Collectors.toMap(Menu::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
		Set<UUID> setIds = menus.stream()
			.filter(Menu::isSet)
			.map(Menu::getId)
			.collect(Collectors.toSet());

		Map<UUID, List<MenuSetItem>> setItemsBySetId = setIds.isEmpty()
			? Map.of()
			: menuSetItemRepository.findBySetMenuIdIn(setIds).stream()
				.collect(Collectors.groupingBy(MenuSetItem::getSetMenuId));

		Set<UUID> componentIds = setItemsBySetId.values().stream()
			.flatMap(List::stream)
			.map(MenuSetItem::getComponentMenuId)
			.collect(Collectors.toSet());
		Map<UUID, Menu> componentMenusById = componentIds.isEmpty()
			? Map.of()
			: menuRepository.findByIdIn(componentIds).stream()
				.collect(Collectors.toMap(Menu::getId, Function.identity()));

		Map<UUID, MenuStatus> effectiveStatuses = new HashMap<>();
		for (Menu menu : menusById.values()) {
			effectiveStatuses.put(menu.getId(), resolveEffectiveStatus(menu, setItemsBySetId, componentMenusById));
		}
		for (Menu componentMenu : componentMenusById.values()) {
			effectiveStatuses.putIfAbsent(componentMenu.getId(), componentMenu.getStatus());
		}

		return new MenuViewContext(effectiveStatuses, setItemsBySetId, componentMenusById);
	}

	private MenuStatus resolveEffectiveStatus(Menu menu, Map<UUID, List<MenuSetItem>> setItemsBySetId,
		Map<UUID, Menu> componentMenusById) {
		if (menu.isSingle()) {
			return menu.getStatus();
		}
		if (!menu.getStatus().isOnSale()) {
			return MenuStatus.sold_out;
		}

		List<MenuSetItem> items = setItemsBySetId.getOrDefault(menu.getId(), Collections.emptyList());
		if (items.isEmpty()) {
			return MenuStatus.sold_out;
		}

		for (MenuSetItem item : items) {
			Menu componentMenu = componentMenusById.get(item.getComponentMenuId());
			if (componentMenu == null || !componentMenu.isSingle() || componentMenu.getStatus() != MenuStatus.on_sale) {
				return MenuStatus.sold_out;
			}
		}
		return MenuStatus.on_sale;
	}

	private String normalizeKeyword(String keyword) {
		return StringUtils.hasText(keyword) ? keyword.trim() : null;
	}

	private MenuType parseMenuType(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return MenuType.from(value);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "유효하지 않은 메뉴 유형입니다.");
		}
	}

	private MenuStatus parseMenuStatus(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return MenuStatus.from(value);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "유효하지 않은 메뉴 상태입니다.");
		}
	}

	private record MenuViewContext(
		Map<UUID, MenuStatus> effectiveStatuses,
		Map<UUID, List<MenuSetItem>> setItemsBySetId,
		Map<UUID, Menu> componentMenusById
	) {
	}

	private record MenuImageView(
		String imageUrl,
		Long imageUrlExpiresInSeconds
	) {
		private static MenuImageView empty() {
			return new MenuImageView(null, null);
		}
	}
}
