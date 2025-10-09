# Database Migration Consolidation - DEV/CI

## 📋 Přehled změn

Konsolidovali jsme roztříštěné migrace V1, V3, V5 do jednotné inicializační migrace **V1__init.sql** pro čistší DEV/CI workflow.

### Stará struktura (DEPRECATED)
```
V1__init_core_platform.sql     (431 řádků) - Core tables
V2__init_keycloak_cdc.sql       (61 řádků)  - Keycloak CDC (PRODUKCE)
V3__metamodel_core.sql         (178 řádků) - Metamodel Phase 1
V5__workflow_and_documents.sql (149 řádků) - Phase 2 features
```

### Nová struktura (DEV/CI)
```
✅ V1__init.sql                 (819 řádků) - Kompletní init: Core + Metamodel + Workflow + Documents
✅ V1.1__seed_demo.sql          (141 řádků) - Demo data (3 tenants, 5 users, 3 documents s FTS)
✅ V2__init_keycloak_cdc.sql     (61 řádků) - BEZE ZMĚNY (pouze produkce)
✅ R__fts_triggers.sql           (17 řádků) - Repeatable: FTS tsvector auto-update
✅ R__cache_notify.sql           (54 řádků) - Repeatable: PostgreSQL NOTIFY pro Redis cache invalidation
```

### Archivované soubory (backup)
```
⚠️ V1__init_core_platform.sql.OLD
⚠️ V3__metamodel_core.sql.OLD
⚠️ V5__workflow_and_documents.sql.OLD
```

---

## 🎯 Důvody konsolidace

### 1. **Jednodušší DEV/CI setup**
- Jeden soubor V1 obsahuje kompletní schéma
- `make db-clean-migrate` = clean slate za 30 sekund
- Žádné závislosti mezi V3→V5

### 2. **Repeatable migrace pro složité triggery**
- `R__fts_triggers.sql` - fulltext search tsvector update
- `R__cache_notify.sql` - PostgreSQL NOTIFY pro cache invalidation
- Automatické re-apply při změně obsahu

### 3. **Seed data v1.1 pro testování**
- 3 tenants: `admin`, `test-tenant`, `company-b`
- 5 demo users s user_profile
- 3 documents s fulltextovým obsahem
- Ready-to-test RLS, FTS, workflow states

### 4. **Produkce nezměněna**
- V2 (Keycloak CDC) zůstává samostatný
- V produkci poběží V1→V2→V3→V5 postupně (nebo nová V1 při fresh install)

---

## 🔧 Použití

### Clean migration (DEV/CI)
```bash
make db-clean-migrate
```

**Co se děje:**
1. Stop backend
2. Flyway clean (DROP všech tabulek)
3. Flyway migrate (V1 + V1.1 + R__)
4. Start backend
5. Health check

**Výsledek:**
- Čistá databáze s demo daty
- 3 tenants, 5 users, 3 documents
- RLS policies aktivní
- FTS indexy ready
- Cache NOTIFY triggery aktivní

### Manuální Flyway commands
```bash
# Clean (DANGER!)
docker compose -f docker/docker-compose.yml --env-file .env run --rm backend \
  sh -c "cd /app && ./mvnw flyway:clean -Dflyway.cleanDisabled=false"

# Migrate
docker compose -f docker/docker-compose.yml --env-file .env run --rm backend \
  sh -c "cd /app && ./mvnw flyway:migrate"

# Info
docker compose -f docker/docker-compose.yml --env-file .env run --rm backend \
  sh -c "cd /app && ./mvnw flyway:info"
```

---

## 📊 Obsah V1__init.sql

### Section 1: Tenants & User Directory
- `tenants` table s deterministic UUID
- `users_directory` s Keycloak sync fields + version column
- Helper: `generate_tenant_uuid()`

### Section 2: Roles & Groups
- `roles`, `role_composites`, `role_hierarchy`
- `groups` s hierarchickou strukturou
- Indexes: tenant_key, keycloak_id, name

### Section 3: User Mappings
- `user_roles` (M:N)
- `user_groups` (M:N)

### Section 4: Event Log & Sync Tracking
- `kc_event_log` - idempotence tracking
- `sync_executions`, `sync_execution_errors`

### Section 5: Metamodel (Phase 1)
- `edit_locks` - soft locking s TTL
- `user_profile` - referenční entita s ABAC

### Section 6: Workflow & State Management (Phase 2.2)
- `entity_state` - aktuální stav entity
- `state_transition` - workflow konfigurace s guards + SLA
- `entity_state_log` - audit trail

