# Loki Native Monitoring UI - User Guide

## 📊 Overview

Core Platform nyní používá **nativní Loki UI** místo Grafana iframe integrace pro monitoring a log analysis. Tato změna přináší:

- ✅ Lepší výkon (žádné iframe overhead)
- ✅ Konzistentní UX s platformou
- ✅ Automatická tenant isolation (admin vidí pouze své logy)
- ✅ Flexibilní LogQL query builder
- ✅ CSV export pro další analýzu

## 🎯 Klíčové Komponenty

### 1. LogViewer Component

Interaktivní log viewer s pokročilými funkcemi:

```tsx
<LogViewer 
  defaultQuery='{service="backend"} |~ "(?i)error"'
  defaultHours={3}
/>
```

**Funkce:**
- **LogQL Query Input**: Zadejte vlastní LogQL dotaz
- **Time Range Selector**: 1h, 3h, 6h, 12h, 24h
- **Auto-Refresh**: Automatické načítání každých 30s
- **CSV Export**: Stažení logů do CSV souboru
- **Colored Log Levels**: ERROR (červená), WARN (oranžová), INFO (modrá)

**Příklad LogQL dotazů:**
```logql
# Všechny error logy
{service="backend"} |~ "(?i)error"

# Security events
{service=~".+"} |~ "(?i)(401|403|unauthorized|denied)"

# Audit log (CRUD operace)
{service="backend"} |~ "(?i)(created|updated|deleted|transition)"

# Kafka/Streaming events
{service="backend"} |~ "(?i)(kafka|outbox|dlq)"
```

### 2. MetricCard Component

Dashboard karta zobrazující agregované metriky:

```tsx
<MetricCard 
  title="System Metrics"
  hours={1}
/>
```

**Zobrazuje:**
- **Total Logs**: Počet všech logů za období
- **Error Logs**: Počet ERROR logů
- **Error Rate**: Procentuální podíl chyb (zdraví: <5%)

**Health Indicators:**
- 🟢 Zelená: Error rate < 5% (HEALTHY)
- 🔴 Červená: Error rate ≥ 5% (DEGRADED)

## 📍 Dostupnost v UI

### Admin Pages

1. **Monitoring Page** (`/admin/monitoring`)
   - 3 taby: System, Security, Audit
   - Každý tab má LogViewer s přednastaveným query

2. **Security Page** (`/admin/security`)
   - MetricCard + LogViewer pro security events
   - Default query: `{service=~".+"} |~ "(?i)(401|403|unauthorized)"`
   - Time range: 24h

3. **Audit Page** (`/admin/audit`)
   - MetricCard + LogViewer pro audit logy
   - Default query: `{service="backend"} |~ "(?i)(audit|created|updated)"`
   - Time range: 12h

4. **Streaming Dashboard** (`/admin/streaming`)
   - Existující metrics cards (queueDepth, DLQ) + LogViewer
   - Default query: `{service="backend"} |~ "(?i)(streaming|kafka|outbox)"`
   - Time range: 6h

### Reports

5. **Reports Page** (`/admin/reports`)
   - 3 taby: System Logs, Application Logs, Security Logs
   - Každý tab má MetricCard + LogViewer

## 🔒 Tenant Isolation

**Automatická tenant izolace** zajištěna na backend BFF úrovni:

Frontend posílá dotaz:
```logql
{service="backend"} |~ "(?i)error"
```

Backend BFF automaticky přidá tenant filter:
```logql
{tenant="admin",service="backend"} |~ "(?i)error"
```

**Výsledek:** Admin tenant vidí **pouze své vlastní logy**, nikdy ne logy jiných tenantů.

## 🛠️ Backend API (BFF)

### Endpoints

#### 1. GET `/api/monitoring/logs`

Načte logy z Loki s automatickou tenant isolation.

**Query parametry:**
- `query` (string): LogQL dotaz (bez tenant filter)
- `hours` (int): Časový rozsah v hodinách (default: 1)

**Response:**
```json
{
  "data": {
    "result": [
      {
        "stream": {
          "service": "backend",
          "level": "ERROR",
          "tenant": "admin"
        },
        "values": [
          ["1761428257658000000", "NullPointerException at line 42"]
        ]
      }
    ]
  },
  "stats": {
    "bytesProcessed": 1024,
    "linesProcessed": 150
  }
}
```

#### 2. GET `/api/monitoring/labels`

Vrací dostupné Loki labely (tenant-scoped).

**Response:**
```json
{
  "data": ["service", "level", "tenant", "host"]
}
```

#### 3. GET `/api/monitoring/labels/{label}/values`

Vrací možné hodnoty pro daný label.

**Příklad:** `/api/monitoring/labels/service/values`

**Response:**
```json
{
  "data": ["backend", "frontend", "nginx"]
}
```

#### 4. GET `/api/monitoring/metrics-summary`

Agregované metriky za časové období.

**Query parametry:**
- `hours` (int): Časový rozsah (default: 1)

**Response:**
```json
{
  "totalLogs": 1523,
  "errorLogs": 42,
  "errorRate": 2.76
}
```

## 🔧 Configuration

### Backend (`application.properties`)

