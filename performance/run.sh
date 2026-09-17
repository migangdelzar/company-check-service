#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
service_root="$(CDPATH= cd -- "$script_dir/.." && pwd)"
workspace_root="$(CDPATH= cd -- "$service_root/.." && pwd)"
cd "$workspace_root"

if docker compose version >/dev/null 2>&1; then
  compose=(docker compose)
elif docker-compose version >/dev/null 2>&1; then
  compose=(docker-compose)
else
  printf 'Docker Compose is required (docker compose or docker-compose)\n' >&2
  exit 2
fi

topology="${PERFORMANCE_TOPOLOGY:-single}"
compose+=( -f "$workspace_root/compose.yaml" )
case "$topology" in
  single) compose+=( -f "$workspace_root/compose.single.yaml" ) ;;
  distributed) compose+=( -f "$workspace_root/compose.distributed.yaml" ) ;;
  *)
    printf 'PERFORMANCE_TOPOLOGY must be single or distributed: %s\n' "$topology" >&2
    exit 2
    ;;
esac

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

resolve_image() {
  local name="$1" default_image="$2" configured_image digest_image
  configured_image="${!name:-$default_image}"
  if [[ "$configured_image" != *@sha256:* ]]; then
    digest_image="$(docker image inspect --format '{{index .RepoDigests 0}}' "$configured_image" 2>/dev/null || true)"
    [[ -n "$digest_image" ]] || {
      printf '%s is not available locally: %s. Build or pull it, or set %s to an immutable image digest.\n' \
        "$name" "$configured_image" "$name" >&2
      exit 2
    }
    configured_image="$digest_image"
  fi
  require_digest_image "$name" "$configured_image"
  printf -v "$name" '%s' "$configured_image"
  export "$name"
}

resolve_image COMPANY_CHECK_SERVICE_IMAGE company-check-service:local
resolve_image COMPANY_CHECK_PROVIDER_IMAGE company-check-provider:local
resolve_image POSTGRES_IMAGE postgres:17-alpine
resolve_image REDIS_IMAGE redis:7-alpine
resolve_image LOCUST_IMAGE locustio/locust:2.32.10

scenario_file="${PERFORMANCE_SCENARIOS_FILE:-$service_root/performance/scenarios.env}"
set -a
. "$scenario_file"
set +a

artifacts="${PERFORMANCE_ARTIFACTS_DIR:-$workspace_root/.performance-artifacts}"
mkdir -p "$artifacts"

cleanup() {
  status=$?
  if (( status != 0 )); then
    "${compose[@]}" --profile performance logs --no-color >"$artifacts/compose.log" 2>&1 || true
    "${compose[@]}" --profile performance ps --all >"$artifacts/compose-ps.txt" 2>&1 || true
  fi
  "${compose[@]}" --profile performance down >/dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT INT TERM

"${compose[@]}" --profile performance up -d
timeout_seconds="${COMPOSE_WAIT_TIMEOUT_SECONDS:-120}"
deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
  status="$("${compose[@]}" ps --all --format '{{.Service}} {{.State}} {{.Health}}')"
  case "$status" in
    *" exited "*|*" dead "*) printf '%s\n' "$status" >&2; exit 1 ;;
  esac
  [[ "$status" == *"backend running healthy"* ]] && break
done
(( SECONDS < deadline )) || { printf 'backend did not become healthy before timeout\n' >&2; exit 1; }

"${compose[@]}" --profile performance run --rm locust \
  --headless \
  -f /mnt/performance/locustfile.py \
  --host http://backend:8080 \
  --users "${PERFORMANCE_USERS:-5}" \
  --spawn-rate "${PERFORMANCE_SPAWN_RATE:-1}" \
  --run-time "${PERFORMANCE_DURATION:-30s}" \
  --only-summary \
  --html /mnt/artifacts/report.html \
  --csv /mnt/artifacts/locust