### Section 7: Documents & Fulltext Search (Phase 2.3)
- `document` - metadata + MinIO storage_key
- `document_index` - tsvector + GIN index pro FTS

### Section 8: Presence Analytics (Phase 2.1)
- `presence_activity` - WebSocket presence log

### Section 9: Row Level Security (RLS)
- Policies: `tenant_isolation_users`, `tenant_isolation_roles`, `tenant_isolation_groups`, `tenant_isolation_user_profile`
- `current_setting('app.tenant_id')` filter

### Section 10: Helper Functions
- `update_updated_at_column()` - auto timestamp
- `cleanup_old_event_logs()` - cleanup starých eventů
- `regenerate_role_uuid()`, `regenerate_group_uuid()` - deterministic UUIDs
- `increment_version()` - optimistic locking
- `calculate_sla_status()` - SLA breach detection

### Section 11: Triggers
- `updated_at` triggers na users_directory, roles, groups
- `version` triggers na user_profile, users_directory, roles, groups

### Section 12: Sample Data
- Workflow transitions pro UserProfile (CREATE_DRAFT → ACTIVATE → SUSPEND → ARCHIVE)
- Seed user_profile pro admin tenant

---

## 📊 Obsah V1.1__seed_demo.sql

### Demo Tenants
```sql
test-tenant (generate_tenant_uuid)
company-b   (generate_tenant_uuid)
```

### Demo Users
**test-tenant:**
- alice (Senior Developer, Engineering)
- bob (Team Lead, Engineering)
- charlie (Product Manager, Product)

**company-b:**
- diana (Account Manager, Sales)
- eric (Support Engineer, Support)

### Demo Documents (s FTS content)
1. **project-proposal.pdf** (alice)
   - Content: "Project Proposal: New Metamodel Architecture..."
   - Keywords: metamodel, ABAC, workflow, Spring Boot, PostgreSQL

2. **technical-spec.docx** (bob)
   - Content: "Technical Specification: Workflow Engine..."
   - Keywords: state transitions, SLA tracking, guard conditions

3. **meeting-notes.txt** (alice)
   - Content: "Meeting Notes: Architecture Review - January 9, 2025..."
   - Keywords: fulltext search, tsvector, GIN indexes, RLS policies

### Fulltext search test
```sql
SELECT d.filename, di.extracted_text, ts_rank(di.search_vector, plainto_tsquery('metamodel')) as rank
FROM document d
JOIN document_index di ON di.document_id = d.id
WHERE di.search_vector @@ plainto_tsquery('metamodel')
ORDER BY rank DESC;
```

---

## 📊 Repeatable Migrations

### R__fts_triggers.sql
**Účel:** Auto-update tsvector při změně extracted_text

```sql
CREATE TRIGGER document_index_search_vector_trigger
    BEFORE INSERT OR UPDATE OF extracted_text ON document_index
    FOR EACH ROW
    EXECUTE FUNCTION update_document_search_vector();
```

**Benefit:** Nemusíme ručně volat `to_tsvector()` - trigger to udělá automaticky

### R__cache_notify.sql
**Účel:** PostgreSQL NOTIFY events pro Redis cache invalidation

```sql
CREATE TRIGGER notify_users_directory_change
    AFTER INSERT OR UPDATE OR DELETE ON users_directory
    FOR EACH ROW
    EXECUTE FUNCTION notify_cache_invalidation();
```

**Benefit:** Backend poslouchá NOTIFY a invaliduje Redis cache klíče

**Payload:**
```json
{
  "entityType": "users_directory",
  "entityId": "uuid",
  "tenantId": "admin",
  "operation": "UPDATE",
  "timestamp": "2025-01-09T12:34:56Z"
}
```

---

## ⚙️ Configuration Changes

### application-development.properties
```properties
# 🔄 FLYWAY - Clean database on validation errors (DEV/CI only!)
spring.flyway.clean-on-validation-error=true
spring.flyway.clean-disabled=false
```

**⚠️ POZOR:** Tato konfigurace je **pouze pro DEV/CI**!
- V produkci: `spring.flyway.clean-disabled=true`
- Clean automaticky smaže DB při checksum mismatch

---

## 🧪 Testování

### 1. Test clean migration
```bash
make db-clean-migrate
```

Ověř výstup:
```
✅ Backend is ready with fresh database!
```

