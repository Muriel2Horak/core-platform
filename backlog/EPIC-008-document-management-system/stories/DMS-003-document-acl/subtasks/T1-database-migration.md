---
id: DMS-003-T1
story: DMS-003
title: "Database Migration (0.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-003-T1: Database Migration (0.5h)

> **Parent Story:** [DMS-003](../README.md)  
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

- `backend/src/main/resources/db/migration/V4__document_acl.sql`

## 🔧 Implementation Details

### Code Example 1 (sql)

```sql
-- ===================================================================
-- V4: Document ACL (Access Control List)
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_acl table
CREATE TABLE document_acl (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    
    -- Principal (USER, ROLE, PUBLIC)
    principal_type TEXT NOT NULL,
    principal_id TEXT,  -- user_id for USER, role_name for ROLE, NULL for PUBLIC
    
    -- Permissions (granular)
    can_read BOOLEAN NOT NULL DEFAULT true,
    can_write BOOLEAN NOT NULL DEFAULT false,
    can_delete BOOLEAN NOT NULL DEFAULT false,
    can_share BOOLEAN NOT NULL DEFAULT false,
    
    -- Expiry
    expires_at TIMESTAMPTZ,
    
    -- Audit
    granted_by TEXT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Constraints
    CHECK (principal_type IN ('USER', 'ROLE', 'PUBLIC')),
    CHECK (principal_type = 'PUBLIC' OR principal_id IS NOT NULL),
    UNIQUE (document_id, principal_type, principal_id)
);

-- 2. Indexes
CREATE INDEX idx_document_acl_document ON document_acl(document_id);
CREATE INDEX idx_document_acl_principal ON document_acl(principal_type, principal_id);
CREATE INDEX idx_document_acl_expires ON document_acl(expires_at) WHERE expires_at IS NOT NULL;

-- 3. Function to check permission
CREATE OR REPLACE FUNCTION has_document_permission(
    p_document_id UUID,
    p_user_id TEXT,
    p_user_roles TEXT[],
    p_permission TEXT  -- 'read' | 'write' | 'delete' | 'share'
) RETURNS BOOLEAN AS $$
DECLARE
    v_has_permission BOOLEAN;
BEGIN
    -- Check if user has permission via:
    -- 1. Direct USER grant
    -- 2. ROLE grant (any of user's roles)
    -- 3. PUBLIC grant
    
    SELECT EXISTS (
        SELECT 1 FROM document_acl
        WHERE document_id = p_document_id
          AND (expires_at IS NULL OR expires_at > now())
          AND (
              -- Direct USER permission

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

This subtask is part of DMS-003. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-003-T1): Database Migration (0.5h)`
