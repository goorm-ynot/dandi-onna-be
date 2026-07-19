import { Trend } from 'k6/metrics';

import { loginConsumer } from '../lib/auth.js';
import { authGet } from '../lib/api.js';
import { buildReadOptions, PERF_ENV } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const homeStoresDuration = new Trend('home_stores_duration_ms');

export const options = buildReadOptions('home-stores', {
	home_stores_duration_ms: ['p(95)<3000'],
});

export function setup() {
	return {
		consumerToken: loginConsumer().data.accessToken,
	};
}

export default function(data) {
	const { response } = authGet(
		`/api/v1/home/stores?lat=${PERF_ENV.lat}&lon=${PERF_ENV.lon}&page=0&size=${PERF_ENV.homeStorePageSize}`,
		data.consumerToken,
		'home stores'
	);
	homeStoresDuration.add(response.timings.duration);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'home-stores',
	title: 'GET /api/v1/home/stores',
	endpoints: ['GET /api/v1/home/stores'],
	customMetrics: ['home_stores_duration_ms'],
	notes: [
		`기준 좌표는 lat=${PERF_ENV.lat}, lon=${PERF_ENV.lon} 입니다.`,
		'조회 결과에는 PostGIS 거리 정렬과 매장 이미지 presign URL 생성 비용이 포함됩니다.',
	],
});
