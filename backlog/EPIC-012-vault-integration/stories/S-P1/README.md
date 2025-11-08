# S-P1: PKI for Edge TLS (NGINX Certs via Vault)

> **Dynamic Certificates:** Vault PKI engine issues *.core-platform.local certs, Vault Agent templates for NGINX, auto-renewal

## 📋 Story

**As a** platform administrator  
**I want** NGINX to use TLS certificates issued by Vault PKI  
**So that** certificates are centrally managed, auto-renewed, and auditable

## 🎯 Acceptance Criteria

**GIVEN** Vault PKI engine is configured  
**WHEN** running `make vault-pki-issue`  
**THEN** Vault issues a certificate for `*.core-platform.local`  
**AND** Vault Agent templates certificate to `/etc/nginx/ssl/edge.crt`  
**AND** NGINX reloads with new certificate

**GIVEN** Certificate TTL is 24 hours  
**WHEN** 23 hours elapse  
**THEN** Vault Agent automatically renews certificate  
**AND** NGINX reloads without downtime

**GIVEN** NGINX is serving HTTPS  
**WHEN** running `curl https://admin.core-platform.local`  
**THEN** certificate is issued by Vault intermediate CA  
**AND** SAN includes `*.core-platform.local, core-platform.local`

## 🏗️ Implementation

### 1. Vault PKI Setup

#### 1.1 Root CA (Offline, Long-Lived)

```bash
# docker/vault/init/pki-setup.sh

#!/bin/bash
set -e

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
ROOT_TOKEN=$(cat /vault/data/root-token.txt)

vault login "$ROOT_TOKEN" > /dev/null

echo "🔐 Setting up Vault PKI..."

# ========================================
# 1. ROOT CA (Offline, 10-year validity)
# ========================================

echo "1️⃣  Creating Root CA..."

# Enable PKI secrets engine for root
vault secrets enable -path=pki_root pki

# Tune max lease TTL (10 years)
vault secrets tune -max-lease-ttl=87600h pki_root

# Generate root CA certificate
vault write -format=json pki_root/root/generate/internal \
  common_name="Core Platform Root CA" \
  issuer_name="root-2025" \
  ttl=87600h \
  key_bits=4096 \
  exclude_cn_from_sans=true \
  | tee /vault/data/pki-root-ca.json

# Save root CA cert for distribution
vault read -field=certificate pki_root/cert/ca > /vault/data/root-ca.crt

echo "   ✅ Root CA generated"
echo "      Issuer: root-2025"
echo "      TTL: 10 years"
echo "      Certificate: /vault/data/root-ca.crt"

# ========================================
# 2. INTERMEDIATE CA (Online, 1-year)
# ========================================

echo ""
echo "2️⃣  Creating Intermediate CA..."

# Enable PKI secrets engine for intermediate
vault secrets enable -path=pki_int pki

# Tune max lease TTL (1 year)
vault secrets tune -max-lease-ttl=8760h pki_int

# Generate CSR for intermediate
vault write -format=json pki_int/intermediate/generate/internal \
  common_name="Core Platform Intermediate CA" \
  issuer_name="intermediate-2025" \
  key_bits=4096 \
  exclude_cn_from_sans=true \
  | tee /vault/data/pki-int-csr.json

# Extract CSR
CSR=$(jq -r '.data.csr' /vault/data/pki-int-csr.json)

# Sign intermediate CSR with root CA
vault write -format=json pki_root/root/sign-intermediate \
  issuer_ref="root-2025" \
  csr="$CSR" \
  format=pem_bundle \
  ttl=8760h \
  | tee /vault/data/pki-int-signed.json

# Extract signed certificate
CERT=$(jq -r '.data.certificate' /vault/data/pki-int-signed.json)

# Import signed certificate back to intermediate CA
vault write pki_int/intermediate/set-signed \
  certificate="$CERT"

echo "   ✅ Intermediate CA created and signed"
echo "      Issuer: intermediate-2025"
echo "      TTL: 1 year"

# ========================================
# 3. PKI ROLE (*.core-platform.local)
# ========================================

echo ""
echo "3️⃣  Creating PKI role for edge certificates..."

vault write pki_int/roles/edge-tls \
  issuer_ref="intermediate-2025" \
  allowed_domains="core-platform.local" \
  allow_subdomains=true \
  allow_glob_domains=false \
  allow_wildcard_certificates=true \
  max_ttl=24h \
  key_bits=2048 \
  key_type=rsa \
  server_flag=true \
  client_flag=false \
  enforce_hostnames=true \
  require_cn=false

echo "   ✅ Role 'edge-tls' created"
echo "      Allowed domains: *.core-platform.local, core-platform.local"
echo "      Max TTL: 24 hours"
echo "      Auto-renewal window: 1 hour before expiry"

# ========================================
# 4. TEST CERTIFICATE ISSUANCE
# ========================================

echo ""
echo "4️⃣  Testing certificate issuance..."

vault write -format=json pki_int/issue/edge-tls \
  common_name="admin.core-platform.local" \
  alt_names="*.core-platform.local,core-platform.local" \
  ttl=24h \
  | tee /vault/data/test-cert.json

# Extract and verify
CERT_PEM=$(jq -r '.data.certificate' /vault/data/test-cert.json)
echo "$CERT_PEM" | openssl x509 -text -noout | grep -A2 "Subject Alternative Name"

echo ""
echo "   ✅ Test certificate issued successfully"
echo ""
echo "🎉 PKI setup complete!"
echo ""
echo "Next steps:"
echo "  1. make vault-pki-issue     # Issue production cert"
echo "  2. Distribute root CA cert to clients (browsers, curl)"
echo "     File: /vault/data/root-ca.crt"
```

