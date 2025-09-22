# 🔐 Keycloak SSL/TLS Configuration Guide

## 📋 Aktuální konfigurace (PEM certifikáty)

Keycloak je nakonfigurován pro použití PEM certifikátů z `./ssl/` adresáře:
- `cert.pem` - SSL certifikát
- `key.pem` - Privátní klíč

## 🔄 Alternativa: PKCS#12 Keystore

### Environment proměnné pro PKCS#12:
```bash
# Místo KC_HTTPS_CERTIFICATE_FILE a KC_HTTPS_CERTIFICATE_KEY_FILE použijte:
- KC_HTTPS_KEY_STORE_FILE=/opt/keycloak/conf/tls/keycloak.p12
- KC_HTTPS_KEY_STORE_PASSWORD=changeit
- KC_HTTPS_KEY_STORE_TYPE=PKCS12
- KC_HTTPS_KEY_STORE_ALIAS=keycloak
```

### Vytvoření PKCS#12 ze stávajících PEM certifikátů:
```bash
# Vytvoření keycloak.p12 z cert.pem a key.pem
openssl pkcs12 -export \
  -in ./ssl/cert.pem \
  -inkey ./ssl/key.pem \
  -out ./ssl/keycloak.p12 \
  -name keycloak \
  -passout pass:changeit
```

## 🧪 Post-deploy testovací příkazy

### 1. Health endpointy (HTTP):
```bash
# Keycloak ready check
curl -s -o /dev/null -w "ready %{http_code}\n" http://localhost:8081/health/ready

# Keycloak live check
curl -s -o /dev/null -w "live %{http_code}\n" http://localhost:8081/health/live
```

### 2. OIDC Discovery (HTTPS se self-signed certifikátem):
```bash
# OIDC discovery endpoint s kontrolou issuer
curl -sk https://core-platform.local:8443/realms/core-platform/.well-known/openid-configuration | jq -r .issuer

# Očekávaná hodnota: https://core-platform.local/realms/core-platform
```

### 3. Kontrola běžící konfigurace:
```bash
# Výpis relevantní Keycloak konfigurace
docker compose exec keycloak bash -lc "/opt/keycloak/bin/kc.sh show-config | egrep -i 'http|https|hostname|certificate|cache|health'"
```

## 🛠️ Troubleshooting

### JGroups cluster loop:
- **Problém**: Opakující se JGroups logy v konzoli
- **Řešení**: Ověřte že `KC_CACHE=local` je aktivní v `show-config` výstupu
- **Kontrola**: `docker compose exec keycloak bash -lc "/opt/keycloak/bin/kc.sh show-config | grep -i cache"`

### "Key material not provided to setup HTTPS":
- **Problém**: Keycloak nemůže najít SSL certifikáty
- **Kontrola mountu**: `docker compose exec keycloak ls -la /opt/keycloak/conf/tls/`
- **Kontrola práv**: Certifikáty by měly mít práva `0644` (čitelné pro všechny)
- **Doporučená oprava práv**:
  ```bash
  chmod 644 ./ssl/cert.pem ./ssl/key.pem
  ```

### Self-signed certifikát varování:
- **Prohlížeč**: Bude zobrazovat bezpečnostní varování - to je normální pro self-signed certifikáty
- **curl**: Používejte parametr `-k` pro ignorování SSL varování
- **Produkce**: Použijte certifikát od důvěryhodné CA (Let's Encrypt, apod.)

### Síťová konektivita:
- **Lokální doména**: Ujistěte se, že `core-platform.local` je v `/etc/hosts`:
  ```bash
  echo "127.0.0.1 core-platform.local" | sudo tee -a /etc/hosts
  ```
- **Porty**: Ověřte že porty 8081 (HTTP) a 8443 (HTTPS) nejsou blokované firewallem

## 📊 Monitorování

### Metriky endpointy:
```bash
# Keycloak metriky (vyžaduje KC_METRICS_ENABLED=true)
curl -s http://localhost:8081/metrics

# Health check s detaily
curl -s http://localhost:8081/health | jq .
```