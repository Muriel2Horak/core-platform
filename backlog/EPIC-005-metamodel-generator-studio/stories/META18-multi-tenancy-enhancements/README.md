---
id: META-018
epic: EPIC-005-metamodel-generator-studio
title: "Multi-Tenancy Enhancements"
priority: P3
status: ready
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "40 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META18-multi-tenancy-enhancements/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---


# META-018: Multi-Tenancy Enhancements

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **READY**
**Priorita:** P3 (Low)  
**Estimated LOC:** ~600 řádků  
**Effort:** 1 týden (40 hodin)

---

## 📋 Story Description

Tenant-specific schema variations, column-level tenant isolation, shared vs dedicated tables.

---

## 🎯 Acceptance Criteria

### AC1: Tenant-Specific Fields

```yaml
entity: Product
tenant_aware: true
tenant_fields:
  - tenant: acme-corp
    fields:
      - name: custom_sku
        type: string
  - tenant: example-inc
    fields:
      - name: internal_code
        type: string
```

### AC2: Tenant Overlay Isolation
- **GIVEN** tenant-specific field pro `acme-corp`
- **WHEN** tenant `example-inc` cte schema
- **THEN** field `custom_sku` neni dostupny

### AC3: Publish Unified Artifact
- **GIVEN** platform model + tenant overlay
- **WHEN** publikuji metamodel
- **THEN** runtime pouziva `platform + tenant overlay` pro aktualni realm

### AC4: Guardrails & Validation
- **GIVEN** tenant overlay zasahuje do `tenant_scope: GLOBAL`
- **THEN** validace vrati error a vyzaduje admin approval

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Rozsirit schema o tenant overlays + parser | 12h | META-001 |
| 2 | Overlay merge + storage strategy (shared/dedicated) | 12h | T1 |
| 3 | Governance/approval flow + migration generator | 10h | T1 |
| 4 | Testy + docs (tenant scope) | 6h | T2, T3 |

---

**Story Owner:** Backend Team  
**Priority:** P3  
**Effort:** 1 týden
