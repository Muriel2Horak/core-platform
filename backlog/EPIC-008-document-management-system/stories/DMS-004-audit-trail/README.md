# DMS-004: Document Audit Trail

**Epic:** [EPIC-008 Document Management System](../README.md)  
**Priority:** 🔴 P1 (KRITICKÉ - Compliance & Security)  
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~300 LOC)  
**Dependencies:** DMS-001 (Versioning)

---

## 🎯 Story Goal

Implementovat **kompletní audit trail** pro dokumenty:
- **All operations logged** (UPLOAD, DOWNLOAD, VIEW, EDIT, DELETE, LOCK, UNLOCK, SIGN, SHARE)
- **IP address + user agent** tracking
- **Compliance-ready** export (CSV, JSON)
- **Filtrace** (by document, user, action, date range)

---

## 📊 Current State vs. Target

### ❌ SOUČASNÝ STAV

**Problém:** Žádný audit trail pro DMS operace.
- Nevíš **kdo** stáhl dokument
- Nevíš **kdy** byl dokument smazán
- Compliance audit = FAIL

### ✅ TARGET STAV

```sql
document_audit (
  id UUID PRIMARY KEY,
  document_id UUID,  -- NULL pro global actions (list all)
  action TEXT NOT NULL,  -- UPLOAD, DOWNLOAD, VIEW, EDIT, DELETE...
  user_id TEXT NOT NULL,
  ip_address TEXT,
  user_agent TEXT,
  details JSONB,  -- Flexible metadata (e.g., {"versionNumber": 3})
  performed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## 🛠️ Implementation Tasks

### Task 1: Database Migration (0.5h)

**File:** `backend/src/main/resources/db/migration/V5__document_audit.sql`

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

---

### Task 2: Java Entity & Repository (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentAudit.java`

```java
package cz.muriel.core.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Document audit trail entry.
 * Logs ALL document operations for compliance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAudit {
    private UUID id;
    private UUID documentId;
    
    // Action
    private AuditAction action;
    
    // User context
    private String userId;
    private String ipAddress;
    private String userAgent;
    
    // Metadata (flexible)
    private JsonNode details;
    
    // Timestamp
    private Instant performedAt;
    
    public enum AuditAction {
        UPLOAD,
        DOWNLOAD,
        VIEW,
        EDIT,
        DELETE,
        LOCK,
        UNLOCK,
        SIGN,
        SHARE,
        UNSHARE,
        GRANT_PERMISSION,
        REVOKE_PERMISSION,
        LINK,
        UNLINK,
        ROLLBACK,
        LIST_ALL
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentAuditRepository.java`

```java
package cz.muriel.core.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentAuditRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    private final RowMapper<DocumentAudit> ROW_MAPPER = (rs, rowNum) -> {
        try {
            return DocumentAudit.builder()
                .id(UUID.fromString(rs.getString("id")))
                .documentId(rs.getString("document_id") != null 
                    ? UUID.fromString(rs.getString("document_id")) : null)
                .action(DocumentAudit.AuditAction.valueOf(rs.getString("action")))
                .userId(rs.getString("user_id"))
                .ipAddress(rs.getString("ip_address"))
                .userAgent(rs.getString("user_agent"))
                .details(rs.getString("details") != null 
                    ? objectMapper.readTree(rs.getString("details")) : null)
                .performedAt(rs.getTimestamp("performed_at").toInstant())
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map DocumentAudit", e);
        }
    };
    
    /**
     * Log audit event.
     */
    public DocumentAudit save(DocumentAudit audit) {
        if (audit.getId() == null) {
            audit.setId(UUID.randomUUID());
        }
        
        String sql = """
            INSERT INTO document_audit (
                id, document_id, action, user_id, ip_address, user_agent, details
            ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
            RETURNING *
            """;
        
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER,
            audit.getId(),
            audit.getDocumentId(),
            audit.getAction().name(),
            audit.getUserId(),
            audit.getIpAddress(),
            audit.getUserAgent(),
            audit.getDetails() != null ? audit.getDetails().toString() : "{}"
        );
    }
    
    /**
     * Get audit log for document.
     */
    public List<DocumentAudit> findByDocument(UUID documentId) {
        String sql = """
            SELECT * FROM document_audit
            WHERE document_id = ?
            ORDER BY performed_at DESC
            LIMIT 1000
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, documentId);
    }
    
    /**
     * Get audit log for user.
     */
    public List<DocumentAudit> findByUser(String userId, Instant from, Instant to) {
        String sql = """
            SELECT * FROM document_audit
            WHERE user_id = ?
              AND performed_at >= ?
              AND performed_at < ?
            ORDER BY performed_at DESC
            LIMIT 1000
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, userId, from, to);
    }
    
    /**
     * Get global audit log (admin only).
     */
    public List<DocumentAudit> findAll(Instant from, Instant to, String action) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM document_audit
            WHERE performed_at >= ?
              AND performed_at < ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(from);
        params.add(to);
        
        if (action != null) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        
        sql.append(" ORDER BY performed_at DESC LIMIT 10000");
        
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }
}
```

---

### Task 3: Audit Service (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentAuditService.java`

