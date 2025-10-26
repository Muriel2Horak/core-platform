# 📊 Grafana Ops Access Guide

**URL:** https://ops.core-platform.local/grafana  
**Účel:** Standalone monitoring tool pro DevOps/SRE  
**Přístup:** SSO přes Keycloak (admin realm)

---

## 🔐 Přihlášení

1. Přejdi na https://ops.core-platform.local/grafana
2. Klikni na **"Sign in with Keycloak"**
3. Zadej své admin realm credentials
4. Automatické mapování rolí podle Keycloak skupin

---

## 👥 Role Mapping

Přístupová práva jsou řízena přes Keycloak groups v admin realm:

| Keycloak Group | Grafana Role | Oprávnění |
|----------------|--------------|-----------|
| `grafana-admin` | **Admin** | Full access (dashboards, datasources, users, settings) |
| `grafana-editor` | **Editor** | Tvorba/úprava dashboardů, query explorer |
| _(ostatní)_ | **Viewer** | Read-only access, prohlížení dashboardů |

### Jak přidat uživatele do skupiny

```bash
# Keycloak Admin Console: https://admin.core-platform.local/admin
# 1. Admin realm → Groups → Create group
# 2. Users → [user] → Groups → Join Group
```

---

## 📂 Data Sources

### Loki (Logs)
- **URL:** `http://loki:3100`
- **Typ:** Loki
- **Použití:** Vyhledávání logů, alerting
- **Příklad query:**
  ```logql
  {service="core-backend"} |= "ERROR"
  ```

### Prometheus (Metriky)
- **URL:** `http://prometheus:9090`
- **Typ:** Prometheus
- **Použití:** Metriky (CPU, paměť, HTTP requests)
- **Příklad query:**
  ```promql
  rate(http_requests_total[5m])
  ```

---

## 📊 Dashboards

### Provisioning (Git → Grafana)

Všechny dashboardy jsou verzované v Gitu:

```
docker/grafana/
├── provisioning/
│   ├── dashboards/
│   │   └── default.yml         # Provisioning config
│   └── datasources/
│       ├── loki.yml            # Loki datasource
│       └── prometheus.yml      # Prometheus datasource
└── dashboards/
    ├── ops-system-health.json  # System overview
    ├── ops-service-metrics.json
    └── ops-loki-logs.json
```

### Workflow: Dashboard Creation

1. **Vytvoř dashboard v UI** (Editor+)
   - Grafana → Dashboards → New Dashboard
   - Ulož s popisem a tags

2. **Export to JSON**
   - Dashboard settings (⚙️) → JSON Model → Copy to clipboard
   - Nebo: Share → Export → Save to file

3. **Commit to Git**
   ```bash
   # Save JSON to dashboards/
   vim docker/grafana/dashboards/ops-my-dashboard.json
   
   # Test syntax
   jq . docker/grafana/dashboards/ops-my-dashboard.json > /dev/null
   
   # Commit
   git add docker/grafana/dashboards/ops-my-dashboard.json
   git commit -m "feat(grafana): Add ops-my-dashboard"
   git push
   ```

4. **Deploy**
   ```bash
   make up
   # Grafana auto-reloads dashboards každých 30s
   ```

### CI Validation (budoucnost)

```yaml
# .github/workflows/grafana-dashboards.yml
- name: Validate Grafana Dashboards
  run: |
    for file in docker/grafana/dashboards/*.json; do
      jq . "$file" > /dev/null || exit 1
    done
```

---

## 🚨 Alerting

### Option A: Alertmanager (preferováno)

```yaml
# docker/prometheus/alerting-rules/grafana.yml
groups:
  - name: grafana-ops
    rules:
      - alert: HighErrorRate
        expr: rate(loki_lines_total{level="error"}[5m]) > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
```

### Option B: Grafana Alerting

```yaml
# docker/grafana/provisioning/alerting/alerts.yml
apiVersion: 1
groups:
  - name: ops-alerts
    folder: Ops
    interval: 1m
    rules:
      - uid: high-error-rate
        title: High Error Rate
        condition: A
        data:
          - refId: A
            queryType: ''
            relativeTimeRange:
              from: 300
              to: 0
            datasourceUid: loki
            model:
              expr: 'rate({service="core-backend"} |= "ERROR" [5m])'
```

---

## 🔧 Troubleshooting

### Grafana nenaběhne

```bash
# Check logs
make logs | grep grafana

# Common issues:
# 1. DB connection failed → check PostgreSQL
# 2. OIDC secret chybí → check .env GRAFANA_OIDC_SECRET
# 3. Keycloak není ready → wait for keycloak healthy
```

### SSO login nefunguje

```bash
# 1. Check Keycloak client config
https://admin.core-platform.local/admin
→ Clients → grafana-ops
→ Valid Redirect URIs: https://ops.core-platform.local/grafana/login/generic_oauth

# 2. Check Grafana logs
docker logs core-grafana | grep -i oauth

# 3. Test OIDC endpoint
curl -k https://ops.core-platform.local/realms/admin/.well-known/openid-configuration
```

### Dashboard nejde uložit

**Problém:** Provisioned dashboards jsou read-only.

**Řešení:**
1. Save as Copy (jiný název)
2. Nebo: Export JSON → edit v Git → commit

---

## 📈 Best Practices

### 1. Dashboard Organization

```
Folders:
├── Ops/              # System-level dashboards (CPU, disk, network)
├── Services/         # Per-service dashboards (backend, frontend)
├── Business/         # Business metrics (user sign-ups, orders)
└── Development/      # Dev/staging experimental dashboards
```

### 2. Naming Convention

```
Format: [category]-[component]-[metric].json
Examples:
- ops-system-health.json
- service-backend-performance.json
- business-user-funnel.json
```

### 3. Variables & Templating

```json
{
  "templating": {
    "list": [
      {
        "name": "tenant",
        "type": "custom",
        "query": "core-platform,test-tenant,acme",
        "current": {"value": "$__all"}
      }
    ]
  }
}
```

Query example:
```logql
{service="core-backend", tenant="$tenant"} |= "ERROR"
```

### 4. Time Ranges

- **Quick ranges:** Last 15m, Last 1h, Last 6h, Last 24h, Last 7d
- **Default:** Last 6h (balance mezi detail a overview)
- **Auto-refresh:** 30s (real-time monitoring), 5m (historical)

---

## 🔗 Quick Links

- **Grafana UI:** https://ops.core-platform.local/grafana
- **Loki query:** https://ops.core-platform.local/grafana/explore?ds=loki
- **Prometheus query:** https://ops.core-platform.local/grafana/explore?ds=prometheus
- **Keycloak Admin:** https://admin.core-platform.local/admin

---

## 📝 FAQ

**Q: Můžu sdílet dashboard s ostatními?**  
A: Ano, přes "Share" → "Link" (vyžaduje autentizaci) nebo "Snapshot" (anonymní, read-only).

**Q: Jak nastavit alert notifications (email, Slack)?**  
A: Grafana → Alerting → Contact points → Add contact point.

**Q: Grafana je pomalá při velkých time ranges**  
A: Použij kratší range nebo downsampling (`rate()`, `avg_over_time()`).

**Q: Může tenant vidět jen svoje logy?**  
A: Ne - Grafana ops je jen pro admin/ops tým. Tenanti používají FE Loki UI (přes BFF s tenant izolací).

---

**Poslední update:** 26. října 2025  
**Kontakt:** DevOps tým
