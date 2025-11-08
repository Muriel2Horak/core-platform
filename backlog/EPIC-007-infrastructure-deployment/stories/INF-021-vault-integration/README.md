# INF-021: HashiCorp Vault Integration

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 3 dny, ~900 LOC  
**Owner:** Security + Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```bash
# Secrets management je KATASTROFA:
.env                    # 47 plain-text variables
docker/ssl/*.key.pem   # SSL private keys
secrets/*.txt          # Docker secrets (still files!)

# ŽÁDNÝ centrální secrets management
# ŽÁDNÁ rotation automation
# ŽÁDNÝ audit trail (kdo přistupoval k secretům)
```

**Issues:**
- Secrets v plain text files (Git leak risk)
- Manual rotation (zapomene se)
- Žádný audit (kdo četl DB password?)
- Different secrets per environment (dev/staging/prod)

### Goal

**Vault jako single source of truth:**

```
HashiCorp Vault
  ├─ KV Secrets Engine
  │  ├─ secret/core-platform/dev/*
  │  ├─ secret/core-platform/staging/*
  │  └─ secret/core-platform/prod/*
  │
  ├─ Database Secrets Engine
  │  └─ Dynamic DB credentials (auto-rotation)
  │
  ├─ PKI Secrets Engine
  │  └─ Certificate generation + renewal
  │
  └─ Audit Log
     └─ WHO accessed WHAT WHEN
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Vault Deployment**
   - Docker Compose service
   - HA mode (3 replicas)
   - Auto-unseal (cloud KMS)

2. ✅ **Application Integration**
   - Spring Cloud Vault
   - Dynamic config reload
   - Lease renewal automation

3. ✅ **Dynamic Secrets**
   - PostgreSQL: Dynamic user creation
   - Rotation: Every 24 hours
   - Revocation on service shutdown

4. ✅ **PKI Management**
   - Internal CA v Vault
   - Certificate issuance automation
   - Auto-renewal 30 days before expiry

### Implementation

**File:** `docker-compose.yml` (Vault service)

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
      VAULT_CLUSTER_ADDR: 'http://vault:8201'
    volumes:
      - ./vault/config:/vault/config:ro
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

# Auto-unseal with cloud KMS (production)
seal "awskms" {
  region     = "eu-central-1"
  kms_key_id = "${VAULT_UNSEAL_KEY_ID}"
}

# Telemetry
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname = true
}
```

**File:** `scripts/vault/init-vault.sh` (Initial setup)

```bash
#!/bin/bash
set -euo pipefail

VAULT_ADDR=http://localhost:8200

echo "🔐 Initializing Vault..."

# Initialize (first time only)
vault operator init -key-shares=5 -key-threshold=3 \
    -format=json > vault-keys.json

# Unseal
UNSEAL_KEYS=$(jq -r '.unseal_keys_b64[]' vault-keys.json | head -3)
for KEY in $UNSEAL_KEYS; do
    vault operator unseal $KEY
done

# Login as root
ROOT_TOKEN=$(jq -r '.root_token' vault-keys.json)
vault login $ROOT_TOKEN

echo "✅ Vault initialized and unsealed"

# Enable secrets engines
vault secrets enable -path=secret kv-v2
vault secrets enable database
vault secrets enable pki

echo "✅ Secrets engines enabled"
```

**File:** `scripts/vault/configure-database-secrets.sh`

```bash
#!/bin/bash
set -euo pipefail

VAULT_ADDR=http://localhost:8200

# Configure PostgreSQL database connection
vault write database/config/core-db \
    plugin_name=postgresql-database-plugin \
    allowed_roles="backend,keycloak,grafana" \
    connection_url="postgresql://{{username}}:{{password}}@db:5432/core?sslmode=disable" \
    username="postgres" \
    password="$POSTGRES_ADMIN_PASSWORD"

# Create role for backend (dynamic credentials)
vault write database/roles/backend \
    db_name=core-db \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

echo "✅ Database secrets engine configured"
```

**File:** `backend/pom.xml` (Spring Cloud Vault)

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

**File:** `backend/src/main/resources/bootstrap.yml`

