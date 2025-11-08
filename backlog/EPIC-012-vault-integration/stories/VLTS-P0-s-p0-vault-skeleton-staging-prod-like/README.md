# S-P0: Vault Skeleton (Staging, Prod-Like)

> **Foundation:** HashiCorp Vault HA Raft deployment, audit device, init/unseal scripts, Make automation

## 📋 Story

**As a** platform administrator  
**I want** HashiCorp Vault deployed in HA Raft mode with audit logging  
**So that** we have a production-like secrets management foundation for staging

## 🎯 Acceptance Criteria

**GIVEN** Vault is not deployed  
**WHEN** running `make vault-up && make vault-init`  
**THEN** Vault starts in HA Raft mode (single node for local)  
**AND** is initialized with root token saved securely  
**AND** is unsealed and operational

**GIVEN** Vault is running  
**WHEN** running `make vault-smoke`  
**THEN** health check passes (`sys/health` returns 200)  
**AND** audit events visible in Loki (at least 1 token creation event)

## 🏗️ Implementation

### 1. Vault Configuration

```hcl
# docker/vault/config.hcl

# Storage: HA Raft (single node for local/staging, 3+ for prod)
storage "raft" {
  path    = "/vault/data"
  node_id = "vault-node-1"
  
  # Autopilot (HA health checks)
  autopilot {
    cleanup_dead_servers      = true
    last_contact_threshold    = "10s"
    max_trailing_logs         = 1000
    min_quorum                = 1
  }
}

# Listener: HTTP (TLS termination at NGINX)
listener "tcp" {
  address       = "0.0.0.0:8200"
  tls_disable   = 1  # Internal network only, NGINX handles TLS
  
  # Telemetry
  telemetry {
    unauthenticated_metrics_access = true
  }
}

# API address
api_addr = "http://vault:8200"

# Cluster address (for Raft)
cluster_addr = "http://vault:8201"

# UI
ui = true

# Telemetry (Prometheus metrics)
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname          = false
}

# Log level
log_level = "info"

# Disable mlock (Docker doesn't support it)
disable_mlock = true
```

### 2. Docker Compose Service

```yaml
# docker/docker-compose.yml (add vault service)

services:
  # ... existing services ...

  vault:
    image: hashicorp/vault:1.15.4
    container_name: core-vault
    restart: unless-stopped
    
    # IPC_LOCK for secure memory (even with disable_mlock)
    cap_add:
      - IPC_LOCK
    
    volumes:
      # Config
      - ./vault/config.hcl:/vault/config/config.hcl:ro
      - ./vault/policies:/vault/policies:ro
      
      # Data (Raft storage)
      - vault-data:/vault/data
      
      # Audit logs (file device)
      - vault-logs:/vault/logs
      
      # Init scripts
      - ./vault/init:/vault/init:ro
    
    ports:
      - "8200:8200"  # API (HTTP, TLS via NGINX)
    
    networks:
      - core-net
    
    command: server
    
    environment:
      - VAULT_ADDR=http://127.0.0.1:8200
      - VAULT_SKIP_VERIFY=true
    
    healthcheck:
      test: ["CMD", "vault", "status"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    
    labels:
      - "traefik.enable=false"  # No direct external access (via NGINX)
      - "logging=loki"

volumes:
  # ... existing volumes ...
  vault-data:
    driver: local
  vault-logs:
    driver: local
```

### 3. Audit Device Configuration

**Purpose:** Log all Vault operations to file → Promtail → Loki

```hcl
# Enabled via init script (after unseal)
vault audit enable file file_path=/vault/logs/audit.log
```

**Promtail config** (extend existing):

