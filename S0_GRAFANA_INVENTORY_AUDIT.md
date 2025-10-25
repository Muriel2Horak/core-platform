# S0 - Grafana/Scenes Inventory Audit Report

**Datum:** 25. října 2025  
**EPIC:** Remove Grafana FE Integration → Add Loki as Metamodel DataSource → Build Monitoring UI

---

## 📊 Executive Summary

**Celkem nalezeno:**
- 🟥 **Frontend:** 10 souborů s GrafanaEmbed importy/usage
- 🟥 **Backend:** 1 nový soubor (GrafanaAuthBridgeController) + existující služby
- 🟥 **Nginx:** 2 lokace s auth_request grafana
- 🟥 **E2E:** 2 test soubory
- 🟥 **Docs:** ~5 MD souborů s Grafana references

---

## 🗂️ Detailní Inventory

### Frontend - K ODSTRANĚNÍ

#### 1. **Core Components**
```
✅ SMAZAT: frontend/src/components/GrafanaEmbed.tsx (83 řádků)
✅ SMAZAT: frontend/src/components/Monitoring/GrafanaEmbed.tsx (123 řádků)
✅ SMAZAT: frontend/src/components/Monitoring/index.tsx (export GrafanaEmbed)
```

#### 2. **Pages Using GrafanaEmbed** (10 souborů)
```
⚠️  UPRAVIT: frontend/src/pages/Reports.jsx
   - Odstranit import GrafanaEmbed (řádek 4)
   - Odstranit 3x <GrafanaEmbed /> usage (řádky 39, 42, 45)
   - Nahradit placeholderem nebo smazat sekce

⚠️  UPRAVIT: frontend/src/pages/Admin/AdminSecurityPage.tsx
   - Odstranit import (řádek 4)
   - Odstranit <GrafanaEmbed path="/d/axiom_security..." /> (řádek 23)

⚠️  UPRAVIT: frontend/src/pages/Admin/MonitoringComprehensivePage.tsx
   - Odstranit import (řádek 43)
   - Odstranit 6x <GrafanaEmbed /> (řádky 266, 271, 276, 281, 286, 291)
   - Toto je HLAVNÍ monitoring page - bude nahrazena v S4 novým UI

⚠️  UPRAVIT: frontend/src/pages/Admin/StreamingDashboardPage.tsx
   - Odstranit import (řádek 14)
   - Odstranit <GrafanaEmbed path="/d/streaming-overview..." /> (řádek 130)

⚠️  UPRAVIT: frontend/src/pages/Admin/AxiomMonitoringPage.tsx
   - Odstranit import (řádek 6)
   - Odstranit 10x <GrafanaEmbed /> (řádky 156, 175, 185, 195, 222, 248, 274, 300, 326, 352)

⚠️  UPRAVIT: frontend/src/pages/Admin/AdminAuditPage.tsx
   - Odstranit import (řádek 4)
   - Odstranit <GrafanaEmbed path="/d/axiom_audit..." /> (řádek 23)

⚠️  UPRAVIT: frontend/src/pages/Admin/MonitoringPage.tsx
   - Odstranit import (řádek 6)
   - Odstranit 3x <GrafanaEmbed /> (řádky 75, 79, 83)
```

#### 3. **Hooks**
```
✅ SMAZAT: frontend/src/hooks/useGrafanaOrgId.ts
   - Hook pro získání orgId z API
   - Dependency pro GrafanaEmbed
```

#### 4. **Routes** (TODO: zkontrolovat router config)
```
⚠️  NAJÍT: frontend/src/routes/* nebo App.tsx
   - Vyhledat route "/core-admin/monitoring" nebo "Monitoring" menu
   - Odstranit nebo nahradit placeholderem
```

#### 5. **Grafana Scenes Packages** (TODO: zkontrolovat package.json)
```
⚠️  ZKONTROLOVAT: frontend/package.json
   - Najít dependencies: @grafana/scenes, @grafana/runtime, @grafana/ui
   - NEMAZAT hned - může být potřeba pro jiné komponenty
   - Feature flag first, pak cleanup
```

---

### Backend - K DEPRECACI/SMAZÁNÍ

#### 1. **Nové SSO Bridge (vytvořeno dnes - SMAZAT)**
```
✅ SMAZAT: backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java
   - /internal/auth/grafana endpoint
   - Nikdy nefungoval správně, způsobil restart loop
```

#### 2. **Existující Grafana Services** (TODO: inventarizovat)
```
⚠️  ZKONTROLOVAT: backend/src/main/java/cz/muriel/core/monitoring/
   - GrafanaJwtService.java - KEEP (může být užitečný pro API proxy)
   - GrafanaAdminClient.java - KEEP ZA FEATURE FLAG (admin provisioning)
   - GrafanaProvisioningService.java - KEEP ZA FEATURE FLAG
   - GrafanaTenantRegistry.java - KEEP ZA FEATURE FLAG
   - AuthRequestController.java - SMAZAT (pokud existuje)
```

#### 3. **Properties**
```
⚠️  UPRAVIT: backend/src/main/resources/application.yml nebo application.properties
   - Najít všechny grafana.* properties
   - Schovat za feature flag: monitoring.grafana.enabled=false
```

---

### Nginx - K ODSTRANĚNÍ

