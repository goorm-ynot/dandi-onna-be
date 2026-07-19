import { Trend } from 'k6/metrics';

import { loginOwner } from '../lib/auth.js';
import { authGet, kstDaysAgo } from '../lib/api.js';
import { buildReadOptions, PERF_ENV } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const ownerSalesDuration = new Trend('owner_sales_duration_ms');

export const options = buildReadOptions('owner-sales', {
	owner_sales_duration_ms: ['p(95)<3000'],
});

export function setup() {
	return {
		ownerToken: loginOwner().data.accessToken,
	};
}

export default function(data) {
	const startDate = kstDaysAgo(PERF_ENV.salesWindowDays);
	const endDate = kstDaysAgo(1);
	const { response } = authGet(
		`/api/v1/owner/sales?startDate=${startDate}&endDate=${endDate}&page=0&size=${PERF_ENV.ownerMenuPageSize}`,
		data.ownerToken,
		'owner sales'
	);
	ownerSalesDuration.add(response.timings.duration);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'owner-sales',
	title: 'GET /api/v1/owner/sales',
	endpoints: ['GET /api/v1/owner/sales'],
	customMetrics: ['owner_sales_duration_ms'],
	notes: [
		'owner sales 조회는 yyyy-MM-dd 날짜 입력과 페이지 기반 조회를 전제로 측정합니다.',
	],
});
