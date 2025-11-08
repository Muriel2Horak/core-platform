# META-010: Naming Convention System

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED**  
**Priorita:** P1 (High Priority)  
**Estimated LOC:** ~300 řádků  
**Effort:** 1 týden (40 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **konzistentní naming conventions napříč celým stackem**, abych **zajistil předvídatelné názvy DB tabulek, Kafka topics, REST endpoints a FE routes**.

---

## 🎯 Business Value

**HIGH-LEVEL požadavek:**
> 3️⃣ Naming & Konvence: DB: `core_{context}_{entity}`, Kafka: `core.{context}.{entity}.{event}`, REST: `/api/{context}/{entity}`, FE: `/app/{context}/{entity}`. Copilot/kód se podle toho má řídit a existující věci se mají postupně dorovnat.

---

## 🎯 Acceptance Criteria

### AC1: DB Table Naming Convention
- **GIVEN** entity `Order` v bounded context `sales`
- **THEN** table name: `core_sales_order`
- **VALIDATION**: Musí být lowercase, snake_case

### AC2: Kafka Topic Naming
- **GIVEN** event `OrderCreated`
- **THEN** topic: `core.sales.order.created`

### AC3: REST API Path
- **GIVEN** entity `Order`
- **THEN** API paths:
  - `/api/sales/orders` (list)
  - `/api/sales/orders/{id}` (detail)

### AC4: Frontend Route
- **GIVEN** entity `Order`
- **THEN** FE route: `/app/sales/orders`

### AC5: Validation on Metamodel Load
- **GIVEN** YAML s nesprávným názvem:
  ```yaml
  entity: UserProfile  # PascalCase OK
  table: user-profile  # ❌ CHYBA (kebab-case)
  ```
- **THEN** validace selže: "Table name must be snake_case"

---

## 🏗️ Implementation

```java
@Component
public class NamingConventionValidator {
    
    public void validate(EntitySchema schema) {
        // DB table: snake_case
        if (!schema.getTable().matches("^[a-z_]+$")) {
            throw new ValidationException("Table name must be snake_case: " + schema.getTable());
        }
        
        // Entity: PascalCase
        if (!schema.getEntity().matches("^[A-Z][a-zA-Z]+$")) {
            throw new ValidationException("Entity name must be PascalCase: " + schema.getEntity());
        }
    }
    
    public String buildKafkaTopic(String boundedContext, String entityName, String eventType) {
        // core.sales.order.created
        return String.format("core.%s.%s.%s",
            boundedContext.toLowerCase(),
            entityName.toLowerCase(),
            eventType.toLowerCase()
        );
    }
}
```

---

**Story Owner:** Backend Team  
**Priority:** P1  
**Effort:** 1 týden
