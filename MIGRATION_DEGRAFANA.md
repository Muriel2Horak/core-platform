# Migration: Grafana FE → Native Loki Monitoring UI

**Datum Start:** 25. října 2025  
**Status:** 🟡 IN PROGRESS - S0 Complete, S1 Starting  
**Rollback Tag:** `pre-degrafana-v1.0.0`

---

## 🎯 Migration Goal

**FROM:** Grafana iframe embeds with SSO bridge (nefunkční, 7 dní debugging)  
**TO:** Native React monitoring UI nad Loki přes BFF API

---

## 📋 Migration Phases

### ✅ PHASE S0 - Preflight & Feature Flags (COMPLETE)
**Status:** ✅ Done  
**Commit:** `2b30a08`

**Provedeno:**
- [x] Inventory audit - nalezeno 10 FE souborů, 2 Nginx bloky, 1 BE controller
- [x] Feature flags:
  ```properties
  # Backend
  monitoring.grafana.enabled=false
  monitoring.loki.enabled=true
  
  # Frontend
  VITE_MONITORING_GRAFANA_ENABLED=false
  VITE_MONITORING_LOKI_ENABLED=true
  ```
- [x] Git tag: `pre-degrafana-v1.0.0`
- [x] Dokumenty: `S0_GRAFANA_INVENTORY_AUDIT.md`, `GRAFANA_SSO_COMPLETE_ANALYSIS.md`

---

### 🔄 PHASE S1 - De-Grafana FE Cleanup (IN PROGRESS)
**Status:** 🟡 Starting  
**ETA:** 30 minut

**Akce:**
1. [ ] Smazat FE komponenty:
   ```bash
   rm frontend/src/components/GrafanaEmbed.tsx
   rm frontend/src/components/Monitoring/GrafanaEmbed.tsx
   rm frontend/src/hooks/useGrafanaOrgId.ts
   ```

2. [ ] Upravit Pages (10 souborů):
   - Reports.jsx - odstranit 3x GrafanaEmbed
   - AdminSecurityPage.tsx - odstranit 1x
   - MonitoringComprehensivePage.tsx - odstranit 6x + přidat placeholder
   - StreamingDashboardPage.tsx - odstranit 1x
   - AxiomMonitoringPage.tsx - odstranit 10x
   - AdminAuditPage.tsx - odstranit 1x
   - MonitoringPage.tsx - odstranit 3x

3. [ ] Smazat Backend SSO:
   ```bash
   rm backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java
   ```

4. [ ] Upravit Nginx:
   - Odstranit `location ^~ /core-admin/monitoring/` bloky (2x)
   - Odstranit `auth_request /_auth/grafana`
   - Odstranit `location = /_auth/grafana` internal endpoint

5. [ ] Smazat E2E testy:
   ```bash
   rm e2e/specs/monitoring/grafana-sso-debug.spec.ts
   rm e2e/debug-grafana-sso.spec.ts
   rm e2e/test-auth-endpoint.js
   ```

6. [ ] Build & verify:
   ```bash
   make clean-fast
   # Ověřit že build prochází
   ```

**DoD S1:**
- [ ] CI zelené (build bez chyb)
- [ ] Žádné Grafana importy v FE
- [ ] Nginx bez auth_request grafana
- [ ] Dokument `DE_GRAFANA_FE_CLEANUP.md`

---

### ⏳ PHASE S2 - Metamodel: Loki DataSource (PENDING)
**Status:** ⏸️ Čeká na S1  
**ETA:** 2-3 dny

**Plán:**
- Rozšířit metamodel o `DataSource` entity (type: LOKI)
- Dataset kind: LOGS s capabilities (readOnly, timeRequired)
- Query DSL extension (timeRange, filter, aggregates)
- Security: tenant injection, label whitelist

---

### ⏳ PHASE S3 - BFF Loki Adapter + API (PENDING)
**Status:** ⏸️ Čeká na S2  
**ETA:** 2-3 dny

**Plán:**
- LokiQueryCompiler (DSL → LogQL)
- LokiClient (HTTP, retry, cache)
- REST endpoints: /query, /stats, /labels, /tail
- RBAC + rate-limit

---

### ⏳ PHASE S4 - FE Monitoring Framework (PENDING)
**Status:** ⏸️ Čeká na S3  
**ETA:** 3-4 dny

**Plán:**
- LogsPanel (virtuální tabulka, live tail)
- TimeSeriesPanel (echarts, zoom/pan)
- Dashboard layout (react-grid-layout)
- Saved Views

---

### ⏳ PHASE S5-S7 - Security, Tests, Docs (PENDING)
**Status:** ⏸️ Čeká na S4  
**ETA:** 2 dny

---

## 🔙 Rollback Procedure

```bash
# Emergency rollback
git reset --hard pre-degrafana-v1.0.0
git push --force  # POZOR: pouze pokud nikdo nepullnul změny

# Nebo soft rollback (zachová commity)
git revert HEAD~5..HEAD
```

---

## ⚠️ Known Issues & Risks

### High Risk
1. **MonitoringComprehensivePage bude prázdná po S1**
   - Mitigace: Přidat placeholder "Coming Soon - Loki UI"
   
2. **Backend může failovat když chybí GrafanaAuthBridgeController**
   - Mitigace: Už smazán, nikdy nebyl v produkci

3. **Nginx syntax error**
   - Mitigace: `nginx -t` před restartem

### Medium Risk
1. **Uživatelé si stěžují že zmizelo monitorování**
   - Mitigace: Komunikace + rychlá implementace S4

---

## 📊 Progress Tracker

```
S0 Preflight       ████████████████████ 100% ✅
S1 De-Grafana FE   ░░░░░░░░░░░░░░░░░░░░   0% 🟡
S2 Metamodel       ░░░░░░░░░░░░░░░░░░░░   0% ⏸️
S3 BFF API         ░░░░░░░░░░░░░░░░░░░░   0% ⏸️
S4 FE Framework    ░░░░░░░░░░░░░░░░░░░░   0% ⏸️
S5-S7 Final        ░░░░░░░░░░░░░░░░░░░░   0% ⏸️
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Overall            ████░░░░░░░░░░░░░░░░  16% 🟡
```

---

## 🎓 Lessons Learned (Průběžně)

### Co Nefungovalo
- ❌ Grafana SSO přes Nginx auth_request - 7 dní debugging, restart loop
- ❌ Iframe embedding - security headaches, CORS issues
- ❌ JWT header forwarding - křehký, latency

### Co Děláme Jinak
- ✅ API proxy místo iframe
- ✅ Backend-side Loki queries (tenant enforcement)
- ✅ Native React UI (úplná kontrola)
- ✅ Feature flags (safe rollback)

---

**Last Update:** 25. října 2025 20:00  
**Next Milestone:** S1 Complete (ETA: dnes večer)
