#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

MIGRATIONS_DIR="backend/src/main/resources/db/migration"

MISSING=()

for v_file in "$MIGRATIONS_DIR"/V*.sql; do
  version=$(basename "$v_file" | sed -E 's/^V([0-9]+).*/\1/')
  if ! ls "$MIGRATIONS_DIR"/U${version}__*.sql >/dev/null 2>&1; then
    MISSING+=("U${version}")
  fi
done

if [[ ${#MISSING[@]} -eq 0 ]]; then
  echo "✅ All V migrations have U counterparts"
  exit 0
fi

echo "❌ Missing undo migrations:"
printf '  - %s\n' "${MISSING[@]}"
exit 1
