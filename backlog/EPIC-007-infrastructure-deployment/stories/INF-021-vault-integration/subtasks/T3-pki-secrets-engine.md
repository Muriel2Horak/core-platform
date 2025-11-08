# T3: PKI Secrets Engine for SSL Certificate Management

**Parent Story:** INF-021 Vault Integration  
**Status:** 🔴 TODO  
**Priority:** 🔥 HIGH  
**Effort:** 4 hours  
**Owner:** DevOps

---

## 🎯 Objective

Configure Vault PKI engine as internal CA for auto-renewing SSL certificates.

---

## 📋 Tasks

### 1. Enable PKI Secrets Engine

```bash
# SSH to Vault container
docker exec -it core-vault sh

# Enable PKI at path pki/
vault secrets enable pki
vault secrets tune -max-lease-ttl=87600h pki  # 10 years

# Enable intermediate CA at path pki_int/
vault secrets enable -path=pki_int pki
vault secrets tune -max-lease-ttl=43800h pki_int  # 5 years
```

### 2. Generate Root CA

```bash
# Generate root CA certificate (self-signed)
vault write -field=certificate pki/root/generate/internal \
  common_name="Core Platform Root CA" \
  ttl=87600h > /tmp/ca.crt

# Configure CA and CRL URLs
vault write pki/config/urls \
  issuing_certificates="https://vault.core-platform.local:8200/v1/pki/ca" \
  crl_distribution_points="https://vault.core-platform.local:8200/v1/pki/crl"
```

### 3. Generate Intermediate CA

```bash
# Generate intermediate CA CSR
vault write -format=json pki_int/intermediate/generate/internal \
  common_name="Core Platform Intermediate CA" \
  ttl=43800h | jq -r '.data.csr' > /tmp/pki_int.csr

# Sign intermediate CA with root CA
vault write -format=json pki/root/sign-intermediate \
  csr=@/tmp/pki_int.csr \
  format=pem_bundle \
  ttl=43800h | jq -r '.data.certificate' > /tmp/signed_cert.pem

# Set signed certificate as intermediate CA
vault write pki_int/intermediate/set-signed \
  certificate=@/tmp/signed_cert.pem
```

### 4. Create PKI Role for SSL Certificates

```bash
# Create role for wildcard certificates
vault write pki_int/roles/core-platform \
  allowed_domains="core-platform.local" \
  allow_subdomains=true \
  allow_glob_domains=true \
  allow_wildcard_certificates=true \
  max_ttl="720h" \
  generate_lease=true

# Test certificate generation
vault write pki_int/issue/core-platform \
  common_name="admin.core-platform.local" \
  ttl="720h"
```

### 5. Integrate with Nginx (Auto-Renewal)

**File:** `docker/vault/renew-ssl.sh`

```bash
#!/bin/bash
# Automatic SSL certificate renewal script

set -e

VAULT_ADDR="https://vault.core-platform.local:8200"
VAULT_TOKEN=$(cat /run/secrets/vault-token)
DOMAIN="admin.core-platform.local"
CERT_PATH="/etc/nginx/ssl"

# Request new certificate from Vault
CERT_DATA=$(vault write -format=json pki_int/issue/core-platform \
  common_name="$DOMAIN" \
  alt_names="*.core-platform.local,core-platform.local" \
  ttl="720h")

# Extract certificate and private key
echo "$CERT_DATA" | jq -r '.data.certificate' > "$CERT_PATH/server.crt.pem"
echo "$CERT_DATA" | jq -r '.data.private_key' > "$CERT_PATH/server.key.pem"
echo "$CERT_DATA" | jq -r '.data.issuing_ca' > "$CERT_PATH/ca.crt.pem"

# Reload Nginx
docker exec core-nginx nginx -s reload

echo "✅ SSL certificate renewed successfully"
```

### 6. Add Cron Job for Auto-Renewal

**File:** `docker/vault/crontab`

```cron
# Renew SSL certificate 30 days before expiration
0 2 * * * /opt/vault/scripts/renew-ssl.sh >> /var/log/vault-ssl-renewal.log 2>&1
```

**Add to docker-compose.yml:**

```yaml
services:
  vault:
    volumes:
      - ./docker/vault/renew-ssl.sh:/opt/vault/scripts/renew-ssl.sh:ro
      - ./docker/vault/crontab:/etc/cron.d/vault-ssl:ro
    command: >
      sh -c "
        crond &&
        vault server -config=/vault/config/vault.hcl
      "
```

### 7. Create Backend Integration (Java)

**File:** `backend/src/main/java/cz/muriel/core/vault/VaultPkiService.java`

```java
package cz.muriel.core.vault;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VaultPkiService {

    private final VaultTemplate vaultTemplate;

    public void renewSslCertificate(String commonName, String certPath) {
        log.info("Renewing SSL certificate for {}", commonName);

        VaultResponse response = vaultTemplate.write(
            "pki_int/issue/core-platform",
            Map.of(
                "common_name", commonName,
                "ttl", "720h",
                "alt_names", "*.core-platform.local,core-platform.local"
            )
        );

        if (response == null || response.getData() == null) {
            throw new RuntimeException("Failed to issue certificate");
        }

        String certificate = (String) response.getData().get("certificate");
        String privateKey = (String) response.getData().get("private_key");
        String ca = (String) response.getData().get("issuing_ca");

        try {
            Files.writeString(Path.of(certPath, "server.crt.pem"), certificate);
            Files.writeString(Path.of(certPath, "server.key.pem"), privateKey);
            Files.writeString(Path.of(certPath, "ca.crt.pem"), ca);
            
            log.info("✅ SSL certificate renewed and saved to {}", certPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write certificates", e);
        }
    }
}
```

### 8. Test PKI Integration

```bash
# 1. Verify PKI engine enabled
vault secrets list | grep pki

# 2. Test certificate generation
vault write pki_int/issue/core-platform \
  common_name="test.core-platform.local" \
  ttl="720h"

# Expected output:
# certificate         -----BEGIN CERTIFICATE-----
# issuing_ca          -----BEGIN CERTIFICATE-----
# private_key         -----BEGIN RSA PRIVATE KEY-----
# serial_number       7c:4a:7e:8f:...

# 3. Test auto-renewal script
bash docker/vault/renew-ssl.sh

# 4. Verify new certificate
openssl x509 -in docker/ssl/server.crt.pem -noout -dates
# Expected: notAfter 30 days from now
```

---

## ✅ Acceptance Criteria

- [ ] PKI secrets engine enabled with root + intermediate CA
- [ ] Role `core-platform` created for wildcard certificates
- [ ] SSL certificate generation tested successfully
- [ ] Auto-renewal script created and tested
- [ ] Cron job configured for 30-day renewal
- [ ] Backend VaultPkiService implemented
- [ ] Certificates auto-renew before expiration
- [ ] Nginx reloads without downtime on renewal

---

## 🔗 Dependencies

- **REQUIRES:** T1 (Deploy Vault), T2 (Dynamic DB Credentials)
- Vault must be unsealed and accessible
