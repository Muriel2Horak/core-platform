---
id: DMS-002-T4
story: DMS-002
title: "REST API Endpoints (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-002-T4: REST API Endpoints (1h)

> **Parent Story:** [DMS-002](../README.md)  
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

- `backend/src/main/java/cz/muriel/core/document/DocumentController.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
// ===== ADD TO EXISTING DocumentController.java =====

@RestController
@RequestMapping("/api/dms/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    // ===== DOCUMENT LINK ENDPOINTS =====
    
    /**
     * Link document to entity.
     */
    @PostMapping("/{documentId}/links")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentLink> linkToEntity(
        @PathVariable UUID documentId,
        @RequestBody LinkRequest request,
        Principal principal
    ) {
        DocumentLink link = documentService.linkDocumentToEntity(
            documentId,
            request.getEntityType(),
            request.getEntityId(),
            request.getRole(),
            principal.getName()
        );
        
        return ResponseEntity.ok(link);
    }
    
    /**
     * Unlink document from entity.
     */
    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<Void> unlinkFromEntity(
        @PathVariable UUID linkId,
        Principal principal
    ) {
        documentService.unlinkDocumentFromEntity(linkId, principal.getName());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * List all documents for entity (ordered by display_order).
     */
    @GetMapping("/entities/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentWithLink>> listDocumentsForEntity(
        @PathVariable String entityType,
        @PathVariable String entityId
    ) {
        List<DocumentWithLink> documents = documentService.listDocumentsForEntity(
            entityType, entityId
        );
        return ResponseEntity.ok(documents);
    }
    
    /**
     * List all entities linked to document.

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
- [ ] Committed with message: `feat(DMS-002-T4): REST API Endpoints (1h)`
