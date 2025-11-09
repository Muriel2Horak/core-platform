---
id: DMS-004-T5
story: DMS-004
title: "REST API Endpoints (0.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-004-T5: REST API Endpoints (0.5h)

> **Parent Story:** [DMS-004](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

REST API Endpoints (0.5h)

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
    private final DocumentAuditRepository auditRepository;
    
    // ===== AUDIT ENDPOINTS =====
    
    /**
     * Get audit log for document.
     */
    @GetMapping("/{documentId}/audit")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentAudit>> getDocumentAuditLog(
        @PathVariable UUID documentId
    ) {
        List<DocumentAudit> auditLog = auditRepository.findByDocument(documentId);
        return ResponseEntity.ok(auditLog);
    }
    
    /**
     * Get global audit log (admin only).
     */
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('DOCUMENT_ADMIN')")
    public ResponseEntity<List<DocumentAudit>> getGlobalAuditLog(
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(required = false) String action
    ) {
        List<DocumentAudit> auditLog = auditRepository.findAll(from, to, action);
        return ResponseEntity.ok(auditLog);
    }
    
    /**
     * Export audit log as CSV.
     */
    @GetMapping("/audit/export")
    @PreAuthorize("hasAuthority('DOCUMENT_ADMIN')")
    public ResponseEntity<String> exportAuditLog(
        @RequestParam Instant from,
        @RequestParam Instant to
    ) {
        List<DocumentAudit> auditLog = auditRepository.findAll(from, to, null);
        
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Action,User,IP,Document,Details\n");
        
        for (DocumentAudit audit : auditLog) {
            csv.append(audit.getPerformedAt()).append(",")
               .append(audit.getAction()).append(",")
               .append(audit.getUserId()).append(",")
               .append(audit.getIpAddress()).append(",")
               .append(audit.getDocumentId()).append(",")
               .append(audit.getDetails()).append("\n");
        }
        
        return ResponseEntity.ok()

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
- [ ] Committed with message: `feat(DMS-004-T5): REST API Endpoints (0.5h)`
