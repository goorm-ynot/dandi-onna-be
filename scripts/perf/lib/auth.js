import http from 'k6/http';

import { PERF_ENV } from './env.js';
import { expectApiSuccess, resolveUrl } from './api.js';

function login(loginId, password, label) {
	const response = http.post(
		resolveUrl('/api/v1/auth/login'),
		JSON.stringify({ loginId, password }),
		{
			headers: {
				'Content-Type': 'application/json',
			},
			tags: {
				action: 'login',
				login_as: label,
			},
		}
	);

	return {
		response,
		data: expectApiSuccess(response, `${label} login`, 200),
	};
}

export function loginConsumer() {
	return login(PERF_ENV.consumerLoginId, PERF_ENV.consumerPassword, 'consumer');
}

export function loginOwner() {
	try {
		return login(PERF_ENV.ownerLoginId, PERF_ENV.ownerPassword, 'owner-primary');
	} catch (error) {
		if (!PERF_ENV.ownerFallbackLoginId) {
			throw error;
		}
		console.warn(
			`owner-primary 로그인 실패로 fallback 계정(${PERF_ENV.ownerFallbackLoginId})을 사용합니다: ${error.message}`
		);
		return login(PERF_ENV.ownerFallbackLoginId, PERF_ENV.ownerFallbackPassword, 'owner-fallback');
	}
}

export function bearerTagParams(token, extraTags = {}) {
	return {
		headers: {
			Authorization: `Bearer ${token}`,
		},
		tags: extraTags,
	};
}
