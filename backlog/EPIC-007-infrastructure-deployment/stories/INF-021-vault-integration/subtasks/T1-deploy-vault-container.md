# T1: Deploy HashiCorp Vault Container

**Parent Story:** INF-021 HashiCorp Vault Integration  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 4 hours  
**Owner:** DevOps

---

## 🎯 Objective

Deploy Vault container with persistent storage and HA configuration.

---

## 📋 Tasks

### 1. Create Vault Configuration

**File:** `docker/vault/config/vault.hcl`

```hcl
storage "file" {
  path = "/vault/file"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 1  # TLS handled by Nginx
}

api_addr = "http://vault:8200"
cluster_addr = "http://vault:8201"
ui = true

# Telemetry for Prometheus
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname = true
}
```

### 2. Add to docker-compose.yml

```yaml
services:
  vault:
    image: hashicorp/vault:1.18
    container_name: core-vault
    cap_add:
      - IPC_LOCK
    environment:
      VAULT_ADDR: 'http://0.0.0.0:8200'
      VAULT_API_ADDR: 'http://vault:8200'
    volumes:
      - ./docker/vault/config:/vault/config:ro
      - vault-data:/vault/file
      - vault-logs:/vault/logs
    ports:
      - "8200:8200"
      - "8201:8201"
    command: server
    networks:
      - core-net
    healthcheck:
      test: ["CMD", "vault", "status"]
      interval: 10s
      timeout: 3s
      retries: 3

volumes:
  vault-data:
  vault-logs:
```

### 3. Initialize Vault

**File:** `scripts/vault/init-vault.sh`

```bash
#!/bin/bash
set -euo pipefail

VAULT_ADDR=http://localhost:8200

echo "🔐 Initializing Vault..."

# Initialize (first time only)
vault operator init -key-shares=5 -key-threshold=3 \
    -format=json > vault-keys.json

echo "📝 Vault keys saved to vault-keys.json"
echo "⚠️  IMPORTANT: Store these keys securely!"

# Unseal
echo "🔓 Unsealing Vault..."
UNSEAL_KEYS=$(jq -r '.unseal_keys_b64[]' vault-keys.json | head -3)
for KEY in $UNSEAL_KEYS; do
    vault operator unseal $KEY
done

# Login as root
ROOT_TOKEN=$(jq -r '.root_token' vault-keys.json)
vault login $ROOT_TOKEN

echo "✅ Vault initialized and unsealed"
echo "🎫 Root token: $ROOT_TOKEN"
```

### 4. Enable Secrets Engines

```bash
#!/bin/bash
vault login $ROOT_TOKEN

# KV secrets engine
vault secrets enable -path=secret kv-v2

# Database secrets engine
vault secrets enable database

# PKI secrets engine
vault secrets enable pki

echo "✅ Secrets engines enabled"
```

### 5. Test Vault

```bash
# Health check
curl http://localhost:8200/v1/sys/health

# UI access
open http://localhost:8200/ui

# Write test secret
vault kv put secret/test password="hello"

# Read test secret
vault kv get secret/test
```

---

## ✅ Acceptance Criteria

- [ ] Vault container running
- [ ] Vault initialized (5 keys, threshold 3)
- [ ] Vault unsealed successfully
- [ ] UI accessible at http://localhost:8200/ui
- [ ] Secrets engines enabled (kv, database, pki)
- [ ] Health check endpoint returns 200

---

## 🔗 Dependencies

- Requires Docker Compose
- Blocks T2 (Spring Cloud Vault integration)
