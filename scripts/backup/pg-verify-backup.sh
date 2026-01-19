#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

BACKUP_DIR="${BACKUP_DIR:-backups/postgres}"
DB_LIST=("core" "keycloak" "grafana")

echo "🔍 Verifying latest backups..."

for db in "${DB_LIST[@]}"; do
  latest=$(ls -t "$BACKUP_DIR/full/${db}_"*.dump 2>/dev/null | head -1 || true)
  if [[ -z "$latest" ]]; then
    echo "❌ Missing backup for $db"
    exit 1
  fi

  echo "✅ Found backup for $db: $latest"
  if ! pg_restore --list "$latest" >/dev/null 2>&1; then
    echo "❌ Backup verification failed for $db"
    exit 1
  fi
done

echo "✅ Backup verification passed"
