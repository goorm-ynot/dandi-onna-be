import http from 'k6/http';
import { check, fail } from 'k6';

import { PERF_ENV } from './env.js';

function allowedStatuses(expectedStatus) {
	return Array.isArray(expectedStatus) ? expectedStatus : [expectedStatus];
}

function mergeParams(token, params = {}, jsonBody = true) {
	const headers = { ...(params.headers || {}) };
	if (jsonBody && !headers['Content-Type']) {
		headers['Content-Type'] = 'application/json';
	}
	if (token) {
		headers.Authorization = `Bearer ${token}`;
	}
	return {
		...params,
		headers,
	};
}

export function resolveUrl(pathOrUrl) {
	if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
		return pathOrUrl;
	}
	return `${PERF_ENV.baseUrl}${pathOrUrl}`;
}

export function parseJson(response, label) {
	try {
		return response.json();
	} catch (error) {
		fail(`${label}: JSON 파싱 실패 (${error.message})`);
	}
}

export function expectApiSuccess(response, label, expectedStatus = 200) {
	const statuses = allowedStatuses(expectedStatus);
	check(response, {
		[`${label} status`]: (r) => statuses.includes(r.status),
	}) || fail(`${label}: 예상 상태코드 ${statuses.join(',')} / 실제 ${response.status}`);

	const body = parseJson(response, label);
	if (!body || body.success !== true) {
		fail(`${label}: API 응답이 success=true 가 아닙니다. body=${JSON.stringify(body)}`);
	}
	return body.data;
}

export function authGet(pathOrUrl, token, label, params = {}, expectedStatus = 200) {
	const response = http.get(resolveUrl(pathOrUrl), mergeParams(token, params, false));
	return {
		response,
		data: expectApiSuccess(response, label, expectedStatus),
	};
}

export function authPost(pathOrUrl, token, payload, label, params = {}, expectedStatus = 200) {
	const response = http.post(
		resolveUrl(pathOrUrl),
		JSON.stringify(payload),
		mergeParams(token, params, true)
	);
	return {
		response,
		data: expectApiSuccess(response, label, expectedStatus),
	};
}

export function authDelete(pathOrUrl, token, label, params = {}, expectedStatus = 200) {
	const response = http.del(resolveUrl(pathOrUrl), null, mergeParams(token, params, false));
	return {
		response,
		data: expectApiSuccess(response, label, expectedStatus),
	};
}

export function extractHeader(response, headerName, label) {
	const value = response.headers[headerName] || response.headers[headerName.toLowerCase()];
	if (!value) {
		fail(`${label}: ${headerName} 헤더가 없습니다.`);
	}
	return value;
}

export function formatKstDate(date) {
	const shifted = new Date(date.getTime() + 9 * 60 * 60 * 1000);
	return shifted.toISOString().slice(0, 10);
}

export function kstDaysAgo(daysAgo) {
	return formatKstDate(new Date(Date.now() - daysAgo * 24 * 60 * 60 * 1000));
}

export function roundToTen(amount) {
	const remainder = amount % 10;
	if (remainder === 0) {
		return amount;
	}
	if (remainder >= 5) {
		return amount + (10 - remainder);
	}
	return amount - remainder;
}
