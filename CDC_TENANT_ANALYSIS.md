# 🔍 ANALÝZA: CDC Pipeline & Tenant Management

**Datum:** 2024-10-13  
**Autor:** GitHub Copilot + Martin Horák  
**Status:** 🔴 Problémy identifikovány

---

## 📋 ODPOVĚDI NA OTÁZKY

### 1️⃣ **Tenant Management - Metamodel Integration**

#### ❌ ZJIŠTĚNÍ: Tenant NENÍ přes Metamodel

**Současný stav:**
```java
// TenantManagementController.java
@PostMapping
public ResponseEntity<Map<String, Object>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
    keycloakRealmManagementService.createTenant(request.getKey(), request.getDisplayName());
    // Přímá API logika, NE metamodel
}
```

**Flow:**
```
POST /api/admin/tenants
  ↓
TenantManagementController.createTenant()
  ↓
KeycloakRealmManagementService.createTenant()
  ↓
1. Load template
2. Create Keycloak realm
3. TenantService.createTenantRegistryWithRealmId() ← Direct JPA
4. GrafanaProvisioningService.provisionTenant() ← Direct service call
```

**Důsledky:**
- ❌ Tenant operace NEJSOU trackovány v Kafka
- ❌ Tenant změny NEJSOU dostupné pro streaming
- ❌ Tenant CRUD NEMÁ lifecycle eventy
- ❌ Žádná integrace s metamodel workflow

**Řešení:**
- ⚠️ **NE pro tenanty** - metamodel je pro business entity (users, documents, atd.)
- ✅ **Správné:** Tenant je infrastrukturní entita, direct API je OK
- 💡 **Alternativa:** Vytvořit `TenantEntity` metamodel pokud chceme tracking

---

### 2️⃣ **Flyway Migration V3 vs V1**

#### ✅ DOPORUČENÍ: Přesunout do V1__init.sql

**Aktuální stav:**
- V1__init.sql - hlavní schema (users, tenants, atd.)
- V2__init_keycloak_cdc.sql - change_events table
- **V3__grafana_tenant_bindings.sql** - nová tabulka

**Proč přesunout do V1:**
```sql
-- V1__init.sql je 36KB, obsahuje všechny core tabulky
-- V3 je jenom 1KB (grafana_tenant_bindings)
-- Když rebuilduješ prostředí od začátku, V3 stejně poběží
```

**Akce:**
1. Smazat V3__grafana_tenant_bindings.sql
2. Přidat do V1__init.sql na konec:

```sql
-- ====== GRAFANA TENANT BINDINGS (MONITORING) ======

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

COMMENT ON TABLE grafana_tenant_bindings IS 'Mapping between tenants and Grafana organizations with service account tokens for monitoring';
```

---

### 3️⃣ **Admin Tenant - Service Account při Bootstrap**

#### 🤔 PROBLÉM: Kdy provisionovat admin tenant?

**Současný stav:**
```java
// KeycloakInitializationService.java - Bootstrap
@PostConstruct
public void init() {
    if (firstRun) {
        initializeMasterRealmClient();
        createAdminRealm();
        tenantService.createTenantRegistry("admin"); // ← Admin tenant
        // ❌ ALE: GrafanaProvisioningService.provisionTenant() se NEVOLÁ!
    }
}
```

**Proč provisioning NEVOLÁ:**
- Admin tenant se vytváří v `@PostConstruct` (bootstrap)
- `KeycloakRealmManagementService.createTenant()` se používá jen pro nové tenanty přes API
- Bootstrap používá přímý `TenantService.createTenantRegistry()`

**Důsledky:**
- ❌ Admin tenant NEMÁ Grafana org/SA/token
- ❌ Dashboard pro admin tenant bude mít spinner
- ❌ Manual provisioning nutný

