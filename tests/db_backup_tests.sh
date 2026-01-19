#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

BACKUP_DIR="backups/postgres"
BASE_DIR="$BACKUP_DIR/base/base_test"
TMP_BACKUP="$(mktemp)"

cleanup() {
  rm -f "$TMP_BACKUP"
  rm -rf "$BASE_DIR"
}

trap cleanup EXIT

mkdir -p "$BASE_DIR"

echo "🧪 Backup dry-run"
bash scripts/backup/pg-backup.sh --dry-run

echo "🧪 Restore dry-run (file)"
bash scripts/backup/pg-restore.sh --dry-run --db core --file "$TMP_BACKUP"

echo "🧪 PITR dry-run"
bash scripts/backup/pg-restore.sh --dry-run --timestamp "2025-11-08T14:30:00Z"

echo "✅ Backup/restore tests passed"
