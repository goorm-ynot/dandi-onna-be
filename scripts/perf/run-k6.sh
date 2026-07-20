#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/perf/load-env.sh"
load_perf_env

SCENARIO="${1:-}"
PROFILE="${2:-smoke}"
APP_PORT="${SERVER_PORT:-18080}"

if [[ -z "${SCENARIO}" ]]; then
  echo "usage: ./scripts/perf/run-k6.sh <scenario> [smoke|measure]" >&2
  exit 1
fi

SCRIPT_PATH="scripts/perf/scenarios/${SCENARIO}.js"
if [[ ! -f "${ROOT_DIR}/${SCRIPT_PATH}" ]]; then
  echo "unknown scenario: ${SCENARIO}" >&2
  exit 1
fi

case "${PROFILE}" in
  smoke|measure) ;;
  *)
    echo "invalid profile: ${PROFILE} (expected: smoke or measure)" >&2
    exit 1
    ;;
esac

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RESULT_REL="artifacts/perf/${TIMESTAMP}/${SCENARIO}/${PROFILE}"
RESULT_DIR="${ROOT_DIR}/${RESULT_REL}"
mkdir -p "${RESULT_DIR}"

echo "Running k6 scenario=${SCENARIO} profile=${PROFILE}"
echo "Results will be written to ${RESULT_DIR}"

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  --user "$(id -u):$(id -g)" \
  -v "${ROOT_DIR}:/work" \
  -w /work \
  -e BASE_URL="${BASE_URL:-http://host.docker.internal:${APP_PORT}}" \
  -e PERF_PROFILE="${PROFILE}" \
  -e PERF_RESULT_DIR="${RESULT_REL}" \
  -e PERF_DATASET_SIZE="${PERF_DATASET_SIZE:-small}" \
  -e PERF_LAT="${PERF_LAT:-37.3940}" \
  -e PERF_LON="${PERF_LON:-127.1100}" \
  -e CONSUMER_LOGIN_ID="${CONSUMER_LOGIN_ID:-Customer1}" \
  -e CONSUMER_PASSWORD="${CONSUMER_PASSWORD:-111111}" \
  -e OWNER_LOGIN_ID="${OWNER_LOGIN_ID:-PERF_OWNER_MAIN}" \
  -e OWNER_PASSWORD="${OWNER_PASSWORD:-111111}" \
  -e OWNER_FALLBACK_LOGIN_ID="${OWNER_FALLBACK_LOGIN_ID:-CEO1}" \
  -e OWNER_FALLBACK_PASSWORD="${OWNER_FALLBACK_PASSWORD:-111111}" \
  -e PERF_HOME_STORE_PAGE_SIZE="${PERF_HOME_STORE_PAGE_SIZE:-20}" \
  -e PERF_STORE_POST_PAGE_SIZE="${PERF_STORE_POST_PAGE_SIZE:-20}" \
  -e PERF_OWNER_MENU_PAGE_SIZE="${PERF_OWNER_MENU_PAGE_SIZE:-20}" \
  -e PERF_SALES_WINDOW_DAYS="${PERF_SALES_WINDOW_DAYS:-14}" \
  -e PERF_SMOKE_ITERATIONS="${PERF_SMOKE_ITERATIONS:-1}" \
  -e PERF_MEASURE_READ_VUS="${PERF_MEASURE_READ_VUS:-5}" \
  -e PERF_MEASURE_READ_DURATION="${PERF_MEASURE_READ_DURATION:-30s}" \
  -e PERF_MEASURE_WRITE_VUS="${PERF_MEASURE_WRITE_VUS:-1}" \
  -e PERF_MEASURE_WRITE_ITERATIONS="${PERF_MEASURE_WRITE_ITERATIONS:-10}" \
  -e PERF_MEASURE_FLOW_ITERATIONS="${PERF_MEASURE_FLOW_ITERATIONS:-3}" \
  -e PERF_POLL_INTERVAL_MS="${PERF_POLL_INTERVAL_MS:-1000}" \
  -e PERF_POLL_TIMEOUT_MS="${PERF_POLL_TIMEOUT_MS:-120000}" \
  grafana/k6:latest run "${SCRIPT_PATH}"
