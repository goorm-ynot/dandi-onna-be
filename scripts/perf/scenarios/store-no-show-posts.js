import { Trend } from 'k6/metrics';

import { loginConsumer } from '../lib/auth.js';
import { authGet } from '../lib/api.js';
import { discoverStoreContext } from '../lib/discovery.js';
import { buildReadOptions, PERF_ENV } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const storeNoShowPostsDuration = new Trend('store_no_show_posts_duration_ms');

export const options = buildReadOptions('store-no-show-posts', {
	store_no_show_posts_duration_ms: ['p(95)<3000'],
});

export function setup() {
	const consumerToken = loginConsumer().data.accessToken;
	const storeContext = discoverStoreContext(consumerToken);
	return {
		consumerToken,
		storeId: storeContext.store.storeId,
	};
}

export default function(data) {
	const { response } = authGet(
		`/api/v1/stores/${data.storeId}/no-show-posts?page=0&size=${PERF_ENV.storePostPageSize}`,
		data.consumerToken,
		'store no-show posts'
	);
	storeNoShowPostsDuration.add(response.timings.duration);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'store-no-show-posts',
	title: 'GET /api/v1/stores/{storeId}/no-show-posts',
	endpoints: ['GET /api/v1/stores/{storeId}/no-show-posts'],
	customMetrics: ['store_no_show_posts_duration_ms'],
	notes: [
		'매장 상세 조회에는 매장 이미지, 메뉴 이미지 presign 과 즐겨찾기 여부 조회가 포함됩니다.',
	],
});
