#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="${BUILD_DOCTOR_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"

MIN_FREE_GB=10
MIN_FREE_KB=$((MIN_FREE_GB * 1024 * 1024))

if ! command -v df >/dev/null 2>&1; then
  echo "❌ df command not available"
  exit 1
fi

AVAILABLE_KB=$(df -Pk "$PROJECT_ROOT" | tail -1 | awk '{print $4}')
AVAILABLE_GB=$((AVAILABLE_KB / 1024 / 1024))

if [[ "$AVAILABLE_KB" -lt "$MIN_FREE_KB" ]]; then
  echo "❌ Not enough disk space: ${AVAILABLE_GB}GB free"
  echo "💡 Free up space: docker system prune -af"
  exit 1
fi
