---
id: DMS-012-T1
story: DMS-012
title: "Signing Request Entity"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-012-T1: Signing Request Entity

> **Parent Story:** [DMS-012](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Signing Request Entity

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
CREATE TABLE signing_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id UUID NOT NULL REFERENCES document_version(id),
    signing_token VARCHAR(64) UNIQUE NOT NULL,
    signer_name VARCHAR(255) NOT NULL,
    signer_email VARCHAR(255) NOT NULL,
    signer_personal_id VARCHAR(20),
    signing_method VARCHAR(20) NOT NULL, -- BANKID, EID, MANUAL
    status VARCHAR(20) NOT NULL, -- PENDING, SIGNED, EXPIRED, CANCELLED
    signature_data TEXT,
    certificate_data TEXT,
    signed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_signing_request_token ON signing_request(signing_token);
CREATE INDEX idx_signing_request_status ON signing_request(status);
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-012. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-012-T1): Signing Request Entity`
