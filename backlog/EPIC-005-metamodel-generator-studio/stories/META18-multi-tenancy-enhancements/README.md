---
id: META-018
epic: EPIC-005-metamodel-generator-studio
title: "Multi-Tenancy Enhancements"
priority: P3
status: todo
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
**Status:** 🟢 **NICE TO HAVE**  
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

---

**Story Owner:** Backend Team  
**Priority:** P3  
**Effort:** 1 týden
