import { Trend } from 'k6/metrics';
import { fail, sleep } from 'k6';

import { loginConsumer } from '../lib/auth.js';
import { discoverStoreContext, loadStorePosts } from '../lib/discovery.js';
import { buildFlowOptions, PERF_ENV } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const postExpiryLag = new Trend('post_expiry_lag_ms');

export const options = buildFlowOptions('post-expiry', {
	post_expiry_lag_ms: ['p(95)<120000'],
});

export function setup() {
	const consumerToken = loginConsumer().data.accessToken;
	const storeContext = discoverStoreContext(consumerToken);
	const expiryProbe = storeContext.posts.find((post) => post.menuName === 'PERF Expiry Probe');

	if (!expiryProbe) {
		fail('PERF Expiry Probe 게시글을 찾지 못했습니다. perf seed 를 다시 적용하세요.');
	}

	return {
		consumerToken,
		storeId: storeContext.store.storeId,
		expiryProbePostId: expiryProbe.postId,
		expireAt: expiryProbe.expireAt,
	};
}

export default function(data) {
	const expireAtMs = Date.parse(data.expireAt);
	const startedAt = Date.now();

	while (Date.now() - startedAt <= PERF_ENV.pollTimeoutMs) {
		const storePosts = loadStorePosts(data.consumerToken, data.storeId, PERF_ENV.storePostPageSize);
		const stillVisible = (storePosts.posts || []).some((post) => post.postId === data.expiryProbePostId);
		if (!stillVisible) {
			postExpiryLag.add(Date.now() - expireAtMs);
			return;
		}
		sleep(PERF_ENV.pollIntervalMs / 1000);
	}

	fail('PostExpiryScheduler 결과를 poll timeout 내에 확인하지 못했습니다.');
}

export const handleSummary = buildSummaryHandler({
	scenario: 'post-expiry',
	title: 'PostExpiryScheduler publish lag proxy',
	endpoints: ['GET /api/v1/stores/{storeId}/no-show-posts'],
	customMetrics: ['post_expiry_lag_ms'],
	notes: [
		'PERF Expiry Probe 게시글이 소비자 목록에서 사라질 때까지의 지연을 PostExpiryScheduler 반영 시간의 proxy 로 기록합니다.',
	],
});
