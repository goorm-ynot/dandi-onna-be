import { Trend } from 'k6/metrics';
import { fail, sleep } from 'k6';

import { loginOwner } from '../lib/auth.js';
import { authGet, authPost } from '../lib/api.js';
import { loadOwnerMenus, selectOwnerSingleMenus } from '../lib/discovery.js';
import { buildFlowOptions, PERF_ENV } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const scheduleCreateDuration = new Trend('schedule_create_ms');
const schedulePublishLag = new Trend('schedule_publish_lag_ms');

export const options = buildFlowOptions('no-show-schedule', {
	schedule_create_ms: ['p(95)<5000'],
	schedule_publish_lag_ms: ['p(95)<60000'],
});

function waitUntilPublished(token, scheduleId) {
	const startedAt = Date.now();
	while (Date.now() - startedAt <= PERF_ENV.pollTimeoutMs) {
		const { data } = authGet(
			`/api/v1/owner/no-show-post-schedules/${scheduleId}`,
			token,
			'schedule detail'
		);

		if (data.status === 'PUBLISHED') {
			if (!data.publishedAt || !data.requestedAt) {
				fail(`schedule ${scheduleId} 는 PUBLISHED 이지만 publishedAt/requestedAt 이 비어 있습니다.`);
			}
			return Date.parse(data.publishedAt) - Date.parse(data.requestedAt);
		}
		if (data.status === 'FAILED' || data.status === 'CANCELLED') {
			fail(`schedule ${scheduleId} 가 ${data.status} 상태로 종료되었습니다.`);
		}
		sleep(PERF_ENV.pollIntervalMs / 1000);
	}
	fail(`schedule ${scheduleId} polling timeout`);
}

export function setup() {
	const ownerToken = loginOwner().data.accessToken;
	const menuPage = loadOwnerMenus(ownerToken, 50);
	const singleMenus = selectOwnerSingleMenus(menuPage, 2);

	return {
		ownerToken,
		menuIds: singleMenus.map((menu) => menu.id),
	};
}

export default function(data) {
	const payload = {
		items: data.menuIds.map((menuId) => ({
			menuId,
			quantity: 1,
		})),
	};

	const { response, data: createData } = authPost(
		'/api/v1/owner/no-show-post-schedules',
		data.ownerToken,
		payload,
		'create no-show schedule',
		{},
		201
	);
	scheduleCreateDuration.add(response.timings.duration);
	if (!createData.scheduleId) {
		fail('schedule create 응답에 scheduleId 가 없습니다.');
	}

	const lagMs = waitUntilPublished(data.ownerToken, createData.scheduleId);
	schedulePublishLag.add(lagMs);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'no-show-schedule',
	title: 'POST /api/v1/owner/no-show-post-schedules + publish lag',
	endpoints: [
		'POST /api/v1/owner/no-show-post-schedules',
		'GET /api/v1/owner/no-show-post-schedules/{scheduleId}',
	],
	customMetrics: ['schedule_create_ms', 'schedule_publish_lag_ms'],
	notes: [
		'기본 프리셋의 saleDelayMinutes 를 0으로 둔 perf seed 를 전제로 publish lag 를 측정합니다.',
	],
});
