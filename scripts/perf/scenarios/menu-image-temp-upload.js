import http from 'k6/http';
import { Trend } from 'k6/metrics';
import { fail } from 'k6';

import { loginOwner } from '../lib/auth.js';
import { authPost, extractHeader } from '../lib/api.js';
import { buildFlowOptions } from '../lib/env.js';
import { buildSummaryHandler } from '../lib/summary.js';

const presignDuration = new Trend('menu_image_presign_ms');
const uploadDuration = new Trend('menu_image_put_ms');
const confirmDuration = new Trend('menu_image_confirm_ms');
const totalDuration = new Trend('menu_image_e2e_ms');

const FILE_BYTES = open('../fixtures/menu-image.svg', 'b');
const FILE_NAME = 'perf-menu-image.svg';
const FILE_TYPE = 'image/svg+xml';

export const options = buildFlowOptions('menu-image-temp-upload', {
	menu_image_presign_ms: ['p(95)<5000'],
	menu_image_confirm_ms: ['p(95)<5000'],
	menu_image_e2e_ms: ['p(95)<10000'],
});

export function setup() {
	return {
		ownerToken: loginOwner().data.accessToken,
	};
}

export default function(data) {
	const startedAt = Date.now();
	const { response: presignResponse, data: presignData } = authPost(
		'/api/v1/owner/menu-images/temp/presign',
		data.ownerToken,
		{
			fileName: FILE_NAME,
			contentType: FILE_TYPE,
		},
		'menu image temp presign'
	);
	presignDuration.add(presignResponse.timings.duration);

	const uploadResponse = http.put(presignData.url, FILE_BYTES, {
		headers: {
			'Content-Type': FILE_TYPE,
		},
		tags: {
			action: 'menu-image-put',
		},
	});
	if (uploadResponse.status < 200 || uploadResponse.status >= 300) {
		fail(`menu image direct PUT 실패: status=${uploadResponse.status}`);
	}
	uploadDuration.add(uploadResponse.timings.duration);

	const etag = extractHeader(uploadResponse, 'ETag', 'menu image direct PUT').replaceAll('"', '');
	const { response: confirmResponse, data: confirmData } = authPost(
		'/api/v1/owner/menu-images/temp/confirm',
		data.ownerToken,
		{
			uploadToken: presignData.uploadToken,
			etag,
		},
		'menu image temp confirm'
	);
	if (confirmData.confirmed !== true) {
		fail('menu image confirm 응답이 confirmed=true 가 아닙니다.');
	}
	confirmDuration.add(confirmResponse.timings.duration);
	totalDuration.add(Date.now() - startedAt);
}

export const handleSummary = buildSummaryHandler({
	scenario: 'menu-image-temp-upload',
	title: 'POST /presign + direct PUT + POST /confirm',
	endpoints: [
		'POST /api/v1/owner/menu-images/temp/presign',
		'PUT {presigned URL}',
		'POST /api/v1/owner/menu-images/temp/confirm',
	],
	customMetrics: [
		'menu_image_presign_ms',
		'menu_image_put_ms',
		'menu_image_confirm_ms',
		'menu_image_e2e_ms',
	],
	notes: [
		'presign 응답, MinIO direct PUT, confirm 응답을 모두 포함한 end-to-end 업로드 흐름입니다.',
	],
});
