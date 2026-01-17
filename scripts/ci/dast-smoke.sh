#!/usr/bin/env bash
set -euo pipefail

if [[ "${RUN_DAST:-0}" != "1" ]]; then
  echo "DAST skipped (RUN_DAST=1 to enable)."
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker not available for DAST."
  exit 1
fi

TARGET_URL="${DAST_TARGET_URL:-https://admin.core-platform.local}"

docker run --rm -t owasp/zap2docker-stable zap-baseline.py \
  -t "$TARGET_URL" \
  -r zap_report.html \
  -J zap_report.json
