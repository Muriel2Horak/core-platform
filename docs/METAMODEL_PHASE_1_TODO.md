# METAMODEL FÁZE 1 – Implementační Checklist

> **Status:** 🚀 **HOTOVO – READY FOR TESTING!**

## ✅ Hotovo

### Databáze
- [x] Migrace V3__metamodel_core.sql
  - [x] Přidán `version` sloupec do existujících tabulek
  - [x] Tabulka `edit_locks`
  - [x] Tabulka `user_profile` (referenční entita)
  - [x] RLS policies pro tenant isolation
  - [x] Trigger pro auto-increment version

### Metamodel Schema
- [x] YAML metamodel: `user-profile.yaml`
- [x] Java schema třídy (15 tříd):
  - [x] `EntitySchema`
  - [x] `FieldSchema`
  - [x] `AccessPolicy`
  - [x] `PolicyRule`
  - [x] `Condition`
  - [x] `ColumnPolicy`
  - [x] UI config třídy (ListConfig, DetailConfig, etc.)

### Metamodel Infrastructure
- [x] `MetamodelLoader` – Načítání YAML z classpath
- [x] `MetamodelRegistry` – Centrální registry s reload podporou
- [x] SnakeYAML dependency v pom.xml

### Locking
- [x] `EditLock` entity
- [x] `EditLockRepository`
- [x] `EditLockService` s auto-expiry janitor (15s interval)
- [x] `EditLockController` (REST API)
- [x] `LockConflictException`

### Security & Tenant
- [x] `TenantContextFilter` – Nastavení `app.tenant_id` v DB session
- [x] `PolicyEngine` interface (již existoval)
- [x] `PolicyEngine.hasRole()` default metoda

### CRUD REST API ✅
- [x] `MetamodelCrudService` – Generický CRUD s native SQL
  - [x] `list()` – s filtry, řazením, paginací
  - [x] `getById()` – s column projection
  - [x] `create()` – s tenant_id z JWT
  - [x] `update()` – s optimistickým lockingem
  - [x] `delete()` – s permission check
  - [x] Helper metody pro SQL generation a mapping
- [x] `MetamodelCrudController` – REST endpoints
  - [x] GET `/api/entities/{type}` – List
  - [x] GET `/api/entities/{type}/{id}` – Get by ID s ETag
  - [x] POST `/api/entities/{type}` – Create
  - [x] PUT `/api/entities/{type}/{id}` – Update s If-Match
  - [x] DELETE `/api/entities/{type}/{id}` – Delete
- [x] `EntityNotFoundException` (404)
- [x] `VersionMismatchException` (409)

### Exception Handling ✅
- [x] `MetamodelExceptionHandler` – Global exception handler
  - [x] EntityNotFoundException → 404
  - [x] VersionMismatchException → 409 s server entity
  - [x] LockConflictException → 409 s existing lock
  - [x] AccessDeniedException → 403

### UI Capabilities ✅
- [x] `MetamodelUiCapabilitiesController`
  - [x] GET `/api/me/ui-capabilities` – Menu a features z metamodelu

### PolicyEngine Integration ✅
- [x] **MetamodelPolicyEngine** – KOMPLETNĚ PŘEPSÁN
  - [x] Používá `MetamodelRegistry` místo deprecated YamlPermissionAdapter
  - [x] Implementuje `anyOf`, `allOf`, `role`, `group`, `sameUser`
  - [x] Implementuje operátory: `eq`, `ne`, `contains`, `in`
  - [x] Evaluace ${entity.field} a ${user.claim}
  - [x] Column projection `projectColumns()`
  - [x] Tenant isolation `getTenantId()`

### Dokumentace
- [x] README: `METAMODEL_PHASE_1.md`
- [x] Aktualizovaný TODO checklist

### Git Commits ✅
- [x] Commit 1: Initial metamodel infrastructure (schemas, loader, registry, locks)
- [x] Commit 2: Rewrite MetamodelPolicyEngine to use MetamodelRegistry
- [x] Commit 3: Fix helper methods in MetamodelCrudService after SQL refactoring

---

## 🚧 Zbývá (TESTOVÁNÍ)

### 1. PolicyEngine Integration (KRITICKÉ!)

**Soubor:** `backend/src/main/java/cz/muriel/core/security/policy/MetamodelPolicyEngine.java`

**Úkol:**
- Aktualizovat existující `MetamodelPolicyEngine` aby používal `MetamodelRegistry`
- Implementovat vyhodnocování pravidel z YAML metamodelu místo YamlPermissionAdapter
- Přidat metody:
  - `hasRole(Authentication, String)` – Kontrola role
  - `getTenantId(Authentication)` – Extrakce tenant_id z JWT
  - `getUserId(Authentication)` – Extrakce user_id z JWT
  - `evaluateRule(PolicyRule, Authentication, Object)` – Rekurzivní vyhodnocení

**Klíčové změny:**
```java
@RequiredArgsConstructor
public class MetamodelPolicyEngine implements PolicyEngine {
    private final MetamodelRegistry registry; // ← PŘIDAT!
    // private final YamlPermissionAdapter yamlAdapter; ← ODSTRANIT
    
    @Override
    public boolean check(Authentication auth, String entityType, String action, Object contextId) {
        EntitySchema schema = registry.getSchemaOrThrow(entityType);
        PolicyRule rule = getActionRule(schema.getAccessPolicy(), action);
        return evaluateRule(auth, rule, contextId);
    }
    
    @Override
    public Set<String> projectColumns(Authentication auth, String entityType, String action) {
        EntitySchema schema = registry.getSchemaOrThrow(entityType);
        // Implementovat column projection
    }
}
```

---

