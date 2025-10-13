# 🔍 Analýza: CDC + Tenant Management + User Synchronization

**Datum:** 2024-10-13  
**Status:** 🔴 Kritické problémy identifikovány

---

## 📋 Uživatelovy Otázky

### 1️⃣ Tenant Management - Metamodel?
❓ **Otázka:** "Tenant jeho založení změna a spol tedy nejde přes metamodel?"

**Odpověď:** ❌ **NE, tenant management NEJDE přes metamodel**

**Aktuální implementace:**
```java
// KeycloakRealmManagementService.createTenant()
// ↓
// TenantService.createTenantRegistryWithRealmId()
// ↓
// tenantRepository.save(tenant)  ← Přímý JPA save
```

**Důvod:**
- Tenant je **systémová entita**, ne business data
- Nemá workflow states (draft/proposal/version)
- Není potřeba verzování ani approval proces
- Přímo se ukládá do `tenants` tabulky

**Je to správně?** ✅ **ANO**, tenanty by NEMĚLY jít přes metamodel:
- Rychlejší operace
- Jednodušší kód
- Žádné workflow overhead
- Admin operace, ne end-user data

---

### 2️⃣ Flyway Migration V3 → V1
❓ **Otázka:** "V3 nemusíš dělat dej to do V1 přímo sestavíme prostředí znovu"

**Odpověď:** ✅ **Dobře, přesunu Grafana binding tabulku do V1__init.sql**

**Akce:**
```sql
-- Přidat na konec V1__init.sql:

-- ====== GRAFANA TENANT BINDINGS ======
CREATE TABLE grafana_tenant_bindings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    grafana_org_id BIGINT NOT NULL,
    service_account_id BIGINT NOT NULL,
    service_account_name VARCHAR(200) NOT NULL,
    service_account_token VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_grafana_tenant_bindings_tenant_id ON grafana_tenant_bindings(tenant_id);
CREATE INDEX idx_grafana_tenant_bindings_grafana_org_id ON grafana_tenant_bindings(grafana_org_id);
```

**Poté smazat:** `V3__grafana_tenant_bindings.sql`

---

### 3️⃣ Admin Tenant + Grafana Provisioning
❓ **Otázka:** "admin tenant se zakládá při sestavení prostředí, tam ty serviceaccounty založíme jak?"

**Aktuální stav:**
```java
// KeycloakInitializationService.initializeKeycloak()
// ↓
// tenantService.createTenantRegistry("admin")  ← NEZAVOLÁ provisioning!
```

**Problém:** 
- `createTenantRegistry()` NETRIGGERUJE Grafana provisioning
- Pouze `KeycloakRealmManagementService.createTenant()` volá provisioning
- Admin tenant se vytváří při startu, ne přes API

**Řešení A: Manual Provisioning při Startu**
```java
@Component
public class GrafanaInitializationService {
  
  @Autowired private GrafanaProvisioningService grafanaProvisioningService;
  @Autowired private TenantRepository tenantRepository;
  
  @PostConstruct
  @Order(20) // Po KeycloakInitializationService
  public void initializeGrafanaForExistingTenants() {
    List<Tenant> tenants = tenantRepository.findAll();
    
    for (Tenant tenant : tenants) {
      if (!grafanaProvisioningService.isTenantProvisioned(tenant.getKey())) {
        try {
          grafanaProvisioningService.provisionTenant(tenant.getKey());
          log.info("✅ Grafana provisioned for existing tenant: {}", tenant.getKey());
        } catch (Exception e) {
          log.error("⚠️ Failed to provision Grafana for tenant: {}", tenant.getKey(), e);
        }
      }
    }
  }
}
```

**Řešení B: Seed Data v V1__init.sql**
```sql
-- Po vytvoření grafana_tenant_bindings tabulky
-- Předpokládáme, že Grafana orgs jsou vytvořeny manuálně

-- INSERT pro admin tenant (org 1, manual SA token)
INSERT INTO grafana_tenant_bindings 
  (tenant_id, grafana_org_id, service_account_id, service_account_name, service_account_token)
VALUES 
  ('admin', 1, 1, 'sa-admin', '${GRAFANA_SAT_ADMIN}')
ON CONFLICT (tenant_id) DO NOTHING;
```

