# 🧪 Metamodel Testing Guide

**Date:** 2025-10-09  
**Status:** Testing Strategy for Phase 1-3  

---

## 📋 Test Pyramid

```
        /\
       /  \    E2E Tests (CI/CD only)
      /____\   
     /      \  Integration Tests (Testcontainers)
    /________\ 
   /          \ Unit Tests (Local + CI)
  /____________\
```

---

## 🔬 Unit Tests (Local Development)

### TypeConversionRegistry ✅

**File:** `TypeConversionRegistryTest.java`

**Coverage:**
- ✅ Safe conversions (VARCHAR→TEXT, INTEGER→BIGINT, etc.)
- ✅ Risky conversions (TEXT→VARCHAR, BIGINT→INTEGER)
- ✅ Unsupported conversions
- ✅ Type normalization
- ✅ SQL generation
- ✅ Edge cases (null, empty, case-insensitive)

**Run locally:**
```bash
cd backend
./mvnw test -Dtest=TypeConversionRegistryTest
```

**Expected:** ✅ All tests pass (20+ test cases)

---

### LifecycleHookExecutor (TODO)

**Test Cases:**
```java
@Test
void testValidate_Required() {
  // GIVEN: Entity with null required field
  // WHEN: executeValidate()
  // THEN: IllegalArgumentException thrown
}

@Test
void testValidate_MinLength() {
  // GIVEN: String shorter than minLength
  // WHEN: executeValidate()
  // THEN: IllegalArgumentException thrown
}

@Test
void testValidate_Pattern() {
  // GIVEN: String not matching regex
  // WHEN: executeValidate()
  // THEN: IllegalArgumentException thrown
}
```

**Run locally:**
```bash
./mvnw test -Dtest=LifecycleHookExecutorTest
```

---

## 🐳 Integration Tests (Testcontainers)

### MetamodelSchemaGeneratorTest (TODO)

**Setup:**
```java
@Testcontainers
class MetamodelSchemaGeneratorTest {
  
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Test
  void testDetectChanges_AddColumn() {
    // GIVEN: YAML has new field, DB doesn't
    // WHEN: detectChanges()
    // THEN: ColumnChange.ADD detected
  }

  @Test
  void testDetectChanges_AlterType() {
    // GIVEN: YAML has BIGINT, DB has INTEGER
    // WHEN: detectChanges()
    // THEN: ColumnChange.ALTER_TYPE detected, safe conversion
  }

  @Test
  void testApplyChanges_SafeChanges() {
    // GIVEN: Safe changes detected
    // WHEN: applyChanges()
    // THEN: SQL executed, columns added
  }

  @Test
  void testApplyChanges_RiskyChanges() {
    // GIVEN: Risky changes detected
    // WHEN: applyChanges()
    // THEN: Changes skipped, warning logged
  }

  @Test
  void testCreateManyToManyJunctionTables() {
    // GIVEN: Schema with M:N field
    // WHEN: createManyToManyJunctionTables()
    // THEN: Junction table created with correct columns
  }
}
```

**Run in CI/CD:**
```bash
./mvnw verify -P integration-tests
```

---

### RelationshipResolverTest (TODO)

```java
@Testcontainers
class RelationshipResolverTest {

  @Test
  void testLoadOneToMany() {
    // GIVEN: User with roles (1:N)
    // WHEN: loadOneToMany()
    // THEN: Roles loaded from DB
  }

  @Test
  void testLoadManyToMany() {
    // GIVEN: User with groups (M:N via junction table)
    // WHEN: loadManyToMany()
    // THEN: Groups loaded via junction table query
  }

  @Test
  void testSaveRelationships() {
    // GIVEN: Entity with M:N relationships
    // WHEN: saveRelationships()
    // THEN: Junction table records created
  }
}
```

---

## 🌐 E2E Tests (API Testing)

### Hot Reload API

**Test Scenario 1: Reload Metamodel**
```bash
# 1. Modify YAML
cat > backend/src/main/resources/metamodel/user.yaml << EOF
entity: User
table: users_directory
fields:
  - name: username
    type: string
    required: true
  - name: bio
    type: text  # NEW FIELD
EOF

# 2. Reload via API
curl http://localhost:8080/api/admin/metamodel/reload | jq

# Expected:
{
  "status": "success",
  "changesDetected": 1,
  "changes": {
    "User": {
      "totalChanges": 1,
      "safeChanges": 1,
      "details": [
        {
          "type": "ADD",
          "column": "bio",
          "newType": "TEXT"
        }
      ]
    }
  }
}
```

**Test Scenario 2: Apply Safe Changes**
```bash
curl -X POST http://localhost:8080/api/admin/metamodel/apply-safe-changes | jq

# Expected:
{
  "status": "success",
  "message": "Safe schema changes applied successfully"
}

# Verify in DB:
psql -c "SELECT column_name, data_type FROM information_schema.columns WHERE table_name='users_directory' AND column_name='bio';"
```

**Test Scenario 3: Risky Change Detection**
```bash
# 1. Modify YAML (risky change)
cat > backend/src/main/resources/metamodel/user.yaml << EOF
fields:
  - name: username
    type: string
    maxLength: 50  # DB has VARCHAR(255), trying to shrink
EOF

# 2. Reload
curl http://localhost:8080/api/admin/metamodel/reload | jq

# Expected:
{
  "changes": {
    "User": {
      "hasRiskyChanges": true,
      "riskyChanges": 1,
      "details": [
        {
          "type": "ALTER_TYPE",
          "risky": "true",
          "riskDescription": "Data will be truncated to 50 characters"
        }
      ]
    }
  }
}

# 3. Try to apply (should skip risky)
curl -X POST http://localhost:8080/api/admin/metamodel/apply-safe-changes | jq

# Expected: Risky changes skipped
```