**Řešení - Varianta A: Auto-provision při bootstrap**
```java
// KeycloakInitializationService.java
@PostConstruct
public void init() {
    if (firstRun) {
        initializeMasterRealmClient();
        createAdminRealm();
        
        // Create tenant registry
        tenantService.createTenantRegistry("admin");
        
        // 🚀 AUTO-PROVISION GRAFANA
        try {
            grafanaProvisioningService.provisionTenant("admin");
            log.info("✅ Grafana provisioned for admin tenant");
        } catch (Exception e) {
            log.error("⚠️ Failed to provision Grafana for admin tenant (manual setup required)", e);
        }
    }
}
```

**Řešení - Varianta B: Manual provision při prvním spuštění**
```bash
# Po build prostředí zavolat:
curl -X POST http://localhost:8080/api/admin/grafana/provision/admin \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### 4️⃣ **Service Account Per Tenant - Je To Nutné?**

#### ✅ ANO, Je To Správný Design

**Důvody:**

**1. Security Isolation:**
```
Tenant A → SA Token A → Org 1 → Only Tenant A logs
Tenant B → SA Token B → Org 2 → Only Tenant B logs
```
- ✅ Token leak v Tenant A neohrozí Tenant B
- ✅ Každý tenant má vlastní scope/permissions

**2. Monitoring Separation:**
```
Grafana Org 1 (Tenant A):
  - Dashboards: Tenant A specific
  - Data Sources: Filtered by tenant_id=A
  - Alerts: Tenant A only

Grafana Org 2 (Tenant B):
  - Dashboards: Tenant B specific
  - Data Sources: Filtered by tenant_id=B
  - Alerts: Tenant B only
```

**3. Multi-tenancy Best Practices:**
- ✅ **SPRÁVNÉ:** Každý tenant = vlastní org + SA
- ❌ **ŠPATNÉ:** Shared SA pro všechny tenanty (security risk)

**Alternativní Architektury:**

**Option 1: Single SA with Org Switching (NEDOPORUČENO)**
```java
// Teoreticky možné, ale složité
grafanaClient.switchOrg(tenantOrgId);
grafanaClient.query(datasource, query);
// ⚠️ Race conditions, složitý error handling
```

**Option 2: Master Token per Tenant Org (MOŽNÉ)**
```
Tenant A → Master Token → API call with X-Grafana-Org-Id: 1
Tenant B → Master Token → API call with X-Grafana-Org-Id: 2
```
- ⚠️ Master token má příliš velké permissions
- ⚠️ Token leak = přístup ke všem tenantům

**Doporučení:** ✅ **Ponechat 1 SA per tenant** (současný design je správný)

---

### 5️⃣ **CDC Pipeline - User Directory Synchronizace Nefunguje**

#### 🔴 KRITICKÝ PROBLÉM: Data netečou z Keycloaku do User Directory

**Symptoms:**
- ✅ Změny v Keycloak admin console proběhnou
- ❌ UserDirectory tabulka zůstává prázdná/nezměněná
- ❌ Žádné chyby v backend logu

**CDC Pipeline Flow (Teoretický):**
```
Keycloak DB Change
  ↓
Postgres Trigger (INSERT INTO change_events)
  ↓
ChangeEventPollingService.pollAndProcessEvents() (@Scheduled)
  ↓
KeycloakEventProjectionService.processCdcEvent()
  ↓
KeycloakSyncService.syncUserFromKeycloak()
  ↓
MetamodelCrudService.createOrUpdate("User", userData)
  ↓
user_directory table updated
```

#### 🔍 DIAGNOSTIC CHECKLIST

**Step 1: Verify Trigger Exists**
```sql
-- Zkontrolovat triggery v Keycloak DB
SELECT trigger_name, event_manipulation, event_object_table 
FROM information_schema.triggers 
WHERE trigger_schema = 'public' 
  AND event_object_table IN ('user_entity', 'user_role_mapping', 'user_group_membership');
