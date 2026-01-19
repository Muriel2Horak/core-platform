#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

DRY_RUN=false
DB_NAME="core"
BACKUP_FILE=""
TARGET_TIME=""
BACKUP_DIR="${BACKUP_DIR:-backups/postgres}"
WAL_ARCHIVE_DIR="${WAL_ARCHIVE_DIR:-docker/db/wal-archive}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --db)
      DB_NAME="$2"
      shift 2
      ;;
    --file)
      BACKUP_FILE="$2"
      shift 2
      ;;
    --timestamp)
      TARGET_TIME="$2"
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

PG_USER="${POSTGRES_USER:-postgres}"
PG_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

if [[ -n "$TARGET_TIME" ]]; then
  echo "⏰ PITR restore to $TARGET_TIME"
  BASE_BACKUP_DIR=$(ls -dt "$BACKUP_DIR"/base/base_* 2>/dev/null | head -1 || true)
  if [[ -z "$BASE_BACKUP_DIR" ]]; then
    echo "❌ No base backup found in $BACKUP_DIR/base"
    exit 1
  fi

  echo "📦 Using base backup: $BASE_BACKUP_DIR"
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "🧪 Dry-run: would restore base backup and replay WAL from $WAL_ARCHIVE_DIR"
    exit 0
  fi

  echo "🛑 Stopping database..."
  docker compose -f docker/docker-compose.yml --env-file .env stop db

  echo "♻️  Restoring base backup into data directory..."
  docker run --rm \
    --volumes-from core-db \
    -v "${BASE_BACKUP_DIR}:/base_backup:ro" \
    -v "${PROJECT_ROOT}/${WAL_ARCHIVE_DIR}:/wal-archive:ro" \
    postgres:16 bash -c "\
      rm -rf /var/lib/postgresql/data/* && \
      tar -xzf /base_backup/base.tar.gz -C /var/lib/postgresql/data && \
      echo \"restore_command = 'cp /wal-archive/%f %p'\" >> /var/lib/postgresql/data/postgresql.auto.conf && \
      echo \"recovery_target_time = '$TARGET_TIME'\" >> /var/lib/postgresql/data/postgresql.auto.conf && \
      touch /var/lib/postgresql/data/recovery.signal"

  echo "🚀 Starting database..."
  docker compose -f docker/docker-compose.yml --env-file .env start db
  exit 0
fi

if [[ -z "$BACKUP_FILE" ]]; then
  echo "Usage: $0 --db core --file backups/postgres/full/core_<timestamp>.dump"
  echo "   or: $0 --timestamp \"2025-11-08T14:30:00Z\""
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "❌ Backup file not found: $BACKUP_FILE"
  exit 1
fi

RESTORE_DB="${DB_NAME}_restore"
echo "🔄 Restoring $DB_NAME into $RESTORE_DB"

if [[ "$DRY_RUN" == "true" ]]; then
  echo "🧪 Dry-run: would create $RESTORE_DB and restore from $BACKUP_FILE"
  exit 0
fi

docker exec -e PGPASSWORD="$PG_PASSWORD" core-db \
  psql -U "$PG_USER" -d postgres -c "DROP DATABASE IF EXISTS ${RESTORE_DB};"
docker exec -e PGPASSWORD="$PG_PASSWORD" core-db \
  psql -U "$PG_USER" -d postgres -c "CREATE DATABASE ${RESTORE_DB};"

docker exec -e PGPASSWORD="$PG_PASSWORD" -i core-db \
  pg_restore -U "$PG_USER" -d "$RESTORE_DB" < "$BACKUP_FILE"

echo "✅ Restore completed to ${RESTORE_DB}"
