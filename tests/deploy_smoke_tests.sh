#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "🧪 Smoke test script dry-run"
bash scripts/deploy/smoke-tests.sh --dry-run

echo "✅ Smoke test dry-run passed"
