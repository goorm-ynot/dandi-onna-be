#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/perf/load-env.sh"
load_perf_env

if [[ ! -f "${PERF_ENV_FILE_RESOLVED}" ]]; then
  echo "perf env file not found: ${PERF_ENV_FILE_RESOLVED}" >&2
  echo "copy env/perf.local.env.example to env/perf.local.env and fill in the values first." >&2
  exit 1
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-perf}"
export SERVER_PORT="${SERVER_PORT:-18080}"
export BASE_URL="${BASE_URL:-http://host.docker.internal:${SERVER_PORT}}"

if [[ $# -eq 0 ]]; then
  set -- bootRun
fi

echo "Loaded perf env from ${PERF_ENV_FILE_RESOLVED}"
echo "Starting app with profile=${SPRING_PROFILES_ACTIVE} port=${SERVER_PORT}"
cd "${ROOT_DIR}"
exec "${ROOT_DIR}/gradlew" "$@"