**Doporučení:** ✅ **Řešení A** (auto-provisioning při startu)
- Eliminuje manuální setup
- Konzistentní s novými tenanty
- Idempotentní (kontroluje `isTenantProvisioned()`)

---

### 4️⃣ Service Accounts Per Tenant - Má To Smysl?
❓ **Otázka:** "je nutné mít serviceAccounts pro každý tenant? Dává to smysl?"

**Aktuální návrh:**
```
Tenant A → Grafana Org 1 → Service Account 1 → Token 1
Tenant B → Grafana Org 2 → Service Account 2 → Token 2
Tenant C → Grafana Org 3 → Service Account 3 → Token 3
```

**Alternativa 1: Shared Service Account**
```
Tenant A ─┐
Tenant B ─┼→ Grafana Org 1 (Main Org) → Service Account 1 → Token 1
Tenant C ─┘
           └─ Datasource filters by tenant_id label
```

**Alternativa 2: Shared Token, Multi-Org**
```
All Tenants → Same Token → Grafana switches context based on X-Grafana-Org-Id header
```

### 🔍 Analýza

| Aspekt | Per-Tenant SA | Shared SA | Doporučení |
|--------|---------------|-----------|------------|
| **Security Isolation** | ✅ Úplná izolace | ⚠️ Logická separace | Per-Tenant |
| **Complexity** | ⚠️ Více tokenů | ✅ Jeden token | Shared |
| **Cost** | ⚠️ N × orgs | ✅ 1 org | Shared |
| **Scaling** | ⚠️ Limit on orgs | ✅ Neomezené | Shared |
| **Maintenance** | ⚠️ Token rotation × N | ✅ Jednou | Shared |
| **Multi-tenancy Best Practice** | ✅ **Standard** | ⚠️ Custom | **Per-Tenant** |

**Verdikt:** ✅ **Per-Tenant Service Accounts DÁVÁ SMYSL**

**Důvody:**
1. **True Multi-Tenancy**: Každý tenant má vlastní Grafana org
2. **Security**: Zero-trust - tenant A nemůže vidět data tenant B
3. **RBAC**: Per-org permissions, per-org dashboards
4. **Compliance**: GDPR/SOC2 - data isolation
5. **Debugging**: Snadná identifikace, kdo generuje traffic

**Alternativa jen pokud:**
- Máte stovky/tisíce tenantů (Grafana org limit)
- Chcete ušetřit Grafana licence (enterprise feature)
- Dostatečně robustní label-based filtering v Loki

**Náš případ:** ✅ Máme desítky tenantů, ne tisíce → **Per-tenant SA je správně**

---

### 5️⃣ CDC User Synchronization - Nefunguje! 🔴
❓ **Otázka:** "nefunguje synchronizace uživatelů do userdirectory není tam žádná chyba ale data nedotečou. V keycloaku se to změní"

## 🚨 KRITICKÝ PROBLÉM: CDC Synchronization Broken

### Symptomy
- ✅ Keycloak změny (user created/updated/deleted) → Keycloak DB
- ❌ Data se **NEOBJEVUJÍ** v `user_directory` tabulce
- ❌ **Žádné chybové zprávy** v logách

### Diagnostika - Co Kontrolovat

#### 1. Keycloak CDC Triggers
```sql
-- Zkontrolovat triggery
SELECT 
  tgname AS trigger_name,
  tgrelid::regclass AS table_name,
  proname AS function_name,
  tgenabled AS enabled
FROM pg_trigger
JOIN pg_proc ON tgfoid = pg_proc.oid
WHERE tgname LIKE '%change_event%'
ORDER BY tgrelid::regclass::text;
```

**Očekáváme:**
- `user_entity_change_event_trigger` na `user_entity`
- `realm_change_event_trigger` na `realm`
- Status: `O` (enabled)

