---
id: INF-004
epic: EPIC-007-infrastructure-deployment
title: "SSL Certificate Rotation Automation"
priority: P2
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-004-ssl-rotation/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---


# INF-004: SSL Certificate Rotation Automation

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** MEDIUM  
**Effort:** 2 dny, ~400 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```bash
# SSL cert generation je MANUÁLNÍ:
bash docker/ssl/generate-ssl.sh
# → Creates self-signed cert
# → 90 days validity
# → Manual renewal required
# → NENÍ tracking expiry date
```

**Git Evidence:**
- Multiple commits fixing expired certificates
- "fix(ssl): Regenerate expired certificates"
- Manual rotation každé 3 měsíce

### Goal

**Automated SSL rotation:**

```bash
# Cron job (weekly check):
0 0 * * 0 /opt/core-platform/scripts/ssl/check-and-rotate.sh

# Auto-rotate if expiry < 30 days
# Zero-downtime reload
# Notification on rotation
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Expiry Detection**
   - Check cert expiry daily
   - Alert if < 30 days remaining
   - Auto-rotate if < 7 days

2. ✅ **Zero-Downtime Rotation**
   - Generate new cert
   - Hot-reload Nginx/Traefik
   - No service interruption

3. ✅ **Audit Trail**
   - Log: Rotation timestamp
   - Log: Old cert expiry
   - Log: New cert expiry

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [TASK-004-01: Expiry check + rotation script](subtasks/TASK-004-01-expiry-check-rotation-script.md) | 6h | none |
| 2 | [TASK-004-02: Zero-downtime reload + audit log](subtasks/TASK-004-02-zero-downtime-reload-logging.md) | 6h | TASK-004-01 |
| 3 | [TASK-004-03: Cron schedule + notifications](subtasks/TASK-004-03-cron-notify.md) | 4h | TASK-004-01, TASK-004-02 |

### Implementation

**File:** `scripts/ssl/check-and-rotate.sh`

```bash
#!/bin/bash
set -euo pipefail

CERT_FILE=docker/ssl/server.crt.pem
EXPIRY_DAYS=$(openssl x509 -in $CERT_FILE -noout -enddate | \
              awk -F= '{print $2}' | xargs -I{} date -d "{}" +%s)
NOW=$(date +%s)
DAYS_LEFT=$(( ($EXPIRY_DAYS - $NOW) / 86400 ))

echo "📅 SSL Certificate expires in $DAYS_LEFT days"

if [ $DAYS_LEFT -lt 7 ]; then
    echo "🔄 Auto-rotating SSL certificate..."
    
    # Backup old cert
    cp $CERT_FILE docker/ssl/server.crt.pem.backup
    
    # Generate new cert
    bash docker/ssl/generate-ssl.sh
    
    # Reload services
    docker compose exec nginx nginx -s reload
    docker compose restart keycloak
    
    echo "✅ SSL certificate rotated successfully"
    
    # Notify
    curl -X POST https://slack.com/webhooks/YOUR_WEBHOOK \
         -d "{\"text\": \"✅ SSL cert rotated. Valid for 90 days.\"}"
fi
```

**File:** `scripts/ssl/generate-ssl.sh` (enhanced)

```bash
#!/bin/bash
set -euo pipefail

DOMAIN=${DOMAIN:-core-platform.local}
VALIDITY_DAYS=90

# Generate private key
openssl genrsa -out docker/ssl/server.key.pem 4096

# Generate CSR
openssl req -new \
    -key docker/ssl/server.key.pem \
    -out docker/ssl/server.csr \
    -subj "/CN=*.${DOMAIN}/O=Core Platform/C=CZ"

# Generate certificate
openssl x509 -req \
    -in docker/ssl/server.csr \
    -signkey docker/ssl/server.key.pem \
    -out docker/ssl/server.crt.pem \
    -days $VALIDITY_DAYS \
    -extensions v3_req \
    -extfile <(cat <<EOF
[v3_req]
subjectAltName = DNS:*.${DOMAIN},DNS:${DOMAIN}
EOF
)

# Verify
openssl x509 -in docker/ssl/server.crt.pem -text -noout

echo "✅ SSL certificate generated (valid for $VALIDITY_DAYS days)"
```

**Crontab Entry:**

```bash
# /etc/cron.d/ssl-rotation
0 2 * * * root /opt/core-platform/scripts/ssl/check-and-rotate.sh >> /var/log/ssl-rotation.log 2>&1
```

**Effort:** 2 dny  
**LOC:** ~400  
**Blocks:** INF-005 (Let's Encrypt uses this for manual fallback)

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
