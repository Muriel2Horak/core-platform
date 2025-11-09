---
id: DMS-006-T2
story: DMS-006
title: "Lock Service"
status: todo
assignee: ""
estimate: "1 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-006-T2: Lock Service

> **Parent Story:** [DMS-006](../README.md)  
> **Status:** todo | **Estimate:** 1 hours

## 🎯 Subtask Goal

```java

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

### Code Example 1 (java)

```java
public void lockDocument(UUID documentId, String userId) {
    jdbcTemplate.update(
        "UPDATE document SET locked_by = ?, locked_at = now(), lock_expires_at = now() + interval '1 hour' WHERE id = ?",
        userId, documentId
    );
}

public void unlockDocument(UUID documentId, String userId) {
    jdbcTemplate.update(
        "UPDATE document SET locked_by = NULL, locked_at = NULL, lock_expires_at = NULL WHERE id = ? AND locked_by = ?",
        documentId, userId
    );
}

public String getWebDavUrl(UUID documentId) {
    // Return presigned MinIO URL (1h expiry)
    return minioClient.getPresignedObjectUrl(...);
}
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-006. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-006-T2): Lock Service`