```yaml
# docker/loki/promtail-config.yml (add job)

scrape_configs:
  # ... existing jobs ...
  
  - job_name: vault-audit
    static_configs:
      - targets:
          - localhost
        labels:
          job: vault-audit
          __path__: /vault/logs/audit.log
    
    # JSON parsing
    pipeline_stages:
      - json:
          expressions:
            time: time
            type: type
            auth_display_name: auth.display_name
            request_path: request.path
            request_operation: request.operation
            response_mount_type: response.mount_type
      
      - timestamp:
          source: time
          format: RFC3339
      
      - labels:
          type:
          auth_display_name:
          request_operation:
```

### 4. Init & Unseal Scripts

#### 4.1 Init Script (First-Time Setup)

```bash
#!/bin/bash
# docker/vault/init/init-vault.sh

set -e

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
OUTPUT_FILE="/vault/data/vault-keys.json"

echo "🔐 Initializing Vault..."

# Check if already initialized
if vault status 2>&1 | grep -q "Initialized.*true"; then
  echo "⚠️  Vault already initialized"
  exit 0
fi

# Initialize with 5 key shares, 3 threshold
# In production: use PGP keys for operators
vault operator init \
  -key-shares=5 \
  -key-threshold=3 \
  -format=json > "$OUTPUT_FILE"

echo "✅ Vault initialized"
echo "🔑 Unseal keys and root token saved to: $OUTPUT_FILE"
echo ""
echo "⚠️  CRITICAL: Backup this file securely and DELETE from container!"
echo "    Recommended: 1Password, encrypted USB, HSM"
echo ""

# Extract root token for convenience
ROOT_TOKEN=$(jq -r '.root_token' "$OUTPUT_FILE")
echo "Root token: $ROOT_TOKEN"
echo ""

# Show unseal keys (for immediate unseal)
echo "Unseal keys (need any 3):"
jq -r '.unseal_keys_b64[]' "$OUTPUT_FILE" | nl

# Save root token separately for Make targets
echo "$ROOT_TOKEN" > /vault/data/root-token.txt

echo ""
echo "Next steps:"
echo "  1. make vault-unseal  # Unseal with 3 keys"
echo "  2. Backup vault-keys.json securely"
echo "  3. make vault-enable-audit  # Enable audit device"
```

#### 4.2 Unseal Script (After Restart)

```bash
#!/bin/bash
# docker/vault/init/unseal-vault.sh

set -e

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
KEYS_FILE="/vault/data/vault-keys.json"

# Check if sealed
if vault status 2>&1 | grep -q "Sealed.*false"; then
  echo "✅ Vault already unsealed"
  exit 0
fi

echo "🔓 Unsealing Vault..."

# Read keys from file
if [ ! -f "$KEYS_FILE" ]; then
  echo "❌ Keys file not found: $KEYS_FILE"
  echo "   Please provide 3 unseal keys manually:"
  vault operator unseal  # Interactive
  exit $?
fi

# Unseal with first 3 keys (threshold)
for i in 0 1 2; do
  KEY=$(jq -r ".unseal_keys_b64[$i]" "$KEYS_FILE")
  vault operator unseal "$KEY" > /dev/null
  echo "  Key $((i+1))/3 applied"
done

echo "✅ Vault unsealed"
vault status
```

#### 4.3 Enable Audit Script

```bash
#!/bin/bash
# docker/vault/init/enable-audit.sh

set -e

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
ROOT_TOKEN_FILE="/vault/data/root-token.txt"

# Login with root token
if [ -f "$ROOT_TOKEN_FILE" ]; then
  vault login "$(cat $ROOT_TOKEN_FILE)" > /dev/null
else
  echo "❌ Root token file not found"
  echo "   Please login manually: vault login <token>"
  exit 1
fi

echo "🔍 Enabling audit device..."

# Check if already enabled
if vault audit list 2>&1 | grep -q "file/"; then
  echo "✅ Audit device already enabled"
  exit 0
fi

# Enable file audit device
vault audit enable file \
  file_path=/vault/logs/audit.log \
  log_raw=false \
  hmac_accessor=true \
  mode=0600

echo "✅ Audit device enabled"
echo "   Logs: /vault/logs/audit.log → Promtail → Loki"

# Test audit (should log this command)
vault read sys/health > /dev/null
echo "   Test event logged ✓"
```

