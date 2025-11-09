---
id: DMS-004-T3
story: DMS-004
title: "Audit Service (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-004-T3: Audit Service (1h)

> **Parent Story:** [DMS-004](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Audit Service (1h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/document/DocumentAuditService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
package cz.muriel.core.document;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAuditService {
    private final DocumentAuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Log audit event.
     */
    public void log(
        DocumentAudit.AuditAction action,
        UUID documentId,
        String userId,
        HttpServletRequest request,
        ObjectNode details
    ) {
        DocumentAudit audit = DocumentAudit.builder()
            .documentId(documentId)
            .action(action)
            .userId(userId)
            .ipAddress(getClientIp(request))
            .userAgent(request.getHeader("User-Agent"))
            .details(details)
            .build();
        
        auditRepository.save(audit);
        
        log.debug("Audit: {} - document={}, user={}", action, documentId, userId);
    }
    
    /**
     * Convenience methods for common actions.
     */
    public void logUpload(UUID documentId, String userId, HttpServletRequest request, String filename, long sizeBytes) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("filename", filename);
        details.put("sizeBytes", sizeBytes);
        log(DocumentAudit.AuditAction.UPLOAD, documentId, userId, request, details);
    }
    
    public void logDownload(UUID documentId, String userId, HttpServletRequest request, int versionNumber) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("versionNumber", versionNumber);
        log(DocumentAudit.AuditAction.DOWNLOAD, documentId, userId, request, details);
    }
    
    public void logDelete(UUID documentId, String userId, HttpServletRequest request) {
        log(DocumentAudit.AuditAction.DELETE, documentId, userId, request, null);

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
- [ ] Committed with message: `feat(DMS-004-T3): Audit Service (1h)`
