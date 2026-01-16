#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "🧪 Undo migration validation"
bash scripts/db/validate-undo-scripts.sh

echo "🧪 Rollback dry-run"
CURRENT_VERSION_OVERRIDE=3 bash scripts/db/rollback.sh --dry-run 2

echo "✅ Rollback tests passed"
