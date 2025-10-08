# METAMODEL FÁZE 1 – Implementační Checklist

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
- [x] Java schema třídy:
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
- [x] `EditLockService` s auto-expiry janitor
- [x] `EditLockController` (REST API)
- [x] `LockConflictException`

### Security & Tenant
- [x] `TenantContextFilter` – Nastavení `app.tenant_id` v DB session
- [x] `PolicyEngine` interface (již existoval)

### Dokumentace
- [x] README: `METAMODEL_PHASE_1.md`

---

## 🚧 Zbývá implementovat

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

### 2. CRUD REST Controller (HLAVNÍ PRÁCE!)

**Soubor:** `backend/src/main/java/cz/muriel/core/entities/MetamodelCrudController.java`

**Úkol:**
Implementovat generický CRUD controller s:

#### Endpoints:
```java
@RestController
@RequestMapping("/api/entities")
public class MetamodelCrudController {
    
    @GetMapping("/{type}")
    ResponseEntity<List<Map<String, Object>>> list(
        @PathVariable String type,
        @RequestParam Map<String, String> filters,
        @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size,
        Authentication auth
    );
    
    @GetMapping("/{type}/{id}")
    ResponseEntity<Map<String, Object>> getById(
        @PathVariable String type,
        @PathVariable String id,
        Authentication auth
    );
    
    @PostMapping("/{type}")
    ResponseEntity<Map<String, Object>> create(
        @PathVariable String type,
        @RequestBody Map<String, Object> data,
        Authentication auth
    );
    
    @PutMapping("/{type}/{id}")
    ResponseEntity<?> update(
        @PathVariable String type,
        @PathVariable String id,
        @RequestHeader("If-Match") String ifMatch,
        @RequestBody Map<String, Object> data,
        Authentication auth
    );
    
    @DeleteMapping("/{type}/{id}")
    ResponseEntity<Void> delete(
        @PathVariable String type,
        @PathVariable String id,
        Authentication auth
    );
}
```

#### Service Layer:

**Soubor:** `backend/src/main/java/cz/muriel/core/entities/MetamodelCrudService.java`

```java
@Service
@RequiredArgsConstructor
public class MetamodelCrudService {
    private final MetamodelRegistry registry;
    private final PolicyEngine policyEngine;
    private final EntityManager entityManager;
    
    public List<Map<String, Object>> list(String entityType, Map<String, String> filters, 
                                          int page, int size, Authentication auth) {
        // 1. PolicyEngine.check(auth, entityType, "read", null)
        // 2. Get allowed columns from projectColumns()
        // 3. Build CriteriaQuery with filters (= / like / in)
        // 4. Apply pagination
        // 5. Project tylko allowed columns
    }
    
    public Map<String, Object> getById(String entityType, String id, Authentication auth) {
        // 1. Load entity from DB
        // 2. PolicyEngine.check(auth, entityType, "read", entity)
        // 3. Project columns
    }
    
    public Map<String, Object> create(String entityType, Map<String, Object> data, Authentication auth) {
        // 1. PolicyEngine.check(auth, entityType, "create", null)
        // 2. Doplnit tenant_id z JWT
        // 3. Nastavit version=0
        // 4. INSERT
    }
    
    public Map<String, Object> update(String entityType, String id, long expectedVersion,
                                     Map<String, Object> data, Authentication auth) {
        // 1. Load entity
        // 2. PolicyEngine.check(auth, entityType, "update", entity)
        // 3. Check version match → 409 if mismatch
        // 4. UPDATE ... WHERE id=? AND version=?
        // 5. version++
    }
    
    public void delete(String entityType, String id, Authentication auth) {
        // 1. Load entity
        // 2. PolicyEngine.check(auth, entityType, "delete", entity)
        // 3. DELETE
    }
}
```

**Poznámky:**
- Použít JPA `CriteriaBuilder` pro dynamické dotazy
- Filtry: `field=value`, `field__like=%pattern%`, `field__in=val1,val2`
- Sort: `-field` = descending
- Column projection: `SELECT ONLY(allowedColumns)`

---

### 3. UI Capabilities Endpoint

**Soubor:** `backend/src/main/java/cz/muriel/core/controller/UiCapabilitiesController.java`

