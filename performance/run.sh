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
elif docker-cli-plugin-docker-compose version >/dev/null 2>&1; then
  compose=(docker-cli-plugin-docker-compose)
else
  printf 'Docker Compose is required (docker compose, docker-compose, or mise-managed Compose)\n' >&2
  exit 2
fi

topology="${PERFORMANCE_TOPOLOGY:-single}"
compose+=( -f "$workspace_root/compose.yaml" )
compose_profiles=(--profile performance)
if [[ "${PERFORMANCE_OBSERVABILITY:-false}" == "true" ]]; then
  compose+=( -f "$workspace_root/compose.observability.yaml" )
  compose_profiles+=(--profile observability)
fi
compose_scale=()
case "$topology" in
  single) compose+=( -f "$workspace_root/compose.single.yaml" ) ;;
  distributed)
    compose+=( -f "$workspace_root/compose.distributed.yaml" )
    compose_scale+=( --scale "backend=${PERFORMANCE_BACKEND_REPLICAS:-2}" )
    ;;
  *)
    printf 'PERFORMANCE_TOPOLOGY must be single or distributed: %s\n' "$topology" >&2
    exit 2
    ;;
esac

configured_service_image="${COMPANY_CHECK_SERVICE_IMAGE-}"
configured_provider_image="${COMPANY_CHECK_PROVIDER_IMAGE-}"
configured_postgres_image="${POSTGRES_IMAGE-}"
configured_redis_image="${REDIS_IMAGE-}"
configured_locust_image="${LOCUST_IMAGE-}"
configured_compose_project="${COMPOSE_PROJECT_NAME-}"
configured_service_port="${COMPANY_CHECK_SERVICE_PORT-}"
configured_topology="${PERFORMANCE_TOPOLOGY-}"
configured_users_from_env="${PERFORMANCE_USERS-}"
configured_spawn_rate_from_env="${PERFORMANCE_SPAWN_RATE-}"
configured_duration_from_env="${PERFORMANCE_DURATION-}"
configured_requests_from_env="${PERFORMANCE_REQUESTS-}"
configured_artifacts_from_env="${PERFORMANCE_ARTIFACTS_DIR-}"
service_image_was_configured=0
provider_image_was_configured=0
postgres_image_was_configured=0
redis_image_was_configured=0
locust_image_was_configured=0
compose_project_was_configured=0
service_port_was_configured=0
topology_was_configured=0
users_from_env_were_configured=0
spawn_rate_from_env_was_configured=0
duration_from_env_was_configured=0
requests_from_env_was_configured=0
artifacts_from_env_were_configured=0
[[ ${COMPANY_CHECK_SERVICE_IMAGE+x} ]] && service_image_was_configured=1
[[ ${COMPANY_CHECK_PROVIDER_IMAGE+x} ]] && provider_image_was_configured=1
[[ ${POSTGRES_IMAGE+x} ]] && postgres_image_was_configured=1
[[ ${REDIS_IMAGE+x} ]] && redis_image_was_configured=1
[[ ${LOCUST_IMAGE+x} ]] && locust_image_was_configured=1
[[ ${COMPOSE_PROJECT_NAME+x} ]] && compose_project_was_configured=1
[[ ${COMPANY_CHECK_SERVICE_PORT+x} ]] && service_port_was_configured=1
[[ ${PERFORMANCE_TOPOLOGY+x} ]] && topology_was_configured=1
[[ ${PERFORMANCE_USERS+x} ]] && users_from_env_were_configured=1
[[ ${PERFORMANCE_SPAWN_RATE+x} ]] && spawn_rate_from_env_was_configured=1
[[ ${PERFORMANCE_DURATION+x} ]] && duration_from_env_was_configured=1
[[ ${PERFORMANCE_REQUESTS+x} ]] && requests_from_env_was_configured=1
[[ ${PERFORMANCE_ARTIFACTS_DIR+x} ]] && artifacts_from_env_were_configured=1

if [[ -f .env ]]; then
  set -a
  . ./.env
  set +a
fi

