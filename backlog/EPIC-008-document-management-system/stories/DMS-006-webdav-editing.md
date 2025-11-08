# DMS-006: WebDAV Lock Mechanism (Office Integration)

**Epic:** [EPIC-008 Document Management System](../README.md)  
**Priority:** 🟡 P2 (Advanced Features)  
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~500 LOC)  
**Dependencies:** DMS-001 (Versioning)

---

## 🎯 Story Goal

Implementovat **WebDAV-like lock mechanism** pro Office integration:
- **Lock dokument** při otevření v MS Office/LibreOffice
- **Secure WebDAV URL** s expirací (1h)
- **Auto-save → nová verze** při uložení v Office
- **Prevent concurrent edits** (lock enforcement)

---

## 📊 Flow

```
1. User clicks "Edit in Office"
2. Backend: Lock document (locked_by=user, lock_expires_at=now+1h)
3. Backend: Generate secure WebDAV URL (presigned MinIO URL)
4. Frontend: Open Office: ms-word:ofe|u|<webdav-url>
5. User edits in Office → auto-save
6. Backend: Upload new version on save
7. User closes Office → unlock document
```

---

## 🛠️ Implementation

### Database Migration

```sql
-- Add lock columns to document table
ALTER TABLE document ADD COLUMN locked_by TEXT;
ALTER TABLE document ADD COLUMN locked_at TIMESTAMPTZ;
ALTER TABLE document ADD COLUMN lock_expires_at TIMESTAMPTZ;
CREATE INDEX idx_document_locked_by ON document(locked_by) WHERE locked_by IS NOT NULL;
```

### Lock Service

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

### REST API

```
POST   /api/dms/documents/{id}/lock          → Acquire lock
DELETE /api/dms/documents/{id}/lock          → Release lock
GET    /api/dms/documents/{id}/lock          → Get lock status
POST   /api/dms/documents/{id}/webdav-url    → Get WebDAV URL
```

---

## ✅ Acceptance Criteria

- [ ] Lock document (only 1 user can edit)
- [ ] Unlock document (release lock)
- [ ] Lock expiry (auto-unlock after 1h)
- [ ] WebDAV URL generation (secure, 1h expiry)
- [ ] Office protocol handler: `ms-word:ofe|u|<url>`
- [ ] Prevent concurrent edits (lock check before upload)

---

**Estimated Completion:** 0.5 dne (~500 LOC)