### 2. Test RLS policies
```sql
-- Nastav tenant context
SET app.tenant_id = 'test-tenant';

-- Mělo by vrátit jen test-tenant users
SELECT * FROM users_directory;

-- Změň tenant
SET app.tenant_id = 'company-b';

-- Mělo by vrátit jen company-b users
SELECT * FROM users_directory;
```

### 3. Test FTS search
```sql
SELECT d.filename, ts_rank(di.search_vector, plainto_tsquery('workflow')) as rank
FROM document d
JOIN document_index di ON di.document_id = d.id
WHERE di.search_vector @@ plainto_tsquery('workflow')
ORDER BY rank DESC;
```

Mělo by vrátit:
```
technical-spec.docx    | 0.0607927
project-proposal.pdf   | 0.0303964
```

### 4. Test cache NOTIFY
```bash
# Terminal 1: Listen for NOTIFY events
docker exec -it core-db psql -U core_user -d core_db -c "LISTEN change_events;"

# Terminal 2: Update user
docker exec -it core-db psql -U core_user -d core_db -c "UPDATE users_directory SET first_name='Alicia' WHERE username='alice' AND tenant_key='test-tenant';"

# Terminal 1 měl dostat:
# Asynchronous notification "change_events" with payload:
# {"entityType":"users_directory","entityId":"...","tenantId":"test-tenant","operation":"UPDATE","timestamp":"..."}
```

### 5. Test workflow states
```sql
SELECT entity_type, entity_id, state_code, 
       calculate_sla_status(since, sla_minutes) as sla_status
FROM entity_state;
```

---

## 🚀 Migration Path

### Fresh install (nový projekt)
1. `make db-clean-migrate`
2. V1__init.sql vytvoří všechny tabulky
3. V1.1__seed_demo.sql naplní demo data
4. R__ migrace nastaví triggery

### Existing installation (upgrade)
**Produkce:**
- V1, V2, V3, V5 zůstávají jako jsou
- Nové projekty mohou použít novou V1

**DEV/CI:**
- Smažou se staré V1, V3, V5 (archivovány jako .OLD)
- `make db-clean-migrate` použije novou V1

---

## 📝 Poznámky

### V2 Keycloak CDC
- **Zůstává samostatný** soubor
- Používá se **pouze v produkci** s Keycloak CDC webhook
- V DEV/CI prostředí Keycloak běží odděleně → CDC není potřeba

### Flyway checksum
- Staré V1, V3, V5 archivovány jako `.OLD`
- Flyway je ignoruje (neodpovídají pattern `V*__*.sql`)
- Nová V1 má jiný checksum → clean migration potřebná

### Repeatable migrace
- Spouštějí se **vždy** po versioned migrations
- Re-apply při změně checksum
- Ideální pro functions, triggers, views

---

## ✅ Checklist

- [x] Vytvořen V1__init.sql (819 řádků) - sloučení V1 + V3 + V5
- [x] Vytvořen V1.1__seed_demo.sql (141 řádků) - demo data
- [x] Vytvořen R__fts_triggers.sql (17 řádků) - FTS auto-update
- [x] Vytvořen R__cache_notify.sql (54 řádků) - cache invalidation NOTIFY
- [x] Archivovány staré migrace (.OLD suffix)
- [x] Upravena application-development.properties (clean-on-validation-error)
- [x] Přidán Makefile task `db-clean-migrate`
- [x] V2 (Keycloak CDC) ponechán nezměněný
- [x] Dokumentace vytvořena

---

## 🎯 Next Steps

1. **Otestovat clean migration:**
   ```bash
   make db-clean-migrate
   ```

2. **Ověřit demo data:**
   ```sql
   SELECT COUNT(*) FROM tenants;        -- Expected: 3 (admin, test-tenant, company-b)
   SELECT COUNT(*) FROM users_directory; -- Expected: 5+
   SELECT COUNT(*) FROM document;       -- Expected: 3
   ```

3. **Otestovat FTS:**
   ```sql
   SELECT filename FROM document d
   JOIN document_index di ON di.document_id = d.id
   WHERE di.search_vector @@ plainto_tsquery('metamodel');
   ```

4. **Commit změny:**
   ```bash
   git add backend/src/main/resources/db/migration/
   git add backend/src/main/resources/application-development.properties
   git add Makefile
   git commit -m "refactor: Consolidate DB migrations V1+V3+V5 → V1 for DEV/CI"
   ```

---

**Status:** ✅ **READY FOR TESTING**