(( service_image_was_configured )) && COMPANY_CHECK_SERVICE_IMAGE="$configured_service_image"
(( provider_image_was_configured )) && COMPANY_CHECK_PROVIDER_IMAGE="$configured_provider_image"
(( postgres_image_was_configured )) && POSTGRES_IMAGE="$configured_postgres_image"
(( redis_image_was_configured )) && REDIS_IMAGE="$configured_redis_image"
(( locust_image_was_configured )) && LOCUST_IMAGE="$configured_locust_image"
(( compose_project_was_configured )) && COMPOSE_PROJECT_NAME="$configured_compose_project"
(( service_port_was_configured )) && COMPANY_CHECK_SERVICE_PORT="$configured_service_port"
(( topology_was_configured )) && PERFORMANCE_TOPOLOGY="$configured_topology"
(( users_from_env_were_configured )) && PERFORMANCE_USERS="$configured_users_from_env"
(( spawn_rate_from_env_was_configured )) && PERFORMANCE_SPAWN_RATE="$configured_spawn_rate_from_env"
(( duration_from_env_was_configured )) && PERFORMANCE_DURATION="$configured_duration_from_env"
(( requests_from_env_was_configured )) && PERFORMANCE_REQUESTS="$configured_requests_from_env"
(( artifacts_from_env_were_configured )) && PERFORMANCE_ARTIFACTS_DIR="$configured_artifacts_from_env"

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
configured_project_name="${COMPOSE_PROJECT_NAME-}"
configured_users="${PERFORMANCE_USERS-}"
configured_spawn_rate="${PERFORMANCE_SPAWN_RATE-}"
configured_duration="${PERFORMANCE_DURATION-}"
configured_requests="${PERFORMANCE_REQUESTS-}"
configured_p95_ms="${PERFORMANCE_P95_MS-}"
configured_failure_percent="${PERFORMANCE_FAILURE_PERCENT-}"
configured_artifacts_dir="${PERFORMANCE_ARTIFACTS_DIR-}"
project_name_was_configured=0
users_were_configured=0
spawn_rate_was_configured=0
duration_was_configured=0
requests_were_configured=0
p95_was_configured=0
failure_percent_was_configured=0
artifacts_dir_was_configured=0
project_name_was_configured="$compose_project_was_configured"
[[ ${PERFORMANCE_USERS+x} ]] && users_were_configured=1
[[ ${PERFORMANCE_SPAWN_RATE+x} ]] && spawn_rate_was_configured=1
[[ ${PERFORMANCE_DURATION+x} ]] && duration_was_configured=1
[[ ${PERFORMANCE_REQUESTS+x} ]] && requests_were_configured=1
[[ ${PERFORMANCE_P95_MS+x} ]] && p95_was_configured=1
[[ ${PERFORMANCE_FAILURE_PERCENT+x} ]] && failure_percent_was_configured=1
[[ ${PERFORMANCE_ARTIFACTS_DIR+x} ]] && artifacts_dir_was_configured=1
set -a
. "$scenario_file"
set +a
(( users_were_configured )) && PERFORMANCE_USERS="$configured_users"
(( spawn_rate_was_configured )) && PERFORMANCE_SPAWN_RATE="$configured_spawn_rate"
(( duration_was_configured )) && PERFORMANCE_DURATION="$configured_duration"
(( requests_were_configured )) && PERFORMANCE_REQUESTS="$configured_requests"
(( p95_was_configured )) && PERFORMANCE_P95_MS="$configured_p95_ms"
(( failure_percent_was_configured )) && PERFORMANCE_FAILURE_PERCENT="$configured_failure_percent"
(( artifacts_dir_was_configured )) && PERFORMANCE_ARTIFACTS_DIR="$configured_artifacts_dir"
(( project_name_was_configured )) && COMPOSE_PROJECT_NAME="$configured_project_name"
if (( ! project_name_was_configured )); then
  process_id="$$"
  COMPOSE_PROJECT_NAME="${PERFORMANCE_COMPOSE_PROJECT_NAME:-company-check-performance-${process_id}}"
  export COMPOSE_PROJECT_NAME
fi

artifacts="${PERFORMANCE_ARTIFACTS_DIR:-$workspace_root/.performance-artifacts}"
mkdir -p "$artifacts"
chmod 0777 "$artifacts"

if [[ -n "${PERFORMANCE_REQUESTS:-}" && ! "$PERFORMANCE_REQUESTS" =~ ^[1-9][0-9]*$ ]]; then
  printf 'PERFORMANCE_REQUESTS must be a positive integer: %s\n' "$PERFORMANCE_REQUESTS" >&2
  exit 2
fi

cleanup() {
  status=$?
  if (( status != 0 )); then
    "${compose[@]}" "${compose_profiles[@]}" logs --no-color >"$artifacts/compose.log" 2>&1 || true
    "${compose[@]}" "${compose_profiles[@]}" ps --all >"$artifacts/compose-ps.txt" 2>&1 || true
  fi
  if [[ "${PERFORMANCE_KEEP_STACK:-false}" != "true" ]]; then
    "${compose[@]}" "${compose_profiles[@]}" down -v >/dev/null 2>&1 || true
  else
    printf 'Keeping the stack running for observability inspection\n'
  fi
  exit "$status"
}
trap cleanup EXIT INT TERM

