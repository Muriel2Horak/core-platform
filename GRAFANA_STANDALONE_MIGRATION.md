# 🎯 Grafana Standalone Migration Plan

**Datum:** 26. října 2025  
**Cíl:** Vypreparovat Grafanu z FE/BFF integrace a ponechat ji jako standalone ops tool s OIDC SSO

---

## 📋 Executive Summary

### Současný stav
- ❌ Grafana embedovaná přes iframe ve FE (Scenes)
- ❌ BFF minting Grafana JWT přes `/internal/auth/grafana`
- ❌ Nginx `auth_request` orchestrace + header rewrites
- ❌ Multi-org juggling (tenant → Grafana org mapping)
- ❌ Složitý provisioning (org + service accounts per tenant)

### Cílový stav
- ✅ Grafana standalone na `https://ops.core-platform.local/grafana`
- ✅ OIDC SSO přes Keycloak (jediná autentizace)
- ✅ Žádná integrace s FE/BFF
- ✅ Datasources: Loki + Prometheus (pro ops/SRE)
- ✅ CSP `frame-ancestors 'none'` (zakázat embed)
- ✅ Dashboards as code (Git provisioning)

---

## 🗑️ Co odstranit

### Backend (Java)

#### Controllers & Services (k odstranění)
```
backend/src/main/java/cz/muriel/core/monitoring/
├── AuthRequestController.java                 ❌ SMAZAT (nginx auth_request endpoint)
├── GrafanaJwtService.java                     ❌ SMAZAT (JWT minting)
├── GrafanaTenantRegistry.java                 ❌ SMAZAT (tenant → org mapping)
└── grafana/
    ├── GrafanaProvisioningService.java        ❌ SMAZAT (org+SA provisioning)
    ├── GrafanaAdminClient.java                ❌ SMAZAT (REST client)
    ├── entity/GrafanaTenantBinding.java       ❌ SMAZAT (DB entity)
    ├── repository/GrafanaTenantBindingRepository.java ❌ SMAZAT
    ├── dto/Create*.java, *Response.java       ❌ SMAZAT (všechny DTOs)
    └── GrafanaApiException.java               ❌ SMAZAT
```

#### Konfigurace properties
```yaml
# application.yml - SMAZAT:
monitoring:
  grafana:
    enabled: false  # celá sekce ven
grafana:
  jwt:              # celá sekce ven
  provisioning:     # celá sekce ven
```

#### Database migrace
```sql
-- Liquibase changelog - přidat:
DROP TABLE grafana_tenant_bindings;
```

#### DTO cleanup
```java
backend/src/main/java/cz/muriel/core/dto/UserDto.java
- private Long grafanaOrgId;  ❌ SMAZAT field
```

#### Test cleanup
```
backend/src/test/java/cz/muriel/core/monitoring/grafana/
├── GrafanaProvisioningServiceIT.java          ❌ SMAZAT
└── všechny testy mock config s grafanaOrgId   ❌ VYČISTIT
```

### Frontend (TypeScript/React)

#### Komponenty (už vyčištěno ✅)
- Žádné Grafana komponenty nenalezeny
- Jen zmínky v komentářích (MonitoringComprehensivePage, AxiomMonitoringPage)

#### Routing cleanup
```typescript
// frontend/src/shared/ui/SidebarNav.tsx
- description: 'Grafana dashboards s business metrikami'  ❌ UPRAVIT text
```

### Nginx konfigurace

#### nginx-ssl.conf.template
```nginx
# ODSTRANIT celý blok (řádky ~110-142):
location ^~ /core-admin/monitoring/ {
    auth_request /_auth/grafana;           ❌ SMAZAT
    auth_request_set $grafana_token ...    ❌ SMAZAT
    # ... celý proxy block
}

# ODSTRANIT auth endpoint:
location /_auth/grafana {                  ❌ SMAZAT
    internal;
    proxy_pass http://backend/internal/auth/grafana;
}
```

### E2E testy

