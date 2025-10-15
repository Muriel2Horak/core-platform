# TODO & Warnings Cleanup Plan

**Datum:** 15. října 2025  
**Účel:** Vyřešit všechny TODO komentáře a Java warnings v kódu

---

## 📊 Přehled problémů

| Kategorie | Počet | Priorita | Časová náročnost |
|-----------|-------|----------|------------------|
| **Type Safety Warnings** | 4 | 🔴 HIGH | ~30 min |
| **Unnecessary @SuppressWarnings** | 4 | 🟡 MEDIUM | ~15 min |
| **Missing Security Context** | 1 | 🔴 HIGH | ~1 hour |
| **Missing RBAC Integration** | 1 | 🔴 HIGH | ~2 hours |
| **Future Features (Kafka)** | 1 | 🟢 LOW | N/A (future) |
| **Disabled Tests** | 2 | 🟡 MEDIUM | ~3-4 hours |
| **Missing Strict Reads Check** | 1 | 🟡 MEDIUM | ~30 min |

**Celkem:** 14 položek  
**Odhadovaný čas:** ~8-10 hodin

---

## 🔴 PRIORITA 1: Type Safety & Warnings (KRITICKÉ)

### 1.1 Type Safety Warnings v `WorkflowRuntimeServiceTest.java`

**Problém:** Raw type usage v Mockito `thenReturn()`

**Lokace:**
- Řádek 172
- Řádek 217
- Řádek 244-254
- Řádek 301-304

**Současný kód:**
```java
@SuppressWarnings({ "unchecked", "rawtypes" })
var slaQueryStub = when(
    jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
        anyString(), anyString(), anyString()));
slaQueryStub.thenReturn(List.of(30)); // ❌ Raw type warning
```

**Řešení:**
```java
// Použít typed stub
when(jdbcTemplate.query(
    anyString(), 
    any(RowMapper.class),
    anyString(), anyString(), anyString()
)).thenReturn(List.of(30));
```

**Akce:**
- [ ] Odstranit všechny `@SuppressWarnings({ "unchecked", "rawtypes" })`
- [ ] Přepsat všechny 4 instance na typed stubs
- [ ] Ověřit, že testy stále projdou

**Časová náročnost:** ~30 minut  
**Risk:** LOW (pouze testovací kód)

---

## 🔴 PRIORITA 2: Security Context Integration (KRITICKÉ)

### 2.1 Missing Tenant from Security Context

**Problém:** `AiContextController.java` používá placeholder UUID místo skutečného tenant ID ze security contextu

**Lokace:** Řádek 58-60

**Současný kód:**
```java
if (tenantId == null) {
  // TODO: Get from security context
  tenantId = UUID.randomUUID(); // ❌ Placeholder - NEBEZPEČNÉ!
}
```

**Řešení:**
```java
if (tenantId == null) {
  // Get from JWT claims or security context
  Authentication auth = SecurityContextHolder.getContext().getAuthentication();
  if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
    tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
  } else {
    throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED, 
      "Tenant ID required - not found in security context"
    );
  }
}
```

**Akce:**
- [ ] Implementovat `TenantContextHolder` helper class (podobně jako Spring SecurityContextHolder)
- [ ] Extrahovat tenant ID z JWT claims
- [ ] Přidat validaci tenant existence
- [ ] Přidat unit testy pro různé scénáře
- [ ] Aktualizovat dokumentaci API

**Časová náročnost:** ~1 hodina  
**Risk:** HIGH (bezpečnostní díra - může přistupovat k cizím tenant datům!)

---

### 2.2 Missing RBAC Integration

**Problém:** `McpController.java` vrací stub capabilities místo skutečných RBAC oprávnění

**Lokace:** Řádek 132

**Současný kód:**
```java
// TODO: Implement actual RBAC integration
// For now, return stub capabilities
Map<String, Object> capabilities = Map.of(
  "canView", true, 
  "canEdit", false, 
  "canExecute", Collections.emptyList(), 
  "note", "RBAC integration pending"
);
```

**Řešení:**
```java
// Get actual capabilities from RBAC service
RbacCapabilities capabilities = rbacService.getUserCapabilities(
  auth.getPrincipal(), 
  resourceType, 
  resourceId
);

return ResponseEntity.ok(Map.of(
  "canView", capabilities.hasPermission("view"),
  "canEdit", capabilities.hasPermission("edit"),
  "canExecute", capabilities.getExecutableActions(),
  "note", "Capabilities from RBAC system"
));
```

**Akce:**
- [ ] Vytvořit `RbacCapabilitiesService`
- [ ] Integrovat s existujícím RBAC systémem
- [ ] Implementovat permission checks
- [ ] Přidat caching (Redis) pro výkon
- [ ] Přidat unit a integration testy