```properties
# Loki HTTP API endpoint
loki.url=http://loki:3100

# Query timeout (ISO-8601 duration)
loki.query.timeout=30s

# Max entries per query
loki.query.max-entries=5000

# Feature flag (enable Loki integration)
monitoring.loki.enabled=true

# Deprecated Grafana services (disabled)
monitoring.grafana.enabled=false
```

### Circuit Breaker (Resilience4j)

LokiClient používá Circuit Breaker pro resilience:

```yaml
resilience4j.circuitbreaker:
  instances:
    loki:
      sliding-window-size: 10
      failure-rate-threshold: 50
      wait-duration-in-open-state: 10s
```

## 📊 Usage Examples

### Security Monitoring

```tsx
// Monitoring Page - Security Tab
<LogViewer 
  defaultQuery='{service=~".+"} |~ "(?i)(401|403|unauthorized|failed|denied|security)"'
  defaultHours={24}
/>
```

**Use case:** Detekce neautorizovaných přístupů, failed login attempts, security threats.

### Application Error Tracking

```tsx
// Reports - Application Logs Tab
<LogViewer 
  defaultQuery='{service="backend"} |= "ERROR"'
  defaultHours={3}
/>
```

**Use case:** Real-time monitoring aplikačních chyb, stack traces, exceptions.

### Audit Compliance

```tsx
// Admin Audit Page
<LogViewer 
  defaultQuery='{service="backend"} |~ "(?i)(audit|created|updated|deleted|transition)"'
  defaultHours={12}
/>
```

**Use case:** Sledování CRUD operací, workflow transitions, compliance reporting.

### Streaming/Kafka Debugging

```tsx
// Streaming Dashboard
<LogViewer 
  defaultQuery='{service="backend"} |~ "(?i)(streaming|kafka|outbox|dlq)"'
  defaultHours={6}
/>
```

**Use case:** Kafka lag analysis, outbox pattern monitoring, DLQ troubleshooting.

## 🚀 Advanced Features

### Custom LogQL Queries

LogQL syntax podporuje:

- **Label matchers**: `{service="backend",level="ERROR"}`
- **Regex filtering**: `|~ "(?i)pattern"`
- **Negation**: `!= "exclude"`
- **JSON parsing**: `| json | line_format "{{.message}}"`

### CSV Export

Export všech viditelných logů do CSV:

1. Klikněte na **Export CSV** tlačítko
2. Soubor se stáhne jako `logs-YYYY-MM-DD.csv`
3. Obsahuje: Timestamp, Level, Service, Message

**Příklad CSV:**
```csv
Timestamp,Level,Service,Message
2025-01-05T14:30:00Z,ERROR,backend,NullPointerException
2025-01-05T14:29:55Z,WARN,backend,High memory usage
```

### Auto-Refresh

Toggle **Auto Refresh** pro real-time monitoring:

- **Enabled**: Načítá nové logy každých 30s
- **Disabled**: Manuální refresh pouze

**Use case:** Production incident monitoring, live debugging.

## 🐛 Troubleshooting

### "Failed to load logs"

**Příčiny:**
- Loki není dostupný (check `http://loki:3100`)
- Circuit breaker OPEN (příliš mnoho failures)
- Invalid LogQL syntax

**Řešení:**
```bash
# Check Loki health
curl http://loki:3100/ready

# View Loki logs
make logs | grep loki

# Verify backend BFF logs
make logs-backend | grep -i loki
```

### "No logs available"

**Příčiny:**
- Žádné logy v daném časovém rozsahu
- Tenant filter příliš restriktivní
- LogQL query nesedí na žádné logy

**Řešení:**
1. Zvětši time range (1h → 24h)
2. Zjednoduš query: `{service=~".+"}`
3. Zkontroluj tenant má ingested logy

### CSV export selže

**Příčiny:**
- Prohlížeč blokuje download
- Příliš velký dataset (>5000 entries)

**Řešení:**
1. Zkus jiný browser
2. Zmenši time range
3. Použij specifičtější query

## 📚 Additional Resources

- [LogQL Documentation](https://grafana.com/docs/loki/latest/logql/)
- [Loki HTTP API](https://grafana.com/docs/loki/latest/api/)
- [Circuit Breaker Pattern](https://resilience4j.readme.io/docs/circuitbreaker)
- [Tenant Isolation Best Practices](../docs/MULTI_TENANCY.md)

## 🎓 Migration Notes

Pokud jste dříve používali Grafana iframe:

- ✅ Všechny dashboardy nahrazeny LogViewer komponentami
- ✅ Grafana SSO bridge deprecated (monitoring.grafana.enabled=false)
- ✅ Lepší výkon (žádné iframe embedding overhead)
- ✅ Konzistentní dark/light theme s platformou

**Migration timeline:**
- S1: Grafana FE removal ✅ (Commit 1541884)
- S2: Loki HTTP API integration ✅
- S3: BFF monitoring endpoints ✅
- S4: Frontend components ✅
- S5: Replace all placeholders ✅ (Commit 9715b41)
- S6: E2E tests ✅
- S7: Documentation ✅ (this doc)

---

**Version:** 1.0.0  
**Last Updated:** 2025-01-05  
**Author:** Core Platform Team
