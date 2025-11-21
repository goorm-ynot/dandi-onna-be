package com.mvp.v1.dandionna.noshow_post.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mvp.v1.dandionna.common.dto.ErrorCode;
import com.mvp.v1.dandionna.common.exeption.BusinessException;
import com.mvp.v1.dandionna.favorite.service.FavoriteNotificationService;
import com.mvp.v1.dandionna.menu.entity.Menu;
import com.mvp.v1.dandionna.menu.repository.MenuRepository;
import com.mvp.v1.dandionna.noshow_post.NoShowConstants;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowBatchCreateRequest;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostDetailResponse;
import com.mvp.v1.dandionna.noshow_post.dto.NoShowPostsResponse;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPost;
import com.mvp.v1.dandionna.noshow_post.entity.NoShowPostHistory;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostHistoryRepository;
import com.mvp.v1.dandionna.noshow_post.repository.NoShowPostRepository;
import com.mvp.v1.dandionna.store.entity.Store;
import com.mvp.v1.dandionna.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoShowPostService {

	private final StoreRepository storeRepository;
	private final MenuRepository menuRepository;
	private final NoShowPostRepository noShowPostRepository;
	private final NoShowPostHistoryRepository noShowPostHistoryRepository;
	private final FavoriteNotificationService favoriteNotificationService;

	private static final String HISTORY_REASON_REPLACED = "REPLACED";

	@Transactional
	public void createBatch(UUID userId, NoShowBatchCreateRequest request) {
		Store store = storeRepository.findByOwnerUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		validateRequest(request);

		OffsetDateTime startAtUtc = OffsetDateTime.now(NoShowConstants.DB_ZONE).withSecond(0).withNano(0);
		OffsetDateTime expireAtUtc = normalizeExpireAt(startAtUtc, store.getCloseTime(), request.expireAt());

		Map<UUID, Menu> menuMap = loadMenus(store.getId(), request.items());

		for (NoShowBatchCreateRequest.Item item : request.items()) {
			Menu menu = menuMap.get(item.menuId());
			int qty = toQuantity(item.quantity());
			int originalUnitPrice = menu.getPriceKrw();
			int discountedUnitPrice = applyDiscount(originalUnitPrice, request.discountPercent());

			noShowPostRepository.findForUpdate(store.getId(), item.menuId(), expireAtUtc)
				.ifPresentOrElse(existing -> {
					noShowPostHistoryRepository.save(NoShowPostHistory.from(existing, startAtUtc, HISTORY_REASON_REPLACED));
					int combinedQty = combineQuantities(existing.getQtyRemaining(), qty);
					existing.overrideListing(
						request.discountPercent(),
						discountedUnitPrice,
						originalUnitPrice,
						combinedQty,
						startAtUtc,
						expireAtUtc
					);
				}, () -> {
					NoShowPost post = NoShowPost.create(
						store.getId(),
						item.menuId(),
						request.discountPercent(),
						discountedUnitPrice,
						originalUnitPrice,
						qty,
						startAtUtc,
						expireAtUtc
					);
					noShowPostRepository.save(post);
					favoriteNotificationService.notifyNoShowPost(store, menu, post);
				});
		}
	}

	@Transactional(readOnly = true)
	public NoShowPostsResponse listPosts(UUID userId, int page, int size, LocalDate date) {
		Store store = storeRepository.findByOwnerUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		LocalDate targetDate = date != null ? date : LocalDate.now();
		OffsetDateTime startOfDay = targetDate.atStartOfDay(NoShowConstants.ZONE_KST).withZoneSameInstant(NoShowConstants.DB_ZONE).toOffsetDateTime();
		OffsetDateTime endOfDay = targetDate.plusDays(1).atStartOfDay(NoShowConstants.ZONE_KST).withZoneSameInstant(NoShowConstants.DB_ZONE).toOffsetDateTime();

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expireAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));
		Page<NoShowPost> posts = noShowPostRepository.findByStoreIdAndExpireAtBetween(store.getId(), startOfDay, endOfDay, pageable);

		Map<UUID, Menu> menuMap = loadMenus(store.getId(),
			posts.stream().map(NoShowPost::getMenuId).distinct()
				.map(id -> new NoShowBatchCreateRequest.Item(id, 1))
				.toList());

		List<NoShowPostsResponse.PostItem> items = posts.getContent().stream()
			.map(post -> {
				Menu menu = menuMap.get(post.getMenuId());
				return new NoShowPostsResponse.PostItem(
					post.getId(),
					post.getMenuId(),
					menu != null ? menu.getName() : "",
					post.getExpireAt(),
					Math.max(0, post.getQtyRemaining()),
					post.getPricePercent()
				);
			}).toList();

		NoShowPostsResponse.PageInfo pageInfo = new NoShowPostsResponse.PageInfo(
			posts.getNumber(),
			posts.getSize(),
			posts.getTotalElements(),
			posts.getTotalPages(),
			posts.hasNext()
		);

		return new NoShowPostsResponse(items, pageInfo);
	}

	private void validateRequest(NoShowBatchCreateRequest request) {
		if (request.discountPercent() < NoShowConstants.MIN_DISCOUNT_PERCENT
			|| request.discountPercent() > NoShowConstants.MAX_DISCOUNT_PERCENT) {
			throw new BusinessException(ErrorCode.LISTING_DISCOUNT_INVALID, "할인율은 30~90% 사이여야 합니다.");
		}
		if (request.expireAt() == null) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "마감 시간을 입력해 주세요.");
		}
		for (NoShowBatchCreateRequest.Item item : request.items()) {
			if (item.quantity() < NoShowConstants.MIN_QUANTITY) {
				throw new BusinessException(ErrorCode.LISTING_QTY_INVALID, "수량은 1 이상이어야 합니다.");
			}
		}
	}

	@Transactional(readOnly = true)
	public NoShowPostDetailResponse getPostDetail(UUID userId, Long postId) {
		Store store = storeRepository.findByOwnerUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "매장을 찾을 수 없습니다."));

		NoShowPost post = noShowPostRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "노쇼 글을 찾을 수 없습니다."));

		if (!post.getStoreId().equals(store.getId())) {
			throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "해당 노쇼 글을 조회할 권한이 없습니다.");
		}

		Menu menu = menuRepository.findById(post.getMenuId())
			.orElse(null);

		return new NoShowPostDetailResponse(
			post.getId(),
			post.getMenuId(),
			post.getExpireAt(),
			menu != null ? menu.getName() : "",
			Math.max(0, post.getQtyRemaining()),
			post.getOriginalUnitPrice() != null ? post.getOriginalUnitPrice() : 0,
			post.getPricePercent()
		);
	}

	private Map<UUID, Menu> loadMenus(UUID storeId, List<NoShowBatchCreateRequest.Item> items) {
		Set<UUID> ids = items.stream()
			.map(NoShowBatchCreateRequest.Item::menuId)
			.collect(Collectors.toSet());
		if (ids.size() != items.size()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "동일한 메뉴가 중복되었습니다.");
		}
		List<Menu> menus = menuRepository.findByStoreIdAndIdIn(storeId, ids);
		if (menus.size() != ids.size()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 메뉴가 포함되어 있습니다.");
		}
		Map<UUID, Menu> map = new HashMap<>();
		for (Menu menu : menus) {
			map.put(menu.getId(), menu);
		}
		return map;
	}

	private int toQuantity(int quantity) {
		if (quantity < NoShowConstants.MIN_QUANTITY || quantity > Short.MAX_VALUE) {
			throw new BusinessException(ErrorCode.LISTING_QTY_INVALID, "수량은 1 이상이어야 합니다.");
		}
		return quantity;
	}

	private int applyDiscount(int original, int discountPercent) {
		BigDecimal finalPrice = BigDecimal.valueOf(original)
			.multiply(BigDecimal.valueOf(100 - discountPercent))
			.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
		return finalPrice.intValue();
	}

	private OffsetDateTime normalizeExpireAt(OffsetDateTime nowUtc, LocalTime closeTime, OffsetDateTime requestedExpireAt) {
		if (requestedExpireAt == null) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "마감 시간을 입력해 주세요.");
		}

		ZonedDateTime nowSeoul = nowUtc.atZoneSameInstant(NoShowConstants.ZONE_KST);
		ZonedDateTime candidate = requestedExpireAt.atZoneSameInstant(NoShowConstants.ZONE_KST)
			.withSecond(0)
			.withNano(0);

		if (!candidate.isAfter(nowSeoul)) {
			candidate = nowSeoul.plusMinutes(1).withSecond(0).withNano(0);
		}

		long diffMinutes = Duration.between(nowSeoul, candidate).toMinutes();
		if (diffMinutes < NoShowConstants.MIN_EXPIRE_MINUTES || diffMinutes > NoShowConstants.MAX_EXPIRE_MINUTES) {
			throw new BusinessException(ErrorCode.LISTING_TTL_INVALID, "마감 시간은 현재로부터 0~300분 사이여야 합니다.");
		}

		int remainder = candidate.getMinute() % 10;
		if (remainder != 0) {
			candidate = candidate.plusMinutes(10 - remainder);
		}
		candidate = candidate.withSecond(0).withNano(0);

		diffMinutes = Duration.between(nowSeoul, candidate).toMinutes();
		if (diffMinutes > NoShowConstants.MAX_EXPIRE_MINUTES) {
			throw new BusinessException(ErrorCode.LISTING_TTL_INVALID, "마감 시간이 허용 범위를 초과했습니다.");
		}

		ZonedDateTime closeSeoul = nowSeoul.withHour(closeTime.getHour())
			.withMinute(closeTime.getMinute())
			.withSecond(0)
			.withNano(0);

		if (!closeSeoul.isAfter(nowSeoul)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "영업시간이 이미 종료되었습니다.");
		}
		if (candidate.isAfter(closeSeoul)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "영업시간을 넘어설 수 없습니다.");
		}

		return candidate.withZoneSameInstant(NoShowConstants.DB_ZONE).toOffsetDateTime();
	}

	private int combineQuantities(int existingRemaining, int additional) {
		long sum = (long) existingRemaining + additional;
		if (sum > Short.MAX_VALUE) {
			throw new BusinessException(ErrorCode.LISTING_QTY_INVALID, "수량 합계가 허용 범위를 넘었습니다.");
		}
		return (int) sum;
	}
}