#### 2. Change Events Tabulka
```sql
-- Zkontrolovat, jestli se generují change events
SELECT 
  id,
  table_name,
  operation,
  old_data,
  new_data,
  changed_at,
  processed,
  consumed
FROM change_events
ORDER BY changed_at DESC
LIMIT 20;
```

**Očekáváme:**
- Nové řádky při každé změně v Keycloak
- `processed = false`
- `consumed = false`

**Pokud NIC:** ❌ **Triggery nefungují nebo neexistují**

#### 3. Kafka Connect Status
```bash
# Zkontrolovat Debezium connector
curl http://localhost:8083/connectors/keycloak-cdc-connector/status | jq
```

**Očekáváme:**
```json
{
  "name": "keycloak-cdc-connector",
  "connector": {
    "state": "RUNNING"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING"
    }
  ]
}
```

**Pokud FAILED/PAUSED:** ❌ **Debezium neskenuje change_events**

#### 4. Kafka Topics
```bash
# Zkontrolovat, jestli teče data do Kafka
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic keycloak.cdc.change_events \
  --from-beginning \
  --max-messages 10
```

**Očekáváme:**
- CDC events v JSON formátu
- Každá změna v Keycloak → event

**Pokud NIC:** ❌ **Debezium nepublikuje do Kafky**

#### 5. Backend Consumer
```bash
# Zkontrolovat backend logy
docker logs backend | grep -i "keycloak.*cdc\|change.*event\|user.*sync"
```

**Očekáváme:**
```
✅ Keycloak CDC listener started
✅ Consumed change event: user_entity/INSERT
✅ Processing user sync: user-123
✅ User synced to user_directory: user-123
```

**Pokud NIC:** ❌ **Backend nekonsumuje z Kafky**

#### 6. User Directory Table
```sql
-- Zkontrolovat, jestli jsou data v user_directory
SELECT 
  id,
  username,
  email,
  first_name,
  last_name,
  tenant_id,
  keycloak_user_id,
  synced_at
FROM user_directory
ORDER BY synced_at DESC NULLS LAST
LIMIT 20;
```

**Očekáváme:**
- Všichni Keycloak users
- `synced_at` timestamp aktuální

**Pokud PRÁZDNÉ:** ❌ **Backend nezapisuje do DB**

---

## 🔬 Detailní CDC Flow Analysis

### Celá Cesta: Keycloak → Kafka → Backend → user_directory

```
┌─────────────┐
│  Keycloak   │  User created/updated/deleted
│  (DB)       │
└──────┬──────┘
       │
       │ 1. PostgreSQL Trigger
       │    (user_entity_change_event_trigger)
       ↓
┌─────────────────┐
│ change_events   │  INSERT INTO change_events (...)
│ (Keycloak DB)   │
└──────┬──────────┘
       │
       │ 2. Debezium Connector
       │    (Polling change_events table)
       ↓
┌─────────────────┐
│ Kafka Topic     │  keycloak.cdc.change_events
│ (Kafka)         │
└──────┬──────────┘
       │
       │ 3. Spring Kafka Listener
       │    (@KafkaListener)
       ↓
┌─────────────────────────┐
│ KeycloakChangeEventConsumer │  processChangeEvent()
│ (Backend)                    │
└──────┬──────────────────────┘
       │
       │ 4. UserSyncService
       │    (Business Logic)
       ↓
┌─────────────────┐
│ user_directory  │  INSERT/UPDATE user data
│ (Core DB)       │
└─────────────────┘
```

### 🔍 Kde Se To Láme?

Pojďme najít broken link v řetězci:

#### Krok 1: Triggery (Keycloak DB)
**Soubor:** `backend/src/main/resources/db/migration/V2__init_keycloak_cdc.sql`

**Kontrola:**
```bash
docker exec -it db psql -U keycloak -d keycloak -c "
  SELECT tgname, tgrelid::regclass, tgenabled 
  FROM pg_trigger 
  WHERE tgname LIKE '%change_event%'
"
```

**Očekáváme:**
```
              tgname               |   tgrelid   | tgenabled
-----------------------------------+-------------+-----------
 user_entity_change_event_trigger  | user_entity | O
 realm_change_event_trigger        | realm       | O
```

