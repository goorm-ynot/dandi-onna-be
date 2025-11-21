package com.mvp.v1.dandionna.store.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.common.service.SecurityUtils;
import com.mvp.v1.dandionna.favorite.entity.FavoriteId;
import com.mvp.v1.dandionna.favorite.repository.FavoriteRepository;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostStatus;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostRepository;
import com.mvp.v1.dandionna.s3.service.UploadService;
import com.mvp.v1.dandionna.store.dto.StoreNoShowPostsResponse;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreConsumerService {

	private final StoreRepository storeRepository;
	private final NoShowPostRepository noShowPostRepository;
	private final MenuRepository menuRepository;
	private final UploadService uploadService;
	private final FavoriteRepository favoriteRepository;

	@Transactional(readOnly = true)
	public StoreNoShowPostsResponse getNoShowPosts(UUID storeId, int page, int size) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));
		UUID consumerId = SecurityUtils.getCurrentUserId();

		int pageNumber = Math.max(page, 0);
		int pageSize = size > 0 ? size : 10;
		Pageable pageable = PageRequest.of(pageNumber, pageSize);

		Page<NoShowPost> postsPage = noShowPostRepository
			.findByStoreIdAndStatusAndExpireAtAfterAndDeletedAtIsNullOrderByExpireAtAsc(
				storeId,
				NoShowPostStatus.open,
				OffsetDateTime.now(),
				pageable
			);

		Map<UUID, Menu> menuMap = loadMenus(storeId, postsPage.getContent());

		List<StoreNoShowPostsResponse.PostSummary> posts = postsPage.getContent().stream()
			.map(post -> mapPost(post, menuMap.get(post.getMenuId())))
			.toList();

		boolean favorited = favoriteRepository.existsById(new FavoriteId(consumerId, storeId));

		return new StoreNoShowPostsResponse(
			mapStore(store),
			posts,
			new StoreNoShowPostsResponse.PageInfo(
				postsPage.getNumber(),
				postsPage.getSize(),
				postsPage.getTotalElements(),
				postsPage.getTotalPages(),
				postsPage.hasNext()
			),
			favorited
		);
	}

	private Map<UUID, Menu> loadMenus(UUID storeId, List<NoShowPost> posts) {
		if (posts.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<UUID> menuIds = posts.stream()
			.map(NoShowPost::getMenuId)
			.collect(Collectors.toSet());
		return menuRepository.findByStoreIdAndIdIn(storeId, menuIds).stream()
			.collect(Collectors.toMap(Menu::getId, Function.identity()));
	}

	private StoreNoShowPostsResponse.PostSummary mapPost(NoShowPost post, Menu menu) {
		String imageUrl = null;
		if (menu != null && StringUtils.hasText(menu.getImageKey())) {
			imageUrl = uploadService.presignDownload(menu.getImageKey()).url();
		}
		int originalPrice = post.getOriginalUnitPrice() != null
			? post.getOriginalUnitPrice()
			: menu != null ? menu.getPriceKrw() : post.getDiscountedUnitPrice();

		return new StoreNoShowPostsResponse.PostSummary(
			post.getId(),
			post.getExpireAt(),
			menu != null ? menu.getName() : null,
			menu != null ? menu.getDescription() : null,
			originalPrice,
			post.getPricePercent(),
			post.getDiscountedUnitPrice(),
			post.getQtyRemaining(),
			imageUrl
		);
	}

	private StoreNoShowPostsResponse.StoreInfo mapStore(Store store) {
		String storeImage = null;
		if (StringUtils.hasText(store.getImageKey())) {
			storeImage = uploadService.presignDownload(store.getImageKey()).url();
		}
		return new StoreNoShowPostsResponse.StoreInfo(
			store.getId(),
			store.getName(),
			store.getDescription(),
			store.getAddressRoad(),
			storeImage
		);
	}
}
