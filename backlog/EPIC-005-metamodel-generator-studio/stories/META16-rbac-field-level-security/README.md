---
id: META-016
epic: EPIC-005-metamodel-generator-studio
title: "RBAC & Field-Level Security"
priority: P0
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "120 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META16-rbac-field-level-security/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-016: RBAC & Field-Level Security

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🔴 **CRITICAL**  
**Priorita:** P0 (Critical - Security!)  
**Estimated LOC:** ~1,800 řádků  
**Effort:** 3 týdny (120 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **field-level security a RBAC z metamodelu**, abych **zajistil že uživatelé vidí pouze data, která mohou**.

---

## 🎯 Business Value

**HIGH-LEVEL požadavek:**
> 🔟 RBAC, bezpečnost, audit: Metamodel řeší role/permissions k entitám, polím, akcím, workflow přechodům, adminOnly a sensitive flagy, auditovatelné akce (CRUD, workflow, export, AI dotazy), parametry pro anonymizaci / omezení pro AI / export.

---

## 🎯 Acceptance Criteria

### AC1: Field-Level Visibility
```yaml
entity: Employee
fields:
  - name: name
    type: string
    visibility: public
  
  - name: salary
    type: decimal
    visibility: adminOnly  # Pouze role ADMIN
    pii: true             # Sensitive data
  
  - name: performance_review
    type: text
    visibility: ["MANAGER", "HR"]
```

### AC2: Role-Based Access Runtime
- **GIVEN** User s rolí `EMPLOYEE`
- **WHEN** volá `GET /api/employees/123`
- **THEN** response:
```json
{
  "id": 123,
  "name": "John Doe",
  // "salary": HIDDEN (adminOnly)
  // "performance_review": HIDDEN (MANAGER/HR only)
}
```

### AC3: PII Masking for AI
- **GIVEN** pole s `pii: true`
- **WHEN** AI query přes MCP server
- **THEN** hodnota je maskovaná:
  - Email: `j***@example.com`
  - Phone: `+420 ***-***-789`
  - SSN: `***-**-1234`

### AC4: Audit Trail
- **GIVEN** jakákoli změna entity
- **THEN** audit log:
```json
{
  "action": "UPDATE",
  "entity": "Employee",
  "entityId": "123",
  "field": "salary",
  "oldValue": 50000,
  "newValue": 55000,
  "user": "admin@company.com",
  "timestamp": "2025-11-08T10:30:00Z",
  "reason": "Annual raise"
}
```

### AC5: Export Permission Checks
- **GIVEN** pole s `exportable: false` nebo `visibility: adminOnly`
- **WHEN** uživatel bez permissions volá export
- **THEN** pole je vynecháno z CSV/Excel výstupu

---

## 🏗️ Implementation

```java
@Component
public class FieldLevelSecurityFilter {
    
    public void filterFields(Object entity, EntitySchema schema, Authentication auth) {
        for (FieldSchema field : schema.getFields()) {
            if (!canUserSeeField(field, auth)) {
                setFieldValue(entity, field.getName(), null); // Hide field
            } else if (field.isPii() && shouldMaskForContext(auth)) {
                Object value = getFieldValue(entity, field.getName());
                setFieldValue(entity, field.getName(), maskPii(value, field.getType()));
            }
        }
    }
    
    private boolean canUserSeeField(FieldSchema field, Authentication auth) {
        if (field.getVisibility() == null || field.getVisibility().equals("public")) {
            return true;
        }
        
        if (field.getVisibility().equals("adminOnly")) {
            return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        
        // List of allowed roles
        if (field.getVisibility() instanceof List) {
            return field.getVisibility().stream()
                .anyMatch(role -> auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        
        return false;
    }
    
    private String maskPii(Object value, String type) {
        if (value == null) return null;
        
        String str = value.toString();
        switch (type) {
            case "email":
                return str.replaceAll("(?<=.).(?=.*@)", "*");
            case "phone":
                return str.replaceAll("\\d(?=\\d{3})", "*");
            case "ssn":
                return "***-**-" + str.substring(str.length() - 4);
            default:
                return "***REDACTED***";
        }
    }
}

@Aspect
@Component
public class AuditLogAspect {
    
    @Around("@annotation(Audited)")
    public Object logAuditTrail(ProceedingJoinPoint joinPoint) throws Throwable {
        Object oldValue = getCurrentValue(joinPoint);
        Object result = joinPoint.proceed();
        Object newValue = getNewValue(result);
        
        AuditLog log = AuditLog.builder()
            .action(determineAction(joinPoint))
            .entity(extractEntityType(joinPoint))
            .entityId(extractEntityId(joinPoint))
            .field(extractFieldName(joinPoint))
            .oldValue(oldValue)
            .newValue(newValue)
            .user(SecurityContextHolder.getContext().getAuthentication().getName())
            .timestamp(Instant.now())
            .build();
        
        auditLogRepository.save(log);
        
        return result;
    }
}
```

---

**Story Owner:** Backend Team  
**Priority:** P0 - CRITICAL (Security!)  
**Effort:** 3 týdny
