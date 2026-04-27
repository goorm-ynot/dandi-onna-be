#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEFAULT_PERF_ENV_FILE="${ROOT_DIR}/env/perf.local.env"

load_perf_env() {
  local env_file="${PERF_ENV_FILE:-${DEFAULT_PERF_ENV_FILE}}"
  export PERF_ENV_FILE_RESOLVED="${env_file}"

  if [[ -f "${env_file}" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
  fi
}
