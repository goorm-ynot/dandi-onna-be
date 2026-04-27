#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/perf/load-env.sh"
load_perf_env

APP_PORT="${SERVER_PORT:-18080}"
BASE_URL_VALUE="${BASE_URL:-http://host.docker.internal:${APP_PORT}}"
MINIO_ENDPOINT_VALUE="${MINIO_ENDPOINT:-http://localhost:19090}"
HEALTH_URL="http://127.0.0.1:${APP_PORT}/actuator/health"
APP_HEALTH_WAIT_SECONDS="${APP_HEALTH_WAIT_SECONDS:-20}"

required_containers=(
  "postgres-server"
  "redis-server"
  "minio-server"
)

optional_containers=(
  "prometheus-prometheus-1"
  "prometheus-grafana-1"
)

check_container() {
  local container_name="$1"
  local required="$2"

  if docker ps --format '{{.Names}}' | grep -qx "${container_name}"; then
    echo "[OK] container ${container_name} is running"
    return 0
  fi

  if [[ "${required}" == "required" ]]; then
    echo "[FAIL] container ${container_name} is not running" >&2
    return 1
  fi

  echo "[WARN] optional container ${container_name} is not running"
  return 0
}

status=0

echo "Perf env file: ${PERF_ENV_FILE_RESOLVED}"
echo "App port: ${APP_PORT}"
echo "k6 BASE_URL: ${BASE_URL_VALUE}"
echo "MinIO endpoint: ${MINIO_ENDPOINT_VALUE}"
echo

for container_name in "${required_containers[@]}"; do
  check_container "${container_name}" required || status=1
done

for container_name in "${optional_containers[@]}"; do
  check_container "${container_name}" optional || true
done

echo
app_health_ok=0
for _ in $(seq 1 "${APP_HEALTH_WAIT_SECONDS}"); do
  if curl -fsS "${HEALTH_URL}" >/tmp/dandionna-perf-health.json 2>/dev/null; then
    app_health_ok=1
    break
  fi
  sleep 1
done

if [[ "${app_health_ok}" -eq 1 ]]; then
  echo "[OK] app health check passed: ${HEALTH_URL}"
  cat /tmp/dandionna-perf-health.json
  echo
else
  echo "[FAIL] app health check failed after ${APP_HEALTH_WAIT_SECONDS}s: ${HEALTH_URL}" >&2
  status=1
fi

if curl -fsS "${MINIO_ENDPOINT_VALUE}/minio/health/live" >/dev/null 2>&1; then
  echo "[OK] MinIO live check passed: ${MINIO_ENDPOINT_VALUE}/minio/health/live"
else
  echo "[WARN] MinIO live check failed: ${MINIO_ENDPOINT_VALUE}/minio/health/live"
fi

exit "${status}"
