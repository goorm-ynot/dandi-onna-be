import { Trend } from 'k6/metrics';

import { loginOwner } from '../lib/auth.js';
import { authGet } from '../lib/api.js';
import { buildReadOptions, PERF_ENV } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const ownerMenusDuration = new Trend('owner_menus_duration_ms');

export const options = buildReadOptions('owner-menus', {
	owner_menus_duration_ms: ['p(95)<3000'],
});

export function setup() {
	return {
		ownerToken: loginOwner().data.accessToken,
	};
}

export default function(data) {
	const { response } = authGet(
		`/api/v1/owner/menus?page=0&size=${PERF_ENV.ownerMenuPageSize}`,
		data.ownerToken,
		'owner menus'
	);
	ownerMenusDuration.add(response.timings.duration);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'owner-menus',
	title: 'GET /api/v1/owner/menus',
	endpoints: ['GET /api/v1/owner/menus'],
	customMetrics: ['owner_menus_duration_ms'],
	notes: [
		'메뉴 목록 조회에는 세트 구성 계산과 메뉴 이미지 presign URL 생성 비용이 포함됩니다.',
	],
});