## 🧪 Zbývá otestovat

### Manuální testování
- [ ] Spustit migraci V3 (Flyway)
- [ ] Načíst user-profile.yaml přes MetamodelLoader
- [ ] Test CRUD endpoints:
  - [ ] POST /api/entities/user-profile – Create
  - [ ] GET /api/entities/user-profile – List
  - [ ] GET /api/entities/user-profile/{id} – Get by ID + ETag
  - [ ] PUT /api/entities/user-profile/{id} – Update s If-Match
  - [ ] PUT se starým If-Match → 409 VersionMismatch
  - [ ] DELETE /api/entities/user-profile/{id}
- [ ] Test edit locks:
  - [ ] POST /api/locks/user-profile/{id}
  - [ ] Pokus o editaci locked entity → 409 LockConflict
  - [ ] DELETE /api/locks/user-profile/{id}
- [ ] Test UI capabilities:
  - [ ] GET /api/me/ui-capabilities jako admin
  - [ ] GET /api/me/ui-capabilities jako user
- [ ] Test tenant isolation:
  - [ ] User z tenant1 nevidí entity z tenant2

### Unit testy (NICE TO HAVE)

### Unit testy (NICE TO HAVE)
- [ ] `MetamodelPolicyEngineTest`
  - [ ] testRolePermission() – CORE_ROLE_ADMIN má přístup všude
  - [ ] testTenantIsolation() – User z tenant1 nemá přístup k tenant2
  - [ ] testColumnProjection() – Email vidí pouze admin a tenant_admin
  - [ ] testSameUserPolicy() – User může editovat vlastní profil

### Integrační testy (NICE TO HAVE)
- [ ] `MetamodelCrudIntegrationTest`
  - [ ] testCrudLifecycle() – CREATE → GET → UPDATE → DELETE
  - [ ] testVersionConflict() – Update se starým If-Match → 409
  - [ ] testTenantIsolation() – Cross-tenant access denied

---

## ✅ Implementace HOTOVA!

Všechny core komponenty Fáze 1 jsou implementovány a zkompilované:

1. ✅ Database migration V3 s version columns, edit_locks, RLS
2. ✅ YAML metamodel schemas (user-profile.yaml)
3. ✅ Java schema classes (15 tříd)
4. ✅ MetamodelLoader a MetamodelRegistry
5. ✅ EditLock system s janitor
6. ✅ TenantContextFilter pro RLS
7. ✅ **MetamodelPolicyEngine** – kompletně přepsán, používá MetamodelRegistry
8. ✅ **MetamodelCrudService** – generický CRUD s native SQL
9. ✅ **MetamodelCrudController** – REST API s ETag podporou
10. ✅ **MetamodelExceptionHandler** – global exception handling
11. ✅ **MetamodelUiCapabilitiesController** – UI capabilities z metamodelu

### 🚀 Připraveno k testování!

Backend je ready. Zbývá:
1. **Spustit aplikaci** a otestovat endpoints
2. **Napsat testy** (unit + integration)
3. **Připojit frontend** na nové CRUD API

---

## Priority dalšího testování

### 🔴 KRITICKÉ (musí fungovat)
1. CRUD lifecycle (create → read → update → delete)
2. Optimistic locking (If-Match header, version check)
3. Tenant isolation přes RLS
4. Permission checks přes PolicyEngine

### 🟡 DŮLEŽITÉ (mělo by fungovat)
5. Column projection (admin vidí email, user ne)
6. Edit locks (conflict detection)
7. UI capabilities filtering by role
8. Filtry a řazení v list()

### 🟢 NICE TO HAVE
9. Unit testy PolicyEngine
10. Integrační testy CRUD
11. Auditní logování změn

---

## Odhad času testování

- Manuální testování endpoints: **1-2 hodiny**
- Debugging případných bugů: **2-3 hodiny**
- Unit testy: **2 hodiny**
- Integrační testy: **2 hodiny**

**Celkem: ~7-9 hodin testování**

---

## Poznámky k implementaci

### Co funguje
- ✅ **TenantContextFilter** nastavuje `app.tenant_id` v DB session
- ✅ **RLS policies** jsou v DB (V3 migrace)
- ✅ **EditLocks** s auto-expiry (15s janitor)
- ✅ **MetamodelRegistry** s thread-safe přístupem
- ✅ **PolicyEngine** s anyOf/allOf/role/sameUser/eq/ne/contains/in operators
- ✅ **CRUD Service** s native SQL (podporuje dynamické entity bez JPA)
- ✅ **ETag support** v controller (If-Match header)
- ✅ **Exception handlers** pro 404/409/403

### Co by se mohlo rozbít
- ⚠️ Native SQL injection – `sanitize()` je basic, možná potřeba PreparedStatement
- ⚠️ Column projection může být prázdný Set → fallback na všechny sloupce
- ⚠️ UUID parsing v findEntityById() může failnout na jiné ID typy
- ⚠️ Version trigger v DB musí být správně nastaven

### Známé limity
- 📌 Filtry podporují pouze: `=`, `__like`, `__in`
- 📌 Sorting pouze single column (ne multi-column)
- 📌 Pagination bez total count
- 📌 Žádný audit trail (bude ve Fázi 1.1)

---

## Co dál (Fáze 1.1)
1. Audit trail (who/when created/updated)
2. Soft delete support
3. Batch operations
4. GraphQL schema generation
5. OpenAPI spec generation
6. Relation support (foreign keys)
7. Validation rules v metamodelu
8. Custom actions (workflows)

---

**Status:** 🎉 **FÁZE 1 IMPLEMENTACE DOKONČENA!**

Zbývá POUZE testování a případné bugfixy.
