import { fail } from 'k6';

function readString(name, fallback) {
	const value = __ENV[name];
	return value !== undefined && value !== null && value !== '' ? value : fallback;
}

function readInt(name, fallback) {
	const value = readString(name, String(fallback));
	const parsed = Number.parseInt(value, 10);
	if (Number.isNaN(parsed)) {
		fail(`환경변수 ${name} 값이 정수가 아닙니다: ${value}`);
	}
	return parsed;
}

function readFloat(name, fallback) {
	const value = readString(name, String(fallback));
	const parsed = Number.parseFloat(value);
	if (Number.isNaN(parsed)) {
		fail(`환경변수 ${name} 값이 숫자가 아닙니다: ${value}`);
	}
	return parsed;
}

function baseTags(scenario) {
	return {
		scenario,
		profile: PERF_ENV.profile,
		dataset: PERF_ENV.datasetSize,
	};
}

function baseThresholds(extraThresholds) {
	if (PERF_ENV.profile !== 'measure') {
		return {
			http_req_failed: ['rate<0.05'],
		};
	}
	return {
		http_req_failed: ['rate<0.05'],
		...extraThresholds,
	};
}

function buildScenarioOptions(scenario, smokeExecutor, measureExecutor, extraThresholds) {
	return {
		discardResponseBodies: false,
		summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
		scenarios: {
			[scenario]: {
				exec: 'default',
				tags: baseTags(scenario),
				...(PERF_ENV.profile === 'measure' ? measureExecutor : smokeExecutor),
			},
		},
		thresholds: baseThresholds(extraThresholds),
	};
}

export const PERF_ENV = Object.freeze({
	baseUrl: readString('BASE_URL', 'http://host.docker.internal:18080'),
	profile: readString('PERF_PROFILE', 'smoke'),
	datasetSize: readString('PERF_DATASET_SIZE', 'small'),
	resultDir: readString('PERF_RESULT_DIR', 'artifacts/perf/manual'),
	lat: readFloat('PERF_LAT', 37.3940),
	lon: readFloat('PERF_LON', 127.1100),
	consumerLoginId: readString('CONSUMER_LOGIN_ID', 'Customer1'),
	consumerPassword: readString('CONSUMER_PASSWORD', '111111'),
	ownerLoginId: readString('OWNER_LOGIN_ID', 'PERF_OWNER_MAIN'),
	ownerPassword: readString('OWNER_PASSWORD', '111111'),
	ownerFallbackLoginId: readString('OWNER_FALLBACK_LOGIN_ID', 'CEO1'),
	ownerFallbackPassword: readString('OWNER_FALLBACK_PASSWORD', '111111'),
	homeStorePageSize: readInt('PERF_HOME_STORE_PAGE_SIZE', 20),
	storePostPageSize: readInt('PERF_STORE_POST_PAGE_SIZE', 20),
	ownerMenuPageSize: readInt('PERF_OWNER_MENU_PAGE_SIZE', 20),
	salesWindowDays: readInt('PERF_SALES_WINDOW_DAYS', 14),
	smokeIterations: readInt('PERF_SMOKE_ITERATIONS', 1),
	measureReadVus: readInt('PERF_MEASURE_READ_VUS', 5),
	measureReadDuration: readString('PERF_MEASURE_READ_DURATION', '30s'),
	measureWriteVus: readInt('PERF_MEASURE_WRITE_VUS', 1),
	measureWriteIterations: readInt('PERF_MEASURE_WRITE_ITERATIONS', 10),
	measureFlowIterations: readInt('PERF_MEASURE_FLOW_ITERATIONS', 3),
	pollIntervalMs: readInt('PERF_POLL_INTERVAL_MS', 1000),
	pollTimeoutMs: readInt('PERF_POLL_TIMEOUT_MS', 120000),
});

export function buildReadOptions(scenario, extraThresholds = {}) {
	return buildScenarioOptions(
		scenario,
		{
			executor: 'per-vu-iterations',
			vus: 1,
			iterations: PERF_ENV.smokeIterations,
			maxDuration: '1m',
		},
		{
			executor: 'constant-vus',
			vus: PERF_ENV.measureReadVus,
			duration: PERF_ENV.measureReadDuration,
		},
		extraThresholds
	);
}

export function buildWriteOptions(scenario, extraThresholds = {}) {
	return buildScenarioOptions(
		scenario,
		{
			executor: 'per-vu-iterations',
			vus: 1,
			iterations: 1,
			maxDuration: '1m',
		},
		{
			executor: 'per-vu-iterations',
			vus: PERF_ENV.measureWriteVus,
			iterations: PERF_ENV.measureWriteIterations,
			maxDuration: '10m',
		},
		extraThresholds
	);
}

export function buildFlowOptions(scenario, extraThresholds = {}) {
	return buildScenarioOptions(
		scenario,
		{
			executor: 'per-vu-iterations',
			vus: 1,
			iterations: 1,
			maxDuration: '5m',
		},
		{
			executor: 'per-vu-iterations',
			vus: 1,
			iterations: PERF_ENV.measureFlowIterations,
			maxDuration: '20m',
		},
		extraThresholds
	);
}
