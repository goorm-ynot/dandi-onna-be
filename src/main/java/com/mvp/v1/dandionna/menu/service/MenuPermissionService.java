package com.mvp.v1.dandionna.menu.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuPermissionService {

	private final StoreRepository storeRepository;
	private final MenuRepository menuRepository;

	public Menu verifyOwner(UUID ownerId, UUID menuId) {
		Store store = storeRepository.findByOwnerUserId(ownerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
		return menuRepository.findByIdAndStoreId(menuId, store.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메뉴를 찾을 수 없습니다."));
	}
}
