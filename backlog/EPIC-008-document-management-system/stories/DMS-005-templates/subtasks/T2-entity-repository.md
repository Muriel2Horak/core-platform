---
id: DMS-005-T2
story: DMS-005
title: "Template Entity & Repository (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-005-T2: Template Entity & Repository (1h)

> **Parent Story:** [DMS-005](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Template Entity & Repository (1h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/document/DocumentTemplate.java`

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate {
    private UUID id;
    private String tenantId;
    
    // Metadata
    private String name;
    private String description;
    private TemplateType templateType;
    
    // Template file
    private UUID templateFileId;
    
    // Field mappings
    private JsonNode fieldMappings;
    
    // Output
    private OutputFormat outputFormat;
    
    // Versioning
    private Integer version;
    private Boolean isActive;
    
    // Audit
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;
    
    public enum TemplateType {
        CONTRACT,
        INVOICE,
        RECEIPT,
        REPORT,
        LETTER
    }
    
    public enum OutputFormat {
        PDF,
        DOCX,
        ODT,
        XLSX
    }
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

This subtask is part of DMS-005. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-005-T2): Template Entity & Repository (1h)`
