# DMS-007: Public Share Links

**Epic:** [EPIC-008 Document Management System](../README.md)  
**Priority:** 🟡 P2 (Advanced Features)  
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~400 LOC)  
**Dependencies:** DMS-001 (Versioning)

---

## 🎯 Story Goal

Implementovat **public sharing** dokumentů:
- **Generate share link** s random token
- **Password protection** (optional)
- **Max downloads** limit
- **Expires_at** enforcement

---

## 🛠️ Implementation

### Database Migration

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

### Share Service

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

### REST API

```
POST /api/dms/documents/{id}/share                → Create share link
GET  /api/dms/share/{token}                       → Get share info (public endpoint)
GET  /api/dms/share/{token}/download              → Download via share link
```

---

## ✅ Acceptance Criteria

- [ ] Generate public share link
- [ ] Password protection (optional)
- [ ] Max downloads limit enforcement
- [ ] Expires_at enforcement
- [ ] Download count tracking
- [ ] Public download page: `/public/share/{token}`

---

**Estimated Completion:** 0.5 dne (~400 LOC)
