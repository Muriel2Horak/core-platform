---
id: DMS-004-T4
story: DMS-004
title: "Integrate Audit Logging (0.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-004-T4: Integrate Audit Logging (0.5h)

> **Parent Story:** [DMS-004](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Integrate Audit Logging (0.5h)

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
- [ ] Committed with message: `feat(DMS-004-T4): Integrate Audit Logging (0.5h)`