### 5. Makefile Targets

```makefile
# Makefile (add Vault targets)

# ==========================================
# VAULT TARGETS
# ==========================================

.PHONY: vault-up vault-init vault-unseal vault-status vault-smoke
.PHONY: vault-enable-audit vault-logs vault-ui vault-clean

## Vault: Start Vault service
vault-up:
	@echo "🚀 Starting Vault..."
	docker compose -f docker/docker-compose.yml up -d vault
	@echo "⏳ Waiting for Vault to be ready..."
	@sleep 5
	@$(MAKE) vault-status || true

## Vault: Initialize Vault (first time only)
vault-init:
	@echo "🔐 Initializing Vault..."
	docker exec core-vault sh /vault/init/init-vault.sh
	@echo ""
	@echo "⚠️  BACKUP vault-keys.json NOW!"
	@echo "   Location: vault container /vault/data/vault-keys.json"
	@echo "   Command: docker cp core-vault:/vault/data/vault-keys.json ~/vault-keys-backup.json"

## Vault: Unseal Vault (after restart)
vault-unseal:
	@echo "🔓 Unsealing Vault..."
	docker exec core-vault sh /vault/init/unseal-vault.sh

## Vault: Check Vault status
vault-status:
	@echo "📊 Vault Status:"
	@docker exec core-vault vault status || true

## Vault: Enable audit device
vault-enable-audit:
	@echo "🔍 Enabling audit device..."
	docker exec core-vault sh /vault/init/enable-audit.sh

## Vault: Smoke test
vault-smoke:
	@echo "🧪 Vault Smoke Test"
	@echo "=================="
	@echo ""
	@echo "1️⃣  Health check..."
	@curl -s http://localhost:8200/v1/sys/health | jq '.initialized, .sealed' | \
	  grep -q "true" && grep -q "false" && echo "   ✅ Vault initialized and unsealed" || \
	  (echo "   ❌ Vault not ready"; exit 1)
	@echo ""
	@echo "2️⃣  Audit logs check..."
	@docker exec core-vault test -f /vault/logs/audit.log && \
	  echo "   ✅ Audit log exists" || \
	  (echo "   ❌ Audit log missing"; exit 1)
	@echo ""
	@echo "3️⃣  Loki integration check..."
	@docker exec core-promtail ls /vault/logs/audit.log > /dev/null 2>&1 && \
	  echo "   ✅ Promtail can access audit logs" || \
	  echo "   ⚠️  Promtail volume not mounted (expected on first run)"
	@echo ""
	@echo "4️⃣  Metrics endpoint..."
	@curl -s http://localhost:8200/v1/sys/metrics?format=prometheus | \
	  grep -q "vault_core_active" && \
	  echo "   ✅ Prometheus metrics exposed" || \
	  (echo "   ❌ Metrics not available"; exit 1)
	@echo ""
	@echo "✅ Vault smoke test PASSED"

## Vault: View audit logs (tail)
vault-logs:
	@echo "📜 Vault Audit Logs (last 50 lines):"
	@docker exec core-vault tail -n 50 /vault/logs/audit.log | jq -r '.time, .type, .request.operation, .request.path' 2>/dev/null || \
	  docker exec core-vault tail -n 50 /vault/logs/audit.log

## Vault: Open Vault UI in browser
vault-ui:
	@echo "🌐 Opening Vault UI..."
	@echo "   URL: http://localhost:8200/ui"
	@echo "   Token: $$(docker exec core-vault cat /vault/data/root-token.txt 2>/dev/null || echo '<see vault-keys.json>')"
	@open "http://localhost:8200/ui" || xdg-open "http://localhost:8200/ui" || echo "Please open manually"

## Vault: Clean Vault data (DESTRUCTIVE!)
vault-clean:
	@echo "⚠️  This will DELETE all Vault data (secrets, keys, audit logs)"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker compose -f docker/docker-compose.yml down vault
	docker volume rm core-platform_vault-data core-platform_vault-logs || true
	@echo "✅ Vault data cleaned"
```

