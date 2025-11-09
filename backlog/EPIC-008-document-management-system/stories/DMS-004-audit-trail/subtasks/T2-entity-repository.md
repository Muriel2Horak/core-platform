---
id: DMS-004-T2
story: DMS-004
title: "Java Entity & Repository (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-004-T2: Java Entity & Repository (1h)

> **Parent Story:** [DMS-004](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Java Entity & Repository (1h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/document/DocumentAudit.java`
- `backend/src/main/java/cz/muriel/core/document/DocumentAuditRepository.java`

## 🔧 Implementation Details

### Code Example 1 (java)

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

### Code Example 2 (java)

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

This subtask is part of DMS-004. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-004-T2): Java Entity & Repository (1h)`
