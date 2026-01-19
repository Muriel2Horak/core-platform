#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=false
RESTORE_POINT=""
DB_NAME="core"
OFFSITE_BUCKET=${OFFSITE_BUCKET:-s3://core-platform-backups}
TARGET_DIR=${TARGET_DIR:-backups/offsite}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --restore-point)
      RESTORE_POINT="$2"
      shift 2
      ;;
    --db)
      DB_NAME="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

log() {
  printf '%s\n' "$1"
}

log "Starting offsite restore for ${DB_NAME}"
log "Bucket: ${OFFSITE_BUCKET}"
log "Target dir: ${TARGET_DIR}"

if [[ "$DRY_RUN" == "true" ]]; then
  log "Dry run enabled - skipping download and restore"
  exit 0
fi

if ! command -v aws >/dev/null 2>&1; then
  log "aws cli is required to sync offsite backups"
  exit 1
fi

mkdir -p "${TARGET_DIR}"

log "Syncing latest backups from offsite"
aws s3 sync "${OFFSITE_BUCKET}" "${TARGET_DIR}" --exclude "*" --include "*/full/*.dump"

if [[ -n "$RESTORE_POINT" ]]; then
  log "Restoring using PITR timestamp ${RESTORE_POINT}"
  bash scripts/backup/pg-restore.sh --db "$DB_NAME" --timestamp "$RESTORE_POINT"
  exit 0
fi

LATEST_BACKUP=$(ls -t "${TARGET_DIR}"/full/*.dump 2>/dev/null | head -1 || true)
if [[ -z "$LATEST_BACKUP" ]]; then
  log "No backup files found in ${TARGET_DIR}/full"
  exit 1
fi

log "Restoring from ${LATEST_BACKUP}"
bash scripts/backup/pg-restore.sh --db "$DB_NAME" --file "$LATEST_BACKUP"
