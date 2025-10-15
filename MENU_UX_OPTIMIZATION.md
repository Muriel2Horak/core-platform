# Menu UX Optimization & Grafana Scenes Migration

**Datum:** 2025-10-15  
**Cíl:** Optimalizovat strukturu menu, přidat chybějící screeny, migrovat Grafana IFRAME na Scenes

---

## 📋 Audit Současného Stavu

### Stránky v Projektu (Admin)
✅ **Hotové:**
1. `AdminUsersPage.tsx` - Správa uživatelů
2. `AdminRolesPage.tsx` - Správa rolí  
3. `AdminTenantsPage.tsx` - Správa tenantů
4. `AdminSecurityPage.tsx` - Bezpečnost (s Grafana IFRAME)
5. `AdminAuditPage.tsx` - Audit (s Grafana IFRAME)
6. `MonitoringPage.tsx` - Monitoring (s Grafana IFRAME - 7 panelů)
7. `StreamingDashboardPage.tsx` - Streaming dashboard (s Grafana IFRAME)
8. `KeycloakSyncPage.jsx` - Keycloak synchronizace
9. `SyncHistoryPage.jsx` - Historie synchronizací
10. `WorkflowDesignerPage.tsx` - Workflow designer
11. `MetamodelStudioPage.tsx` - Metamodel Studio (Entity Editor + AI Config)
12. `UserEditPage.tsx` - Editace uživatele
13. `TenantEditPage.tsx` - Editace tenantu
14. `RoleEditPage.tsx` - Editace role

### Současná Struktura Menu (SidebarNav.tsx)

```
📊 Dashboard
👤 Adresář
📈 Reporting

⚙️ Administrace
  └─ 🔐 Správa Keycloak
     ├─ 👥 Uživatelé
     ├─ 🔒 Role
     ├─ 👥 Skupiny
     ├─ 🏢 Tenanti
     ├─ 🔄 Synchronizace
     └─ 📋 Historie Sync
  ├─ 📊 Monitoring
  └─ 🛡️ Bezpečnost
      └─ 🐛 Audit

🏢 Tenant Administrace
  ├─ 📊 Dashboard
  ├─ 👥 Uživatelé
  ├─ 🔒 Role
  ├─ 👥 Skupiny
  └─ 🔄 Synchronizace

📊 DataTable (DEMO)
📋 Kanban (DEMO)
```

---

## ❌ Problémy Současného Menu

### 1. **Chybějící Stránky v Menu**
- ❌ **Workflow Designer** (`/core-admin/workflows`) - CHYBÍ V MENU
- ❌ **Metamodel Studio** (`/core-admin/studio`) - CHYBÍ V MENU
- ❌ **Streaming Dashboard** (`/core-admin/streaming`) - CHYBÍ V MENU
- ❌ **Reporting Explorer** (`/reporting`) - Existuje route, ale není v menu

### 2. **Grafana IFRAME místo Scenes**
- ❌ **MonitoringPage** - 7 iframe panelů
- ❌ **AdminSecurityPage** - 1 iframe panel
- ❌ **AdminAuditPage** - 1 iframe panel
- ❌ **StreamingDashboardPage** - 3 iframe panely

### 3. **Nelogická Hierarchie**
- "Monitoring" je na stejné úrovni jako "Správa Keycloak" (měl by být samostatný)
- "Bezpečnost" má jen 1 child (Audit) - mohlo by být ploché
- DEMO položky na root úrovni (měly by být pod "Examples")

### 4. **Duplicitní Reporting**
- `/reports` (Grafana Scenes s BFF)
- `/reporting` (Reporting Explorer - Advanced data grid)
- Uživatel neví, co je co

---

## ✅ Navrhovaná Nová Struktura Menu

### Optimalizované Hierarchie