#### 1.2 Makefile Targets for PKI

```makefile
# Makefile (add PKI targets)

.PHONY: vault-pki-setup vault-pki-issue vault-pki-status vault-pki-renew

## Vault PKI: Setup root + intermediate CAs
vault-pki-setup:
    @echo "🔐 Setting up Vault PKI (root + intermediate)..."
    docker exec core-vault sh /vault/init/pki-setup.sh
    @echo ""
    @echo "📄 Root CA certificate available at:"
    @echo "   docker exec core-vault cat /vault/data/root-ca.crt"
    @echo ""
    @echo "💡 Import root CA to your system:"
    @echo "   macOS:  sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain root-ca.crt"
    @echo "   Linux:  sudo cp root-ca.crt /usr/local/share/ca-certificates/ && sudo update-ca-certificates"

## Vault PKI: Issue edge certificate for NGINX
vault-pki-issue:
    @echo "📜 Issuing edge certificate for *.core-platform.local..."
    @docker exec core-vault vault write -format=json pki_int/issue/edge-tls \
      common_name="admin.core-platform.local" \
      alt_names="*.core-platform.local,core-platform.local" \
      ttl=24h \
      > /tmp/vault-edge-cert.json
    @echo "   ✅ Certificate issued (TTL: 24h)"
    @echo ""
    @echo "Certificate details:"
    @jq -r '.data.certificate' /tmp/vault-edge-cert.json | openssl x509 -text -noout | grep -A2 "Subject Alternative Name"

## Vault PKI: Check PKI status
vault-pki-status:
    @echo "📊 Vault PKI Status:"
    @echo ""
    @echo "Root CA:"
    @docker exec core-vault vault read -format=json pki_root/cert/ca | \
      jq -r '.data.certificate' | openssl x509 -text -noout | grep -E "Issuer|Subject|Not After"
    @echo ""
    @echo "Intermediate CA:"
    @docker exec core-vault vault read -format=json pki_int/cert/ca | \
      jq -r '.data.certificate' | openssl x509 -text -noout | grep -E "Issuer|Subject|Not After"
    @echo ""
    @echo "Issued certificates:"
    @docker exec core-vault vault list pki_int/certs || echo "   (none)"

## Vault PKI: Manual renewal test
vault-pki-renew:
    @echo "🔄 Testing certificate renewal..."
    @$(MAKE) vault-pki-issue
    @echo "   ✅ Renewal simulated (new cert issued)"
```

### 2. Vault Agent Configuration

**Purpose:** Template Vault-issued certs to NGINX, auto-renew before expiry

#### 2.1 Vault Agent Config

