---
id: DMS-002-T2
story: DMS-002
title: "Java Entity & Repository (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-002-T2: Java Entity & Repository (1h)

> **Parent Story:** [DMS-002](../README.md)  
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

- `backend/src/main/java/cz/muriel/core/document/DocumentLink.java`
- `backend/src/main/java/cz/muriel/core/document/DocumentLinkRepository.java`

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
 * M:N link between Document and Entity (Case, UserProfile, Invoice...).
 * Allows 1 document to be attached to multiple entities with different roles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLink {
    private UUID id;
    private UUID documentId;
    
    // Polymorphic entity reference
    private String entityType;  // Case, UserProfile, Invoice...
    private String entityId;
    
    // Link metadata
    private LinkRole linkRole;
    private Integer displayOrder;
    
    // Audit
    private String linkedBy;
    private Instant linkedAt;
    
    // Custom metadata (flexible)
    private JsonNode metadata;
    
    public enum LinkRole {
        PRIMARY,      // Main document (e.g., primary contract)
        ATTACHMENT,   // Generic attachment
        CONTRACT,     // Contract document
        EVIDENCE,     // Evidence/proof document
        INVOICE,      // Invoice document
        RECEIPT       // Payment receipt
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentLinkRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    private final RowMapper<DocumentLink> ROW_MAPPER = (rs, rowNum) -> {
        try {
            return DocumentLink.builder()
                .id(UUID.fromString(rs.getString("id")))
                .documentId(UUID.fromString(rs.getString("document_id")))
                .entityType(rs.getString("entity_type"))
                .entityId(rs.getString("entity_id"))
                .linkRole(DocumentLink.LinkRole.valueOf(rs.getString("link_role").toUpperCase()))
                .displayOrder(rs.getInt("display_order"))
                .linkedBy(rs.getString("linked_by"))
                .linkedAt(rs.getTimestamp("linked_at").toInstant())
                .metadata(rs.getString("metadata") != null 
                    ? objectMapper.readTree(rs.getString("metadata")) 
                    : null)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map DocumentLink", e);
        }
    };
    
    /**
     * Create link between document and entity.
     */
    public DocumentLink save(DocumentLink link) {
        if (link.getId() == null) {
            link.setId(UUID.randomUUID());
        }
        
        String sql = """
            INSERT INTO document_link (
                id, document_id, entity_type, entity_id,
                link_role, display_order, linked_by, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (document_id, entity_type, entity_id, link_role)
            DO UPDATE SET
                display_order = EXCLUDED.display_order,
                metadata = EXCLUDED.metadata
            RETURNING *
            """;
        

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

This subtask is part of DMS-002. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-002-T2): Java Entity & Repository (1h)`