```
e2e/specs/monitoring/
├── grafana-scenes-integration.spec.ts         ❌ SMAZAT
└── grafana-sso-*.spec.ts                      ❌ SMAZAT (vše s "grafana" v názvu)

frontend/tests/e2e/monitoring/
└── grafana-embed.spec.ts                      ❌ SMAZAT
```

---

## ✅ Co přidat/upravit

### Grafana konfigurace (docker-compose)

#### Nová environment variables
```yaml
# docker/docker-compose.yml - grafana service:
environment:
  # ========== OIDC SSO ==========
  GF_AUTH_GENERIC_OAUTH_ENABLED: "true"
  GF_AUTH_GENERIC_OAUTH_NAME: "Keycloak"
  GF_AUTH_GENERIC_OAUTH_CLIENT_ID: "grafana-ops"
  GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET: "${GRAFANA_OIDC_SECRET}"
  GF_AUTH_GENERIC_OAUTH_SCOPES: "openid profile email"
  GF_AUTH_GENERIC_OAUTH_AUTH_URL: "https://${DOMAIN}/realms/admin/protocol/openid-connect/auth"
  GF_AUTH_GENERIC_OAUTH_TOKEN_URL: "https://keycloak:8443/realms/admin/protocol/openid-connect/token"
  GF_AUTH_GENERIC_OAUTH_API_URL: "https://keycloak:8443/realms/admin/protocol/openid-connect/userinfo"
  GF_AUTH_GENERIC_OAUTH_ALLOW_SIGN_UP: "true"
  GF_AUTH_GENERIC_OAUTH_AUTO_LOGIN: "false"
  GF_AUTH_GENERIC_OAUTH_ROLE_ATTRIBUTE_PATH: "contains(groups[*], 'grafana-admin') && 'Admin' || contains(groups[*], 'grafana-editor') && 'Editor' || 'Viewer'"
  
  # Disable forms & signup
  GF_AUTH_DISABLE_LOGIN_FORM: "true"
  GF_AUTH_DISABLE_SIGNOUT_MENU: "false"
  GF_USERS_ALLOW_SIGN_UP: "false"
  
  # Security
  GF_SECURITY_DISABLE_INITIAL_ADMIN_CREATION: "true"
  GF_SECURITY_COOKIE_SECURE: "true"
  GF_SECURITY_COOKIE_SAMESITE: "lax"
  
  # NO JWT auth
  GF_AUTH_JWT_ENABLED: "false"  # ❌ vypnout
  
  # CSP - zakázat embed
  GF_SECURITY_CONTENT_SECURITY_POLICY: "true"
  GF_SECURITY_CONTENT_SECURITY_POLICY_TEMPLATE: "frame-ancestors 'none'"
  
  # Subpath
  GF_SERVER_ROOT_URL: "https://ops.${DOMAIN}/grafana"
  GF_SERVER_SERVE_FROM_SUB_PATH: "true"
```

#### Keycloak client setup
```bash
# Vytvořit nový OIDC client v admin realm:
Client ID: grafana-ops
Protocol: openid-connect
Access Type: confidential
Valid Redirect URIs: https://ops.core-platform.local/grafana/login/generic_oauth
Base URL: https://ops.core-platform.local/grafana
Web Origins: https://ops.core-platform.local

# Mappers:
- groups (group membership → "groups" claim)
- email (built-in)
- profile (built-in)

# Roles/Groups:
grafana-admin   → Grafana Admin role
grafana-editor  → Grafana Editor role
(default)       → Grafana Viewer role
```

### Nginx proxy (standalone)

```nginx
# docker/nginx/nginx-ssl.conf.template
# Ops subdoména - nová virtual host sekce:

server {
    listen 443 ssl http2;
    server_name ops.${DOMAIN};
    
    ssl_certificate /etc/nginx/certs/${DOMAIN}.crt;
    ssl_certificate_key /etc/nginx/certs/${DOMAIN}.key;
    
    # Grafana standalone (bez auth_request)
    location /grafana/ {
        proxy_pass http://grafana;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_http_version 1.1;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Keycloak redirect pro SSO
    location /realms/ {
        proxy_pass https://keycloak;
        proxy_set_header Host $host;
        proxy_ssl_verify off;
    }
}
```

