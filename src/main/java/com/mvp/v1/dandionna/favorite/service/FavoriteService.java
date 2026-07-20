package com.mvp.v1.dandionna.favorite.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.favorite.dto.FavoriteResponse;
import com.mvp.v1.dandionna.favorite.entity.Favorite;
import com.mvp.v1.dandionna.favorite.entity.FavoriteId;
import com.mvp.v1.dandionna.favorite.repository.FavoriteRepository;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

	private final FavoriteRepository favoriteRepository;
	private final StoreRepository storeRepository;

	@Transactional
	public FavoriteResponse addFavorite(UUID consumerId, UUID storeId) {
		validateStore(storeId);
		FavoriteId id = new FavoriteId(consumerId, storeId);
		if (favoriteRepository.existsById(id)) {
			return new FavoriteResponse(true, "이미 즐겨찾기에 추가되어 있습니다.");
		}
		favoriteRepository.save(Favorite.create(consumerId, storeId));
		return new FavoriteResponse(true, "즐겨찾기에 추가했습니다.");
	}

	@Transactional
	public FavoriteResponse removeFavorite(UUID consumerId, UUID storeId) {
		validateStore(storeId);
		FavoriteId id = new FavoriteId(consumerId, storeId);
		if (!favoriteRepository.existsById(id)) {
			return new FavoriteResponse(false, "이미 즐겨찾기에서 제거된 상태입니다.");
		}
		favoriteRepository.deleteById(id);
		return new FavoriteResponse(false, "즐겨찾기에서 제거했습니다.");
	}

	@Transactional(readOnly = true)
	public List<UUID> findConsumerIdsByStore(UUID storeId) {
		return favoriteRepository.findConsumerIdsByStoreId(storeId);
	}

	private void validateStore(UUID storeId) {
		if (!storeRepository.existsById(storeId)) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다.");
		}
	}
}
