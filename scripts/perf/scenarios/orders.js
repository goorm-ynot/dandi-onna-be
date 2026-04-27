import { Trend } from 'k6/metrics';

import { loginConsumer } from '../lib/auth.js';
import { authPost } from '../lib/api.js';
import { discoverStoreContext, buildOrderPayload } from '../lib/discovery.js';
import { buildWriteOptions } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const orderCreateDuration = new Trend('order_create_duration_ms');

export const options = buildWriteOptions('orders', {
	order_create_duration_ms: ['p(95)<5000'],
});

export function setup() {
	const consumerToken = loginConsumer().data.accessToken;
	const storeContext = discoverStoreContext(consumerToken);

	return {
		consumerToken,
		orderPayload: buildOrderPayload(storeContext.store.storeId, storeContext.posts),
		targetStoreName: storeContext.store.name,
	};
}

export default function(data) {
	const { response } = authPost(
		'/api/v1/orders',
		data.consumerToken,
		data.orderPayload,
		'create order'
	);
	orderCreateDuration.add(response.timings.duration);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'orders',
	title: 'POST /api/v1/orders',
	endpoints: ['POST /api/v1/orders'],
	customMetrics: ['order_create_duration_ms'],
	notes: [
		'동일한 visitTime 을 가진 노쇼 글을 사용해 주문 payload 를 생성합니다.',
		'주문 생성에는 재고 검증, 주문 저장, 주문 아이템 저장, 알림 enqueue 비용이 포함됩니다.',
	],
});
