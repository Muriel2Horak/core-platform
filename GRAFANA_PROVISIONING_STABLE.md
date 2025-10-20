# Grafana Provisioning - Stabilizace a Best Practices

## 🎯 Přehled

Tento dokument popisuje stabilní konfiguraci Grafana provisioning systému, který byl restrukturalizován pro prevenci chyb a zajištění konzistentní struktury.

## 📁 Struktura Dashboard Souborů

### Organizace

Všechny dashboard JSON soubory MUSÍ být v dedikovaných složkách:

```
docker/grafana/provisioning/dashboards/
├── dashboards.yml          # Provisioning konfigurace
├── custom/                 # Aplikační dashboardy (10 souborů)
│   ├── ai-ops.json
│   ├── ai-overview.json
│   ├── app-overview-dashboard.json
│   └── ...
├── monitoring-bff/         # Monitoring BFF dashboard (1 soubor)
│   └── monitoring-bff-health.json
├── system/                 # System monitoring (1 soubor)
│   └── axiom_sys_overview.json
├── advanced/               # Advanced monitoring (3 soubory)
│   ├── axiom_adv_db.json
│   ├── axiom_adv_redis.json
│   └── axiom_adv_runtime.json
├── streaming/              # Streaming monitoring (1 soubor)
│   └── axiom_kafka_lag.json
├── security/               # Security monitoring (1 soubor)
│   └── axiom_security.json
└── audit/                  # Audit monitoring (1 soubor)
    └── axiom_audit.json
```

### ⚠️ KRITICKÉ PRAVIDLO

**NIKDY** neumísťujte loose JSON soubory přímo do `dashboards/` root složky!

```
❌ ŠPATNĚ:
docker/grafana/provisioning/dashboards/my-dashboard.json

✅ SPRÁVNĚ:
docker/grafana/provisioning/dashboards/custom/my-dashboard.json
```

## 🔧 Provisioning Konfigurace

### dashboards.yml

Každý provider mapuje na dedikovanou složku:

```yaml
apiVersion: 1

providers:
  - name: 'Custom'
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards/custom
      foldersFromFilesStructure: false
  
  - name: 'Monitoring'
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards/monitoring-bff
      foldersFromFilesStructure: false
  
  # ... další providery
```

**Parametry:**
- `foldersFromFilesStructure: false` - složky se neřídí podle souborového systému
- Složka v Grafaně se určí podle `title` v JSON nebo automaticky podle `name`

## 📋 Dashboard JSON Best Practices

### Povinné Atributy

Každý dashboard JSON MUSÍ obsahovat:

```json
{
  "uid": "axiom_sys_overview",        // Stabilní UID (axiom_* konvence)
  "title": "System Overview",         // Čitelný název
  "schemaVersion": 38,                // Aktuální schema verze
  "version": 1,                       // Verze dashboardu
  "timezone": "browser",
  "panels": [...]
}
```

### UID Konvence

- **Axiom dashboardy**: `axiom_<category>_<name>`
  - Příklady: `axiom_sys_overview`, `axiom_adv_db`, `axiom_kafka_lag`
- **Custom dashboardy**: libovolný kebab-case string
  - Příklady: `ai-ops`, `app-overview-dashboard`, `streaming-overview`

### ❌ NEDÁVEJTE DO JSON

```json
{
  "folderUid": "...",  // ❌ Odstraňte - konflikt s file provisioning
  "id": 123            // ❌ Nechte null nebo odstraňte - auto-generuje se
}
```

## 🏗️ Backend Architecture

### Jediný Provisioning Service

**EXISTUJE POUZE JEDEN SERVICE:**

```java
cz.muriel.core.monitoring.grafana.GrafanaProvisioningService
```

**TENTO SERVICE BYL SMAZÁN (duplicita):**

```java
❌ cz.muriel.core.service.GrafanaMonitoringProvisioningService
```

### Použití v Controllerech

```java
@RestController
public class TenantManagementController {
    
    @Autowired
    private GrafanaProvisioningService grafanaProvisioningService;
    
    @PostMapping("/tenants")
    public void createTenant(@RequestBody TenantRequest request) {
        // Provision org + service account pro tenant
        grafanaProvisioningService.provisionTenant(request.getTenantKey());
    }
}
```

## 🧪 Validace a Testování

### Makefile Targety

```bash
# Validace struktury (před commitem)
make validate-dashboard-structure

# Runtime diagnostika (po startu Grafany)
make diag-grafana-provisioning

# Kompletní monitoring test suite
make test-monitoring
```

### Scripts

#### validate-dashboard-structure.sh

Kontroluje:
- ✅ Žádné loose JSON soubory v root
- ✅ Všechny očekávané složky existují
- 📊 Počet dashboardů v každé složce

