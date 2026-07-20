#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/perf/load-env.sh"
load_perf_env

SEED_NAME="${1:-small}"
SQL_DIR="${ROOT_DIR}/scripts/perf/seeds"
CONTAINER_NAME="${PERF_POSTGRES_CONTAINER:-postgres-server}"
PGUSER="${PERF_POSTGRES_USER:-appuser}"
PGDATABASE="${PERF_POSTGRES_DB:-dandi_db}"

if [[ ! -f "${SQL_DIR}/${SEED_NAME}.sql" ]]; then
  echo "unknown seed: ${SEED_NAME}" >&2
  exit 1
fi

echo "Loading perf seed '${SEED_NAME}' into container '${CONTAINER_NAME}' (${PGDATABASE})"
docker exec -i "${CONTAINER_NAME}" psql -v ON_ERROR_STOP=1 -U "${PGUSER}" -d "${PGDATABASE}" < "${SQL_DIR}/common.sql"
docker exec -i "${CONTAINER_NAME}" psql -v ON_ERROR_STOP=1 -U "${PGUSER}" -d "${PGDATABASE}" < "${SQL_DIR}/${SEED_NAME}.sql"