```

**Expected Output:**
```
trigger_name              | event_manipulation | event_object_table
--------------------------+--------------------+---------------------
user_entity_cdc_trigger   | INSERT             | user_entity
user_entity_cdc_trigger   | UPDATE             | user_entity
user_entity_cdc_trigger   | DELETE             | user_entity
role_mapping_cdc_trigger  | INSERT             | user_role_mapping
...
```

**Step 2: Verify Change Events Are Being Written**
```sql
-- V Keycloak DB
SELECT COUNT(*) FROM change_events;
SELECT * FROM change_events ORDER BY timestamp DESC LIMIT 10;
```

**Expected:** Měly by tam být řádky po každé změně v Keycloaku

**Step 3: Verify Polling Service Is Running**
```bash
docker logs backend 2>&1 | grep -i "Change Event Polling"
```

**Expected Output:**
```
🔄 Change Event Polling Service initialized
   - Batch size: 100
   - Flush interval: 10 seconds
   - Listener enabled: true
📨 Found 5 unprocessed change events, processing...
```

**Step 4: Check Configuration**
```properties
# application.properties
app.change-events.listener-enabled=true   # ← MUST BE TRUE
app.change-events.batch-size=100
app.change-events.flush-interval-seconds=10
```

**Step 5: Verify Keycloak DataSource Connection**
```java
// KeycloakDataSourceConfig.java
@Bean(name = "keycloakDataSource")
public DataSource keycloakDataSource() {
    // url=jdbc:postgresql://db:5432/keycloak
    // username=keycloak
    // password=keycloak
}
```

**Step 6: Check Realm ID Mapping**
```bash
docker logs backend 2>&1 | grep "realm_id"
```

**Expected:** Mělo by být vidět mapování `realm_id → tenant_key`

---

## 🐛 PRAVDĚPODOBNÉ PŘÍČINY PROBLÉMU

### Příčina #1: Triggery neexistují v Keycloak DB
```sql
-- V2__init_keycloak_cdc.sql vytvořil change_events table
-- ALE: Triggery na user_entity, user_role_mapping CHYBÍ!
```

**Fix:**
```sql
-- Přidat do V2__init_keycloak_cdc.sql

