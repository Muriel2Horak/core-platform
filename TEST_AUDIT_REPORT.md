# 🔍 TEST AUDIT REPORT - Backend Integration Tests

**Datum**: 14. října 2025  
**Cíl**: Identifikovat testy vyžadující Testcontainers migration  
**Celkem testů**: 26  
**Testy s @SpringBootTest**: 24  
**Již používají AbstractIntegrationTest**: 10  

---

## 📊 KATEGORIZACE

### ✅ CATEGORY A: Již používají AbstractIntegrationTest (10 testů)
**Status**: ✅ Perfect - žádná akce potřeba

1. ✅ `TenantFilterIntegrationTest.java`
2. ✅ `ReportingPropertiesTest.java`
3. ✅ `CubeQueryServiceIT.java`
4. ✅ `PreAggRefreshWorkerIT.java`
5. ✅ `TenantControllerTest.java`
6. ✅ `PresenceServiceIntegrationTest.java`
7. ✅ `PresenceNrtIT.java`
8. ✅ `MonitoringMetricsAndLogsIT.java`
9. ✅ `MonitoringProxyServiceTest.java`
10. ✅ `AbstractIntegrationTest.java` (base class)

**Důvod**: Tyto testy již dědí z AbstractIntegrationTest, mají Testcontainers setup.

---

### 🔧 CATEGORY B: @SpringBootTest BEZ AbstractIntegrationTest (14 testů)
**Status**: ⚠️ POTŘEBUJE FIX - přidat extends AbstractIntegrationTest

#### B1: Reporting Module (5 testů)
1. 🔧 `SecurityHeadersFilterIT.java`
2. 🔧 `RateLimitFilterIT.java`
3. 🔧 `EntityCrudControllerIT.java`
4. 🔧 `BulkUpdateControllerIT.java`
5. 🔧 `ReportQueryControllerIT.java`

**Důvod**: Integration testy pro reporting API - potřebují DB pro CRUD operace

#### B2: Streaming Module (3 testy)
6. 🔧 `PostgresStreamingIT.java`
7. 🔧 `PriorityAndPoliciesIT.java`
8. 🔧 `KafkaStreamingIT.java`

**Důvod**: Streaming integration testy - potřebují PostgreSQL + Kafka

#### B3: Workflow Module (2 testy)
9. 🔧 `WorkflowEventsKafkaIT.java`
10. 🔧 `WorkflowApiIT.java`

**Důvod**: Workflow API testy - potřebují DB pro workflow persistence

#### B4: Monitoring Module (2 testy)
11. 🔧 `MonitoringQueryIT.java`
12. 🔧 `MonitoringHeaderSecurityIT.java`

**Důvod**: Monitoring API testy - potřebují DB pro query validaci

#### B5: Ostatní (4 testy)
13. 🔧 `OpenApiContractIT.java`
14. 🔧 `Phase2IntegrationTest.java`
15. 🔧 `StudioAdminControllerIT.java`
16. 🔧 `BackendApplicationTests.java`

**Důvod**: Integration/smoke testy - potřebují plný Spring context s DB

#### B6: Workflow Versioning (1 test)
17. 🔧 `WorkflowVersionServiceTest.java`

**Důvod**: Service test s DB dependencies

#### B7: Workflow Presence (1 test)
18. 🔧 `PresenceLockIT.java`

**Důvod**: Presence lock test vyžaduje Redis + PostgreSQL

---

### ✅ CATEGORY C: Pure Unit Tests (2 testy)
**Status**: ✅ Ponechat beze změny

Zbytek (26 - 24 @SpringBootTest = 2 unit testy):
- Pure unit testy s @Mock/@InjectMocks
- Žádný Spring context
- Žádná DB/Redis závislost

**Důvod**: Perfektní unit testy, žádná změna

---

## 🎯 IMPLEMENTATION PLAN

### Priority 1: Fix 14 Integration Tests

Pro každý test v **CATEGORY B**:

**Změny**:
1. Přidat `extends AbstractIntegrationTest`
2. Odstranit `@ActiveProfiles("test")` (už je v parent)
3. Odstranit vlastní `@Testcontainers` (už je v parent)
4. Odstranit vlastní `@Container` definice (PostgreSQL/Redis už je v parent)
5. Odstranit vlastní `@DynamicPropertySource` (už je v parent)
6. Ponechat specifické `@TestPropertySource` (business logic properties)
7. Ponechat `@Import` pro custom test config

