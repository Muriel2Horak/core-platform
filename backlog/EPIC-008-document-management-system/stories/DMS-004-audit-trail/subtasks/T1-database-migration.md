---
id: DMS-004-T1
story: DMS-004
title: "Database Migration (0.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-004-T1: Database Migration (0.5h)

> **Parent Story:** [DMS-004](../README.md)  
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

- `backend/src/main/resources/db/migration/V5__document_audit.sql`

## 🔧 Implementation Details

### Code Example 1 (sql)

```sql
-- ===================================================================
-- V5: Document Audit Trail
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_audit table
CREATE TABLE document_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID,  -- NULL for global actions (e.g., LIST_ALL)
    
    -- Action performed
    action TEXT NOT NULL,
    
    -- User context
    user_id TEXT NOT NULL,
    ip_address TEXT,
    user_agent TEXT,
    
    -- Additional metadata (flexible)
    details JSONB DEFAULT '{}'::jsonb,
    
    -- Timestamp
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Constraints
    CHECK (action IN (
        'UPLOAD', 'DOWNLOAD', 'VIEW', 'EDIT', 'DELETE',
        'LOCK', 'UNLOCK', 'SIGN', 'SHARE', 'UNSHARE',
        'GRANT_PERMISSION', 'REVOKE_PERMISSION',
        'LINK', 'UNLINK', 'ROLLBACK', 'LIST_ALL'
    ))
);

-- 2. Indexes for performance
CREATE INDEX idx_document_audit_document ON document_audit(document_id);
CREATE INDEX idx_document_audit_user ON document_audit(user_id);
CREATE INDEX idx_document_audit_action ON document_audit(action);
CREATE INDEX idx_document_audit_performed_at ON document_audit(performed_at DESC);

-- 3. Partitioning by month (for large audit tables)
-- (Optional - enable if audit grows > 10M rows)
-- CREATE TABLE document_audit_2025_11 PARTITION OF document_audit
-- FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

COMMENT ON TABLE document_audit IS 'Audit trail for ALL document operations (compliance-ready)';
COMMENT ON COLUMN document_audit.details IS 'Flexible metadata (e.g., {"versionNumber": 3, "filename": "contract.pdf"})';
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-004. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-004-T1): Database Migration (0.5h)`