#### 1. **Auth Request Locations**
```
⚠️  UPRAVIT: docker/nginx/nginx.conf (řádky 138-166, 378-408)
   
   ODSTRANIT TYTO BLOKY:
   
   # Blok 1 (řádky 138-166)
   location ^~ /core-admin/monitoring/ {
       auth_request /_auth/grafana;
       auth_request_set $grafana_token $upstream_http_grafana_jwt;
       auth_request_set $grafana_org_id $upstream_http_grafana_org_id;
       ...
   }
   
   # Blok 2 (řádky 378-408) - duplicitní
   location ^~ /core-admin/monitoring/ {
       auth_request /_auth/grafana;
       ...
   }
   
   # Redirecty (řádky 107-108, 349-350)
   location = /core-admin/monitoring {
       return 301 /core-admin/monitoring/;
   }
   
   # OLD deprecated location (řádky 411)
   location ~ ^/monitoring/(.*)$ {
       return 301 /core-admin/monitoring/$request_uri;
   }
```

#### 2. **Internal Auth Endpoint**
```
⚠️  NAJÍT A SMAZAT: docker/nginx/nginx.conf
   
   location = /_auth/grafana {
       internal;
       proxy_pass http://backend:8080/internal/auth/grafana;
       ...
   }
```

#### 3. **CSP Headers** (pokud specifické pro Grafana)
```
⚠️  ZKONTROLOVAT: docker/nginx/nginx.conf
   - Hledat "frame-ancestors" nebo "connect-src" s Grafana URL
   - Ponechat obecné CSP, odstranit Grafana-specific
```

---

### E2E Tests - K SMAZÁNÍ

```
✅ SMAZAT: e2e/specs/monitoring/grafana-sso-debug.spec.ts
✅ SMAZAT: e2e/specs/monitoring/grafana-scenes-integration.spec.ts (pokud existuje)
✅ SMAZAT: e2e/debug-grafana-sso.spec.ts
✅ SMAZAT: e2e/test-auth-endpoint.js
```

---

### Documentation - K ARCHIVACI

```
📦 PŘESUNOUT DO ARCHIVE:
   - GRAFANA_SSO_COMPLETE_ANALYSIS.md → archive/
   - MONITORING_IMPLEMENTATION_COMPLETE.md → archive/
   - S9_COMPLETE.md → archive/ (nebo jen upravit sekce)
   - GRAFANA_PROVISIONING_*.md → archive/
   - docs/GRAFANA_INTEGRATION.md → archive/
```

---

## 🎯 Akce K Provedení (S0 + S1)

### Immediate (S0 - Preflight)
```bash
# 1. Feature flags
# Frontend: .env nebo vite.config
VITE_MONITORING_GRAFANA_ENABLED=false
VITE_MONITORING_LOKI_ENABLED=true

# Backend: application.yml
monitoring:
  grafana:
    enabled: false  # Vypne všechny Grafana services
  loki:
    enabled: true   # Připraví na S2

# 2. Git tag (rollback point)
git tag -a "pre-degrafana-v1.0.0" -m "Rollback point before Grafana removal"
git push origin pre-degrafana-v1.0.0

# 3. Migration doc
touch MIGRATION_DEGRAFANA.md
```

### Cleanup (S1 - De-Grafana)
```bash
# Frontend
rm frontend/src/components/GrafanaEmbed.tsx
rm frontend/src/components/Monitoring/GrafanaEmbed.tsx  
rm frontend/src/hooks/useGrafanaOrgId.ts
rm backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java

# E2E
rm e2e/specs/monitoring/grafana-sso-debug.spec.ts
rm e2e/debug-grafana-sso.spec.ts
rm e2e/test-auth-endpoint.js

# Archive docs
mkdir -p archive/grafana-experiment
mv GRAFANA_*.md archive/grafana-experiment/
mv MONITORING_IMPLEMENTATION_COMPLETE.md archive/grafana-experiment/

# Nginx - MANUAL EDIT (komplexní změny)
# Smazat bloky viz výše
```

---

## ⚠️ Risk Assessment

### High Risk
1. **MonitoringComprehensivePage.tsx** - hlavní monitoring UI, bude prázdná po odstranění
   - **Mitigace:** Připravit placeholder v S1, nahradit v S4
   
2. **Nginx config** - 2 duplikované bloky, možná syntax chyba
   - **Mitigace:** Validate nginx -t před restartem

3. **Feature flags** - pokud nejsou správně implementovány, může dojít k build fails
   - **Mitigace:** Condition all Grafana imports v S0

### Medium Risk
1. **Backend services** - GrafanaJwtService může mít dependencies
   - **Mitigace:** Schovat za @ConditionalOnProperty místo mazání

2. **Routes** - odstranění route může rozbít navigaci
   - **Mitigace:** Redirect na placeholder page

---

## ✅ Definition of Done (S0)

- [x] Tento audit report vytvořen
- [ ] Feature flags přidány do FE/.env
- [ ] Feature flags přidány do BE/application.yml
- [ ] Git tag "pre-degrafana-v1.0.0" vytvořen
- [ ] MIGRATION_DEGRAFANA.md vytvořen s plánem

---

## 📝 Notes

- **FilterParser fix:** KEEP - již commitnutý, opravuje testy
- **Copilot instructions:** KEEP - golden rules jsou užitečné
- **Grafana stack:** NEMAZAT z docker-compose - může zůstat jako standalone admin nástroj
- **JWKS endpoint:** KEEP - může být užitečný pro jiné účely

---

**Next Steps:** Po schválení tohoto auditu → S0 Feature Flags → S1 Cleanup → S2 Metamodel Extension
