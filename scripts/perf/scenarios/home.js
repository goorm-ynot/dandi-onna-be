import { Trend } from 'k6/metrics';

import { loginConsumer } from '../lib/auth.js';
import { authGet } from '../lib/api.js';
import { buildReadOptions } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const homeDuration = new Trend('home_duration_ms');

export const options = buildReadOptions('home', {
	home_duration_ms: ['p(95)<3000'],
});

export function setup() {
	return {
		consumerToken: loginConsumer().data.accessToken,
	};
}

export default function(data) {
	const { response } = authGet('/api/v1/home', data.consumerToken, 'home');
	homeDuration.add(response.timings.duration);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'home',
	title: 'GET /api/v1/home',
	endpoints: ['GET /api/v1/home'],
	customMetrics: ['home_duration_ms'],
	notes: [
		'홈 조회에는 오늘 주문 3건 우선 조회와 매장 이미지 presign 비용이 포함됩니다.',
	],
});
