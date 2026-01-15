---
id: META-004
epic: EPIC-005-metamodel-generator-studio
title: "Advanced Constraints (CHECK, FK Cascade)"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "16 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META4-advanced-constraints-check-fk-cascade/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-004: Advanced Constraints (CHECK, FK Cascade)

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED** (Not implemented yet)  
**Priorita:** P2 (Nice-to-have)  
**Estimated LOC:** ~250 řádků  

---

## 📋 Story Description

Jako **platform developer**, chci **podporu pro pokročilé DB constraints (CHECK, FK CASCADE)**, abych **zajistil business rules na DB úrovni a automatizoval referenční integritu**.

---

## 🎯 Acceptance Criteria

### AC1: CHECK Constraints from YAML
- **GIVEN** YAML field s `check: "value > 0"`
- **WHEN** generuji schema
- **THEN** vytvoří:
  ```sql
  ALTER TABLE products 
  ADD CONSTRAINT ck_products_price 
  CHECK (price > 0)
  ```

### AC2: Foreign Key CASCADE Rules
- **GIVEN** YAML relationship:
  ```yaml
  relationships:
    - name: tenant
      type: many-to-one
      target: Tenant
      onDelete: CASCADE  # ← Nová property
  ```
- **WHEN** generuji FK
- **THEN** vytvoří:
  ```sql
  ALTER TABLE users 
  ADD CONSTRAINT fk_users_tenant 
  FOREIGN KEY (tenant_id) 
  REFERENCES tenants(id) 
  ON DELETE CASCADE
  ```

### AC3: Cross-Field Validation
- **GIVEN** YAML constraint:
  ```yaml
  constraints:
    - type: check
      expression: "start_date <= end_date"
  ```
- **THEN** DB odmítne: `start_date='2025-12-31', end_date='2025-01-01'`

### AC4: Enum Constraints
- **GIVEN** YAML enum field:
  ```yaml
  - name: status
    type: string
    enum: [ACTIVE, INACTIVE, PENDING]
  ```
- **THEN** vytvoří:
  ```sql
  ALTER TABLE users 
  ADD CONSTRAINT ck_users_status 
  CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING'))
  ```

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Rozsirit YAML schema + parser o `check`, `constraints`, `onDelete/onUpdate`, `enum` | 4h | META-001 |
| 2 | Implementovat DDL generator pro CHECK + FK CASCADE/RESTRICT | 6h | T1 |
| 3 | Integrace do migraci + testy (positive/negative cases) | 6h | T2, META-002 |

---

## 🏗️ Planned Implementation

### YAML Syntax (Design)

```yaml
entity: Product
table: products
fields:
  - name: price
    type: decimal
    check: "price > 0"  # ← Simple check
    
  - name: discount_percentage
    type: integer
    check: "discount_percentage BETWEEN 0 AND 100"
    
  - name: status
    type: string
    enum: [AVAILABLE, OUT_OF_STOCK, DISCONTINUED]
    # Auto-generates: CHECK (status IN (...))

constraints:
  - type: check
    name: ck_products_dates
    expression: "created_at <= updated_at"

relationships:
  - name: category
    type: many-to-one
    target: Category
    onDelete: CASCADE      # ← New: FK cascade rule
    onUpdate: RESTRICT
```

### CheckConstraintGenerator (Pseudocode)

```java
@Component
public class CheckConstraintGenerator {
    
    public List<String> generateCheckConstraints(EntitySchema schema) {
        List<String> ddl = new ArrayList<>();
        
        // 1. Field-level CHECK constraints
        for (FieldDefinition field : schema.getFields()) {
            if (field.getCheck() != null) {
                String constraintName = "ck_" + schema.getTable() + "_" + field.getName();
                ddl.add(String.format(
                    "ALTER TABLE %s ADD CONSTRAINT %s CHECK (%s)",
                    schema.getTable(), constraintName, field.getCheck()
                ));
            }
            
            // 2. Enum constraints
            if (field.getEnum() != null && !field.getEnum().isEmpty()) {
                String constraintName = "ck_" + schema.getTable() + "_" + field.getName() + "_enum";
                String values = field.getEnum().stream()
                    .map(v -> "'" + v + "'")
                    .collect(Collectors.joining(", "));
                ddl.add(String.format(
                    "ALTER TABLE %s ADD CONSTRAINT %s CHECK (%s IN (%s))",
                    schema.getTable(), constraintName, field.getName(), values
                ));
            }
        }
        
        // 3. Table-level CHECK constraints
        if (schema.getConstraints() != null) {
            for (Constraint c : schema.getConstraints()) {
                if (c.getType() == ConstraintType.CHECK) {
                    ddl.add(String.format(
                        "ALTER TABLE %s ADD CONSTRAINT %s CHECK (%s)",
                        schema.getTable(), c.getName(), c.getExpression()
                    ));
                }
            }
        }
        
        return ddl;
    }
}
```

### FK Cascade Support (Pseudocode)

```java
public String generateForeignKey(Relationship rel) {
    String onDelete = rel.getOnDelete() != null ? "ON DELETE " + rel.getOnDelete() : "";
    String onUpdate = rel.getOnUpdate() != null ? "ON UPDATE " + rel.getOnUpdate() : "";
    
    return String.format(
        "ALTER TABLE %s ADD CONSTRAINT fk_%s_%s FOREIGN KEY (%s) REFERENCES %s(id) %s %s",
        tableName,
        tableName,
        rel.getName(),
        rel.getName() + "_id",
        rel.getTarget(),
        onDelete,
        onUpdate
    );
}
```

---

## 🧪 Planned Tests

```java
@Test
void shouldEnforcePositivePriceCheck() {
    // Given: Product with CHECK (price > 0)
    
    // When: Try to insert negative price
    assertThatThrownBy(() -> {
        jdbcTemplate.execute("INSERT INTO products (price) VALUES (-10)");
    }).hasMessageContaining("ck_products_price");
    
    // Then: Valid price accepted
    jdbcTemplate.execute("INSERT INTO products (price) VALUES (99.99)");
}

@Test
void shouldCascadeDeleteTenant() {
    // Given: Tenant with users
    jdbcTemplate.execute("INSERT INTO tenants VALUES (1, 'Tenant A')");
    jdbcTemplate.execute("INSERT INTO users VALUES (1, 1, 'user1')");  // tenant_id=1
    
    // When: Delete tenant
    jdbcTemplate.execute("DELETE FROM tenants WHERE id = 1");
    
    // Then: User cascade deleted
    Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE tenant_id = 1", Integer.class);
    assertThat(userCount).isZero();
}
```

---

## 💡 Expected Value

### Benefits
- **DB-Level Validation**: Business rules enforced even outside app
- **Data Integrity**: Cannot bypass constraints via direct SQL
- **Cascade Safety**: Automatic cleanup on delete (no orphans)

### Use Cases
- **Price validation**: `CHECK (price > 0 AND discount <= 100)`
- **Date ranges**: `CHECK (start_date <= end_date)`
- **Enum enforcement**: `CHECK (status IN ('ACTIVE', 'INACTIVE'))`
- **Tenant cleanup**: `ON DELETE CASCADE` removes user data when tenant deleted

---

## 🔗 Related

- **Depends On:** [META-003 (UNIQUE Constraints)](META-003.md)
- **Blocks:** [META-005 (Visual Studio)](META-005.md)
- **Future:** EPIC-018 S8 (Audit constraints for compliance)

---

## 📚 References

- **Design:** `docs/metamodel/constraints-design.md` (TODO)
- **PostgreSQL CHECK Docs:** https://www.postgresql.org/docs/current/ddl-constraints.html
