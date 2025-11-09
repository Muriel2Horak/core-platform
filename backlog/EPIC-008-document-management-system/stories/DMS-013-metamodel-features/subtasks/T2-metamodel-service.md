---
id: DMS-013-T2
story: DMS-013
title: "Document Features Service"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-013-T2: Document Features Service

> **Parent Story:** [DMS-013](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Document Features Service

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/metamodel/features/DocumentFeaturesService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
@Service
public class DocumentFeaturesService {
    
    public DocumentFeatures getDocumentFeatures(String entityType) {
        MetamodelEntity entity = metamodelService.getEntity(entityType);
        return entity.getFeatures().getDocuments();
    }
    
    public void validateUpload(String entityType, String filename, long size, String contentType, String userRole) {
        DocumentFeatures features = getDocumentFeatures(entityType);
        
        if (!features.isEnabled()) {
            throw new ValidationException("Documents not enabled for " + entityType);
        }
        
        // Check file type
        if (!features.getAllowedTypes().contains(contentType)) {
            throw new ValidationException("File type not allowed: " + contentType);
        }
        
        // Check file size
        if (size > features.getMaxFileSize()) {
            throw new ValidationException("File size exceeds limit: " + features.getMaxFileSize());
        }
        
        // Check permissions
        if (!features.getPermissions().getUpload().getRoles().contains(userRole)) {
            throw new ForbiddenException("User role not allowed to upload");
        }
    }
    
    public List<RequiredDocument> getMissingDocuments(String entityType, UUID entityId, String state) {
        DocumentFeatures features = getDocumentFeatures(entityType);
        
        return features.getRequiredDocuments().stream()
            .filter(doc -> doc.isRequired() && doc.getStates().contains(state))
            .filter(doc -> !hasDocument(entityId, doc.getType()))
            .collect(Collectors.toList());
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

This subtask is part of DMS-013. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-013-T2): Document Features Service`
