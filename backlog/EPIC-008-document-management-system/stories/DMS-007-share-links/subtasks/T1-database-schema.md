---
id: DMS-007-T1
story: DMS-007
title: "Database Migration"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-007-T1: Database Migration

> **Parent Story:** [DMS-007](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

```sql

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

**See parent story [`../README.md`](../README.md) for exact file paths.**

## 🔧 Implementation Details

### Code Example 1 (sql)

```sql
CREATE TABLE document_share_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    
    -- Share token (random, public)
    share_token TEXT NOT NULL UNIQUE,
    
    -- Security
    password_hash TEXT,  -- Bcrypt hash (if password-protected)
    
    -- Limits
    max_downloads INTEGER,
    current_downloads INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    
    -- Audit
    created_by TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-007. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-007-T1): Database Migration`