#### test-grafana-provisioning.sh

Testuje (přes Grafana REST API):
- ✅ Grafana health endpoint
- ✅ Všech 7 složek je vytvořeno
- ✅ Všech 7 Axiom dashboardů je načteno
- ⚠️ Dashboard render (smoke test)

### Pre-commit Hook

V `lefthook.yml`:

```yaml
pre-commit:
  commands:
    dashboard-structure:
      glob: "docker/grafana/provisioning/dashboards/**/*.json"
      run: bash scripts/validate-dashboard-structure.sh
      fail_text: "❌ Dashboard structure validation failed"
```

Automaticky blokuje commit pokud:
- Loose JSON soubory v root
- Chybějící povinné složky

## 🚀 CI/CD Integration

### GitHub Actions Workflow

`.github/workflows/grafana-provisioning.yml` provádí:

1. **Validate Structure** - kontrola souborové struktury
2. **Runtime Tests** - spuštění Grafany v CI a validace provisioning
3. **UID Verification** - kontrola stabilních UIDs
4. **Summary** - agregovaný report

### Triggery

Workflow běží automaticky při změnách v:
- `docker/grafana/provisioning/**`
- `backend/src/main/java/cz/muriel/core/monitoring/grafana/**`
- `scripts/test-grafana-provisioning.sh`
- `scripts/validate-dashboard-structure.sh`

## 🔍 Diagnostika Problémů

### Grafana Crashuje při Startu

**Symptom:**
```
expected folder, found dashboard
```

**Příčina:** Loose JSON soubory v root složce

**Řešení:**
```bash
# Najdi loose soubory
find docker/grafana/provisioning/dashboards -maxdepth 1 -name "*.json"

# Přesuň do správné složky
mv docker/grafana/provisioning/dashboards/*.json \
   docker/grafana/provisioning/dashboards/custom/
```

### Dashboard se Nenačte

**Diagnostika:**
```bash
# Zkontroluj Grafana API
docker exec core-grafana curl -u admin:admin \
  "http://localhost:3000/api/search?type=dash-db"

# Zkontroluj logy
docker logs core-grafana | grep -i error
```

**Možné příčiny:**
1. Chybí `uid` nebo `title` v JSON
2. Nevalidní JSON syntax
3. Příliš stará `schemaVersion`
4. Provider mapuje na neexistující složku

### Duplicitní Provisioning

**Symptom:** Dashboard se vytváří 2x, konfliktní org creation

**Diagnostika:**
```bash
# Hledej všechny provisioning services
grep -r "provisionTenant\|createOrganization" backend/src --include="*.java"
```

**Řešení:** Pouze jeden service: `GrafanaProvisioningService`

## 📊 Monitoring Metriky

Dashboard provisioning by měl být monitorován:

- **Startup Time** - čas do health check
- **Dashboard Count** - očekáváno 18 dashboardů
- **Folder Count** - očekáváno 7 složek
- **API Response Time** - `/api/search` latence
- **Error Rate** - provisioning errors v logách

## 🎓 Best Practices Summary

### DO ✅

- Umístit dashboardy do dedikovaných složek
- Použít stabilní UIDs (axiom_* konvence)
- Nastavit `foldersFromFilesStructure: false`
- Udržovat aktuální `schemaVersion`
- Validovat strukturu před commitem
- Testovat provisioning v CI

### DON'T ❌

- Loose JSON soubory v root
- Manuální `folderUid` v JSON
- Více provisioning services v backendu
- Hardcoded `id` v dashboardech
- Commitovat bez validace
- Skipovatfailed pre-commit hooks

## 🔗 Související Dokumentace

- [Grafana File Provisioning](https://grafana.com/docs/grafana/latest/administration/provisioning/#dashboards)
- [Dashboard JSON Schema](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/view-dashboard-json-model/)
- [MONITORING_IMPLEMENTATION_COMPLETE.md](./MONITORING_IMPLEMENTATION_COMPLETE.md)
- [GRAFANA_PROVISIONING_README.md](./GRAFANA_PROVISIONING_README.md)

## 📝 Changelog

### 2025-01-20 - Stabilizace a Dokumentace

- ✅ Odstraněn duplicitní `GrafanaMonitoringProvisioningService`
- ✅ Reorganizace do 7 dedikovaných složek
- ✅ Přidána validace struktury (`validate-dashboard-structure.sh`)
- ✅ Přidána runtime diagnostika (`test-grafana-provisioning.sh`)
- ✅ Integrace s lefthook pre-commit hooks
- ✅ CI workflow pro automatické testování
- ✅ Stabilní UIDs pro všechny dashboardy

---

**Autor:** Core Platform Team  
**Poslední update:** 20. října 2025  
**Status:** ✅ Production Ready
