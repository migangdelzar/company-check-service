#!/usr/bin/env bash
set -euo pipefail

image_name="${1:-company-check-service:0.0.1-SNAPSHOT}"
deadline_seconds="${IMAGE_SMOKE_TIMEOUT_SECONDS:-15}"

case "$deadline_seconds" in
  ''|*[!0-9]*) echo "IMAGE_SMOKE_TIMEOUT_SECONDS must be an integer" >&2; exit 2 ;;
esac
(( deadline_seconds > 0 && deadline_seconds <= 120 )) || {
  echo "IMAGE_SMOKE_TIMEOUT_SECONDS must be between 1 and 120" >&2
  exit 2
}

command -v docker >/dev/null || { echo "docker is required" >&2; exit 2; }
docker image inspect "$image_name" >/dev/null
container_id="$(docker create --read-only --cap-drop=ALL --security-opt=no-new-privileges:true "$image_name")"
trap 'docker rm -f "$container_id" >/dev/null 2>&1 || true' EXIT
docker start "$container_id" >/dev/null

status=0
for ((second = 0; second < deadline_seconds; second++)); do
  running="$(docker inspect --format '{{.State.Running}}' "$container_id")"
  [[ "$running" == "false" ]] && break
  sleep 1
done

running="$(docker inspect --format '{{.State.Running}}' "$container_id")"
if [[ "$running" == "true" ]]; then
  echo "image did not reach a terminal state within ${deadline_seconds}s" >&2
  exit 1
fi
docker inspect --format 'exit={{.State.ExitCode}} user={{.Config.User}}' "$container_id"