```java
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UiCapabilitiesController {
    
    private final MetamodelRegistry registry;
    private final PolicyEngine policyEngine;
    
    @GetMapping("/ui-capabilities")
    public ResponseEntity<UiCapabilities> getUiCapabilities(Authentication auth) {
        List<String> menu = new ArrayList<>();
        List<String> features = new ArrayList<>();
        
        for (EntitySchema schema : registry.getAllSchemas().values()) {
            if (schema.getNavigation() != null) {
                for (MenuItemConfig item : schema.getNavigation().getMenu()) {
                    if (item.getRequiredRole() == null || 
                        policyEngine.hasRole(auth, item.getRequiredRole())) {
                        menu.add(item.getId());
                    }
                }
            }
            
            if (schema.getFeatures() != null) {
                for (FeatureConfig feature : schema.getFeatures()) {
                    if (feature.getRequiredRole() == null || 
                        policyEngine.hasRole(auth, feature.getRequiredRole())) {
                        features.add(feature.getId());
                    }
                }
            }
        }
        
        return ResponseEntity.ok()
            .eTag("W/\"" + System.currentTimeMillis() + "\"")
            .body(new UiCapabilities(menu, features));
    }
    
    record UiCapabilities(List<String> menu, List<String> features) {}
}
```

---

### 4. ETag Support

**Soubor:** `backend/src/main/java/cz/muriel/core/web/ETagFilter.java`

```java
@Component
@Order(2)
public class ETagFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // For GET/HEAD requests, set ETag based on version in response
        // For PUT requests, validate If-Match header
        
        if ("PUT".equals(httpRequest.getMethod())) {
            String ifMatch = httpRequest.getHeader("If-Match");
            if (ifMatch == null) {
                httpResponse.sendError(428, "Precondition Required: If-Match header missing");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

**Alternativa:** Implementovat ETag přímo v controlleru (jednodušší).

---

### 5. Exception Handlers

**Soubor:** `backend/src/main/java/cz/muriel/core/web/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(LockConflictException.class)
    public ResponseEntity<?> handleLockConflict(LockConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "lock_conflict",
            "message", ex.getMessage(),
            "existingLock", ex.getExistingLock()
        ));
    }
    
    @ExceptionHandler(VersionMismatchException.class)
    public ResponseEntity<?> handleVersionMismatch(VersionMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "version_mismatch",
            "message", ex.getMessage(),
            "currentVersion", ex.getCurrentVersion(),
            "serverEntity", ex.getServerEntity()
        ));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "access_denied",
            "message", ex.getMessage()
        ));
    }
}
```

---

### 6. Testy

#### Unit testy

**Soubor:** `backend/src/test/java/cz/muriel/core/security/policy/MetamodelPolicyEngineTest.java`

```java
@SpringBootTest
class MetamodelPolicyEngineTest {
    
    @Test
    void testRolePermission() {
        // Test: CORE_ROLE_ADMIN má přístup všude
    }
    
    @Test
    void testTenantIsolation() {
        // Test: User z tenant1 nemá přístup k entitám z tenant2
    }
    
    @Test
    void testColumnProjection() {
        // Test: Email vidí pouze admin a tenant_admin
    }
    
    @Test
    void testSameUserPolicy() {
        // Test: User může editovat vlastní profil
    }
}
```

#### Integrační testy

**Soubor:** `backend/src/test/java/cz/muriel/core/entities/MetamodelCrudIntegrationTest.java`

```java
@SpringBootTest
@Testcontainers
class MetamodelCrudIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
    
    @Test
    void testCrudLifecycle() {
        // 1. CREATE UserProfile
        // 2. GET by ID → kontrola ETag
        // 3. UPDATE s If-Match → success
        // 4. UPDATE se starým If-Match → 409
        // 5. DELETE
    }
    
    @Test
    void testTenantIsolation() {
        // 1. CREATE entity v tenant1
        // 2. LIST z tenant2 → nemá vidět
    }
}
```

---

## Priority

### 🔴 KRITICKÉ (musí fungovat)
1. **PolicyEngine integrace** s MetamodelRegistry
2. **CRUD Service** (list, getById, create, update, delete)
3. **CRUD Controller** s ETag podporou
4. **UI Capabilities endpoint**

### 🟡 DŮLEŽITÉ (mělo by fungovat)
5. Exception handlers (409, 403, 404)
6. Column projection v CRUD
7. Filtry a řazení v list()

### 🟢 NICE TO HAVE
8. Unit testy PolicyEngine
9. Integrační testy CRUD
10. Auditní logování změn

---

## Odhad času

- PolicyEngine: **2 hodiny**
- CRUD Service + Controller: **4 hodiny**
- UI Capabilities: **1 hodina**
- Exception handlers: **1 hodina**
- Testy: **3 hodiny**

**Celkem: ~11 hodin práce**

---

## Poznámky

- **TenantContextFilter** už funguje (nastavuje `app.tenant_id`)
- **RLS policies** jsou v DB (V3 migrace)
- **EditLocks** jsou ready
- **MetamodelRegistry** je ready

Zbývá hlavně **CRUD logika** a **PolicyEngine integrace**.
