package com.mvp.v1.dandionna.menu.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.menu.dto.MenuCreateRequest;
import com.mvp.v1.dandionna.menu.dto.MenuResponse;
import com.mvp.v1.dandionna.menu.dto.MenuUpdateRequest;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.store.entity.ImageStatus;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuRepository menuRepository;
	private final StoreRepository storeRepository;

	private Store getStore(UUID ownerId) {
		return storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
	}

	private Menu getMenu(UUID storeId, UUID menuId) {
		return menuRepository.findByIdAndStoreId(menuId, storeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메뉴를 찾을 수 없습니다."));
	}

	@Transactional
	public MenuResponse create(UUID ownerId, MenuCreateRequest request) {
		Store store = getStore(ownerId);

		Menu menu = Menu.create(
			store.getId(),
			request.name(),
			request.description(),
			request.priceKrw(),
			request.imageKey(),
			request.imageMime(),
			request.imageEtag(),
			request.imageKey() != null ? ImageStatus.uploaded : ImageStatus.pending
		);
		return MenuResponse.from(menuRepository.save(menu));
	}

	@Transactional(readOnly = true)
	public MenuResponse get(UUID ownerId, UUID menuId) {
		Store store = getStore(ownerId);
		Menu menu = getMenu(store.getId(), menuId);
		return MenuResponse.from(menu);
	}

	@Transactional(readOnly = true)
	public Page<MenuResponse> list(UUID ownerId, int page, int size) {
		Store store = getStore(ownerId);
		return menuRepository.findByStoreId(store.getId(), PageRequest.of(page, size))
			.map(MenuResponse::from);
	}

	@Transactional
	public MenuResponse update(UUID ownerId, UUID menuId, MenuUpdateRequest request) {
		Store store = getStore(ownerId);
		Menu menu = getMenu(store.getId(), menuId);
		menu.update(request.name(), request.description(), request.priceKrw());
		if (request.imageKey() != null) {
			menu.updateImage(request.imageKey(), request.imageMime(), request.imageEtag(), ImageStatus.uploaded);
		}
		return MenuResponse.from(menu);
	}

	@Transactional
	public void delete(UUID ownerId, UUID menuId) {
		Store store = getStore(ownerId);
		Menu menu = getMenu(store.getId(), menuId);
		menuRepository.delete(menu);
	}
}