**Časová náročnost:** ~2 hodiny  
**Risk:** HIGH (bezpečnost - nesprávná autorizace)

---

## 🟡 PRIORITA 3: Missing Implementation (STŘEDNÍ)

### 3.1 Strict Reads Check

**Problém:** Chybí kontrola, zda entita není uzamčena během úprav

**Lokace:** `AiContextController.java` řádek 62

**Současný kód:**
```java
// TODO: Implement strict reads check
// If strict=true and entity is UPDATING, return 423 Locked
```

**Řešení:**
```java
if (Boolean.TRUE.equals(strict)) {
  // Check entity lock status
  EntityLockStatus lockStatus = entityLockService.getLockStatus(
    tenantId, 
    entityType, 
    entityId
  );
  
  if (lockStatus.isLocked()) {
    throw new ResponseStatusException(
      HttpStatus.LOCKED, // 423
      "Entity is currently being updated by " + lockStatus.getLockedBy()
    );
  }
}
```

**Akce:**
- [ ] Vytvořit `EntityLockService`
- [ ] Implementovat lock tracking (Redis nebo DB)
- [ ] Přidat TTL pro automatické uvolnění locks
- [ ] Přidat endpoint pro unlock (admin)
- [ ] Unit a integration testy

**Časová náročnost:** ~30 minut  
**Risk:** MEDIUM (může ovlivnit data konzistenci)

---

## 🟢 PRIORITA 4: Future Features (NÍZKÁ)

### 4.1 Kafka Config Change Events

**Problém:** Config změny nejsou publikovány do Kafky pro distribuované systémy

**Lokace:** `AdminAiConfigController.java` řádek 111

**Současný kód:**
```java
// TODO (future): Publish config change event to Kafka
```

**Řešení:**
```java
// Publish event for other services
if (kafkaEnabled) {
  kafkaTemplate.send("config-changes", ConfigChangeEvent.builder()
    .eventType("AI_CONFIG_UPDATED")
    .tenantId(tenantId)
    .config(aiConfig)
    .timestamp(Instant.now())
    .build()
  );
}
```

**Akce:**
- [ ] Rozhodnout, zda je Kafka integrace potřeba
- [ ] Pokud ano, vytvořit `ConfigChangeEvent` model
- [ ] Implementovat Kafka producer
- [ ] Přidat consumers v ostatních službách
- [ ] Dokumentovat event schema

**Časová náročnost:** ~1-2 hodiny (pokud se implementuje)  
**Risk:** LOW (optional feature, marked as "future")

**Doporučení:** ODLOŽIT - není kritické, můžeme implementovat později

---

## 🟡 PRIORITA 5: Disabled Tests (STŘEDNÍ)

### 5.1 TenantControllerTest

**Problém:** Test disabled kvůli security mock problémům

**Lokace:** `TenantControllerTest.java` řádek 28

**Současný stav:**
```java
@Disabled("Complex security configuration - HTTP status code mismatches. Needs security mock refactoring.")
```

**Issues:**
- HTTP 401 → 302 redirects
- HTTP 403 → 404 not found
- HTTP 200 → 500 errors
- Startup time 2+ minuty (testcontainers)

**Řešení:**
```java
@WebMvcTest(TenantController.class)
@Import(TestSecurityConfig.class)
class TenantControllerTest {
  
  @MockBean
  private TenantService tenantService;
  
  @MockBean
  private SecurityService securityService;
  
  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturnTenants_whenAuthenticated() {
    // Arrange
    when(tenantService.findAll()).thenReturn(List.of(tenant1, tenant2));
    
    // Act & Assert
    mockMvc.perform(get("/api/tenants"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2));
  }
}
```

**Akce:**
- [ ] Vytvořit `TestSecurityConfig` s mock security beans
- [ ] Přepsat na `@WebMvcTest` (rychlejší, bez Testcontainers)
- [ ] Mock všechny dependencies
- [ ] Použít `@WithMockUser` pro auth
- [ ] Přidat testy pro všechny security scénáře
- [ ] Odstranit `@Disabled`

**Časová náročnost:** ~2-3 hodiny  
**Risk:** MEDIUM (test coverage gap)

---

### 5.2 Phase2IntegrationTest

**Problém:** Test disabled kvůli chybějícímu Keycloak setupu

**Lokace:** `Phase2IntegrationTest.java` řádek 27

**Současný stav:**
```java
@Disabled("Requires full Keycloak environment - KeycloakAdminService autowiring fails in test context")
```

