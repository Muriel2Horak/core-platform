---
id: DMS-003-T3
story: DMS-003
title: "Permission Check Middleware (1h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-003-T3: Permission Check Middleware (1h)

> **Parent Story:** [DMS-003](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Permission Check Middleware (1h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/document/DocumentPermissionService.java`
- `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
package cz.muriel.core.document;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPermissionService {
    private final DocumentAclRepository aclRepository;
    
    /**
     * Check if user can read document.
     */
    public void requireRead(UUID documentId, String userId, List<String> userRoles) {
        if (!aclRepository.hasPermission(documentId, userId, userRoles, "read")) {
            throw new AccessDeniedException(
                String.format("User %s does not have READ permission for document %s", userId, documentId)
            );
        }
    }
    
    /**
     * Check if user can write document.
     */
    public void requireWrite(UUID documentId, String userId, List<String> userRoles) {
        if (!aclRepository.hasPermission(documentId, userId, userRoles, "write")) {
            throw new AccessDeniedException(
                String.format("User %s does not have WRITE permission for document %s", userId, documentId)
            );
        }
    }
    
    /**
     * Check if user can delete document.
     */
    public void requireDelete(UUID documentId, String userId, List<String> userRoles) {
        if (!aclRepository.hasPermission(documentId, userId, userRoles, "delete")) {
            throw new AccessDeniedException(
                String.format("User %s does not have DELETE permission for document %s", userId, documentId)
            );
        }
    }
    
    /**
     * Check if user can share document.
     */
    public void requireShare(UUID documentId, String userId, List<String> userRoles) {
        if (!aclRepository.hasPermission(documentId, userId, userRoles, "share")) {
            throw new AccessDeniedException(
                String.format("User %s does not have SHARE permission for document %s", userId, documentId)
            );
        }
    }
}
```

### Code Example 2 (java)

```java
// ===== ADD TO EXISTING DocumentService.java =====

@RequiredArgsConstructor
@Service
public class DocumentService {
    private final DocumentPermissionService permissionService;
    private final DocumentAclRepository aclRepository;
    // ... existing fields ...
    
    /**
     * Grant permission to user/role.
     */
    public DocumentAcl grantPermission(
        UUID documentId,
        DocumentAcl.PrincipalType principalType,
        String principalId,
        boolean canRead,
        boolean canWrite,
        boolean canDelete,
        boolean canShare,
        Instant expiresAt,
        String grantedBy
    ) {
        // 1. Verify document exists
        Document document = getDocument(documentId, getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found"));
        
        // 2. Create ACL entry
        DocumentAcl acl = DocumentAcl.builder()
            .documentId(documentId)
            .principalType(principalType)
            .principalId(principalId)
            .canRead(canRead)
            .canWrite(canWrite)
            .canDelete(canDelete)
            .canShare(canShare)
            .expiresAt(expiresAt)
            .grantedBy(grantedBy)
            .build();
        
        DocumentAcl savedAcl = aclRepository.save(acl);
        
        log.info("Granted permissions to {} {} for document {} (tenant {})",
            principalType, principalId, documentId, getCurrentTenantId());
        
        return savedAcl;
    }
    
    /**
     * Revoke permission.
     */
    public void revokePermission(UUID aclId, String userId) {
        aclRepository.delete(aclId);
        log.info("Revoked permission {} by user {}", aclId, userId);
    }
    
    /**
     * List ACL entries for document.
     */
    public List<DocumentAcl> listPermissions(UUID documentId) {

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
- [ ] Committed with message: `feat(DMS-003-T3): Permission Check Middleware (1h)`