**Pattern**:
```java
// BEFORE
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MyIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = ...;
  
  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
  }
}

// AFTER
@SpringBootTest
class MyIntegrationTest extends AbstractIntegrationTest {
  // Všechno už je v AbstractIntegrationTest!
}
```

---

## 📋 FIX CHECKLIST

### Reporting Module (5/14)
- [ ] SecurityHeadersFilterIT.java
- [ ] RateLimitFilterIT.java
- [ ] EntityCrudControllerIT.java
- [ ] BulkUpdateControllerIT.java
- [ ] ReportQueryControllerIT.java

### Streaming Module (3/14)
- [ ] PostgresStreamingIT.java
- [ ] PriorityAndPoliciesIT.java
- [ ] KafkaStreamingIT.java

### Workflow Module (2/14)
- [ ] WorkflowEventsKafkaIT.java
- [ ] WorkflowApiIT.java

### Monitoring Module (2/14)
- [ ] MonitoringQueryIT.java
- [ ] MonitoringHeaderSecurityIT.java

### Ostatní (4/14)
- [ ] OpenApiContractIT.java
- [ ] Phase2IntegrationTest.java
- [ ] StudioAdminControllerIT.java
- [ ] BackendApplicationTests.java

### Workflow Versioning (1/14)
- [ ] WorkflowVersionServiceTest.java

### Workflow Presence (1/14)
- [ ] PresenceLockIT.java

**Total**: 0/14 complete

---

## 🔍 DETAILED ANALYSIS

### Test 1: SecurityHeadersFilterIT.java
```bash
File: src/test/java/cz/muriel/core/reporting/security/SecurityHeadersFilterIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests HTTP security headers, needs running web context + DB
```

### Test 2: RateLimitFilterIT.java
```bash
File: src/test/java/cz/muriel/core/reporting/security/RateLimitFilterIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests rate limiting, needs Redis for rate counter storage
```

### Test 3: EntityCrudControllerIT.java
```bash
File: src/test/java/cz/muriel/core/reporting/api/EntityCrudControllerIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests CRUD operations, needs PostgreSQL for entity persistence
```

### Test 4: BulkUpdateControllerIT.java
```bash
File: src/test/java/cz/muriel/core/reporting/api/BulkUpdateControllerIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests bulk updates, needs PostgreSQL transactions
```

### Test 5: ReportQueryControllerIT.java
```bash
File: src/test/java/cz/muriel/core/reporting/api/ReportQueryControllerIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests report queries, needs PostgreSQL with cube data
```

### Test 6: PostgresStreamingIT.java
```bash
File: src/test/java/cz/muriel/core/streaming/PostgresStreamingIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests PostgreSQL CDC streaming, needs PostgreSQL + Kafka
```

### Test 7: PriorityAndPoliciesIT.java
```bash
File: src/test/java/cz/muriel/core/streaming/PriorityAndPoliciesIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests streaming priorities, needs Kafka + PostgreSQL
```

### Test 8: KafkaStreamingIT.java
```bash
File: src/test/java/cz/muriel/core/streaming/KafkaStreamingIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests Kafka streaming, needs Kafka container
```

### Test 9: WorkflowEventsKafkaIT.java
```bash
File: src/test/java/cz/muriel/core/workflow/WorkflowEventsKafkaIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests workflow events via Kafka, needs Kafka + PostgreSQL
```

### Test 10: WorkflowApiIT.java
```bash
File: src/test/java/cz/muriel/core/workflow/WorkflowApiIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests workflow REST API, needs PostgreSQL for workflow storage
```

### Test 11: MonitoringQueryIT.java
```bash
File: src/test/java/cz/muriel/core/monitoring/bff/MonitoringQueryIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests monitoring queries, needs PostgreSQL for query validation
```

### Test 12: MonitoringHeaderSecurityIT.java
```bash
File: src/test/java/cz/muriel/core/monitoring/bff/MonitoringHeaderSecurityIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests security headers, needs running web context
```

### Test 13: OpenApiContractIT.java
```bash
File: src/test/java/cz/muriel/core/contract/OpenApiContractIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests OpenAPI contract, needs full Spring context with DB
```

### Test 14: Phase2IntegrationTest.java
```bash
File: src/test/java/cz/muriel/core/phase2/Phase2IntegrationTest.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Phase 2 integration test, needs PostgreSQL for end-to-end flow
```