**Pokud NIC:** ❌ **Migration V2 nebyla aplikována na Keycloak DB**

**Fix:**
```bash
# Re-run migration
docker exec -it db psql -U keycloak -d keycloak -f /docker-entrypoint-initdb.d/V2__init_keycloak_cdc.sql
```

---

#### Krok 2: Change Events Table
**Kontrola:**
```bash
docker exec -it db psql -U keycloak -d keycloak -c "
  SELECT COUNT(*) as total_events, 
         COUNT(*) FILTER (WHERE processed = false) as unprocessed
  FROM change_events
"
```

**Očekáváme:**
```
 total_events | unprocessed
--------------+-------------
          127 |          85
```

**Pokud 0/0:** ❌ **Triggery nefungují nebo tabulka neexistuje**

**Test Trigger Manuálně:**
```sql
-- Vytvořit test user v Keycloak
INSERT INTO user_entity (id, username, email, realm_id, created_timestamp)
VALUES ('test-123', 'testuser', 'test@example.com', 'master', EXTRACT(EPOCH FROM NOW()) * 1000);

-- Zkontrolovat change_events
SELECT * FROM change_events WHERE new_data::json->>'id' = 'test-123';
```

---

#### Krok 3: Debezium Connector
**Kontrola:**
```bash
# List connectors
curl http://localhost:8083/connectors | jq

# Check status
curl http://localhost:8083/connectors/keycloak-cdc-connector/status | jq
```

**Očekáváme:**
```json
{
  "name": "keycloak-cdc-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "kafka-connect:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING"
    }
  ]
}
```

**Pokud connector NEEXISTUJE:** ❌ **Není vytvořen**

**Fix - Vytvořit Debezium Connector:**
```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "keycloak-cdc-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "db",
      "database.port": "5432",
      "database.user": "keycloak",
      "database.password": "keycloak",
      "database.dbname": "keycloak",
      "database.server.name": "keycloak",
      "table.include.list": "public.change_events",
      "plugin.name": "pgoutput",
      "publication.autocreate.mode": "filtered",
      "tombstones.on.delete": "false",
      "transforms": "route",
      "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
      "transforms.route.regex": "keycloak.public.change_events",
      "transforms.route.replacement": "keycloak.cdc.change_events"
    }
  }'
```

---

#### Krok 4: Kafka Topic
**Kontrola:**
```bash
# List topics
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list | grep keycloak

# Read messages
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic keycloak.cdc.change_events \
  --from-beginning \
  --max-messages 5
```

**Očekáváme:**
```json
{
  "payload": {
    "table_name": "user_entity",
    "operation": "INSERT",
    "new_data": "{\"id\":\"user-123\", \"username\":\"john\", ...}",
    "changed_at": 1697203200000
  }
}
```

**Pokud NIC:** ❌ **Debezium nepublikuje do Kafky**

**Debug:**
```bash
# Kafka Connect logs
docker logs kafka-connect | grep -i "keycloak\|error"

# Connector tasks
curl http://localhost:8083/connectors/keycloak-cdc-connector/tasks/0/status | jq
```

---

#### Krok 5: Backend Consumer
**Soubor:** `backend/src/main/java/cz/muriel/core/cdc/KeycloakChangeEventConsumer.java`

**Kontrola:**
```bash
docker logs backend | grep -i "kafka.*listener\|keycloak.*cdc"
```

**Očekáváme:**
```
✅ Kafka listener started for topic: keycloak.cdc.change_events
✅ Consumer group: keycloak-cdc-consumer-group
```

**Pokud NIC:** ❌ **@KafkaListener nefunguje nebo není enabled**

**Zkontrolovat konfiguraci:**
```bash
docker exec backend cat /app/application.properties | grep kafka
```

**Očekáváme:**
```properties
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.consumer.group-id=keycloak-cdc-consumer-group
spring.kafka.consumer.auto-offset-reset=earliest
```

---

#### Krok 6: User Sync Service
**Soubor:** `backend/src/main/java/cz/muriel/core/cdc/UserSyncService.java`