```yaml
spring:
  application:
    name: core-backend
  
  cloud:
    vault:
      uri: http://vault:8200
      authentication: TOKEN
      token: ${VAULT_TOKEN}
      
      # KV Secrets
      kv:
        enabled: true
        backend: secret
        application-name: core-platform/${ENVIRONMENT}
      
      # Database secrets (dynamic)
      database:
        enabled: true
        role: backend
        backend: database
        
        # Auto-renew credentials
        renewal:
          interval: 30m
          renew-threshold: 80
      
      # PKI (SSL certificates)
      pki:
        enabled: true
        role: web-server
        common-name: backend.core-platform.local
        alt-names: 
          - api.core-platform.local
          - admin.core-platform.local

# Datasource uses Vault-provided credentials
  datasource:
    url: ${DATABASE_URL}
    username: ${spring.cloud.vault.database.username}
    password: ${spring.cloud.vault.database.password}
```

**File:** `backend/src/main/java/cz/muriel/core/config/VaultConfig.java`

```java
@Configuration
@EnableConfigurationProperties
public class VaultConfig {
    
    @Value("${spring.cloud.vault.database.username}")
    private String dynamicDbUser;
    
    @Value("${spring.cloud.vault.database.password}")
    private String dynamicDbPassword;
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(databaseUrl);
        
        // Dynamic credentials from Vault
        dataSource.setUsername(dynamicDbUser);
        dataSource.setPassword(dynamicDbPassword);
        
        // Lease-aware connection pool
        dataSource.setMaxLifetime(3600000); // 1 hour (match Vault TTL)
        
        return dataSource;
    }
    
    @EventListener(LeaseExpiredEvent.class)
    public void handleLeaseExpired(LeaseExpiredEvent event) {
        log.warn("Vault lease expired: {}", event.getSource());
        // Connection pool will refresh with new credentials
    }
}
```

**File:** `scripts/vault/setup-pki.sh` (PKI for internal SSL)

```bash
#!/bin/bash
set -euo pipefail

# Enable PKI secrets engine
vault secrets enable pki

# Configure max lease TTL (10 years)
vault secrets tune -max-lease-ttl=87600h pki

# Generate root CA
vault write pki/root/generate/internal \
    common_name="Core Platform Internal CA" \
    ttl=87600h

# Configure CA and CRL URLs
vault write pki/config/urls \
    issuing_certificates="http://vault:8200/v1/pki/ca" \
    crl_distribution_points="http://vault:8200/v1/pki/crl"

# Create role for server certificates
vault write pki/roles/web-server \
    allowed_domains="core-platform.local" \
    allow_subdomains=true \
    max_ttl="720h" \
    require_cn=false

echo "✅ PKI configured"
```

**File:** `scripts/vault/issue-certificate.sh`

```bash
#!/bin/bash
set -euo pipefail

DOMAIN=${1:-backend.core-platform.local}

# Request certificate from Vault
vault write -format=json pki/issue/web-server \
    common_name="$DOMAIN" \
    ttl="720h" \
    | jq -r '.data.certificate' > docker/ssl/server.crt.pem

vault write -format=json pki/issue/web-server \
    common_name="$DOMAIN" \
    ttl="720h" \
    | jq -r '.data.private_key' > docker/ssl/server.key.pem

echo "✅ Certificate issued for $DOMAIN"
```

**File:** `docker/prometheus/vault-exporter.yml` (Monitoring)

```yaml
services:
  vault-exporter:
    image: hashicorp/vault-exporter:latest
    command:
      - '--vault.address=http://vault:8200'
      - '--vault.token=${VAULT_PROMETHEUS_TOKEN}'
    ports:
      - "9410:9410"
    networks:
      - core-net
```

**File:** `.github/workflows/vault-audit.yml` (CI check)

```yaml
name: Vault Secrets Audit

on:
  schedule:
    - cron: '0 0 * * *'  # Daily

jobs:
  audit-secrets:
    runs-on: ubuntu-latest
    steps:
      - name: Check for plaintext secrets
        run: |
          # Scan for hardcoded secrets
          if grep -r "password=" *.properties; then
            echo "❌ Plaintext secrets found!"
            exit 1
          fi
      
      - name: Vault health check
        run: |
          vault status
          vault read sys/health
```

**Effort:** 3 dny  
**LOC:** ~900  
**Priority:** 🔥 CRITICAL

---

## 🔗 DEPENDENCIES

**Blocks:**
- INF-003: Docker Secrets (replaces file-based secrets)
- All services (centralized secrets management)

**Requires:**
- Docker Compose setup
- PostgreSQL running

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
