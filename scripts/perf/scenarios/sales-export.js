import exec from 'k6/execution';
import { Trend } from 'k6/metrics';
import { fail, sleep } from 'k6';

import { loginOwner } from '../lib/auth.js';
import { authGet, authPost, kstDaysAgo } from '../lib/api.js';
import { buildFlowOptions, PERF_ENV } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const exportAckDuration = new Trend('export_ack_ms');
const exportDoneDuration = new Trend('export_done_ms');

export const options = buildFlowOptions('sales-export', {
	export_ack_ms: ['p(95)<5000'],
	export_done_ms: ['p(95)<60000'],
});

function buildRequestRange() {
	const iteration = exec.scenario.iterationInTest;
	const shift = iteration % Math.max(PERF_ENV.salesWindowDays - 6, 1);
	const endDaysAgo = shift + 1;
	const startDaysAgo = endDaysAgo + 6;

	return {
		startDate: kstDaysAgo(startDaysAgo),
		endDate: kstDaysAgo(endDaysAgo),
		includeDetail: iteration % 2 === 0,
	};
}

function waitUntilDone(token, jobId) {
	const startedAt = Date.now();
	while (Date.now() - startedAt <= PERF_ENV.pollTimeoutMs) {
		const { data } = authGet(
			`/api/v1/owner/sales/export/${jobId}`,
			token,
			'export status'
		);
		if (data.status === 'DONE') {
			if (!data.downloadUrl) {
				fail(`export status DONE 이지만 downloadUrl 이 비어 있습니다. jobId=${jobId}`);
			}
			return Date.now() - startedAt;
		}
		if (data.status === 'FAILED' || data.status === 'EXPIRED') {
			fail(`export 작업이 실패 상태로 종료되었습니다. status=${data.status} jobId=${jobId}`);
		}
		sleep(PERF_ENV.pollIntervalMs / 1000);
	}
	fail(`export polling timeout: ${PERF_ENV.pollTimeoutMs}ms 내에 DONE 이 되지 않았습니다.`);
}

export function setup() {
	return {
		ownerToken: loginOwner().data.accessToken,
	};
}

export default function(data) {
	const payload = buildRequestRange();
	const { response, data: createData } = authPost(
		'/api/v1/owner/sales/export',
		data.ownerToken,
		payload,
		'create sales export'
	);

	exportAckDuration.add(response.timings.duration);
	const jobId = createData.jobId;
	if (!jobId) {
		fail('export create 응답에 jobId 가 없습니다.');
	}

	const doneMs = waitUntilDone(data.ownerToken, jobId);
	exportDoneDuration.add(doneMs);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'sales-export',
	title: 'POST /api/v1/owner/sales/export + GET /{jobId}',
	endpoints: [
		'POST /api/v1/owner/sales/export',
		'GET /api/v1/owner/sales/export/{jobId}',
	],
	customMetrics: ['export_ack_ms', 'export_done_ms'],
	notes: [
		'ACK 시간은 export 요청 응답까지, DONE 시간은 job 상태가 DONE 이 될 때까지의 전체 시간입니다.',
		'iteration 별로 날짜 범위를 이동시켜 동일 request hash 재사용을 피합니다.',
	],
});
