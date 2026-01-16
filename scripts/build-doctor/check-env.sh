#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="${BUILD_DOCTOR_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
cd "$PROJECT_ROOT"

if [[ ! -f .env ]]; then
  echo "❌ .env file not found"
  echo "💡 Copy from template: cp .env.template .env"
  exit 1
fi

if ! bash scripts/env-validate.sh >/dev/null 2>&1; then
  echo "❌ .env validation failed"
  echo "💡 Run: make env-validate"
  exit 1
fi
