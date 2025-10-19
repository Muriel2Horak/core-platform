# Grafana Provisioning - Race Condition Fix

## 🐛 Problém

Monitoring dashboard zobrazoval **HTTP 400 chyby** i když provisioner logy ukazovaly úspěch (3 rows inserted).

### Root Cause

**Race condition** mezi provisioningem a backend startem:

```
Timeline:
17:17:00 - grafana-provisioner starts (depends_on: grafana, db)
17:17:10 - Provisioner: CREATE TABLE grafana_tenant_bindings
17:17:15 - Provisioner: INSERT INTO ... (3 rows) ✅
17:17:20 - Provisioner exits (summary: 3 rows)
17:17:52 - backend starts (52 second delay!)
17:17:52 - Flyway: CREATE TABLE grafana_tenant_bindings (RECREATES!)
17:17:53 - Result: Database has 0 rows ❌
```

**Evidence:**
- Provisioner logs: `(3 rows)` ✅
- Database query: `(0 rows)` ❌  
- Backend Flyway migration **přepsala tabulku** vytvořenou provisionerem!

## ✅ Řešení

### 1. Přidán `depends_on` na backend

```yaml
grafana-provisioner:
  depends_on:
    backend:
      condition: service_healthy  # ← NOVÉ: Počká na backend!
    grafana:
      condition: service_started
    db:
      condition: service_healthy
```

### 2. Přidán healthcheck do backend

```yaml
backend:
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    interval: 10s
    timeout: 5s
    retries: 30
    start_period: 60s
```

## 📊 Výsledek

**Nový timeline:**
```
17:17:00 - db starts
17:17:05 - backend starts
17:17:52 - backend HEALTHY ✅ (Flyway dokončen)
17:17:53 - grafana-provisioner starts (čeká na backend!)
17:18:05 - Provisioner: INSERT INTO ... (tabulka už existuje)
17:18:10 - Database: 3 rows ✅
```

**Ověření:**
```bash
docker exec core-db psql -U core -d core -c \
  "SELECT tenant_id, grafana_org_id FROM grafana_tenant_bindings;"

# Result:
#   tenant_id  | grafana_org_id 
# -------------+----------------
#  admin       |              2
#  test-tenant |              3
#  company-b   |              4
# (3 rows)  ← DATA PERSISTED! ✅
```

## 🧪 Testing

Dashboard monitoring nyní zobrazuje **reálná Prometheus data** bez HTTP 400 chyb.

## 📝 Files Changed

- `docker/docker-compose.yml`:
  - Přidán `condition: service_healthy` pro backend v provisioner depends_on
  - Přidán healthcheck do backend service
  
## 🔗 Related

- Resolves: "nevím co jsi testoval, protože hned první pohled a nic se nezměnilo"
- Context: 8+ hodinová debugging session, manual provisioning fungoval (protože byl PO backend startu)
- Fix duration: 20 minut (identifikace + implementace)
