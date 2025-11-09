---
id: DMS-007-T2
story: DMS-007
title: "Share Service"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-007-T2: Share Service

> **Parent Story:** [DMS-007](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

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
public String createShareLink(UUID documentId, String password, Integer maxDownloads, Instant expiresAt) {
    String shareToken = UUID.randomUUID().toString();
    String passwordHash = password != null ? bcryptEncoder.encode(password) : null;
    
    jdbcTemplate.update(
        "INSERT INTO document_share_link (document_id, share_token, password_hash, max_downloads, expires_at, created_by) VALUES (?, ?, ?, ?, ?, ?)",
        documentId, shareToken, passwordHash, maxDownloads, expiresAt, userId
    );
    
    return "https://core-platform.local/share/" + shareToken;
}

public byte[] downloadViaShareLink(String token, String password) {
    // 1. Validate share link (exists, not expired, downloads < max)
    // 2. Check password (if required)
    // 3. Increment current_downloads
    // 4. Download file
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

This subtask is part of DMS-007. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-007-T2): Share Service`
