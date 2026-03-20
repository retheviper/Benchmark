#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

APP="${1:?app must be spring-mvc, spring-webflux, ktor-r2dbc, or ktor-jdbc}"
OUTPUT_DIR="${2:?output dir required}"
CONCURRENCY="${3:-16}"
SCENARIOS="${4:-}"
EXTRA_ENV="${5:-}"

case "$APP" in
  spring|spring-mvc)
    PORT=8080
    TARGET="spring-mvc-jdbc@http://localhost:8080"
    RUN_CMD="./gradlew :spring-mvc-jdbc:bootRun"
    ;;
  spring-webflux)
    PORT=8082
    TARGET="spring-webflux-r2dbc@http://localhost:8082"
    RUN_CMD="./gradlew :spring-webflux-r2dbc:bootRun"
    ;;
  ktor|ktor-r2dbc)
    PORT=8081
    TARGET="ktor-r2dbc@http://localhost:8081"
    RUN_CMD="./gradlew :ktor-r2dbc:run"
    ;;
  ktor-jdbc)
    PORT=8083
    TARGET="ktor-jdbc@http://localhost:8083"
    RUN_CMD="./gradlew :ktor-jdbc:run"
    ;;
  *)
    echo "unknown app: $APP" >&2
    exit 1
    ;;
esac

cleanup() {
  if [[ -n "${APP_PID:-}" ]]; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
    wait "${APP_PID}" >/dev/null 2>&1 || true
  fi
  docker compose down >/dev/null 2>&1 || true
}

trap cleanup EXIT

cd "${ROOT_DIR}"
docker compose down -v >/dev/null 2>&1 || true
docker compose up -d postgres >/dev/null

until docker compose ps | grep -q "healthy"; do
  sleep 1
done

mkdir -p "${OUTPUT_DIR}"

if [[ -n "${EXTRA_ENV}" ]]; then
  env ${EXTRA_ENV} ${RUN_CMD} > "${OUTPUT_DIR}/server.log" 2>&1 &
else
  ${RUN_CMD} > "${OUTPUT_DIR}/server.log" 2>&1 &
fi
APP_PID=$!

for _ in $(seq 1 60); do
  if curl -fsS "http://localhost:${PORT}/health" >/dev/null 2>&1; then
    HEALTHY=1
    break
  fi
  sleep 1
done

if [[ "${HEALTHY:-0}" != "1" ]]; then
  echo "App failed to become healthy on port ${PORT}" >&2
  tail -n 120 "${OUTPUT_DIR}/server.log" >&2 || true
  exit 1
fi

ARGS=(
  "--warmup-seconds=1"
  "--measure-seconds=3"
  "--concurrency=${CONCURRENCY}"
  "--targets=${TARGET}"
  "--output-dir=${OUTPUT_DIR}"
)

if [[ -n "${SCENARIOS}" ]]; then
  ARGS+=("--scenarios=${SCENARIOS}")
fi

./gradlew :benchmark-runner:run --args="${ARGS[*]}"