```
🏠 Dashboard
📁 Adresář

📊 Analytics & Monitoring
  ├─ 📊 Reports (Grafana Scenes)
  ├─ 📈 Reporting Explorer (Advanced Grid)
  ├─ 📉 System Monitoring (Grafana Scenes - místo IFRAME)
  ├─ 📡 Streaming Dashboard (Grafana Scenes)
  └─ 🔍 Audit Log (Grafana Scenes)

⚙️ Správa Systému (CORE_ROLE_ADMIN)
  ├─ 👥 Uživatelé
  ├─ 🔒 Role
  ├─ 👥 Skupiny
  ├─ 🏢 Tenanti
  ├─ 🔄 Keycloak Sync
  ├─ 📋 Historie Sync
  └─ 🛡️ Bezpečnost (Grafana Scenes)

🎨 Studio & Design
  ├─ 🧬 Metamodel Studio (Entity + AI Config)
  ├─ 🔀 Workflow Designer
  └─ 📐 Form Builder (budoucí)

🏢 Tenant Administrace (CORE_ROLE_TENANT_ADMIN)
  ├─ 📊 Dashboard
  ├─ 👥 Uživatelé
  ├─ 🔒 Role
  ├─ 👥 Skupiny
  └─ 🔄 Synchronizace

💡 Examples & Demos
  ├─ 📊 DataTable Demo
  └─ 📋 Kanban Demo
```

---

## 🎯 UX Vylepšení

### 1. **Seskupení podle Funkcí**
- **Analytics & Monitoring** - vše co jsou reporty a metriky
- **Správa Systému** - vše co je administrace (ploché, bez subsekce "Keycloak")
- **Studio & Design** - vývojářské nástroje (Metamodel, Workflow)
- **Examples** - oddělené DEMO položky

### 2. **Jasné Pojmenování**
- "Reports" → Grafana dashboards s business metrikami
- "Reporting Explorer" → Advanced data grid pro deep-dive analýzu
- "System Monitoring" → Technické metriky (CPU, RAM, Kafka, PostgreSQL)
- "Streaming Dashboard" → Real-time Kafka metriky

### 3. **Redukce Hierarchie**
- Odstraněno "Správa Keycloak" (přesunuto přímo pod "Správa Systému")
- Odstraněno "Bezpečnost" submenu (Audit + Security přímo pod Analytics)
- Zploštění o 1 úroveň = méně kliků

### 4. **Badge Indikátory**
- `NEW` - nové funkce (Metamodel Studio, Workflow Designer)
- `BETA` - funkce v beta fázi (Streaming Dashboard)
- `DEMO` - ukázkové stránky

---

## 🔄 Migrace Grafana IFRAME → Scenes

### Co Potřebujeme Migrovat

**1. MonitoringPage (7 panelů):**
- CPU Usage
- Memory Usage
- HTTP Requests
- Kafka Messages
- PostgreSQL Connections
- Error Rate
- Response Time

**2. AdminSecurityPage (1 panel):**
- Security Events Dashboard

**3. AdminAuditPage (1 panel):**
- Audit Log Dashboard

**4. StreamingDashboardPage (3 panely):**
- Kafka Lag
- Message Rate
- Consumer Groups

### Migrace Strategie

#### Fáze 1: Vytvoření Grafana Scenes Komponent
```tsx
// frontend/src/components/Grafana/SystemMonitoringScene.tsx
// frontend/src/components/Grafana/SecurityScene.tsx
// frontend/src/components/Grafana/AuditScene.tsx
// frontend/src/components/Grafana/StreamingScene.tsx
```

#### Fáze 2: Nahrazení GrafanaEmbed → GrafanaScene
```tsx
// Před:
<GrafanaEmbed dashboardUid="system-metrics" panelId={1} height="400px" />

// Po:
<SystemMonitoringScene metric="cpu" height={400} />
```

#### Fáze 3: BFF Endpoints pro Data
```
GET /api/monitoring/metrics/cpu
GET /api/monitoring/metrics/memory
GET /api/monitoring/metrics/kafka
GET /api/streaming/lag
GET /api/security/events
GET /api/audit/logs
```

---

## 📝 Implementační Plán

### Krok 1: Aktualizace Menu Struktury (HIGH PRIORITY)
**Soubor:** `frontend/src/shared/ui/SidebarNav.tsx`

**Změny:**
1. Přejmenovat "Administrace" → "Správa Systému"
2. Vytvořit novou sekci "Analytics & Monitoring"
3. Vytvořit novou sekci "Studio & Design"
4. Přesunout DEMO položky pod "Examples"
5. Zploštění Keycloak submenu

**Odhad:** 30 minut

---