### Dashboards provisioning (Git)

```yaml
# docker/grafana/provisioning/dashboards/default.yml
apiVersion: 1

providers:
  - name: 'Default'
    orgId: 1
    folder: 'Ops'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 30
    allowUiUpdates: true
    options:
      path: /etc/grafana/dashboards
```

```yaml
# docker/grafana/provisioning/datasources/loki.yml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: true
    jsonData:
      maxLines: 1000
      timeout: 60
    version: 1
    editable: false
    
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: false
    jsonData:
      timeInterval: "30s"
    version: 1
    editable: false
```

### Dokumentace (ops runbook)

```markdown
# docs/ops/GRAFANA_ACCESS.md

## Grafana Ops Access

**URL:** https://ops.core-platform.local/grafana

### Přihlášení
1. Klikni na "Sign in with Keycloak"
2. Přihlaš se přes admin realm (ops účet)
3. Role mapování:
   - `grafana-admin` → Admin (full access)
   - `grafana-editor` → Editor (tvorba dashboardů)
   - ostatní → Viewer (read-only)

### Datasources
- **Loki:** logs (všechny services)
- **Prometheus:** metriky (CPU, paměť, HTTP)

### Dashboards
- **Ops/** - System Health, Service Metrics
- Dashboards as code: `docker/grafana/dashboards/`
- CI validace: `make validate-dashboards`

### Alerty
- Alertmanager (preferováno) nebo Grafana Alerting
- Alert rules: `docker/grafana/provisioning/alerting/`

### Provisioning workflow
```bash
# 1. Vytvoř/uprav dashboard v UI
# 2. Export JSON (Share → Export → Save to file)
# 3. Commit do Git: docker/grafana/dashboards/ops-health.json
# 4. CI lint + import test
# 5. Deploy: make up
```

### Troubleshooting
- Grafana nenaběhne: `make logs | grep grafana`
- SSO problém: zkontroluj Keycloak admin realm
- Dashboard nejde uložit: zkontroluj provisioning permissions
```

---

## 🔄 Migration Steps (Zero-Downtime)

### Fáze 1: Příprava (1 den)
- [ ] Vytvořit Keycloak client `grafana-ops` v admin realm
- [ ] Vygenerovat OIDC secret → `.env.template`
- [ ] Připravit Grafana OIDC config (docker-compose)
- [ ] Export existujících dashboardů do Git (`docker/grafana/dashboards/`)

### Fáze 2: Backend Cleanup (2 dny)
- [ ] Označit `@Deprecated` všechny Grafana integration třídy
- [ ] Nastavit `monitoring.grafana.enabled=false` v properties
- [ ] Vytvořit Liquibase migration pro `DROP TABLE grafana_tenant_bindings`
- [ ] Smazat `grafanaOrgId` z `UserDto`
- [ ] Unit testy: vyčistit mock Grafana data
- [ ] Rebuild: `make clean-fast`

### Fáze 3: Nginx Cleanup (1 den)
- [ ] Odstranit `auth_request` bloky z `nginx-ssl.conf.template`
- [ ] Přidat nový `server` block pro `ops.${DOMAIN}`
- [ ] Regenerovat config: `make compose-generate`
- [ ] Test: `docker exec core-nginx nginx -t`

### Fáze 4: Grafana Standalone Setup (1 den)
- [ ] Aktualizovat Grafana environment (OIDC, CSP, disable JWT)
- [ ] Provisioning: datasources (Loki, Prometheus)
- [ ] Provisioning: dashboards z Git
- [ ] Test SSO: přihlásit se přes Keycloak
- [ ] Ověřit role mapping (admin/editor/viewer)

