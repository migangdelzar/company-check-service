#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
service_root="$(CDPATH= cd -- "$script_dir/.." && pwd)"
workspace_root="$(CDPATH= cd -- "$service_root/.." && pwd)"
cd "$workspace_root"

if [[ -f .env ]]; then
  set -a
  . ./.env
  set +a
fi

require_digest_image() {
  local name="$1" value="$2"
  [[ "$value" =~ ^[^[:space:]@]+@sha256:[0-9a-fA-F]{64}$ ]] || {
    printf '%s must be an immutable digest image: %s\n' "$name" "$value" >&2
    exit 2
  }
}

require_digest_image COMPANY_CHECK_SERVICE_IMAGE "${COMPANY_CHECK_SERVICE_IMAGE:?set COMPANY_CHECK_SERVICE_IMAGE}"
require_digest_image COMPANY_CHECK_PROVIDER_IMAGE "${COMPANY_CHECK_PROVIDER_IMAGE:?set COMPANY_CHECK_PROVIDER_IMAGE}"
require_digest_image POSTGRES_IMAGE "${POSTGRES_IMAGE:?set POSTGRES_IMAGE}"
require_digest_image REDIS_IMAGE "${REDIS_IMAGE:?set REDIS_IMAGE}"
require_digest_image LOCUST_IMAGE "${LOCUST_IMAGE:?set LOCUST_IMAGE}"

scenario_file="${PERFORMANCE_SCENARIOS_FILE:-$service_root/performance/scenarios.env}"
set -a
. "$scenario_file"
set +a

artifacts="${PERFORMANCE_ARTIFACTS_DIR:-$workspace_root/.performance-artifacts}"
mkdir -p "$artifacts"

cleanup() {
  status=$?
  if (( status != 0 )); then
    docker compose --profile performance logs --no-color >"$artifacts/compose.log" 2>&1 || true
    docker compose --profile performance ps --all >"$artifacts/compose-ps.txt" 2>&1 || true
  fi
  docker compose --profile performance down >/dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT INT TERM

docker compose --profile performance up -d
timeout_seconds="${COMPOSE_WAIT_TIMEOUT_SECONDS:-120}"
deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
  status="$(docker compose ps --all --format '{{.Service}} {{.State}} {{.Health}}')"
  case "$status" in
    *" exited "*|*" dead "*) printf '%s\n' "$status" >&2; exit 1 ;;
  esac
  [[ "$status" == *"backend running healthy"* ]] && break
done
(( SECONDS < deadline )) || { printf 'backend did not become healthy before timeout\n' >&2; exit 1; }

docker compose --profile performance run --rm locust \
  --headless \
  -f /mnt/performance/locustfile.py \
  --host http://backend:8080 \
  --users "${PERFORMANCE_USERS:-5}" \
  --spawn-rate "${PERFORMANCE_SPAWN_RATE:-1}" \
  --run-time "${PERFORMANCE_DURATION:-30s}" \
  --only-summary \
  --html /mnt/artifacts/report.html \
  --csv /mnt/artifacts/locust