```hcl
# docker/nginx/vault-agent.hcl

pid_file = "/var/run/vault-agent.pid"

# Vault address
vault {
  address = "http://vault:8200"
}

# Auto-auth using AppRole
auto_auth {
  method {
    type = "approle"
    
    config = {
      role_id_file_path   = "/vault/approle/role-id"
      secret_id_file_path = "/vault/approle/secret-id"
      remove_secret_id_file_after_reading = false
    }
  }

  sink {
    type = "file"
    config = {
      path = "/vault/token"
    }
  }
}

# Template: edge.crt (certificate + intermediate CA bundle)
template {
  source      = "/vault/templates/edge.crt.tpl"
  destination = "/etc/nginx/ssl/edge.crt"
  perms       = "0644"
  
  # Execute command after template write
  command     = "nginx -s reload"
  
  # Wait config (batch updates)
  wait {
    min = "5s"
    max = "10s"
  }
}

# Template: edge.key (private key)
template {
  source      = "/vault/templates/edge.key.tpl"
  destination = "/etc/nginx/ssl/edge.key"
  perms       = "0600"
  
  # Execute command after template write
  command     = "nginx -s reload"
  
  wait {
    min = "5s"
    max = "10s"
  }
}

# Template: edge.ca-bundle.crt (full chain for OCSP stapling)
template {
  source      = "/vault/templates/edge.ca-bundle.tpl"
  destination = "/etc/nginx/ssl/edge.ca-bundle.crt"
  perms       = "0644"
}
```

#### 2.2 Vault Agent Templates

**Certificate Template** (`edge.crt.tpl`):

```hcl
{{/* docker/nginx/templates/edge.crt.tpl */}}
{{- /* Fetch certificate from Vault PKI */ -}}
{{- with secret "pki_int/issue/edge-tls" "common_name=admin.core-platform.local" "alt_names=*.core-platform.local,core-platform.local" "ttl=24h" -}}
{{ .Data.certificate }}
{{ .Data.issuing_ca }}
{{- end -}}
```

**Private Key Template** (`edge.key.tpl`):

```hcl
{{/* docker/nginx/templates/edge.key.tpl */}}
{{- with secret "pki_int/issue/edge-tls" "common_name=admin.core-platform.local" "alt_names=*.core-platform.local,core-platform.local" "ttl=24h" -}}
{{ .Data.private_key }}
{{- end -}}
```

**CA Bundle Template** (`edge.ca-bundle.tpl`):

```hcl
{{/* docker/nginx/templates/edge.ca-bundle.tpl */}}
{{- with secret "pki_int/cert/ca" -}}
{{ .Data.certificate }}
{{- end -}}
{{- with secret "pki_root/cert/ca" -}}
{{ .Data.certificate }}
{{- end -}}
```

#### 2.3 AppRole for Vault Agent

```bash
# docker/vault/init/create-nginx-approle.sh

#!/bin/bash
set -e

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
ROOT_TOKEN=$(cat /vault/data/root-token.txt)

vault login "$ROOT_TOKEN" > /dev/null

echo "🔑 Creating AppRole for NGINX Vault Agent..."

# Enable AppRole auth method (if not already)
vault auth enable approle 2>/dev/null || echo "   AppRole already enabled"

# Create policy for NGINX (read PKI certs only)
cat > /tmp/nginx-policy.hcl <<EOF
# Read PKI certificates
path "pki_int/issue/edge-tls" {
  capabilities = ["create", "update"]
}

path "pki_int/cert/ca" {
  capabilities = ["read"]
}

path "pki_root/cert/ca" {
  capabilities = ["read"]
}
EOF

vault policy write nginx-policy /tmp/nginx-policy.hcl

# Create AppRole
vault write auth/approle/role/nginx-role \
  token_policies="nginx-policy" \
  token_ttl=1h \
  token_max_ttl=4h \
  secret_id_ttl=0 \
  secret_id_num_uses=0

# Get Role ID
ROLE_ID=$(vault read -field=role_id auth/approle/role/nginx-role/role-id)

# Generate Secret ID
SECRET_ID=$(vault write -field=secret_id -f auth/approle/role/nginx-role/secret-id)

# Save to files for Vault Agent
echo "$ROLE_ID" > /vault/approle/nginx-role-id
echo "$SECRET_ID" > /vault/approle/nginx-secret-id

echo "   ✅ AppRole created: nginx-role"
echo "      Role ID: $ROLE_ID"
echo "      Secret ID: $SECRET_ID (saved to /vault/approle/)"
echo ""
echo "Copy to NGINX container:"
echo "  docker cp core-vault:/vault/approle/nginx-role-id docker/nginx/vault/role-id"
echo "  docker cp core-vault:/vault/approle/nginx-secret-id docker/nginx/vault/secret-id"
```

### 3. NGINX Integration

[... REST OF S-P1 CONTENT - bude pokračovat ...] ...