### Fáze 5: E2E Cleanup (0.5 dne)
- [ ] Smazat `e2e/specs/monitoring/grafana-*.spec.ts`
- [ ] Smazat `frontend/tests/e2e/monitoring/grafana-*.spec.ts`
- [ ] Ponechat jen native monitoring testy (Loki UI)
- [ ] Run: `make test-e2e`

### Fáze 6: Dokumentace (0.5 dne)
- [ ] Napsat ops runbook (`docs/ops/GRAFANA_ACCESS.md`)
- [ ] Aktualizovat architecture docs (odstranit Grafana integration diagrams)
- [ ] Update README: odkaz na ops Grafana

### Fáze 7: Final Cleanup & Deploy (1 den)
- [ ] Smazat deprecated třídy (AuthRequestController, GrafanaJwtService, atd.)
- [ ] Smazat unit testy Grafana integration
- [ ] Full rebuild: `make clean`
- [ ] E2E full pass: `make test-e2e`
- [ ] Deploy do staging → smoke test → production

---

## 📊 Impact Analysis

### Co zůstává funkční
- ✅ FE native monitoring (Loki UI přes BFF)
- ✅ Loki/Prometheus datasources (nezměněno)
- ✅ BFF tenant izolace (žádná změna v MonitoringProxyController)
- ✅ Audit logy (CoreAuditLogger)
- ✅ Všechny uživatelské featury

### Co přestane fungovat
- ❌ Grafana embed ve FE (iframe) - ZÁMĚR
- ❌ Automatic Grafana org provisioning per tenant - ZÁMĚR
- ❌ SSO bridge přes BFF JWT - ZÁMĚR
- ❌ Multi-org switching - nahrazeno folder-per-tenant (budoucnost)

### Bezpečnostní zlepšení
- ✅ Žádné JWT minting (menší attack surface)
- ✅ Žádný nginx auth_request orchestration (jednodušší security audit)
- ✅ CSP `frame-ancestors 'none'` (clickjacking protection)
- ✅ Centralizovaný OIDC (single source of truth)

---

## 🎯 Success Criteria

1. **Grafana přístupná na** `https://ops.core-platform.local/grafana`
2. **SSO funguje** přes Keycloak admin realm
3. **Role mapping** správně aplikován (admin/editor/viewer)
4. **Dashboards** viditelné z Git provisioning
5. **Datasources** (Loki + Prometheus) funkční
6. **FE monitoring UI** nezměněn a funkční (přes BFF)
7. **Zero regression** v E2E testech (mimo Grafana embed)
8. **Backend build** zelený (žádné Grafana dependencies)
9. **Dokumentace** kompletní (ops runbook)

---

## 📝 Rollback Plan

Pokud cosi selže:

1. **Revert Git** (všechny změny v jednom MR/PR)
2. **Restore properties:** `monitoring.grafana.enabled=true`
3. **Rebuild:** `make clean-fast`
4. **Rollback DB:** `liquibase rollbackCount 1` (pokud table drop)

---

## 🚀 Poznámky

### Proč folder-per-tenant místo multi-org?
- **Simple:** 1 org (Default), folders = tenant namespaces
- **Keycloak groups:** `tenant:acme` → viewer access k `Acme/` folderu
- **Teams:** Grafana teams mapované na Keycloak groups
- **Když chci hard-izolaci:** později Grafana-per-tenant (heavy, ale možné)

### Co s alertami?
- **Preferuji:** Alertmanager (mimo Grafana)
- **Alternative:** Grafana Alerting s provisioning (`alerting/*.yml`)
- **Ne:** ad-hoc alerty v UI (nejsou verzované)

### Kdy Grafanu úplně zahodit?
- Když FE pokryje 100% use cases (drill-downs, annotations, multi-axis)
- Realisticky: **nech ji jako ad-hoc SRE tool**, nestojí to nic

---

**Status:** 🟢 READY FOR IMPLEMENTATION  
**Estimated effort:** 6-7 dní (full-time)  
**Risk:** 🟢 LOW (většina již deprecated, žádná user-facing feature)