if ((${#compose_scale[@]})); then
  "${compose[@]}" "${compose_profiles[@]}" up -d "${compose_scale[@]}"
else
  "${compose[@]}" "${compose_profiles[@]}" up -d
fi
timeout_seconds="${COMPOSE_WAIT_TIMEOUT_SECONDS:-120}"
deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
  status="$("${compose[@]}" ps --all --format '{{.Service}} {{.State}} {{.Health}}')"
  case "$status" in
    *" exited "*|*" dead "*) printf '%s\n' "$status" >&2; exit 1 ;;
  esac
  "${compose[@]}" "${compose_profiles[@]}" run --rm --no-deps --entrypoint python locust \
    -c \
    'import urllib.request; urllib.request.urlopen("http://backend:8080/actuator/health", timeout=2)' \
    >/dev/null 2>&1 && break
done
(( SECONDS < deadline )) || { printf 'backend did not become ready before timeout\n' >&2; exit 1; }

"${compose[@]}" "${compose_profiles[@]}" run --rm --no-deps \
  -e "PERFORMANCE_REQUESTS=${PERFORMANCE_REQUESTS:-}" \
  -e "PERFORMANCE_REQUESTS_AT_LEAST=${PERFORMANCE_REQUESTS_AT_LEAST:-false}" \
  locust \
  --headless \
  -f /mnt/performance/locustfile.py \
  --host http://backend:8080 \
  --users "${PERFORMANCE_USERS:-5}" \
  --spawn-rate "${PERFORMANCE_SPAWN_RATE:-1}" \
  --run-time "${PERFORMANCE_DURATION:-30s}" \
  --only-summary \
  --html /mnt/artifacts/report.html \
  --csv /mnt/artifacts/locust

stats_file="$artifacts/locust_stats.csv"
[[ -s "$stats_file" ]] || {
  printf 'Locust did not produce aggregate statistics: %s\n' "$stats_file" >&2
  exit 1
}

aggregated="$(awk -F, '$2 == "Aggregated" { print; exit }' "$stats_file")"
[[ -n "$aggregated" ]] || {
  printf 'Locust aggregate statistics row is missing: %s\n' "$stats_file" >&2
  exit 1
}

IFS=, read -r _ _ request_count failure_count _ _ _ _ _ _ _ _ _ _ _ _ p95 _ <<<"$aggregated"
[[ "$request_count" =~ ^[0-9]+$ && "$failure_count" =~ ^[0-9]+$ ]] || {
  printf 'Locust aggregate statistics are malformed: %s\n' "$aggregated" >&2
  exit 1
}
[[ "$request_count" -gt 0 ]] || {
  printf 'Locust completed without requests\n' >&2
  exit 1
}
if [[ -n "${PERFORMANCE_REQUESTS:-}" ]]; then
  if [[ "${PERFORMANCE_REQUESTS_AT_LEAST:-false}" == "true" ]]; then
    if (( request_count < PERFORMANCE_REQUESTS )); then
      printf 'Locust minimum request budget missed: expected_at_least=%s actual=%s\n' \
        "$PERFORMANCE_REQUESTS" "$request_count" >&2
      exit 1
    fi
  elif [[ "$request_count" -ne "$PERFORMANCE_REQUESTS" ]]; then
    printf 'Locust request budget mismatch: expected=%s actual=%s\n' \
      "$PERFORMANCE_REQUESTS" "$request_count" >&2
    exit 1
  fi
fi

failure_percent="$(awk -v failures="$failure_count" -v requests="$request_count" \
  'BEGIN { printf "%.2f", (failures * 100) / requests }')"
max_failure_percent="${PERFORMANCE_FAILURE_PERCENT:-1}"
max_p95_ms="${PERFORMANCE_P95_MS:-1000}"
[[ "$p95" =~ ^[0-9]+$ && "$max_failure_percent" =~ ^[0-9]+([.][0-9]+)?$ \
  && "$max_p95_ms" =~ ^[0-9]+$ ]] || {
  printf 'Performance thresholds or p95 statistic are malformed (p95=%s failure=%s)\n' \
    "$p95" "$failure_percent" >&2
  exit 1
}

printf 'Performance aggregate: requests=%s failures=%s failure_percent=%s p95_ms=%s\n' \
  "$request_count" "$failure_count" "$failure_percent" "$p95"
awk -v actual="$failure_percent" -v maximum="$max_failure_percent" \
  'BEGIN { exit !(actual <= maximum) }' || {
  printf 'Failure percentage %.2f exceeds threshold %s\n' "$failure_percent" "$max_failure_percent" >&2
  exit 1
}
(( p95 <= max_p95_ms )) || {
  printf 'p95 latency %s ms exceeds threshold %s ms\n' "$p95" "$max_p95_ms" >&2
  exit 1
}
