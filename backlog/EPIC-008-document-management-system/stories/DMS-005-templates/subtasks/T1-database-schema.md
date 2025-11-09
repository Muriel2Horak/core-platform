---
id: DMS-005-T1
story: DMS-005
title: "Database Migration (0.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-005-T1: Database Migration (0.5h)

> **Parent Story:** [DMS-005](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Database Migration (0.5h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/resources/db/migration/V6__document_templates.sql`

## 🔧 Implementation Details

### Code Example 1 (sql)

```sql
-- ===================================================================
-- V6: Document Templates
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_template table
CREATE TABLE document_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    
    -- Template metadata
    name TEXT NOT NULL,
    description TEXT,
    template_type TEXT NOT NULL,  -- CONTRACT | INVOICE | RECEIPT | REPORT | LETTER
    
    -- Template file (stored in DMS)
    template_file_id UUID NOT NULL REFERENCES document(id),
    
    -- Field mappings (JSONB)
    field_mappings JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- Example:
    -- {
    --   "contractNumber": "${entity.contractNumber}",
    --   "supplierName": "${entity.supplierName}",
    --   "now": "${now}",
    --   "user.email": "${user.email}"
    -- }
    
    -- Output format
    output_format TEXT NOT NULL DEFAULT 'PDF',  -- PDF | DOCX | ODT | XLSX
    
    -- Versioning
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit
    created_by TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by TEXT,
    updated_at TIMESTAMPTZ,
    
    UNIQUE (tenant_id, name, version)
);

-- 2. Indexes
CREATE INDEX idx_document_template_tenant ON document_template(tenant_id);
CREATE INDEX idx_document_template_type ON document_template(template_type);
CREATE INDEX idx_document_template_active ON document_template(is_active) WHERE is_active = true;

COMMENT ON TABLE document_template IS 'Document templates for generation (contracts, invoices, reports...)';
COMMENT ON COLUMN document_template.field_mappings IS 'Mapping of placeholders to entity fields (e.g., {"contractNumber": "${entity.contractNumber}"})';
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-005. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-005-T1): Database Migration (0.5h)`
