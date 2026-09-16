#!/usr/bin/env bash
set -euo pipefail

service_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
build_file="$service_root/build.gradle.kts"

required_declarations=(
  'openApiValidate'
  'jacocoTestReport'
  'performanceTest'
  'qualityGate'
  'outputs.cacheIf { false }'
)

for declaration in "${required_declarations[@]}"; do
  if ! grep -Fq "$declaration" "$build_file"; then
    echo "Missing final-gate declaration: $declaration" >&2
    exit 1
  fi
done

for contract in "$service_root"/openapi/*.{yaml,yml,json}; do
  [[ -f "$contract" ]] || continue
  case "$contract" in
    *.yaml|*.yml|*.json) ;;
    *) echo "Unsupported OpenAPI contract: $contract" >&2; exit 1 ;;
  esac
done

echo "Final-gate declarations are present; execution intentionally skipped."
