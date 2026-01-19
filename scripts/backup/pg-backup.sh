#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

DRY_RUN=false
BACKUP_DIR="${BACKUP_DIR:-backups/postgres}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --backup-dir)
      BACKUP_DIR="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

if [[ -f .env ]]; then
  set -a
  source .env
  set +a
fi

DB_LIST=("core" "keycloak" "grafana")
PG_USER="${POSTGRES_USER:-postgres}"
PG_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

mkdir -p "$BACKUP_DIR/full" "$BACKUP_DIR/base"

echo "📦 Starting database backup at $TIMESTAMP"

for db in "${DB_LIST[@]}"; do
  BACKUP_FILE="$BACKUP_DIR/full/${db}_${TIMESTAMP}.dump"
  echo "💾 Backing up $db → $BACKUP_FILE"

  if [[ "$DRY_RUN" == "false" ]]; then
    docker exec -e PGPASSWORD="$PG_PASSWORD" core-db \
      pg_dump -U "$PG_USER" -d "$db" -Fc > "$BACKUP_FILE"
  fi
done

BASE_DIR="base_${TIMESTAMP}"
echo "🗄️  Creating base backup $BASE_DIR"

if [[ "$DRY_RUN" == "false" ]]; then
  docker exec -e PGPASSWORD="$PG_PASSWORD" core-db rm -rf "/tmp/${BASE_DIR}"
  docker exec -e PGPASSWORD="$PG_PASSWORD" core-db \
    pg_basebackup -U "$PG_USER" -D "/tmp/${BASE_DIR}" -Ft -z -X fetch
  docker cp "core-db:/tmp/${BASE_DIR}" "$BACKUP_DIR/base/${BASE_DIR}"
  docker exec core-db rm -rf "/tmp/${BASE_DIR}"
fi

echo "🧹 Cleaning old backups (keep 30 days)"
find "$BACKUP_DIR/full" -name "*.dump" -mtime +30 -delete 2>/dev/null || true
find "$BACKUP_DIR/base" -maxdepth 1 -type d -name "base_*" -mtime +30 -exec rm -rf {} + 2>/dev/null || true

echo "✅ Backup completed"
