---
id: DMS-002-T3
story: DMS-002
title: "Document Service Extensions (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-002-T3: Document Service Extensions (1h)

> **Parent Story:** [DMS-002](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Document Service Extensions (1h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
// ===== ADD TO EXISTING DocumentService.java =====

@RequiredArgsConstructor
@Service
public class DocumentService {
    private final DocumentLinkRepository linkRepository;
    // ... existing fields ...
    
    /**
     * Link document to entity.
     */
    public DocumentLink linkDocumentToEntity(
        UUID documentId,
        String entityType,
        String entityId,
        DocumentLink.LinkRole role,
        String userId
    ) {
        // 1. Verify document exists and user has access
        Document document = getDocument(documentId, getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        
        // 2. Check if link already exists
        if (linkRepository.exists(documentId, entityType, entityId, role)) {
            throw new IllegalStateException(
                String.format("Document %s already linked to %s/%s with role %s",
                    documentId, entityType, entityId, role)
            );
        }
        
        // 3. Get next display order
        int displayOrder = linkRepository.getNextDisplayOrder(entityType, entityId);
        
        // 4. Create link
        DocumentLink link = DocumentLink.builder()
            .documentId(documentId)
            .entityType(entityType)
            .entityId(entityId)
            .linkRole(role)
            .displayOrder(displayOrder)
            .linkedBy(userId)
            .build();
        
        DocumentLink savedLink = linkRepository.save(link);
        
        log.info("Linked document {} to entity {}/{} with role {} (tenant {})",
            documentId, entityType, entityId, role, getCurrentTenantId());
        
        return savedLink;
    }
    
    /**
     * Unlink document from entity.
     */
    public void unlinkDocumentFromEntity(UUID linkId, String userId) {
        // 1. Verify link exists
        DocumentLink link = linkRepository.findById(linkId)
            .orElseThrow(() -> new NotFoundException("Link not found: " + linkId));
        
        // 2. Verify user has access to document

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
- [ ] Committed with message: `feat(DMS-002-T3): Document Service Extensions (1h)`