### Test 15: StudioAdminControllerIT.java
```bash
File: src/test/java/cz/muriel/core/controller/admin/StudioAdminControllerIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests admin API, needs PostgreSQL for admin operations
```

### Test 16: BackendApplicationTests.java
```bash
File: src/test/java/cz/muriel/core/BackendApplicationTests.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Smoke test - verifies application context loads with DB
```

### Test 17: WorkflowVersionServiceTest.java
```bash
File: src/test/java/com/platform/workflow/versioning/WorkflowVersionServiceTest.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest + @SpringBootConfiguration
Reason: Service test with Repository dependencies, needs PostgreSQL
Error: "Unable to find @SpringBootConfiguration" - missing explicit config
```

### Test 18: PresenceLockIT.java
```bash
File: src/test/java/com/platform/workflow/PresenceLockIT.java
Current: @SpringBootTest
Needs: extends AbstractIntegrationTest
Reason: Tests distributed locks, needs Redis + PostgreSQL
```

---

## 🚀 EXECUTION ORDER

### Phase 1: Simple Cases (No Custom Setup)
Start with tests that only need `extends AbstractIntegrationTest`:

1. BackendApplicationTests.java (smoke test)
2. OpenApiContractIT.java (contract test)
3. Phase2IntegrationTest.java (integration test)

**Estimate**: 5 min per test = 15 min

### Phase 2: API Controllers
Tests with `@AutoConfigureMockMvc` and controller setup:

4. EntityCrudControllerIT.java
5. BulkUpdateControllerIT.java
6. ReportQueryControllerIT.java
7. StudioAdminControllerIT.java
8. WorkflowApiIT.java

**Estimate**: 10 min per test = 50 min

### Phase 3: Security Filters
Tests with MockMvc and security setup:

9. SecurityHeadersFilterIT.java
10. RateLimitFilterIT.java
11. MonitoringHeaderSecurityIT.java

**Estimate**: 10 min per test = 30 min

### Phase 4: Streaming & Kafka
Tests potentially needing Kafka container:

12. PostgresStreamingIT.java
13. PriorityAndPoliciesIT.java
14. KafkaStreamingIT.java
15. WorkflowEventsKafkaIT.java

**Estimate**: 15 min per test = 60 min

### Phase 5: Monitoring & Queries
Tests with complex query setup:

16. MonitoringQueryIT.java

**Estimate**: 10 min

### Phase 6: Special Cases
Tests with custom configuration needs:

17. WorkflowVersionServiceTest.java (needs @SpringBootConfiguration fix)
18. PresenceLockIT.java (needs Redis lock testing)

**Estimate**: 20 min per test = 40 min

**Total Estimate**: ~205 min (~3.5 hours)

---

## ✅ SUCCESS CRITERIA

### Per Test:
- [ ] Přidán `extends AbstractIntegrationTest`
- [ ] Odstraněny duplicitní annotations (@ActiveProfiles, @Testcontainers)
- [ ] Odstraněny custom @Container (PostgreSQL/Redis už v parent)
- [ ] Test kompiluje bez chyb
- [ ] Test prochází (`./mvnw test -Dtest=TestName`)
- [ ] Test log ukazuje Testcontainers startup

### Global:
- [ ] Všech 14 testů kompiluje
- [ ] `./mvnw test` prochází (186/186)
- [ ] Žádné selhání s "datasource" errors
- [ ] Testcontainers reuse funguje (rychlé re-runs)
- [ ] Docker logs ukazují postgres:16-alpine + redis:7-alpine

---

## 📈 PROGRESS TRACKING

```
Category A (Already OK):     10/10 ✅ (100%)
Category B (Need Fix):        0/14 🔧 (0%)
Category C (Pure Unit):       2/2  ✅ (100%)

Overall Progress:            12/26 (46%)
Integration Test Coverage:   10/24 (42%)
```

---

## 🎯 NEXT STEPS

1. **Start with Phase 1** (BackendApplicationTests.java)
2. **Test compile**: `./mvnw compile test-compile`
3. **Run single test**: `./mvnw test -Dtest=BackendApplicationTests`
4. **Verify Testcontainers**: Check Docker logs
5. **Repeat for remaining 13 tests**
6. **Final verification**: `./mvnw test` (all 186 tests)

---

**Ready to begin Phase 1?** 🚀
