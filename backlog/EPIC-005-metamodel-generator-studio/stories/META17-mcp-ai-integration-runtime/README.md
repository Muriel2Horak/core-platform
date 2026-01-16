---
id: META-017
epic: EPIC-005-metamodel-generator-studio
title: "MCP/AI Integration Runtime"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "120 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META17-mcp-ai-integration-runtime/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---


# META-017: MCP/AI Integration Runtime

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED**  
**Priorita:** P2 (Medium)  
**Estimated LOC:** ~1,500 řádků  
**Effort:** 3 týdny (120 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **MCP server generovaný z metamodelu**, abych **AI poskytl bezpečný přístup k datům s PII maskingem**.

---

## 🎯 Business Value

**HIGH-LEVEL požadavek:**
> 1️⃣1️⃣ MCP / AI integrace: Z metamodelu se generuje co může AI číst (bezpečné pohledy), jaké nástroje (MCP tools) existují pro které entity, jaká pravidla (PII maskování, tenant scope, limity). AI nikdy neleze přímo na tabulky, ale na metamodel-based view.

---

## 🎯 Acceptance Criteria

### AC1: MCP Tool Generation z Entity

```yaml
entity: Customer
ai:
  tools:
    - name: search_customers
      description: "Search customers by name or email"
      parameters:
        - name: query
          type: string
          required: true
      returns: [id, name, email]  # PII fields excluded
```

### AC2: AI-Safe View

- **GIVEN** pole s `pii: true`
- **WHEN** AI query přes MCP
- **THEN** data jsou maskovaná:
  - `email: "j***@example.com"`
  - `phone: "+420 ***-***-789"`

### AC3: Tenant Scoping

- **GIVEN** AI konverzace v tenantovém kontextu
- **THEN** MCP tools automaticky filtrují: `WHERE tenant_id = {context.tenantId}`

### AC4: Rate Limiting

```yaml
ai:
  rateLimits:
    queriesPerHour: 100
    maxResults: 50
```

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | AI tool schema + policy (PII, scope, limits) | 20h | META-016 |
| 2 | MCP tool generator + handlers s maskingem | 50h | T1 |
| 3 | Rate limiting + audit log + permissions | 25h | T2 |
| 4 | Testy + docs + sample prompts | 25h | T2, T3 |

---

## 🏗️ Implementation

```java
@Component
public class McpServerGenerator {
    
    public McpTool generateTool(EntitySchema schema) {
        AiConfig aiConfig = schema.getAi();
        
        return McpTool.builder()
            .name(aiConfig.getToolName())
            .description(aiConfig.getDescription())
            .parameters(buildParameters(schema))
            .handler(request -> {
                // Apply PII masking
                List<Object> results = repository.findAll(buildSpec(request));
                return results.stream()
                    .map(entity -> maskPiiFields(entity, schema))
                    .collect(Collectors.toList());
            })
            .build();
    }
    
    private Object maskPiiFields(Object entity, EntitySchema schema) {
        for (FieldSchema field : schema.getFields()) {
            if (field.isPii()) {
                Object value = getFieldValue(entity, field.getName());
                setFieldValue(entity, field.getName(), piiMasker.mask(value, field.getType()));
            }
        }
        return entity;
    }
}
```

---

**Story Owner:** Backend Team  
**Priority:** P2  
**Effort:** 3 týdny
