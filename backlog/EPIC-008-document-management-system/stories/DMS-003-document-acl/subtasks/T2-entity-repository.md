---
id: DMS-003-T2
story: DMS-003
title: "Java Entity & Repository (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-003-T2: Java Entity & Repository (1h)

> **Parent Story:** [DMS-003](../README.md)  
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

- `backend/src/main/java/cz/muriel/core/document/DocumentAcl.java`
- `backend/src/main/java/cz/muriel/core/document/DocumentAclRepository.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
package cz.muriel.core.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Document ACL - fine-grained access control.
 * Permissions can be granted to USER, ROLE, or PUBLIC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAcl {
    private UUID id;
    private UUID documentId;
    
    // Principal
    private PrincipalType principalType;
    private String principalId;
    
    // Permissions
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean canDelete;
    private Boolean canShare;
    
    // Expiry
    private Instant expiresAt;
    
    // Audit
    private String grantedBy;
    private Instant grantedAt;
    
    public enum PrincipalType {
        USER,   // Specific user (principalId = user_id)
        ROLE,   // All users with role (principalId = role_name)
        PUBLIC  // Everyone (principalId = null)
    }
    
    /**
     * Check if ACL is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentAclRepository {
    private final JdbcTemplate jdbcTemplate;
    
    private static final RowMapper<DocumentAcl> ROW_MAPPER = (rs, rowNum) -> 
        DocumentAcl.builder()
            .id(UUID.fromString(rs.getString("id")))
            .documentId(UUID.fromString(rs.getString("document_id")))
            .principalType(DocumentAcl.PrincipalType.valueOf(rs.getString("principal_type")))
            .principalId(rs.getString("principal_id"))
            .canRead(rs.getBoolean("can_read"))
            .canWrite(rs.getBoolean("can_write"))
            .canDelete(rs.getBoolean("can_delete"))
            .canShare(rs.getBoolean("can_share"))
            .expiresAt(rs.getTimestamp("expires_at") != null 
                ? rs.getTimestamp("expires_at").toInstant() : null)
            .grantedBy(rs.getString("granted_by"))
            .grantedAt(rs.getTimestamp("granted_at").toInstant())
            .build();
    
    /**
     * Grant permission.
     */
    public DocumentAcl save(DocumentAcl acl) {
        if (acl.getId() == null) {
            acl.setId(UUID.randomUUID());
        }
        
        String sql = """
            INSERT INTO document_acl (
                id, document_id, principal_type, principal_id,
                can_read, can_write, can_delete, can_share,
                expires_at, granted_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (document_id, principal_type, principal_id)
            DO UPDATE SET
                can_read = EXCLUDED.can_read,
                can_write = EXCLUDED.can_write,
                can_delete = EXCLUDED.can_delete,
                can_share = EXCLUDED.can_share,
                expires_at = EXCLUDED.expires_at
            RETURNING *
            """;
        
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER,
            acl.getId(),
            acl.getDocumentId(),

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
- [ ] Committed with message: `feat(DMS-003-T2): Java Entity & Repository (1h)`
