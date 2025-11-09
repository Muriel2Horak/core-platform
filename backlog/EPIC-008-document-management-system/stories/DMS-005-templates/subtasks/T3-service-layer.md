---
id: DMS-005-T3
story: DMS-005
title: "Template Service (3h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-005-T3: Template Service (3h)

> **Parent Story:** [DMS-005](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Template Service (3h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/document/DocumentTemplateService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
package cz.muriel.core.document;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.openxml4j.opc.OPCPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTemplateService {
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    /**
     * Generate document from template.
     */
    public UUID generateDocument(
        UUID templateId,
        Map<String, Object> entityData,
        Map<String, Object> userData,
        String userId
    ) throws IOException {
        // 1. Load template
        DocumentTemplate template = getTemplate(templateId);
        
        // 2. Download template file
        byte[] templateBytes = documentService.downloadDocumentBytes(template.getTemplateFileId());
        
        // 3. Process template (replace placeholders)
        byte[] processedBytes = processTemplate(
            templateBytes,
            template.getFieldMappings(),
            entityData,
            userData
        );
        
        // 4. Convert to output format (if needed)
        if (template.getOutputFormat() == DocumentTemplate.OutputFormat.PDF) {
            processedBytes = convertToPdf(processedBytes);
        }
        
        // 5. Upload generated document
        String generatedFilename = String.format(
            "%s-generated-%s.%s",
            template.getName(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
            template.getOutputFormat().name().toLowerCase()

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

This subtask is part of DMS-005. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-005-T3): Template Service (3h)`