-- Trigger function
CREATE OR REPLACE FUNCTION notify_change_event()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO change_events (
        event_type,
        entity_id,
        realm_id,
        timestamp,
        old_data,
        new_data,
        processed
    ) VALUES (
        TG_OP, -- INSERT/UPDATE/DELETE
        COALESCE(NEW.id, OLD.id),
        COALESCE(NEW.realm_id, OLD.realm_id),
        NOW(),
        to_jsonb(OLD),
        to_jsonb(NEW),
        false
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers na Keycloak tables
CREATE TRIGGER user_entity_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_entity
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER user_role_mapping_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_role_mapping
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER user_group_membership_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_group_membership
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER keycloak_role_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON keycloak_role
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER keycloak_group_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON keycloak_group
FOR EACH ROW EXECUTE FUNCTION notify_change_event();
```

### Příčina #2: Polling Service Disabled
```properties
# .env nebo application.properties
app.change-events.listener-enabled=false  # ← PROBLÉM!
```

**Fix:**
```bash
# .env
APP_CHANGE_EVENTS_LISTENER_ENABLED=true
```

### Příčina #3: Keycloak DataSource Není Správně Nakonfigurován
```bash
# Zkontrolovat credentials
grep KEYCLOAK_DB .env

# MUSÍ být:
KEYCLOAK_DB_USERNAME=keycloak
KEYCLOAK_DB_PASSWORD=keycloak
```

### Příčina #4: Realm ID Mapping Selhává
```java
// ChangeEventPollingService.java
private String mapRealmIdToTenantKey(String realmId) {
    // ❌ Pokud realmId neodpovídá žádnému tenantu, vrátí null
    // ❌ Event se označí jako processed ale nic se neudělá
}
```

**Debug:**
```sql
-- Zjistit realm_id z Keycloaku
SELECT id, name FROM realm;

-- Porovnat s tenants table v core DB
SELECT id, key, keycloak_realm_id FROM tenants;
```

---

## 🔧 ACTION PLAN - FIX CDC SYNCHRONIZATION

### Step 1: Verify Database Triggers
```bash
docker exec -it db psql -U keycloak -d keycloak -c "
SELECT 
    t.trigger_name, 
    t.event_manipulation, 
    t.event_object_table,
    t.action_statement
FROM information_schema.triggers t
WHERE t.trigger_schema = 'public'
  AND t.event_object_table IN ('user_entity', 'user_role_mapping', 'keycloak_role', 'keycloak_group')
ORDER BY t.event_object_table, t.trigger_name;
"
```

**Expected:** Měly by být triggery (pokud NE → pokračuj Step 2)

### Step 2: Add Missing Triggers
```bash
# Vytvořit SQL soubor s triggery
cat > /tmp/add_cdc_triggers.sql << 'EOF'
-- Trigger function for change_events
CREATE OR REPLACE FUNCTION notify_change_event()
RETURNS TRIGGER AS $$
DECLARE
    v_event_type TEXT;
BEGIN
    -- Map trigger operation to event type
    v_event_type := CASE TG_TABLE_NAME
        WHEN 'user_entity' THEN 
            CASE TG_OP
                WHEN 'INSERT' THEN 'USER_CREATED'
                WHEN 'UPDATE' THEN 'USER_UPDATED'
                WHEN 'DELETE' THEN 'USER_DELETED'
            END
        WHEN 'user_role_mapping' THEN 'USER_ROLE_MAPPING_CHANGED'
        WHEN 'user_group_membership' THEN 'USER_GROUP_MEMBERSHIP_CHANGED'
        WHEN 'keycloak_role' THEN
            CASE TG_OP
                WHEN 'INSERT' THEN 'ROLE_CREATED'
                WHEN 'UPDATE' THEN 'ROLE_UPDATED'
                WHEN 'DELETE' THEN 'ROLE_DELETED'
            END
        WHEN 'keycloak_group' THEN
            CASE TG_OP
                WHEN 'INSERT' THEN 'GROUP_CREATED'
                WHEN 'UPDATE' THEN 'GROUP_UPDATED'
                WHEN 'DELETE' THEN 'GROUP_DELETED'
            END
    END;

    INSERT INTO change_events (
        event_type,
        entity_id,
        realm_id,
        timestamp,
        old_data,
        new_data,
        processed
    ) VALUES (
        v_event_type,
        COALESCE(NEW.id, OLD.id),
        COALESCE(NEW.realm_id, OLD.realm_id),
        NOW(),
        CASE WHEN TG_OP != 'INSERT' THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP != 'DELETE' THEN to_jsonb(NEW) ELSE NULL END,
        false
    );

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Drop existing triggers if any
DROP TRIGGER IF EXISTS user_entity_cdc_trigger ON user_entity;
DROP TRIGGER IF EXISTS user_role_mapping_cdc_trigger ON user_role_mapping;
DROP TRIGGER IF EXISTS user_group_membership_cdc_trigger ON user_group_membership;
DROP TRIGGER IF EXISTS keycloak_role_cdc_trigger ON keycloak_role;
DROP TRIGGER IF EXISTS keycloak_group_cdc_trigger ON keycloak_group;

-- Create triggers
CREATE TRIGGER user_entity_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_entity
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER user_role_mapping_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_role_mapping
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER user_group_membership_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_group_membership
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER keycloak_role_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON keycloak_role
FOR EACH ROW EXECUTE FUNCTION notify_change_event();

CREATE TRIGGER keycloak_group_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON keycloak_group
FOR EACH ROW EXECUTE FUNCTION notify_change_event();
EOF

# Aplikovat do Keycloak DB
docker exec -i db psql -U keycloak -d keycloak < /tmp/add_cdc_triggers.sql
```

### Step 3: Verify Polling Service Config
```bash
# Zkontrolovat application.properties
grep -E "change-events|listener-enabled" backend/src/main/resources/application.properties

# Mělo by být:
# app.change-events.listener-enabled=true (nebo chybí = default true)
```

### Step 4: Test CDC Flow
```bash
# 1. Restart backend (aby načetl config)
docker restart backend

# 2. Sledovat logy
docker logs -f backend | grep -E "Change Event|CDC|polling"

# 3. Udělat změnu v Keycloak (přes admin console)
#    - Vytvořit nového usera
#    - Nebo změnit existujícího

# 4. Mělo by se objevit:
# 📨 Found X unprocessed change events, processing...
# 🔄 Processing CDC event: type=USER_CREATED, entity=xxx, tenant=admin
# ✅ CDC event processed
```

### Step 5: Verify Data in User Directory
```bash
docker exec -it db psql -U core -d core -c "SELECT username, email, first_name, last_name, tenant_id FROM user_directory ORDER BY created_at DESC LIMIT 10;"
```

**Expected:** Měli byste vidět uživatele, kteří byli vytvořeni/upraveni v Keycloaku

---

## 📊 DEBUGGING QUERIES

```sql
-- 1. Check change_events table
SELECT COUNT(*) as total,
       SUM(CASE WHEN processed THEN 1 ELSE 0 END) as processed,
       SUM(CASE WHEN NOT processed THEN 1 ELSE 0 END) as pending
FROM change_events;

-- 2. Recent unprocessed events
SELECT id, event_type, entity_id, realm_id, timestamp, processed
FROM change_events
WHERE NOT processed
ORDER BY timestamp DESC
LIMIT 10;

-- 3. Recent processed events
SELECT id, event_type, entity_id, realm_id, timestamp
FROM change_events
WHERE processed
ORDER BY processed_at DESC
LIMIT 10;

-- 4. Tenant to realm mapping
SELECT t.key as tenant_key, t.keycloak_realm_id, r.name as realm_name
FROM tenants t
LEFT JOIN realm r ON t.keycloak_realm_id = r.id;

-- 5. User directory status
SELECT tenant_id, COUNT(*) as user_count
FROM user_directory
GROUP BY tenant_id;
```

---

## ✅ CHECKLIST - Po Opravu

- [ ] Triggery existují v Keycloak DB
- [ ] `change_events` table obsahuje nové záznamy po změně v Keycloaku
- [ ] Polling service je enabled (`app.change-events.listener-enabled=true`)
- [ ] Backend logy ukazují "Found X unprocessed events"
- [ ] Backend logy ukazují "CDC event processed"
- [ ] `user_directory` table obsahuje data po synchronizaci
- [ ] Realm ID → tenant_key mapping funguje
- [ ] Žádné error zprávy v backend logu

---

## 📝 SOUHRNNÉ ZÁVĚRY

1. **Tenant Management:** 
   - ✅ NE přes metamodel (správně)
   - ✅ Direct API je vhodný přístup
   
2. **Grafana Bindings Migration:**
   - ✅ Přesunout do V1__init.sql
   - ⏳ Akce: Manual merge do V1
   
3. **Admin Tenant Provisioning:**
   - ❌ Chybí auto-provisioning při bootstrap
   - 🔧 Fix: Přidat `grafanaProvisioningService.provisionTenant("admin")` do `KeycloakInitializationService.init()`
   
4. **Service Account Per Tenant:**
   - ✅ ANO, je to správné (security + isolation)
   - ✅ Ponechat současný design
   
5. **CDC Pipeline:**
   - 🔴 KRITICKÝ: Pravděpodobně chybí triggery v Keycloak DB
   - 🔧 Fix: Aplikovat CDC triggery (viz Step 2)
   - 🔧 Verify: Polling service config + realm ID mapping

---

**Next Steps:**
1. Ověřit triggery v Keycloak DB
2. Přidat chybějící triggery
3. Testovat CDC flow
4. Opravit admin tenant provisioning
5. Přesunout V3 do V1