**Řešení A: Mock Keycloak Service**
```java
@SpringBootTest
@Import(TestKeycloakConfig.class)
class Phase2IntegrationTest {
  
  @MockBean
  private KeycloakAdminService keycloakAdminService;
  
  @Test
  void shouldHandleWebSocketConnection() {
    // Arrange
    when(keycloakAdminService.validateToken(anyString()))
      .thenReturn(true);
    
    // Act & Assert
    // WebSocket test logic
  }
}
```

**Řešení B: Testcontainers Keycloak**
```java
@SpringBootTest
@Testcontainers
class Phase2IntegrationTest {
  
  @Container
  static KeycloakContainer keycloak = new KeycloakContainer()
    .withRealmImportFile("test-realm.json");
  
  @DynamicPropertySource
  static void keycloakProperties(DynamicPropertyRegistry registry) {
    registry.add("keycloak.auth-server-url", keycloak::getAuthServerUrl);
  }
}
```

**Akce:**
- [ ] Rozhodnout mezi Mock (rychlé) vs Testcontainers (realističtější)
- [ ] Implementovat `TestKeycloakConfig`
- [ ] Vytvořit test realm JSON (pokud Testcontainers)
- [ ] Přepsat testy s correct setup
- [ ] Ověřit, že všechny testy projdou
- [ ] Odstranit `@Disabled`

**Časová náročnost:** ~1-2 hodiny (Mock) nebo ~3-4 hodiny (Testcontainers)  
**Risk:** MEDIUM (test coverage gap)

---

## 📋 Implementation Checklist

### Sprint 1: Critical Fixes (3-4 hodiny)
- [ ] Fix type safety warnings (4 instances)
- [ ] Remove unnecessary @SuppressWarnings
- [ ] Implement tenant from security context
- [ ] Add unit tests for tenant extraction

### Sprint 2: RBAC & Validation (3-4 hodiny)
- [ ] Implement RBAC integration in McpController
- [ ] Implement strict reads check
- [ ] Add entity lock service
- [ ] Unit & integration tests

### Sprint 3: Test Fixes (4-6 hodin)
- [ ] Fix TenantControllerTest (přepsat na @WebMvcTest)
- [ ] Fix Phase2IntegrationTest (mock Keycloak)
- [ ] Ověřit test coverage
- [ ] Remove all @Disabled annotations

### Sprint 4: Optional Features (POZDĚJI)
- [ ] Kafka config change events (pokud potřeba)
- [ ] Dokumentace všech změn

---

## 🎯 Doporučené pořadí implementace

1. **TEĎ (Critical):**
   - Type safety warnings (30 min)
   - Tenant from security context (1 hour)
   - RBAC integration (2 hours)

2. **BRZY (Tento týden):**
   - Strict reads check (30 min)
   - TenantControllerTest fix (2-3 hours)

3. **POZDĚJI (Příští týden):**
   - Phase2IntegrationTest fix (2-4 hours)
   - Kafka events (pouze pokud potřeba)

---

## ⚠️ Rizika a poznámky

### Security Risks:
- 🔴 **CRITICAL:** Placeholder UUID v AiContextController je BEZPEČNOSTNÍ DÍRA
- 🔴 **CRITICAL:** Stub RBAC capabilities může způsobit unauthorized access

### Test Coverage:
- 🟡 **MEDIUM:** 2 disabled testy = chybějící coverage
- 🟡 **MEDIUM:** Security mock issues mohou skrývat další problémy

### Technical Debt:
- Type safety warnings jsou code smell
- TODO komentáře indikují nedokončenou funkcionalitu

---

## 📊 Prioritizace podle dopadu

| Issue | Bezpečnost | Funkčnost | Kvalita | Celkem |
|-------|-----------|-----------|---------|--------|
| Tenant from Security Context | 10 | 10 | 5 | **25** |
| RBAC Integration | 10 | 8 | 5 | **23** |
| Strict Reads Check | 5 | 8 | 5 | **18** |
| Type Safety Warnings | 2 | 2 | 10 | **14** |
| TenantControllerTest | 3 | 5 | 8 | **16** |
| Phase2IntegrationTest | 2 | 5 | 8 | **15** |
| Kafka Events | 1 | 3 | 3 | **7** |

---

## 🚀 Quick Start

Pro okamžité zahájení:

```bash
# 1. Type safety warnings (nejrychlejší win)
git checkout -b fix/type-safety-warnings

# 2. Security context (kritické)
git checkout -b fix/security-context-tenant

# 3. RBAC integration (kritické)
git checkout -b feat/rbac-integration
```

---

**Celková časová náročnost:** ~8-14 hodin  
**Doporučení:** Rozdělit do 3-4 sprintů po 3-4 hodinách
