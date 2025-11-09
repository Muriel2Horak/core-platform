---
id: DMS-003-T4
story: DMS-003
title: "REST API Endpoints (0.5h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-003-T4: REST API Endpoints (0.5h)

> **Parent Story:** [DMS-003](../README.md)  
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
    
    // ===== ACL ENDPOINTS =====
    
    /**
     * Grant permission to user/role.
     */
    @PostMapping("/{documentId}/acl")
    @PreAuthorize("hasAuthority('DOCUMENT_ADMIN')")
    public ResponseEntity<DocumentAcl> grantPermission(
        @PathVariable UUID documentId,
        @RequestBody AclRequest request,
        Principal principal
    ) {
        DocumentAcl acl = documentService.grantPermission(
            documentId,
            request.getPrincipalType(),
            request.getPrincipalId(),
            request.isCanRead(),
            request.isCanWrite(),
            request.isCanDelete(),
            request.isCanShare(),
            request.getExpiresAt(),
            principal.getName()
        );
        
        return ResponseEntity.ok(acl);
    }
    
    /**
     * List ACL entries for document.
     */
    @GetMapping("/{documentId}/acl")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentAcl>> listPermissions(
        @PathVariable UUID documentId
    ) {
        List<DocumentAcl> acls = documentService.listPermissions(documentId);
        return ResponseEntity.ok(acls);
    }
    
    /**
     * Revoke permission.
     */
    @DeleteMapping("/acl/{aclId}")
    @PreAuthorize("hasAuthority('DOCUMENT_ADMIN')")
    public ResponseEntity<Void> revokePermission(
        @PathVariable UUID aclId,
        Principal principal
    ) {
        documentService.revokePermission(aclId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}


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

This subtask is part of DMS-003. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-003-T4): REST API Endpoints (0.5h)`