---

### Validation Hooks

**Test Scenario: Required Field Validation**
```bash
curl -X POST http://localhost:8080/api/entities/User \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'

# Expected: 400 Bad Request
{
  "error": "Field 'username' is required but was null"
}
```

**Test Scenario: MinLength Validation**
```bash
curl -X POST http://localhost:8080/api/entities/User \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ab"  # minLength: 3
  }'

# Expected: 400 Bad Request
{
  "error": "Field 'username' must be at least 3 characters, got 2"
}
```

---

### M:N Relationships

**Test Scenario: Create M:N Relationship**
```bash
# 1. Create User
USER_ID=$(curl -X POST http://localhost:8080/api/entities/User \
  -H "Content-Type: application/json" \
  -d '{"username": "john"}' | jq -r '.id')

# 2. Create Groups
GROUP1=$(curl -X POST http://localhost:8080/api/entities/Group \
  -d '{"name": "Admins"}' | jq -r '.id')

GROUP2=$(curl -X POST http://localhost:8080/api/entities/Group \
  -d '{"name": "Developers"}' | jq -r '.id')

# 3. Assign User to Groups (M:N)
curl -X POST http://localhost:8080/api/entities/User/$USER_ID/relationships/groups \
  -d "{\"groupIds\": [\"$GROUP1\", \"$GROUP2\"]}"

# 4. Verify junction table
psql -c "SELECT * FROM user_groups WHERE user_id='$USER_ID';"

# Expected: 2 rows
```

---

## 🚀 CI/CD Pipeline

### GitHub Actions Workflow

```yaml
name: Metamodel Tests

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run Unit Tests
        run: |
          cd backend
          ./mvnw test

  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
      - name: Run Integration Tests
        run: |
          cd backend
          ./mvnw verify -P integration-tests
      - name: Upload Test Report
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: backend/target/surefire-reports/

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Start Services
        run: docker compose up -d
      - name: Run E2E Tests
        run: |
          npm run test:e2e
      - name: Collect Logs
        if: failure()
        run: docker compose logs > logs.txt
```

---

## 📊 Test Coverage Goals

| Component | Unit | Integration | E2E | Target |
|-----------|------|-------------|-----|--------|
| TypeConversionRegistry | ✅ 90% | N/A | N/A | ✅ |
| MetamodelSchemaGenerator | N/A | ⏳ 80% | ⏳ 90% | 80% |
| RelationshipResolver | N/A | ⏳ 80% | ⏳ 90% | 80% |
| LifecycleHookExecutor | ⏳ 80% | N/A | ⏳ 90% | 80% |
| MetamodelAdminController | N/A | ⏳ 80% | ✅ 100% | 90% |

---

## 🐛 Known Issues & Edge Cases

### 1. Type Conversion Edge Cases
- ⚠️ VARCHAR(large) → VARCHAR(small): Requires explicit maxLength
- ⚠️ TIMESTAMPTZ → TIMESTAMP: Loses timezone info
- ⚠️ BIGINT → INTEGER: Out-of-range values become NULL

### 2. Relationship Edge Cases
- ⚠️ Circular M:N relationships: Requires careful cascade config
- ⚠️ Self-referencing 1:N: Parent-child in same table
- ⚠️ Orphaned junction table records: No automatic cleanup

### 3. Validation Edge Cases
- ⚠️ Custom validators: Not yet supported
- ⚠️ Cross-field validation: Requires manual implementation
- ⚠️ Async validation: Not supported

---

## 📝 Manual Testing Checklist

### Before Deployment

- [ ] Test hot reload API with safe changes
- [ ] Test hot reload API with risky changes
- [ ] Test M:N junction table creation
- [ ] Test 1:N relationship loading
- [ ] Test validation hooks (required, minLength, pattern)
- [ ] Test dev mode detection (DROP TABLE)
- [ ] Test UNIQUE constraint creation
- [ ] Test version trigger creation
- [ ] Test schema drift detection

### Performance Testing

- [ ] Benchmark detectChanges() on large schemas (100+ tables)
- [ ] Benchmark M:N relationship loading (1000+ records)
- [ ] Test hot reload with minimal downtime
- [ ] Test concurrent schema modifications

---

## 🎯 Next Steps

1. **Implement Integration Tests** (Priority P0)
   - MetamodelSchemaGeneratorTest with Testcontainers
   - RelationshipResolverTest with real DB

2. **Add Unit Tests** (Priority P1)
   - LifecycleHookExecutorTest (no DB required)
   - MetamodelAdminControllerTest (mock dependencies)

3. **E2E Test Suite** (Priority P1)
   - Postman/Newman collection
   - Automated API tests in CI/CD

4. **Performance Tests** (Priority P2)
   - JMeter scripts for hot reload
   - Benchmark reports

---

**Status:** 🚧 Testing infrastructure ready, implementation pending  
**Coverage:** TypeConversionRegistry only (90%+)  
**Next:** Integration tests with Testcontainers
