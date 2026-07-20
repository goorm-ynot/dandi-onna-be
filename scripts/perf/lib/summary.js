import { PERF_ENV } from './env.js';

function metricValue(metric, key) {
	if (!metric || !metric.values) {
		return null;
	}
	return metric.values[key] ?? null;
}

function formatNumber(value, digits = 2) {
	if (value === null || value === undefined || Number.isNaN(value)) {
		return '-';
	}
	return Number(value).toFixed(digits);
}

function formatPercent(rate) {
	if (rate === null || rate === undefined) {
		return '-';
	}
	return `${formatNumber(rate * 100)}%`;
}

function formatMetricRow(name, metric) {
	if (!metric) {
		return `| ${name} | - | - | - | - |`;
	}
	return `| ${name} | ${formatNumber(metricValue(metric, 'avg'))} | ${formatNumber(metricValue(metric, 'p(95)'))} | ${formatNumber(metricValue(metric, 'p(99)'))} | ${formatNumber(metricValue(metric, 'max'))} |`;
}

export function buildSummaryHandler({
	scenario,
	title,
	endpoints,
	customMetrics = [],
	notes = [],
}) {
	return function handleSummary(data) {
		const httpDuration = data.metrics.http_req_duration;
		const httpFailed = data.metrics.http_req_failed;

		const payload = {
			scenario,
			title,
			profile: PERF_ENV.profile,
			datasetSize: PERF_ENV.datasetSize,
			baseUrl: PERF_ENV.baseUrl,
			resultDir: PERF_ENV.resultDir,
			endpoints,
			metrics: data.metrics,
		};

		const markdown = [
			`# ${title}`,
			'',
			'## 실행 정보',
			'',
			`- scenario: \`${scenario}\``,
			`- profile: \`${PERF_ENV.profile}\``,
			`- dataset: \`${PERF_ENV.datasetSize}\``,
			`- baseUrl: \`${PERF_ENV.baseUrl}\``,
			`- endpoints: ${endpoints.join(', ')}`,
			'',
			'## HTTP 요약',
			'',
			`- 실패율: ${formatPercent(metricValue(httpFailed, 'rate'))}`,
			'',
			'| metric | avg(ms) | p95(ms) | p99(ms) | max(ms) |',
			'| --- | ---: | ---: | ---: | ---: |',
			formatMetricRow('http_req_duration', httpDuration),
		];

		if (customMetrics.length > 0) {
			markdown.push('', '## 커스텀 지표', '', '| metric | avg(ms) | p95(ms) | p99(ms) | max(ms) |', '| --- | ---: | ---: | ---: | ---: |');
			for (const metricName of customMetrics) {
				markdown.push(formatMetricRow(metricName, data.metrics[metricName]));
			}
		}

		if (notes.length > 0) {
			markdown.push('', '## 메모', '');
			for (const note of notes) {
				markdown.push(`- ${note}`);
			}
		}

		markdown.push('');

		return {
			[`${PERF_ENV.resultDir}/summary.json`]: JSON.stringify(payload, null, 2),
			[`${PERF_ENV.resultDir}/summary.md`]: markdown.join('\n'),
			stdout: `${title} avg=${formatNumber(metricValue(httpDuration, 'avg'))}ms p95=${formatNumber(metricValue(httpDuration, 'p(95)'))}ms fail=${formatPercent(metricValue(httpFailed, 'rate'))}\n`,
		};
	};
}
