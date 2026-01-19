#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

DRY_RUN=false
TARGET_VERSION=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    *)
      TARGET_VERSION="$1"
      shift
      ;;
  esac
done

if [[ -z "$TARGET_VERSION" ]]; then
  echo "Usage: $0 [--dry-run] <version>"
  echo "Example: $0 2  # Roll back to V2"
  exit 1
fi

if [[ -f .env ]]; then
  set -a
  source .env
  set +a
fi

DB_NAME="${DB_INTERNAL_NAME:-core}"
DB_USER="${DB_INTERNAL_USERNAME:-core}"
DB_PASSWORD="${DB_INTERNAL_PASSWORD:-core}"

MIGRATIONS_DIR="backend/src/main/resources/db/migration"

echo "⏪ Rolling back to version $TARGET_VERSION"

if [[ "$DRY_RUN" == "true" ]]; then
  echo "🧪 Dry-run mode enabled"
fi

if [[ "$DRY_RUN" == "true" && -n "${CURRENT_VERSION_OVERRIDE:-}" ]]; then
  CURRENT_VERSION="$CURRENT_VERSION_OVERRIDE"
else
  CURRENT_VERSION=$(PGPASSWORD="$DB_PASSWORD" docker exec core-db psql -U "$DB_USER" -d "$DB_NAME" -tAc \
    "SELECT COALESCE(MAX(CAST(version AS INT)), 0) FROM flyway_schema_history WHERE success = true" || echo "0")
fi

echo "📊 Current version: $CURRENT_VERSION"

if [[ "$CURRENT_VERSION" -le "$TARGET_VERSION" ]]; then
  echo "✅ No rollback needed"
  exit 0
fi

BACKUP_FILE="diagnostics/db-backup-$(date +%Y%m%d-%H%M%S).sql"
if [[ "$DRY_RUN" == "false" ]]; then
  mkdir -p diagnostics
  echo "💾 Creating backup: $BACKUP_FILE"
  PGPASSWORD="$DB_PASSWORD" docker exec core-db pg_dump -U "$DB_USER" "$DB_NAME" > "$BACKUP_FILE"
fi

for version in $(seq "$CURRENT_VERSION" -1 "$((TARGET_VERSION + 1))"); do
  UNDO_FILE=$(ls "$MIGRATIONS_DIR"/U${version}__*.sql 2>/dev/null || true)
  if [[ -z "$UNDO_FILE" ]]; then
    echo "❌ Undo migration not found for version $version"
    exit 1
  fi
  echo "🔄 Executing $UNDO_FILE"
  if [[ "$DRY_RUN" == "false" ]]; then
    PGPASSWORD="$DB_PASSWORD" docker exec -i core-db psql -U "$DB_USER" -d "$DB_NAME" < "$UNDO_FILE"
  fi
done

if [[ "$DRY_RUN" == "false" ]]; then
  PGPASSWORD="$DB_PASSWORD" docker exec -i core-db psql -U "$DB_USER" -d "$DB_NAME" <<EOF
DELETE FROM flyway_schema_history
WHERE version::int > $TARGET_VERSION;
EOF
fi

echo "✅ Rollback completed to version $TARGET_VERSION"