**Kontrola:**
```bash
docker logs backend | grep -i "user.*sync\|user_directory"
```

**Očekáváme:**
```
✅ Processing user sync: user-123
✅ User synced to user_directory: user-123 (tenant: core-platform)
```

**Pokud exception:** ❌ **Business logic error**

**Debug:**
```java
// Přidat debug logging do UserSyncService
log.debug("🔍 Received change event: table={}, operation={}", event.getTableName(), event.getOperation());
log.debug("🔍 New data: {}", event.getNewData());
log.debug("🔍 Parsed user: username={}, email={}", user.getUsername(), user.getEmail());
```

---

## 🛠️ Doporučené Akce

### Priorita 1: Diagnostika
```bash
# Spustit diagnostický skript
cat > /tmp/cdc_diagnostic.sh << 'EOF'
#!/bin/bash
echo "=== CDC Diagnostic Script ==="

echo -e "\n1. Checking Keycloak Triggers..."
docker exec -it db psql -U keycloak -d keycloak -c "
  SELECT tgname, tgrelid::regclass, tgenabled 
  FROM pg_trigger 
  WHERE tgname LIKE '%change_event%'
"

echo -e "\n2. Checking change_events table..."
docker exec -it db psql -U keycloak -d keycloak -c "
  SELECT COUNT(*) as total, 
         COUNT(*) FILTER (WHERE processed = false) as unprocessed,
         MAX(changed_at) as latest_change
  FROM change_events
"

echo -e "\n3. Checking Debezium Connector..."
curl -s http://localhost:8083/connectors/keycloak-cdc-connector/status | jq '.connector.state, .tasks[0].state'

echo -e "\n4. Checking Kafka Topic..."
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic keycloak.cdc.change_events \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 5000

echo -e "\n5. Checking Backend Consumer Logs..."
docker logs backend 2>&1 | grep -i "kafka.*listener" | tail -5

echo -e "\n6. Checking user_directory..."
docker exec -it db psql -U core -d core -c "
  SELECT COUNT(*) as total_users,
         MAX(synced_at) as latest_sync
  FROM user_directory
"

echo -e "\n=== Diagnostic Complete ==="
EOF

chmod +x /tmp/cdc_diagnostic.sh
/tmp/cdc_diagnostic.sh
```

### Priorita 2: Fixes
Na základě výsledků diagnostiky:

**A. Pokud triggery nefungují:**
```sql
-- Re-apply V2 migration
\c keycloak
\i /path/to/V2__init_keycloak_cdc.sql
```

**B. Pokud Debezium connector chybí:**
```bash
# Create connector (viz výše)
curl -X POST http://localhost:8083/connectors ...
```

**C. Pokud backend nekonsumuje:**
```bash
# Restart backend
docker restart backend

# Check logs
docker logs -f backend | grep -i kafka
```

---

## 📊 Shrnutí Odpovědí

| Otázka | Odpověď | Akce |
|--------|---------|------|
| 1. Tenant přes metamodel? | ❌ NE | ✅ Je to správně |
| 2. V3 → V1 migration? | ✅ ANO | 🔄 Přesunout do V1 |
| 3. Admin tenant Grafana? | ⚠️ Chybí | ✅ Přidat GrafanaInitializationService |
| 4. Per-tenant SA nutné? | ✅ ANO | ✅ Má to smysl |
| 5. CDC sync nefunguje? | 🔴 **KRITICKÉ** | 🔍 Spustit diagnostiku |

---

## 🎯 Další Kroky

1. **IMMEDIATE:** Spustit CDC diagnostiku (viz skript výše)
2. **Přesunout Grafana binding** do V1__init.sql
3. **Vytvořit GrafanaInitializationService** pro auto-provisioning
4. **Opravit CDC** na základě diagnostiky
5. **Testovat end-to-end:** Keycloak user create → user_directory sync

---

**Chceš abych:**
1. Spustil diagnostický skript?
2. Přesunul Grafana tabulku do V1?
3. Vytvořil GrafanaInitializationService?
4. Analyzoval konkrétní CDC problém?
