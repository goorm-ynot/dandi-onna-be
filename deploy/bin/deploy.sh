#!/usr/bin/env bash
set -euo pipefail

usage() {
	echo "Usage: $0 <release-id> <jar-path>"
}

if [[ $# -ne 2 ]]; then
	usage
	exit 1
fi

RELEASE_ID="$1"
JAR_SOURCE="$2"
APP_ROOT="${APP_ROOT:-/opt/dandionna}"
SERVICE_NAME="${SERVICE_NAME:-dandionna.service}"
ENV_FILE="${ENV_FILE:-/etc/dandionna/dandionna.env}"
RELEASES_DIR="${APP_ROOT}/releases"
CURRENT_LINK="${APP_ROOT}/current"
JAR_NAME="${JAR_NAME:-dandionna-app.jar}"
HEALTH_HOST="${HEALTH_HOST:-127.0.0.1}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-60}"

if [[ ! -f "$JAR_SOURCE" ]]; then
	echo "Jar file not found: $JAR_SOURCE" >&2
	exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
	set -a
	# shellcheck disable=SC1090
	source "$ENV_FILE"
	set +a
fi

SERVER_PORT="${SERVER_PORT:-18080}"
NEW_RELEASE_DIR="${RELEASES_DIR}/${RELEASE_ID}"
PREVIOUS_TARGET=""

wait_for_health() {
	local started_at now
	started_at="$(date +%s)"
	while true; do
		if curl -fsS "http://${HEALTH_HOST}:${SERVER_PORT}${HEALTH_PATH}" | grep -q '"status":"UP"'; then
			return 0
		fi
		now="$(date +%s)"
		if (( now - started_at >= HEALTH_TIMEOUT_SECONDS )); then
			return 1
		fi
		sleep 1
	done
}

if [[ -L "$CURRENT_LINK" || -e "$CURRENT_LINK" ]]; then
	PREVIOUS_TARGET="$(readlink -f "$CURRENT_LINK" || true)"
fi

mkdir -p "$NEW_RELEASE_DIR"
install -m 0644 "$JAR_SOURCE" "${NEW_RELEASE_DIR}/${JAR_NAME}"
ln -sfn "$NEW_RELEASE_DIR" "$CURRENT_LINK"
echo "Staged release ${RELEASE_ID} at ${NEW_RELEASE_DIR}"

if systemctl is-active --quiet "$SERVICE_NAME"; then
	echo "Service is active. Restarting ${SERVICE_NAME}."
	if systemctl restart "$SERVICE_NAME" && wait_for_health; then
		echo "Deployment completed successfully."
		exit 0
	fi

	echo "Deployment failed. Rolling back to previous release." >&2
	if [[ -n "$PREVIOUS_TARGET" && -d "$PREVIOUS_TARGET" ]]; then
		ln -sfn "$PREVIOUS_TARGET" "$CURRENT_LINK"
		systemctl restart "$SERVICE_NAME"
	fi
	exit 1
fi

echo "Service is inactive. Release staged without starting the application."
