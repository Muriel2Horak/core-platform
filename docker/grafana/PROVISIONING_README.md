# Grafana Multi-Tenant Auto-Provisioning

## 📋 Přehled

Automatický provisioning systém pro Grafana multi-tenancy. Při každém startu Docker Compose:

1. ✅ Vytvoří Grafana organizace pro každý tenant
2. ✅ Vytvoří service accounts s Admin rolí
3. ✅ Vygeneruje API tokeny
4. ✅ Uloží tenant bindings do PostgreSQL databáze

## 🏗️ Architektura

```
docker-compose.yml
├── grafana (main service)
└── grafana-provisioner (init container)
    ├── Čeká na Grafana + DB ready
    ├── Spustí provision-tenants.sh
    └── Ukončí se po dokončení
```

## 🚀 Použití

### Automatický start s Docker Compose

```bash
# Standard start - provisioning se spustí automaticky
docker compose -f docker/docker-compose.yml up -d

# Dev režim s hot reload
make dev-up
```

### Ruční spuštění provisioning scriptu

```bash
# Z hosta
docker compose -f docker/docker-compose.yml run --rm grafana-provisioner

# Nebo přímo bash script
cd docker/grafana
./provision-tenants.sh
```

## ⚙️ Konfigurace

### Environment Variables

Nastavitelné v `.env` nebo `docker-compose.yml`:

```bash
# Grafana přístup
GRAFANA_URL=http://grafana:3000
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin

# Database přístup
DB_HOST=db
DB_PORT=5432
DB_NAME=core
DB_USER=core
DB_PASSWORD=core

# Seznam tenantů (space-separated)
TENANTS="admin test-tenant company-b"
```

### Přidání nového tenanta

1. Přidej tenant ID do `TENANTS` v docker-compose.yml:
   ```yaml
   - TENANTS=admin test-tenant company-b new-tenant
   ```

2. Restart provisioner:
   ```bash
   docker compose -f docker/docker-compose.yml restart grafana-provisioner
   ```

3. Nebo spusť ručně:
   ```bash
   TENANTS="new-tenant" docker compose -f docker/docker-compose.yml run --rm grafana-provisioner
   ```

## 🗄️ Database Schema

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

### Ověření dat

```bash
# Postgres CLI
docker exec core-db psql -U core -d core -c "SELECT * FROM grafana_tenant_bindings;"

# Očekávaný výstup:
#  tenant_id  | grafana_org_id | service_account_id | token_len 
# -------------+----------------+--------------------+-----------
#  admin       |              5 |                  5 |        46
#  test-tenant |              6 |                  6 |        46
#  company-b   |              7 |                  7 |        46
```

## 🔍 Troubleshooting

### Provisioner selhává

```bash
# Zkontroluj logy
docker logs core-grafana-provisioner

# Časté problémy:
# 1. Grafana ještě není ready → script čeká až 60s
# 2. Database není ready → script čeká až 60s
# 3. Organizace již existují → script použije existující (idempotentní)
```

### Organizace chybí v Grafana

```bash
# Zkontroluj Grafana API
docker exec core-grafana curl -s -u 'admin:admin' 'http://localhost:3000/api/orgs'

# Očekávaný výstup:
# [{"id":1,"name":"Main Org."},
#  {"id":5,"name":"Tenant: admin"},
#  {"id":6,"name":"Tenant: test-tenant"},
#  {"id":7,"name":"Tenant: company-b"}]
```

### Bindings chybí v databázi

```bash
# Zkontroluj tabulku
docker exec core-db psql -U core -d core -c "SELECT COUNT(*) FROM grafana_tenant_bindings;"

# Pokud je prázdná, spusť provisioner znovu
docker compose -f docker/docker-compose.yml run --rm grafana-provisioner
```

### 409 Conflict při vytváření organizací

✅ **Normální chování!** Script je idempotentní:
- Pokud organizace existuje → najde ji a použije
- Pokud service account existuje → najde ho a použije
- Pouze tokeny se vždy vytvoří nové

### Backend Java provisioning vs. Docker provisioning

Máme **DVĚ** provisioning strategie:

1. **Docker provisioning** (tento script)
   - Běží při startu Docker Compose
   - Nezávislý na Javě
   - ✅ Spolehlivější pro dev prostředí

2. **Java provisioning** (GrafanaInitializationService)
   - Běží při startu Spring Boot
   - Používá GrafanaAdminClient + Circuit Breaker
   - ✅ Lepší pro production (programový přístup)

**Doporučení:**
- Dev: Použij Docker provisioning (rychlejší, jednodušší)
- Prod: Použij Java provisioning (lepší error handling, monitoring)

## 🧪 Testování

### Test manuálně

```bash
# 1. Smaž všechna data
docker exec core-db psql -U core -d core -c "TRUNCATE TABLE grafana_tenant_bindings;"
docker exec core-grafana curl -s -u 'admin:admin' -X DELETE 'http://localhost:3000/api/orgs/5'
docker exec core-grafana curl -s -u 'admin:admin' -X DELETE 'http://localhost:3000/api/orgs/6'
docker exec core-grafana curl -s -u 'admin:admin' -X DELETE 'http://localhost:3000/api/orgs/7'

# 2. Spusť provisioning
docker compose -f docker/docker-compose.yml run --rm grafana-provisioner

# 3. Ověř výsledek
docker exec core-db psql -U core -d core -c "SELECT COUNT(*) FROM grafana_tenant_bindings;"
# Očekáváno: 3
```

### Test při rebuildu

```bash
# Kompletní rebuild
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d

# Grafana provisioner by se měl spustit automaticky
docker logs core-grafana-provisioner

# Ověř data
docker exec core-db psql -U core -d core -c "SELECT * FROM grafana_tenant_bindings;"
```

## 📝 Implementation Notes

### Proč není v GrafanaInitializationService.java?

Původní plán byl použít Spring `@EventListener(ApplicationReadyEvent)`, ale:

❌ **Problémy:**
1. Backend v dev režimu má restart loop (Maven issues)
2. Závislost na Spring Boot startupu
3. Pomalejší při každém restartu
4. Těžší debugging (Java logs vs. bash logs)

✅ **Docker provisioning výhody:**
1. Nezávislý na Java/Spring
2. Rychlejší (bash + curl)
3. Snadné testování
4. Jasné logy
5. Idempotentní (safe pro opakované spuštění)

### Budoucí vylepšení

- [ ] Přidat health check endpoint do provisioner
- [ ] Přidat Prometheus metriky (počet provisionovaných tenantů)
- [ ] Webhook notifikace po dokončení
- [ ] Token rotation (automatické obnovení každých 30 dní)
- [ ] Support pro custom tenant configs (různé role, limity)

## 🔗 Související

- Backend: `backend/src/main/java/cz/muriel/core/monitoring/grafana/`
- Grafana config: `docker/grafana/provisioning/`
- Database schema: `docker/db/init/`
