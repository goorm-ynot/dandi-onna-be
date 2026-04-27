#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROFILE="${1:-smoke}"

case "${PROFILE}" in
  smoke|measure) ;;
  *)
    echo "usage: ./scripts/perf/run-required.sh [smoke|measure]" >&2
    exit 1
    ;;
esac

SCENARIOS=(
  "home-stores"
  "orders"
  "sales-export"
  "owner-menus"
  "store-no-show-posts"
)

for scenario in "${SCENARIOS[@]}"; do
  "${ROOT_DIR}/scripts/perf/run-k6.sh" "${scenario}" "${PROFILE}"
done
