# Disaster Recovery Plan

## Recovery Objectives

| Metric | Target | Current |
| --- | --- | --- |
| RTO (Recovery Time Objective) | 4 hours | TBD |
| RPO (Recovery Point Objective) | 1 hour | TBD |

## Scope

Systems covered:
- Core Platform backend + frontend
- PostgreSQL databases (core, keycloak, grafana)
- Keycloak, Grafana, Loki, Prometheus
- Redis cache

Out of scope (handled separately):
- External IdP availability
- Customer-managed DNS providers

## Roles and Contacts

| Role | Owner | Contact |
| --- | --- | --- |
| Incident Commander | Platform Lead | incidents@company.com |
| Database Lead | DBA | dba@company.com |
| Infra Lead | Ops | ops@company.com |
| Security Lead | Security | security@company.com |

## Communication Plan

Severity levels:
- P1 Critical: full outage, data loss risk
- P2 High: partial outage, degraded data
- P3 Medium: minor degradation

Notify:
- Slack: #incidents
- Email: incidents@company.com
- Status page: status.core-platform.com

## Primary/Secondary Environments

Primary:
- Region: PRIMARY_REGION
- Domain: core-platform.com
- DNS managed in Route53 (hosted zone ID in secrets)

Secondary:
- Region: SECONDARY_REGION
- Domain: dr.core-platform.com
- Standby deployment in warm state

## Runbooks

### Scenario 1: Database Corruption

Detection:
- Alerts: database errors, query failures
- Logs: PostgreSQL corruption messages

Recovery steps:
1. Freeze writes
   ```bash
   docker compose stop backend frontend
   ```
2. Restore point-in-time backup
   ```bash
   TIMESTAMP=$(date -u -d "1 hour ago" +"%Y-%m-%dT%H:%M:%SZ")
   bash scripts/backup/pg-restore.sh core TIMESTAMP=$TIMESTAMP
   ```
3. Verify data integrity
   ```bash
   make db-verify
   make smoke-tests
   ```
4. Resume services
   ```bash
   docker compose up -d
   ```

Expected RTO: 2 hours

### Scenario 2: Complete Server Failure

Detection:
- Health checks down
- Host unreachable

Recovery steps:
1. Promote secondary region
   ```bash
   bash scripts/dr/failover-to-secondary.sh --dry-run=false
   ```
2. Verify services
   ```bash
   make smoke-tests BASE_URL=https://dr.core-platform.com
   ```
3. Restore latest backups
   ```bash
   bash scripts/dr/restore-from-offsite.sh --dry-run=false
   ```

Expected RTO: 2 hours

### Scenario 3: Ransomware Attack

Detection:
- Unexpected file encryption
- Anti-malware alerts

Recovery steps:
1. Isolate host
   ```bash
   docker compose down
   ```
2. Provision clean host
3. Restore offsite backups
   ```bash
   bash scripts/dr/restore-from-offsite.sh --dry-run=false --restore-point="YYYY-MM-DD"
   ```
4. Rotate credentials, complete security review

Expected RTO: 6-12 hours

### Scenario 4: AWS Region Outage

Detection:
- Region-wide cloud outage
- Primary region unavailable

Recovery steps:
1. Activate secondary region
2. Execute DNS switch
   ```bash
   bash scripts/dr/failover-to-secondary.sh --dry-run=false
   ```
3. Verify critical services
4. Communicate status update

Expected RTO: 2-4 hours

## Failover Automation

Failover script:
- File: scripts/dr/failover-to-secondary.sh
- Inputs: HOSTED_ZONE_ID, PRIMARY_HOST, SECONDARY_HOST, SLACK_WEBHOOK
- Supports --dry-run for validation

## DR Drills

Schedule:
- Quarterly full DR drill
- Monthly backup restore validation

Metrics to capture:
- Start time, end time, total RTO
- Last successful backup timestamp for RPO
- Failures and action items

Drill reporting:
- Log results in docs/disaster-recovery/DR_DRILLS.md
- Track action items until resolved
