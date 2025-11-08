# INF-016: Automated Database Backup & Restore

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** HIGH  
**Effort:** 3 dny, ~600 LOC  
**Owner:** DBA + Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```bash
# ŽÁDNÉ automated backups!
# Backup je pouze při:
# 1. Manual pg_dump před critical operations
# 2. Docker volume snapshot (ručně)

# Recovery:
# - Nelze restore ke konkrétnímu času
# - Žádné Point-in-Time Recovery (PITR)
# - Žádné offsite backups
```

**Risks:**
- Data loss při hardware failure
- Corruption detection delayed (týdny)
- Ransomware risk (backups on same server)

### Goal

**Automated backup strategy:**

```bash
# Backup Schedule:
- Full backup: Daily (2 AM)
- Incremental: Every 6 hours
- Retention: 30 days
- Storage: S3-compatible (MinIO + offsite)

# Point-in-Time Recovery:
make db-restore TIMESTAMP="2025-11-08T14:30:00Z"
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Automated Backups**
   - Full backup: Daily at 2 AM
   - Incremental: 6 hours
   - Upload to S3 (MinIO + AWS)

2. ✅ **Point-in-Time Recovery**
   - WAL archiving enabled
   - Restore to any point within retention window
   - Dry-run mode

3. ✅ **Backup Verification**
   - Weekly: Restore test to separate DB
   - Integrity check (pg_restore --list)
   - Alert if backup fails

### Implementation

**File:** `scripts/backup/pg-backup.sh`

```bash
#!/bin/bash
set -euo pipefail

BACKUP_DIR=/var/backups/postgres
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
S3_BUCKET=s3://core-platform-backups

mkdir -p $BACKUP_DIR

echo "📦 Starting database backup at $TIMESTAMP..."

# 1. Full backup (all databases)
for DB in core keycloak grafana; do
    BACKUP_FILE="$BACKUP_DIR/${DB}_${TIMESTAMP}.sql.gz"
    
    echo "💾 Backing up $DB..."
    pg_dump -h localhost -U postgres -d $DB | gzip > $BACKUP_FILE
    
    # Upload to S3
    aws s3 cp $BACKUP_FILE $S3_BUCKET/$DB/ \
        --storage-class STANDARD_IA
    
    echo "✅ $DB backed up: $(du -h $BACKUP_FILE | cut -f1)"
done

# 2. Archive WAL files (for PITR)
pg_basebackup -h localhost -U postgres \
    -D $BACKUP_DIR/base_${TIMESTAMP} \
    -Ft -z -Xs

# Upload base backup
tar -czf $BACKUP_DIR/base_${TIMESTAMP}.tar.gz \
    $BACKUP_DIR/base_${TIMESTAMP}
aws s3 cp $BACKUP_DIR/base_${TIMESTAMP}.tar.gz \
    $S3_BUCKET/base/

# 3. Cleanup old local backups (keep 7 days)
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "🎉 Backup completed successfully!"
```

**File:** `scripts/backup/pg-restore.sh`

```bash
#!/bin/bash
set -euo pipefail

DB=${1:-core}
BACKUP_FILE=${2:-}
TIMESTAMP=${TIMESTAMP:-}

if [[ -z "$BACKUP_FILE" && -z "$TIMESTAMP" ]]; then
    echo "Usage: $0 <db> <backup_file>"
    echo "   OR: TIMESTAMP=2025-11-08T14:30:00Z $0 <db>"
    exit 1
fi

# Point-in-Time Recovery
if [[ -n "$TIMESTAMP" ]]; then
    echo "⏰ Restoring $DB to point-in-time: $TIMESTAMP..."
    
    # Download base backup
    BASE_BACKUP=$(aws s3 ls $S3_BUCKET/base/ --recursive | \
                  grep ".tar.gz" | tail -1 | awk '{print $4}')
    aws s3 cp s3://core-platform-backups/$BASE_BACKUP /tmp/
    
    # Extract
    tar -xzf /tmp/$(basename $BASE_BACKUP) -C /tmp/
    
    # Configure recovery
    cat > /tmp/recovery.conf <<EOF
restore_command = 'aws s3 cp s3://core-platform-backups/wal/%f %p'
recovery_target_time = '$TIMESTAMP'
recovery_target_action = 'promote'
EOF
    
    # Restore
    docker compose stop db
    docker compose run --rm db \
        pg_ctl -D /tmp/base_* start
    
    echo "✅ PITR restore completed"
    exit 0
fi

# Regular restore from backup file
echo "🔄 Restoring $DB from $BACKUP_FILE..."

# Drop existing DB
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS ${DB}_restore"
psql -h localhost -U postgres -c "CREATE DATABASE ${DB}_restore"

# Restore
gunzip -c $BACKUP_FILE | psql -h localhost -U postgres -d ${DB}_restore

echo "✅ Restore completed to ${DB}_restore"
echo "💡 Verify data, then rename: ALTER DATABASE ${DB}_restore RENAME TO ${DB}"
```

**File:** `docker-compose.yml` (PostgreSQL with WAL archiving)

```yaml
services:
  db:
    image: postgres:16
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./docker/postgres/postgresql.conf:/etc/postgresql/postgresql.conf
    command: postgres -c config_file=/etc/postgresql/postgresql.conf
```

**File:** `docker/postgres/postgresql.conf`

```ini
# Enable WAL archiving for PITR
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://core-platform-backups/wal/%f'
archive_timeout = 300  # 5 minutes

# Backup settings
max_wal_senders = 3
wal_keep_size = 1GB
```

**Crontab:**

```bash
# /etc/cron.d/pg-backup
0 2 * * * root /opt/core-platform/scripts/backup/pg-backup.sh >> /var/log/pg-backup.log 2>&1
0 */6 * * * root /opt/core-platform/scripts/backup/pg-wal-archive.sh >> /var/log/pg-wal.log 2>&1

# Weekly restore test
0 3 * * 0 root /opt/core-platform/scripts/backup/pg-restore-test.sh >> /var/log/pg-restore-test.log 2>&1
```

**Effort:** 3 dny  
**LOC:** ~600  
**Priority:** HIGH (data protection)

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
