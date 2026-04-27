#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "run this script with sudo or as root" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SYSTEMD_DIR="/etc/systemd/system"
APP_USER="${APP_USER:-dandionna}"
APP_GROUP="${APP_GROUP:-dandionna}"
APP_ROOT="${APP_ROOT:-/opt/dandionna}"
ENV_DIR="${ENV_DIR:-/etc/dandionna}"
ENV_FILE="${ENV_FILE:-${ENV_DIR}/dandionna.env}"
ENV_EXAMPLE_SOURCE="${ROOT_DIR}/deploy/env/dandionna.env.example"
ENV_EXAMPLE_TARGET="${ENV_DIR}/dandionna.env.example"
CURRENT_JAR="${APP_ROOT}/current/dandionna-app.jar"
JAVA_BIN="${JAVA_BIN:-/usr/bin/java}"

java_major_version() {
  local java_bin="$1"
  local version_line version

  version_line="$("${java_bin}" -version 2>&1 | head -n 1)"
  version="$(printf '%s\n' "${version_line}" | sed -nE 's/.*version "([0-9]+)(\.[^"]*)?".*/\1/p')"
  printf '%s\n' "${version}"
}

ensure_group() {
  if ! getent group "${APP_GROUP}" >/dev/null; then
    groupadd --system "${APP_GROUP}"
    echo "Created group: ${APP_GROUP}"
  fi
}

ensure_user() {
  if ! id -u "${APP_USER}" >/dev/null 2>&1; then
    useradd --system --home-dir "${APP_ROOT}" --shell /usr/sbin/nologin --gid "${APP_GROUP}" "${APP_USER}"
    echo "Created user: ${APP_USER}"
  fi
}

ensure_directories() {
  install -d -m 0755 -o root -g root "${ENV_DIR}"
  install -d -m 0755 -o "${APP_USER}" -g "${APP_GROUP}" "${APP_ROOT}"
  install -d -m 0755 -o "${APP_USER}" -g "${APP_GROUP}" "${APP_ROOT}/releases"
}

copy_env_example_if_missing() {
  if [[ -f "${ENV_EXAMPLE_SOURCE}" && ! -f "${ENV_EXAMPLE_TARGET}" ]]; then
    install -m 0644 "${ENV_EXAMPLE_SOURCE}" "${ENV_EXAMPLE_TARGET}"
    echo "Installed env example: ${ENV_EXAMPLE_TARGET}"
  fi
}

report_missing_prerequisites() {
  local missing=0
  local java_major=""

  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "[MISSING] ${ENV_FILE}" >&2
    echo "          copy ${ENV_EXAMPLE_TARGET} to ${ENV_FILE} and fill in the real values." >&2
    missing=1
  fi

  if [[ ! -x "${JAVA_BIN}" ]]; then
    echo "[MISSING] Java runtime not found: ${JAVA_BIN}" >&2
    echo "          install Java 21 and set JAVA_BIN in ${ENV_FILE} when the runtime is not at /usr/bin/java." >&2
    missing=1
  else
    java_major="$(java_major_version "${JAVA_BIN}")"
    if [[ -z "${java_major}" ]]; then
      echo "[MISSING] could not detect Java version from ${JAVA_BIN}" >&2
      echo "          verify the runtime manually with: ${JAVA_BIN} -version" >&2
      missing=1
    elif (( java_major < 21 )); then
      echo "[MISSING] Java 21+ runtime is required, but ${JAVA_BIN} is Java ${java_major}." >&2
      echo "          install openjdk-21 and set JAVA_BIN=/usr/lib/jvm/java-21-openjdk-amd64/bin/java in ${ENV_FILE}." >&2
      missing=1
    fi
  fi

  if [[ ! -f "${CURRENT_JAR}" ]]; then
    echo "[MISSING] ${CURRENT_JAR}" >&2
    echo "          build the app with ./gradlew clean test bootJar, then stage it with:" >&2
    echo "          sudo ./deploy/bin/deploy.sh <release-id> ./build/libs/dandionna-app.jar" >&2
    missing=1
  fi

  return "${missing}"
}

ensure_group
ensure_user
ensure_directories
copy_env_example_if_missing

install -m 0644 "${ROOT_DIR}/deploy/systemd/dandionna.service" "${SYSTEMD_DIR}/dandionna.service"
install -m 0644 "${ROOT_DIR}/deploy/systemd/dandionna-restart-if-running.service" "${SYSTEMD_DIR}/dandionna-restart-if-running.service"
install -m 0644 "${ROOT_DIR}/deploy/systemd/dandionna-restart.timer" "${SYSTEMD_DIR}/dandionna-restart.timer"

systemctl daemon-reload
systemctl enable dandionna.service
systemctl enable --now dandionna-restart.timer

if report_missing_prerequisites; then
  systemctl restart dandionna.service
  systemctl status --no-pager dandionna.service
else
  systemctl stop dandionna.service >/dev/null 2>&1 || true
  systemctl reset-failed dandionna.service >/dev/null 2>&1 || true
  echo
  echo "dandionna.service was installed and enabled, but it was not started because required files are missing." >&2
  echo "After preparing the env file and jar, start it with:" >&2
  echo "  sudo systemctl start dandionna.service" >&2
fi

systemctl status --no-pager dandionna-restart.timer