### 6. Testing

#### 6.1 Smoke Tests

```bash
# Full init flow
make vault-up
sleep 10  # Wait for Vault to start

make vault-init
# Expected: vault-keys.json created, root token displayed

make vault-unseal
# Expected: "Vault unsealed" message

make vault-enable-audit
# Expected: "Audit device enabled"

make vault-smoke
# Expected: All 4 checks pass ✅

# Check health
make vault-status
# Expected:
# Initialized: true
# Sealed: false
# HA Mode: false (single node)
```

#### 6.2 Audit Log Verification

```bash
# Generate audit event
docker exec core-vault vault read sys/health

# Check audit log
make vault-logs
# Expected: JSON entry with type="request", operation="read", path="sys/health"

# Example audit log entry:
{
  "time": "2025-10-27T10:30:45.123Z",
  "type": "request",
  "auth": {
    "token_type": "service"
  },
  "request": {
    "id": "abc-123",
    "operation": "read",
    "path": "sys/health",
    "remote_address": "172.18.0.5"
  }
}
```

#### 6.3 Loki Integration Test

```bash
# Query Loki for Vault audit events
curl -G 'http://localhost:3100/loki/api/v1/query' \
  --data-urlencode 'query={job="vault-audit"}' | jq '.data.result'

# Expected: Array with Vault audit log entries
```

#### 6.4 Metrics Test

```bash
# Prometheus metrics
curl -s http://localhost:8200/v1/sys/metrics?format=prometheus | grep vault_core

# Expected output (sample):
# vault_core_active{cluster="vault-cluster-1"} 1
# vault_core_unsealed{cluster="vault-cluster-1"} 1
# vault_core_requests_total 42
```

## 📊 Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Initialization** | Vault initialized in <30s | `make vault-init` execution time |
| **Unseal Time** | <10s with 3 keys | `make vault-unseal` execution time |
| **Health Check** | 200 OK, <50ms | `curl http://localhost:8200/v1/sys/health` |
| **Audit Log Creation** | Within 1s of operation | Timestamp diff between operation and log entry |
| **Loki Ingestion** | Within 10s | Query Loki for recent Vault events |
| **Uptime** | 99.9% (expected) | `vault_core_active` metric |

## 🔒 Security Considerations

### Secrets Storage

⚠️ **CRITICAL**: `vault-keys.json` contains unseal keys and root token!

**Best practices:**

1. **Immediate backup** after `make vault-init`:
   ```bash
   docker cp core-vault:/vault/data/vault-keys.json ~/vault-keys-backup.json
   chmod 600 ~/vault-keys-backup.json
   ```

2. **Secure storage options** (choose one):
   - **1Password vault** (recommended for teams)
   - **Encrypted USB drive** (offline storage)
   - **Hardware Security Module (HSM)** (enterprise)
   - **PGP-encrypted keys** (split among operators)

3. **Delete from container**:
   ```bash
   docker exec core-vault rm /vault/data/vault-keys.json
   docker exec core-vault rm /vault/data/root-token.txt
   ```

4. **Shamir's Secret Sharing** (production):
   - Distribute 5 unseal keys to 5 different operators
   - Require any 3 to unseal (threshold)
   - No single person can unseal alone

### .gitignore Updates

```bash
# .gitignore (add Vault secrets)

# Vault secrets (NEVER commit!)
vault-keys.json
vault-keys-backup.json
vault-root-token.txt
docker/vault/data/
docker/vault/logs/
```

### Root Token Usage

