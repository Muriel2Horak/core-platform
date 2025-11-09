---
id: DMS-002-T1
story: DMS-002
title: "Database Migration (0.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-002-T1: Database Migration (0.5h)

> **Parent Story:** [DMS-002](../README.md)  
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

- `backend/src/main/resources/db/migration/V3__document_links.sql`

## 🔧 Implementation Details

### Code Example 1 (sql)

```sql
-- ===================================================================
-- V3: Document Links (M:N Entity Relationships)
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_link table (M:N vazba)
CREATE TABLE document_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    
    -- Entity reference (polymorphic)
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    
    -- Link metadata
    link_role TEXT NOT NULL DEFAULT 'attachment',
    display_order INTEGER NOT NULL DEFAULT 0,
    
    -- Audit
    linked_by TEXT NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Custom metadata (flexible attributes)
    metadata JSONB DEFAULT '{}'::jsonb,
    
    -- Prevent duplicate links (same doc + entity + role)
    UNIQUE (document_id, entity_type, entity_id, link_role),
    
    -- Validate link_role enum
    CHECK (link_role IN ('primary', 'attachment', 'contract', 'evidence', 'invoice', 'receipt'))
);

-- 2. Indexes for performance
CREATE INDEX idx_document_link_document ON document_link(document_id);
CREATE INDEX idx_document_link_entity ON document_link(entity_type, entity_id);
CREATE INDEX idx_document_link_role ON document_link(link_role);
CREATE INDEX idx_document_link_display_order ON document_link(entity_type, entity_id, display_order);

-- 3. Migrate existing document.entity_type/entity_id to document_link
INSERT INTO document_link (document_id, entity_type, entity_id, link_role, linked_by, linked_at)
SELECT 
    id AS document_id,
    entity_type,
    entity_id,
    'primary' AS link_role,  -- Existing docs = primary
    uploaded_by AS linked_by,
    uploaded_at AS linked_at
FROM document
WHERE entity_type IS NOT NULL AND entity_id IS NOT NULL;

-- 4. Drop old columns (no longer needed)
ALTER TABLE document DROP COLUMN entity_type;
ALTER TABLE document DROP COLUMN entity_id;

-- 5. Function to get next display order
CREATE OR REPLACE FUNCTION get_next_display_order(
    p_entity_type TEXT,
    p_entity_id TEXT
) RETURNS INTEGER AS $$

// ... (see parent story for complete code)
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-002. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-002-T1): Database Migration (0.5h)`