### Krok 2: Přidání Chybějících Položek
**Přidat:**
- Workflow Designer (`/core-admin/workflows`)
- Metamodel Studio (`/core-admin/studio`)
- Streaming Dashboard (`/core-admin/streaming`) - přesunout pod Analytics

**Odhad:** 15 minut

---

### Krok 3: Vytvoření Grafana Scenes Komponent (MEDIUM PRIORITY)
**Nové soubory:**
1. `frontend/src/components/Grafana/SystemMonitoringScene.tsx`
2. `frontend/src/components/Grafana/SecurityScene.tsx`
3. `frontend/src/components/Grafana/AuditScene.tsx`
4. `frontend/src/components/Grafana/StreamingScene.tsx`

**Použití:**
- @grafana/scenes
- @grafana/faro-react
- Backend BFF endpoints

**Odhad:** 4 hodiny (komplexní komponenty)

---

### Krok 4: Migrace MonitoringPage (HIGH IMPACT)
**Soubor:** `frontend/src/pages/Admin/MonitoringPage.tsx`

**Před:**
```tsx
<GrafanaEmbed dashboardUid="system-metrics" panelId={1} />
<GrafanaEmbed dashboardUid="system-metrics" panelId={2} />
...
```

**Po:**
```tsx
<SystemMonitoringScene />
```

**Odhad:** 1 hodina

---

### Krok 5: Migrace Security + Audit + Streaming
**Soubory:**
- `AdminSecurityPage.tsx`
- `AdminAuditPage.tsx`
- `StreamingDashboardPage.tsx`

**Odhad:** 1.5 hodiny

---

### Krok 6: Backend BFF Endpoints
**Soubor:** `backend/src/main/java/cz/muriel/core/monitoring/MonitoringController.java`

**Nové endpointy:**
```java
@GetMapping("/api/monitoring/metrics/{metric}")
@GetMapping("/api/streaming/lag")
@GetMapping("/api/security/events")
@GetMapping("/api/audit/logs")
```

**Odhad:** 2 hodiny

---

### Krok 7: Testing & Cleanup
- E2E testy pro novou menu strukturu
- Cleanup starého GrafanaEmbed componentu
- Dokumentace

**Odhad:** 2 hodiny

---

## 📊 Celkový Odhad

| Úkol | Priorita | Čas |
|------|----------|-----|
| Menu struktura | HIGH | 45 min |
| Grafana Scenes komponenty | MEDIUM | 4 hod |
| Migrace stránek | HIGH | 2.5 hod |
| Backend BFF | MEDIUM | 2 hod |
| Testing | LOW | 2 hod |
| **CELKEM** | | **11 hodin** |

---

## 🎯 Doporučené Pořadí Implementace

### Session 1: Menu Optimalizace (1 hodina)
1. ✅ Aktualizovat `SidebarNav.tsx` s novou strukturou
2. ✅ Přidat chybějící položky (Workflow, Studio, Streaming)
3. ✅ Testovat navigaci
4. ✅ Commit: "feat(menu): optimize menu structure and add missing pages"

### Session 2: Grafana Scenes Foundation (4 hodiny)
1. ✅ Vytvořit base Grafana Scene komponenty
2. ✅ Implementovat SystemMonitoringScene
3. ✅ Implementovat SecurityScene, AuditScene, StreamingScene
4. ✅ Commit: "feat(grafana): migrate from iframe to Grafana Scenes"

### Session 3: Backend + Integration (4 hodiny)
1. ✅ Vytvořit BFF endpoints
2. ✅ Migrovat všechny stránky
3. ✅ Testing
4. ✅ Commit: "feat(monitoring): complete Grafana Scenes migration"

---

## 🚀 Okamžité Akce (Quick Wins)

**Co můžeme udělat TEĎ (30 minut):**
1. ✅ Aktualizovat menu strukturu
2. ✅ Přidat Workflow Designer do menu
3. ✅ Přidat Metamodel Studio do menu
4. ✅ Přesunout Streaming Dashboard do Analytics sekce
5. ✅ Commit změn

**Impact:** Uživatelé ihned uvidí všechny dostupné funkce + logičtější strukturu

---

**Požaduje schválení?** ANO/NE  
**Začít implementaci?** ANO
