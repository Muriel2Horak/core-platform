# Migration: Grafana FE → Native Loki Monitoring UI

**Datum Start:** 25. října 2025  
**Status:** ✅ COMPLETE - All phases S0-S7 finished  
**Rollback Tag:** `pre-degrafana-v1.0.0`

---

## 🎯 Migration Goal

**FROM:** Grafana iframe embeds with SSO bridge (nefunkční, 7 dní debugging)  
**TO:** Native React monitoring UI nad Loki přes BFF API

**✅ ACHIEVED:** Native Loki UI s tenant isolation, LogViewer + MetricCard components, 7 pages migrated.

---

## 📋 Migration Phases - FINAL STATUS

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

### ✅ PHASE S1 - De-Grafana FE Cleanup (COMPLETE)
**Status:** ✅ Done  
**Commit:** `1541884`  
**Čas:** 45 minut

**Provedeno:**
- [x] Smazány FE komponenty: GrafanaEmbed.tsx (2x), useGrafanaOrgId.ts
- [x] Upraveno 9 Pages (Reports, Admin*, Monitoring*, Axiom*)
- [x] Backend: GrafanaAuthBridgeController, GrafanaProvisioningService, GrafanaAdminClient deprecated
- [x] Nginx: Odstráněny auth_request bloky (2x)
- [x] Build: ✅ SUCCESS
- [x] Změny: 20 files, -678 LOC, +342 LOC (placeholders)

---

### ✅ PHASE S2 - Loki HTTP API Integration (COMPLETE)
**Status:** ✅ Done  
**Commit:** `9715b41`  
**Čas:** 1 hodina

**Provedeno:**
- [x] LokiClient s Resilience4j Circuit Breaker
- [x] DTOs: LokiQueryRequest, LokiQueryResponse, LokiLabelsResponse, LokiLabelValuesResponse
- [x] Config: loki.url=http://loki:3100, timeout=30s, max-entries=5000
- [x] Backend compile: ✅ BUILD SUCCESS

---

### ✅ PHASE S3 - BFF Monitoring Endpoints (COMPLETE)
**Status:** ✅ Done  
**Commit:** `9715b41`  
**Čas:** 1 hodina

**Provedeno:**
- [x] MonitoringBffController s 4 REST endpoints
- [x] GET /api/monitoring/logs (automatic tenant isolation)
- [x] GET /api/monitoring/labels
- [x] GET /api/monitoring/labels/{label}/values
- [x] GET /api/monitoring/metrics-summary
- [x] Tenant filter: `{service="backend"}` → `{tenant="admin",service="backend"}`
- [x] Backend compile: ✅ BUILD SUCCESS

---

### ✅ PHASE S4 - Frontend Monitoring Components (COMPLETE)
**Status:** ✅ Done  
**Commit:** `9715b41`  
**Čas:** 2 hodiny

**Provedeno:**
- [x] LogViewer.tsx (266 lines): Table, LogQL query, time range, auto-refresh, CSV export
- [x] MetricCard.tsx (171 lines): totalLogs, errorLogs, errorRate, health indicators
- [x] index.ts: Barrel export

---

### ✅ PHASE S5 - Replace All Placeholders (COMPLETE)
**Status:** ✅ Done  
**Commit:** `9715b41`  
**Čas:** 2 hodiny

**Provedeno:**
- [x] Reports.jsx - 3 tabs with LogViewer
- [x] AdminSecurityPage.tsx - Security events (24h)
- [x] AdminAuditPage.tsx - Audit logs (12h)
- [x] MonitoringPage.tsx - 3 tabs (System/Security/Audit)
- [x] StreamingDashboardPage.tsx - Streaming events + metrics
- [x] MonitoringComprehensivePage.tsx - Redirect message
- [x] AxiomMonitoringPage.tsx - Redirect message
- [x] Frontend build: ✅ SUCCESS (dist/bundle.js 5.1mb)

---

### ✅ PHASE S6 - E2E Tests (COMPLETE)
**Status:** ✅ Done  
**Commit:** (current)  
**Čas:** 1 hodina

**Provedeno:**
- [x] e2e/specs/monitoring/loki-log-viewer.spec.ts
- [x] Test coverage:
  - LogViewer renders
  - Time range selector works
  - Query input filters logs
  - Auto-refresh toggle
  - CSV export downloads
  - Tenant isolation verified
  - Error handling
  - Admin pages integration (Security, Audit, Streaming)
  - Performance (load <5s, handles 1000+ logs)

---

### ✅ PHASE S7 - Documentation & Cleanup (COMPLETE)
**Status:** ✅ Done  
**Commit:** (current)  
**Čas:** 1 hodina

**Provedeno:**
- [x] docs/LOKI_MONITORING_UI.md - Complete user guide
- [x] README.md - Updated monitoring section
- [x] MIGRATION_DEGRAFANA.md - Final status update (this file)

---

## 📊 Migration Summary

**Total Time:** 8-9 hodin  
**Commits:** 2 (1541884, 9715b41)  
**Files Changed:** 22 (9715b41) + 20 (1541884) = 42 files  
**LOC Delta:**
- S1: -678 LOC (removal) + 342 LOC (placeholders) = -336 net
- S2-S5: +1248 LOC (new features) - 573 LOC (deprecated) = +675 net
- **Total:** +339 LOC (net gain from new native UI)

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
