#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPORT_ROOT="${ROOT_DIR}/benchmark-runner/build/reports/benchmarks/five-stack"

mkdir -p "${REPORT_ROOT}"

run_case() {
  local app="$1"
  local label="$2"
  local concurrency="$3"
  local extra_env="${4:-}"
  local output_dir="${REPORT_ROOT}/${label}"

  echo "== Running ${label} (concurrency=${concurrency}) =="
  "${ROOT_DIR}/scripts/run-matrix.sh" "${app}" "${output_dir}" "${concurrency}" "" "${extra_env}"
}

for concurrency in 128 256; do
  run_case spring-mvc "c${concurrency}-spring-mvc-platform" "${concurrency}" \
    "SPRING_THREADS_VIRTUAL_ENABLED=false SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=32 SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=8"
  run_case spring-mvc "c${concurrency}-spring-mvc-virtual" "${concurrency}" \
    "SPRING_THREADS_VIRTUAL_ENABLED=true SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=32 SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=8"
  run_case spring-webflux "c${concurrency}-spring-webflux-r2dbc" "${concurrency}" \
    "SPRING_R2DBC_POOL_INITIAL_SIZE=16 SPRING_R2DBC_POOL_MAX_SIZE=32"
  run_case ktor-jdbc "c${concurrency}-ktor-jdbc" "${concurrency}" \
    "BENCHMARK_DB_MAX_POOL_SIZE=32"
  run_case ktor-r2dbc "c${concurrency}-ktor-r2dbc" "${concurrency}" \
    "BENCHMARK_DB_INITIAL_POOL_SIZE=16 BENCHMARK_DB_MAX_POOL_SIZE=32"
done

python3 "${ROOT_DIR}/scripts/render_five_stack_summary.py"
echo "Five-stack summary: ${REPORT_ROOT}/five-stack-summary.html"