```java
package cz.muriel.core.document;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAuditService {
    private final DocumentAuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Log audit event.
     */
    public void log(
        DocumentAudit.AuditAction action,
        UUID documentId,
        String userId,
        HttpServletRequest request,
        ObjectNode details
    ) {
        DocumentAudit audit = DocumentAudit.builder()
            .documentId(documentId)
            .action(action)
            .userId(userId)
            .ipAddress(getClientIp(request))
            .userAgent(request.getHeader("User-Agent"))
            .details(details)
            .build();
        
        auditRepository.save(audit);
        
        log.debug("Audit: {} - document={}, user={}", action, documentId, userId);
    }
    
    /**
     * Convenience methods for common actions.
     */
    public void logUpload(UUID documentId, String userId, HttpServletRequest request, String filename, long sizeBytes) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("filename", filename);
        details.put("sizeBytes", sizeBytes);
        log(DocumentAudit.AuditAction.UPLOAD, documentId, userId, request, details);
    }
    
    public void logDownload(UUID documentId, String userId, HttpServletRequest request, int versionNumber) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("versionNumber", versionNumber);
        log(DocumentAudit.AuditAction.DOWNLOAD, documentId, userId, request, details);
    }
    
    public void logDelete(UUID documentId, String userId, HttpServletRequest request) {
        log(DocumentAudit.AuditAction.DELETE, documentId, userId, request, null);
    }
    
    public void logRollback(UUID documentId, String userId, HttpServletRequest request, int targetVersion) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("targetVersion", targetVersion);
        log(DocumentAudit.AuditAction.ROLLBACK, documentId, userId, request, details);
    }
    
    public void logSign(UUID documentId, String userId, HttpServletRequest request, String signatureMethod) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("signatureMethod", signatureMethod);
        log(DocumentAudit.AuditAction.SIGN, documentId, userId, request, details);
    }
    
    /**
     * Get client IP address (supports proxies).
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

---

### Task 4: Integrate Audit Logging (0.5h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

```java
// ===== ADD TO EXISTING DocumentService.java =====

@RequiredArgsConstructor
@Service
public class DocumentService {
    private final DocumentAuditService auditService;
    // ... existing fields ...
    
    /**
     * Upload document (with audit).
     */
    public DocumentVersion uploadDocument(
        MultipartFile file,
        String userId,
        HttpServletRequest request
    ) throws IOException {
        // ... existing upload logic ...
        
        // Audit log
        auditService.logUpload(
            savedVersion.getDocumentId(),
            userId,
            request,
            file.getOriginalFilename(),
            file.getSize()
        );
        
        return savedVersion;
    }
    
    /**
     * Download document (with audit).
     */
    public String getDownloadUrl(UUID documentId, String userId, HttpServletRequest request) {
        // ... existing logic ...
        
        // Audit log
        auditService.logDownload(documentId, userId, request, 1);
        
        return downloadUrl;
    }
    
    /**
     * Delete document (with audit).
     */
    public void deleteDocument(UUID documentId, String userId, HttpServletRequest request) {
        // ... existing logic ...
        
        // Audit log
        auditService.logDelete(documentId, userId, request);
    }
}
```

---

### Task 5: REST API Endpoints (0.5h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentController.java`

```java
// ===== ADD TO EXISTING DocumentController.java =====

@RestController
@RequestMapping("/api/dms/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentAuditRepository auditRepository;
    
    // ===== AUDIT ENDPOINTS =====
    
    /**
     * Get audit log for document.
     */
    @GetMapping("/{documentId}/audit")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentAudit>> getDocumentAuditLog(
        @PathVariable UUID documentId
    ) {
        List<DocumentAudit> auditLog = auditRepository.findByDocument(documentId);
        return ResponseEntity.ok(auditLog);
    }
    
    /**
     * Get global audit log (admin only).
     */
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('DOCUMENT_ADMIN')")
    public ResponseEntity<List<DocumentAudit>> getGlobalAuditLog(
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(required = false) String action
    ) {
        List<DocumentAudit> auditLog = auditRepository.findAll(from, to, action);
        return ResponseEntity.ok(auditLog);
    }
    
    /**
     * Export audit log as CSV.
     */
    @GetMapping("/audit/export")
    @PreAuthorize("hasAuthority('DOCUMENT_ADMIN')")
    public ResponseEntity<String> exportAuditLog(
        @RequestParam Instant from,
        @RequestParam Instant to
    ) {
        List<DocumentAudit> auditLog = auditRepository.findAll(from, to, null);
        
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Action,User,IP,Document,Details\n");
        
        for (DocumentAudit audit : auditLog) {
            csv.append(audit.getPerformedAt()).append(",")
               .append(audit.getAction()).append(",")
               .append(audit.getUserId()).append(",")
               .append(audit.getIpAddress()).append(",")
               .append(audit.getDocumentId()).append(",")
               .append(audit.getDetails()).append("\n");
        }
        
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=audit.csv")
            .body(csv.toString());
    }
}
```

---

## ✅ Acceptance Criteria

- [ ] **Database Migration V5** runs successfully
  - `document_audit` table created
  - Indexes for performance

- [ ] **All Operations Logged**
  - Upload, Download, View, Edit, Delete, Lock, Unlock, Sign, Share

- [ ] **IP Address Tracking**
  - Supports X-Forwarded-For (proxy)
  - User-Agent captured

- [ ] **Audit Endpoints** work
  - `GET /api/dms/documents/{id}/audit` returns log for document
  - `GET /api/dms/audit?from=X&to=Y&action=DOWNLOAD` returns global log
  - `GET /api/dms/audit/export` returns CSV

- [ ] **Compliance Ready**
  - Can answer "Who downloaded document X?"
  - Can answer "What did user Y do today?"
  - Export to CSV for auditors

---

## 📦 Deliverables

1. ✅ Database migration: `V5__document_audit.sql`
2. ✅ Java entities: `DocumentAudit.java`, `DocumentAuditRepository.java`
3. ✅ Audit service: `DocumentAuditService.java`
4. ✅ Integration: Audit logging in all DocumentService methods
5. ✅ REST API: 3 endpoints (document log, global log, CSV export)

---

**Ready for Implementation** ✅  
**Estimated Completion:** 0.5 dne (~300 LOC)
