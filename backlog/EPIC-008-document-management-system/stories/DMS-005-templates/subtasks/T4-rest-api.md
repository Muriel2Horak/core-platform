---
id: DMS-005-T4
story: DMS-005
title: "REST API Endpoints (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-005-T4: REST API Endpoints (1h)

> **Parent Story:** [DMS-005](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

REST API Endpoints (1h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/document/DocumentTemplateController.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
package cz.muriel.core.document;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/dms/templates")
@RequiredArgsConstructor
public class DocumentTemplateController {
    private final DocumentTemplateService templateService;
    
    /**
     * List templates for tenant.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentTemplate>> listTemplates(
        @RequestParam(required = false) DocumentTemplate.TemplateType type
    ) {
        List<DocumentTemplate> templates = templateService.listTemplates(type);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Get template by ID.
     */
    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<DocumentTemplate> getTemplate(
        @PathVariable UUID templateId
    ) {
        DocumentTemplate template = templateService.getTemplate(templateId);
        return ResponseEntity.ok(template);
    }
    
    /**
     * Create template.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentTemplate> createTemplate(
        @RequestBody CreateTemplateRequest request,
        Principal principal
    ) {
        DocumentTemplate template = templateService.createTemplate(
            request.getName(),
            request.getDescription(),
            request.getTemplateType(),
            request.getTemplateFileId(),
            request.getFieldMappings(),
            request.getOutputFormat(),
            principal.getName()
        );
        

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
- [ ] Committed with message: `feat(DMS-005-T4): REST API Endpoints (1h)`
