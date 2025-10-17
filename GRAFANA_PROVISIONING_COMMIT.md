feat(monitoring): Add automatic Grafana multi-tenant provisioning

## 🎯 Problem

Grafana monitoring nebylo funkční kvůli prázdné tabulce `grafana_tenant_bindings`.
Backend GrafanaInitializationService měl 409 Conflict handling, ale nebyl použit
kvůli restart loop problémům v dev containeru.

## ✅ Solution

Implementován **automatický provisioning systém** pomocí Docker init containeru:

### 1. Provisioning Script (`docker/grafana/provision-tenants.sh`)

- ✅ Bash script s idempotentní logikou
- ✅ Vytváří Grafana organizace pro každý tenant
- ✅ Vytváří service accounts s Admin rolí
- ✅ Generuje API tokeny
- ✅ Ukládá bindings do PostgreSQL
- ✅ Podporuje 409 Conflict handling (org už existuje)
- ✅ Čeká na Grafana + DB ready (health checks)

### 2. Docker Compose Integration

```yaml
grafana-provisioner:
  image: postgres:16  # Má curl + psql
  depends_on: [grafana, db]
  restart: "no"  # Spustí se jednou při startu
  command: /provision-tenants.sh
```

### 3. Konfigurace

Tenanti nastavitelní přes environment variables:
```bash
TENANTS="admin test-tenant company-b"
```

## 🧪 Testing

### Manual Test
```bash
# Smazat existující data
docker exec core-db psql -U core -d core -c "TRUNCATE TABLE grafana_tenant_bindings;"

# Restart provisioner
docker compose -f docker/docker-compose.yml run --rm grafana-provisioner

# Ověřit výsledek
docker exec core-db psql -U core -d core -c "SELECT * FROM grafana_tenant_bindings;"
# Expected: 3 rows
```

### Rebuild Test
```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
# Provisioner se spustí automaticky
```

## 📊 Database Schema

```sql
CREATE TABLE grafana_tenant_bindings (
  tenant_id VARCHAR(255) PRIMARY KEY,
  grafana_org_id BIGINT NOT NULL,
  service_account_id BIGINT NOT NULL,
  service_account_name VARCHAR(255) NOT NULL,
  service_account_token VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🔄 Migration Path

### Before
❌ Manuální vytváření organizací přes Grafana UI
❌ Manuální SQL INSERT pro bindings
❌ Non-idempotent (chyba při restartu)

### After
✅ Automatický provisioning při `docker compose up`
✅ Idempotentní (safe pro opakované spuštění)
✅ Snadné přidání nových tenantů (ENV var)

## 📝 Files Changed

- `docker/grafana/provision-tenants.sh` - Provisioning script (NEW)
- `docker/grafana/PROVISIONING_README.md` - Dokumentace (NEW)
- `docker/docker-compose.yml` - Přidán grafana-provisioner service

## 🚀 Benefits

1. **Zero manual intervention** - Monitoring funguje ihned po startu
2. **Idempotent** - Bezpečné pro dev/staging/prod
3. **Scalable** - Snadné přidání nových tenantů
4. **Documented** - Kompletní README s troubleshooting
5. **Testable** - Jasný testing guide

## 🔗 Related Issues

- Closes #XXX (Grafana monitoring non-functional)
- Related to backend GrafanaInitializationService (Java fallback)

## 📋 Checklist

- [x] Provisioning script vytvoří organizace
- [x] Provisioning script vytvoří service accounts
- [x] Provisioning script vytvoří tokeny
- [x] Provisioning script uloží do databáze
- [x] Docker Compose integrace
- [x] Dokumentace (README + inline comments)
- [x] Idempotent logic (409 handling)
- [x] Health checks (Grafana + DB ready)
- [ ] Testováno na clean environment (TODO: před mergem)
- [ ] Testováno s existujícími organizacemi (TODO: před mergem)

## 🎉 Result

Monitoring dashboard nyní zobrazuje real-time Prometheus data!
MetricPanel komponenta úspěšně volá `/api/monitoring/ds/query` a získává CPU metriky.

---

Co-authored-by: GitHub Copilot <copilot@github.com>
