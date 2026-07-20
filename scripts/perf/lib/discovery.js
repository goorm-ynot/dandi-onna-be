import { fail } from 'k6';

import { PERF_ENV } from './env.js';
import { authGet, roundToTen } from './api.js';

export function loadHomeStores(token, size = PERF_ENV.homeStorePageSize) {
	return authGet(
		`/api/v1/home/stores?lat=${PERF_ENV.lat}&lon=${PERF_ENV.lon}&page=0&size=${size}`,
		token,
		'home stores discovery',
		{
			tags: {
				action: 'discover-home-stores',
			},
		}
	).data;
}

export function selectTargetStore(storesResponse) {
	const stores = storesResponse?.stores || [];
	if (stores.length === 0) {
		fail('주변 매장 조회 결과가 비어 있습니다. perf seed 또는 기본 데이터 상태를 확인하세요.');
	}
	const perfMainStore = stores.find((store) => String(store.name || '').startsWith('PERF Main Store'));
	return perfMainStore || stores[0];
}

export function loadStorePosts(token, storeId, size = PERF_ENV.storePostPageSize) {
	return authGet(
		`/api/v1/stores/${storeId}/no-show-posts?page=0&size=${size}`,
		token,
		'store posts discovery',
		{
			tags: {
				action: 'discover-store-posts',
			},
		}
	).data;
}

export function discoverStoreContext(token) {
	const storesResponse = loadHomeStores(token);
	const store = selectTargetStore(storesResponse);
	const postsResponse = loadStorePosts(token, store.storeId);
	if (!postsResponse?.posts || postsResponse.posts.length === 0) {
		fail(`대상 매장(${store.storeId})에 주문 가능한 노쇼 글이 없습니다.`);
	}
	return {
		store,
		posts: postsResponse.posts,
		storeResponse: postsResponse,
	};
}

export function buildOrderPayload(storeId, posts) {
	const groups = new Map();
	for (const post of posts) {
		if (!post.expireAt || post.qtyRemaining <= 0) {
			continue;
		}
		if (!groups.has(post.expireAt)) {
			groups.set(post.expireAt, []);
		}
		groups.get(post.expireAt).push(post);
	}

	const sortedGroups = Array.from(groups.entries()).sort((left, right) => right[1].length - left[1].length);
	const selectedGroup = sortedGroups.find((entry) => entry[1].length >= 2) || sortedGroups[0];
	if (!selectedGroup) {
		fail('주문 payload 생성을 위한 주문 가능 글이 없습니다.');
	}

	const [visitTime, groupedPosts] = selectedGroup;
	const alignedPosts = groupedPosts.slice(0, 2);

	const items = alignedPosts.map((post) => ({
		noShowPostId: post.postId,
		menuName: post.menuName,
		quantity: 1,
		originalPrice: post.originalPrice,
		discountRate: post.discountPercent,
	}));

	const originalAmount = items.reduce((sum, item) => sum + item.originalPrice * item.quantity, 0);
	const discountedAmount = alignedPosts.reduce(
		(sum, post) => sum + post.discountedPrice,
		0
	);
	const totalAmount = roundToTen(discountedAmount);
	const appliedDiscountAmount = roundToTen(originalAmount) - totalAmount;

	return {
		storeId,
		visitTime,
		paymentMethod: 'CARD',
		totalAmount,
		appliedDiscountAmount,
		items,
	};
}

export function loadOwnerMenus(token, size = PERF_ENV.ownerMenuPageSize) {
	return authGet(
		`/api/v1/owner/menus?page=0&size=${size}`,
		token,
		'owner menus discovery',
		{
			tags: {
				action: 'discover-owner-menus',
			},
		}
	).data;
}

export function selectOwnerSingleMenus(menuPage, count = 2) {
	const items = menuPage?.content || [];
	const sellableSingles = items.filter(
		(menu) => menu.type === 'single' && menu.effectiveStatus === 'on_sale'
	);
	if (sellableSingles.length < count) {
		fail(`판매 가능한 단품 메뉴가 ${count}개 이상 필요합니다.`);
	}
	return sellableSingles.slice(0, count);
}
