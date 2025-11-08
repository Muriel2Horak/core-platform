# INF-017: Disaster Recovery Plan & Procedures

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** MEDIUM  
**Effort:** 2 dny, ~500 LOC  
**Owner:** Ops + Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```
# ŽÁDNÝ disaster recovery plan!
# Co kdyby:
# - Server crashnul → jak rychle restore?
# - Database corrupted → kde jsou backups?
# - AWS region down → máme failover?
# - Ransomware → jsou backups offsite?

# NENÍ dokumentováno:
# - RTO (Recovery Time Objective)
# - RPO (Recovery Point Objective)
# - Failover procedures
# - Contact list pro incidents
```

### Goal

**Complete DR plan:**

```
RTO: 4 hours   (max downtime)
RPO: 1 hour    (max data loss)

Procedures:
1. Database Recovery (PITR)
2. Service Failover (multi-region)
3. Configuration Restore (from Git)
4. Communication Plan (stakeholders)
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **DR Runbooks**
   - Procedure: Database corruption
   - Procedure: Complete server failure
   - Procedure: Ransomware attack
   - Procedure: AWS outage

2. ✅ **Automated Failover**
   - Multi-region deployment (primary + secondary)
   - Health check: Auto-failover if primary down
   - DNS update automation

3. ✅ **Quarterly DR Drills**
   - Simulate: Server failure
   - Test: Restore from backups
   - Measure: RTO/RPO compliance

### Implementation

**File:** `docs/disaster-recovery/DR_PLAN.md`

```markdown
# Disaster Recovery Plan

## Recovery Objectives

| Metric | Target | Current |
|--------|--------|---------|
| RTO (Recovery Time) | 4 hours | TBD |
| RPO (Data Loss) | 1 hour | TBD |

## Scenarios

### Scenario 1: Database Corruption

**Detection:**
- Alert: Database queries failing
- Log: PostgreSQL corruption errors

**Recovery Steps:**

1. **Isolate** (5 min)
   ```bash
   # Stop writes to corrupted DB
   docker compose stop backend frontend
   ```

2. **Restore** (30-60 min)
   ```bash
   # Point-in-time recovery
   TIMESTAMP=$(date -d "1 hour ago" -Iseconds)
   bash scripts/backup/pg-restore.sh core TIMESTAMP=$TIMESTAMP
   ```

3. **Verify** (15 min)
   ```bash
   # Check data integrity
   make db-verify
   make smoke-tests
   ```

4. **Resume** (5 min)
   ```bash
   # Bring services back online
   docker compose up -d
   ```

**Total RTO:** ~2 hours

---

### Scenario 2: Complete Server Failure

**Detection:**
- Alert: All health checks failing
- Monitoring: Server unreachable

**Recovery Steps:**

1. **Failover to Secondary Region** (15 min)
   ```bash
   # Activate standby server
   ssh ops@dr-server.core-platform.com
   cd /opt/core-platform
   make up ENV=production
   ```

2. **Update DNS** (5 min)
   ```bash
   # Point domain to DR server
   aws route53 change-resource-record-sets \
     --hosted-zone-id Z123ABC \
     --change-batch file://failover-dns.json
   ```

3. **Verify Services** (10 min)
   ```bash
   # Smoke tests on DR server
   make smoke-tests BASE_URL=https://dr.core-platform.com
   ```

4. **Restore Latest Data** (30-60 min)
   ```bash
   # Sync from S3 backups
   aws s3 sync s3://backups/latest /var/backups/
   bash scripts/backup/pg-restore.sh core /var/backups/core_latest.sql.gz
   ```

**Total RTO:** ~2 hours

---

### Scenario 3: Ransomware Attack

**Detection:**
- Alert: Unusual file encryption activity
- Log: Mass file modifications

**Recovery Steps:**

1. **Isolate Immediately** (5 min)
   ```bash
   # Disconnect from network
   docker compose down
   iptables -A INPUT -j DROP
   iptables -A OUTPUT -j DROP
   ```

2. **Assess Damage** (30 min)
   ```bash
   # Check infected files
   find / -name "*.encrypted" -mtime -1
   # Verify backups not affected
   aws s3 ls s3://backups-offsite/
   ```

3. **Restore from Offsite Backups** (2-3 hours)
   ```bash
   # Fresh server deployment
   # Use backups from BEFORE attack
   bash scripts/dr/restore-from-offsite.sh DATE=2025-11-07
   ```

4. **Security Audit** (4-8 hours)
   ```bash
   # Scan for vulnerabilities
   # Update all credentials
   # Patch systems
   ```

**Total RTO:** ~6-12 hours (security priority)

---

## Contact List

| Role | Name | Phone | Email |
|------|------|-------|-------|
| Incident Commander | John Doe | +420 XXX XXX XXX | john@company.com |
| DBA | Jane Smith | +420 XXX XXX XXX | jane@company.com |
| Platform Lead | Bob Johnson | +420 XXX XXX XXX | bob@company.com |
| AWS Support | - | - | aws-support@company.com |

## Communication Plan

**Incident Severity Levels:**

- **P1 (Critical):** Complete outage → Notify ALL stakeholders
- **P2 (High):** Partial outage → Notify tech team + management
- **P3 (Medium):** Degraded performance → Notify tech team

**Notification Channels:**

1. Slack: #incidents channel
2. Email: incidents@company.com
3. SMS: On-call rotation
4. Status Page: status.core-platform.com

## Testing Schedule

| Test Type | Frequency | Next Test |
|-----------|-----------|-----------|
| DR Drill (full) | Quarterly | 2025-12-01 |
| Backup Restore | Monthly | 2025-11-15 |
| Failover Test | Quarterly | 2025-12-15 |
```

**File:** `scripts/dr/failover-to-secondary.sh`

```bash
#!/bin/bash
set -euo pipefail

SECONDARY_HOST=${SECONDARY_HOST:-dr.core-platform.com}
PRIMARY_HOST=${PRIMARY_HOST:-core-platform.com}

echo "🚨 Initiating failover to secondary region..."

# 1. Verify secondary is healthy
if ! curl -sf https://$SECONDARY_HOST/health; then
    echo "❌ Secondary region is NOT healthy!"
    exit 1
fi

# 2. Update DNS (Route53)
aws route53 change-resource-record-sets \
    --hosted-zone-id $HOSTED_ZONE_ID \
    --change-batch '{
      "Changes": [{
        "Action": "UPSERT",
        "ResourceRecordSet": {
          "Name": "'$PRIMARY_HOST'",
          "Type": "A",
          "TTL": 60,
          "ResourceRecords": [{"Value": "'$(dig +short $SECONDARY_HOST)'"}]
        }
      }]
    }'

# 3. Notify
curl -X POST $SLACK_WEBHOOK \
     -d "{\"text\": \"🚨 Failover to secondary region completed\"}"

echo "✅ Failover completed. Monitor: https://$SECONDARY_HOST"
```

**Effort:** 2 dny  
**LOC:** ~500 (docs + scripts)  
**Priority:** MEDIUM

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