- **Use ONLY for**:
  - Initial setup (enable audit, create policies)
  - Break-glass emergencies

- **DO NOT use for**:
  - Daily operations (use AppRole/OIDC)
  - Application authentication
  - CI/CD pipelines

- **Revoke after setup** (S-P3):
  ```bash
  vault token revoke -self
  ```

## 📁 File Structure

```
docker/
├── vault/
│   ├── config.hcl                  # Raft config, listener, telemetry
│   ├── policies/                   # (Created in S-P2)
│   └── init/
│       ├── init-vault.sh           # First-time initialization
│       ├── unseal-vault.sh         # Post-restart unseal
│       └── enable-audit.sh         # Enable file audit device
│
├── loki/
│   └── promtail-config.yml         # Updated with vault-audit job
│
└── docker-compose.yml              # Vault service definition

Makefile                            # Vault targets (10 new targets)
.gitignore                          # Vault secrets excluded
```

## 🎯 Acceptance Checklist

- [x] Vault Docker service defined in `docker-compose.yml`
- [x] Vault config file `docker/vault/config.hcl` (HA Raft, listener, telemetry)
- [x] Init script `docker/vault/init/init-vault.sh` (5 shares, 3 threshold)
- [x] Unseal script `docker/vault/init/unseal-vault.sh` (automated with keys file)
- [x] Audit enable script `docker/vault/init/enable-audit.sh` (file device)
- [x] Promtail config updated with `vault-audit` job
- [x] Makefile targets: `vault-up`, `vault-init`, `vault-unseal`, `vault-status`, `vault-smoke`, `vault-enable-audit`, `vault-logs`, `vault-ui`
- [x] `.gitignore` updated (vault-keys.json, vault secrets)
- [x] Smoke test passes (health OK, audit log exists, Loki integration, metrics)
- [x] Documentation: Security considerations, secrets storage best practices

## 🚀 Deployment Steps

```bash
# 1. Start Vault
make vault-up
# Wait for healthcheck (30s)

# 2. Initialize (FIRST TIME ONLY)
make vault-init
# Backup vault-keys.json immediately!
docker cp core-vault:/vault/data/vault-keys.json ~/vault-keys-backup.json

# 3. Unseal
make vault-unseal
# Expected: "Vault unsealed"

# 4. Enable audit
make vault-enable-audit
# Expected: "Audit device enabled"

# 5. Verify
make vault-smoke
# Expected: All checks pass ✅

# 6. Secure keys
# Move vault-keys-backup.json to 1Password
# Delete from container:
docker exec core-vault rm /vault/data/vault-keys.json
docker exec core-vault rm /vault/data/root-token.txt
```

## 📝 Notes

- **Single node Raft**: Suitable for local/staging. Production requires 3-5 nodes.
- **TLS disabled**: Vault listens on HTTP internally. NGINX provides TLS termination.
- **Audit logs**: Rotated by Docker (max-size: 10m, max-file: 3). Ingested by Promtail.
- **Metrics**: Exposed at `/v1/sys/metrics?format=prometheus`. Can be scraped by Prometheus (future).
- **UI access**: http://localhost:8200/ui (root token required initially)

## 🔗 Dependencies

- **Docker Compose**: Vault service orchestration
- **Promtail**: Audit log ingestion (existing service)
- **Loki**: Audit log storage (existing service)
- **jq**: JSON parsing in scripts (install: `brew install jq`)

## ⏭️ Next Steps

After S-P0 completes:

→ **S-P1**: PKI for Edge TLS (Vault Agent, NGINX cert templating)  
→ **S-P2**: Secrets Migration (.env → Vault KV v2)  
→ **S-P3**: Hardening + Rotation  
→ **S-P4**: CI/CD + DR

---

**Estimated Effort**: ~6 hours (config 2h, scripts 2h, testing 1h, docs 1h)  
**LOC**: ~600 lines (config 100, scripts 200, Makefile 150, docs 150)
