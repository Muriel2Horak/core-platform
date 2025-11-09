# DMS-003: Document ACL (Access Control List)

**Epic:** [EPIC-008 Document Management System](../README.md)  
**Priority:** 🔴 P1 (KRITICKÉ - Security & Compliance)  
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~400 LOC)  
**Dependencies:** DMS-001 (Versioning)

---

## 🎯 Story Goal

Implementovat **fine-grained access control** pro dokumenty:
- **Permissions per principal** (USER, ROLE, PUBLIC)
- **Granular rights** (can_read, can_write, can_delete, can_share)
- **Temporary access** (expires_at)
- **Compliance-ready** audit trail

---

## 📊 Current State vs. Target

### ❌ SOUČASNÝ STAV

**Problém:** Access control pouze na úrovni **tenanta**.  
- Pokud máš přístup k tenant, vidíš **všechny dokumenty** tenant.
- Nelze sdílet dokument pouze s **konkrétním uživatelem**.
- Žádné **temporary access** (expires_at).

### ✅ TARGET STAV

```sql
document_acl (
  id UUID PRIMARY KEY,
  document_id UUID REFERENCES document(id),
  
  -- Principal (kdo má přístup)
  principal_type TEXT NOT NULL,  -- USER | ROLE | PUBLIC
  principal_id TEXT,  -- user_id | role_name | NULL (for PUBLIC)
  
  -- Permissions
  can_read BOOLEAN DEFAULT true,
  can_write BOOLEAN DEFAULT false,
  can_delete BOOLEAN DEFAULT false,
  can_share BOOLEAN DEFAULT false,
  
  -- Expiry
  expires_at TIMESTAMPTZ,
  
  -- Audit
  granted_by TEXT NOT NULL,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Výhoda:**
- Grant permissions **per user** nebo **per role**
- Temporary access (expires_at)
- Compliance audit trail (kdo udělil práva)

---

## 🛠️ Implementation Tasks

### Task 1: Database Migration (0.5h)

**File:** `backend/src/main/resources/db/migration/V4__document_acl.sql`

```sql
-- ===================================================================
-- V4: Document ACL (Access Control List)
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_acl table
CREATE TABLE document_acl (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    
    -- Principal (USER, ROLE, PUBLIC)
    principal_type TEXT NOT NULL,
    principal_id TEXT,  -- user_id for USER, role_name for ROLE, NULL for PUBLIC
    
    -- Permissions (granular)
    can_read BOOLEAN NOT NULL DEFAULT true,
    can_write BOOLEAN NOT NULL DEFAULT false,
    can_delete BOOLEAN NOT NULL DEFAULT false,
    can_share BOOLEAN NOT NULL DEFAULT false,
    
    -- Expiry
    expires_at TIMESTAMPTZ,
    
    -- Audit
    granted_by TEXT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Constraints
    CHECK (principal_type IN ('USER', 'ROLE', 'PUBLIC')),
    CHECK (principal_type = 'PUBLIC' OR principal_id IS NOT NULL),
    UNIQUE (document_id, principal_type, principal_id)
);

-- 2. Indexes
CREATE INDEX idx_document_acl_document ON document_acl(document_id);
CREATE INDEX idx_document_acl_principal ON document_acl(principal_type, principal_id);
CREATE INDEX idx_document_acl_expires ON document_acl(expires_at) WHERE expires_at IS NOT NULL;

-- 3. Function to check permission
CREATE OR REPLACE FUNCTION has_document_permission(
    p_document_id UUID,
    p_user_id TEXT,
    p_user_roles TEXT[],
    p_permission TEXT  -- 'read' | 'write' | 'delete' | 'share'
) RETURNS BOOLEAN AS $$
DECLARE
    v_has_permission BOOLEAN;
BEGIN
    -- Check if user has permission via:
    -- 1. Direct USER grant
    -- 2. ROLE grant (any of user's roles)
    -- 3. PUBLIC grant
    
    SELECT EXISTS (
        SELECT 1 FROM document_acl
        WHERE document_id = p_document_id
          AND (expires_at IS NULL OR expires_at > now())
          AND (
              -- Direct USER permission
              (principal_type = 'USER' AND principal_id = p_user_id)
              
              -- ROLE permission (user has role)
              OR (principal_type = 'ROLE' AND principal_id = ANY(p_user_roles))
              
              -- PUBLIC permission
              OR principal_type = 'PUBLIC'
          )
          AND (
              (p_permission = 'read' AND can_read = true) OR
              (p_permission = 'write' AND can_write = true) OR
              (p_permission = 'delete' AND can_delete = true) OR
              (p_permission = 'share' AND can_share = true)
          )
    ) INTO v_has_permission;
    
    RETURN v_has_permission;
END;
$$ LANGUAGE plpgsql;

-- 4. Cleanup expired ACLs (scheduled job)
CREATE OR REPLACE FUNCTION cleanup_expired_acls()
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM document_acl
    WHERE expires_at IS NOT NULL AND expires_at < now();
    
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE document_acl IS 'Fine-grained access control for documents (USER, ROLE, PUBLIC)';
COMMENT ON COLUMN document_acl.principal_type IS 'Type of principal: USER (specific user), ROLE (all users with role), PUBLIC (everyone)';
COMMENT ON COLUMN document_acl.expires_at IS 'Temporary access expiry (NULL = permanent)';
```

---

### Task 2: Java Entity & Repository (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentAcl.java`

```java
package cz.muriel.core.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Document ACL - fine-grained access control.
 * Permissions can be granted to USER, ROLE, or PUBLIC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAcl {
    private UUID id;
    private UUID documentId;
    
    // Principal
    private PrincipalType principalType;
    private String principalId;
    
    // Permissions
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean canDelete;
    private Boolean canShare;
    
    // Expiry
    private Instant expiresAt;
    
    // Audit
    private String grantedBy;
    private Instant grantedAt;
    
    public enum PrincipalType {
        USER,   // Specific user (principalId = user_id)
        ROLE,   // All users with role (principalId = role_name)
        PUBLIC  // Everyone (principalId = null)
    }
    
    /**
     * Check if ACL is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentAclRepository.java`

```java
package cz.muriel.core.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentAclRepository {
    private final JdbcTemplate jdbcTemplate;
    
    private static final RowMapper<DocumentAcl> ROW_MAPPER = (rs, rowNum) -> 
        DocumentAcl.builder()
            .id(UUID.fromString(rs.getString("id")))
            .documentId(UUID.fromString(rs.getString("document_id")))
            .principalType(DocumentAcl.PrincipalType.valueOf(rs.getString("principal_type")))
            .principalId(rs.getString("principal_id"))
            .canRead(rs.getBoolean("can_read"))
            .canWrite(rs.getBoolean("can_write"))
            .canDelete(rs.getBoolean("can_delete"))
            .canShare(rs.getBoolean("can_share"))
            .expiresAt(rs.getTimestamp("expires_at") != null 
                ? rs.getTimestamp("expires_at").toInstant() : null)
            .grantedBy(rs.getString("granted_by"))
            .grantedAt(rs.getTimestamp("granted_at").toInstant())
            .build();
    
    /**
     * Grant permission.
     */
    public DocumentAcl save(DocumentAcl acl) {
        if (acl.getId() == null) {
            acl.setId(UUID.randomUUID());
        }
        
        String sql = """
            INSERT INTO document_acl (
                id, document_id, principal_type, principal_id,
                can_read, can_write, can_delete, can_share,
                expires_at, granted_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (document_id, principal_type, principal_id)
            DO UPDATE SET
                can_read = EXCLUDED.can_read,
                can_write = EXCLUDED.can_write,
                can_delete = EXCLUDED.can_delete,
                can_share = EXCLUDED.can_share,
                expires_at = EXCLUDED.expires_at
            RETURNING *
            """;
        
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER,
            acl.getId(),
            acl.getDocumentId(),
            acl.getPrincipalType().name(),
            acl.getPrincipalId(),
            acl.getCanRead(),
            acl.getCanWrite(),
            acl.getCanDelete(),
            acl.getCanShare(),
            acl.getExpiresAt(),
            acl.getGrantedBy()
        );
    }
    
    /**
     * List ACL entries for document.
     */
    public List<DocumentAcl> findByDocument(UUID documentId) {
        String sql = """
            SELECT * FROM document_acl
            WHERE document_id = ?
            ORDER BY granted_at DESC
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, documentId);
    }
    
    /**
     * Revoke permission.
     */
    public void delete(UUID aclId) {
        jdbcTemplate.update("DELETE FROM document_acl WHERE id = ?", aclId);
    }
    
    /**
     * Check if user has permission.
     */
    public boolean hasPermission(
        UUID documentId,
        String userId,
        List<String> userRoles,
        String permission  // "read", "write", "delete", "share"
    ) {
        String sql = "SELECT has_document_permission(?, ?, ?, ?)";
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(sql, Boolean.class,
                documentId,
                userId,
                userRoles.toArray(new String[0]),
                permission
            )
        );
    }
}
```

---

### Task 3: Permission Check Middleware (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentPermissionService.java`

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

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

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
        return aclRepository.findByDocument(documentId);
    }
}
```

---

### Task 4: REST API Endpoints (0.5h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentController.java`

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

// ===== REQUEST DTO =====

@Data
class AclRequest {
    private DocumentAcl.PrincipalType principalType;
    private String principalId;
    private boolean canRead = true;
    private boolean canWrite = false;
    private boolean canDelete = false;
    private boolean canShare = false;
    private Instant expiresAt;
}
```

---

## ✅ Acceptance Criteria

- [ ] **Database Migration V4** runs successfully
  - `document_acl` table created
  - Indexes for performance
  - `has_document_permission()` function works

- [ ] **Grant Permission** works
  - `POST /api/dms/documents/{id}/acl` creates ACL entry
  - Support USER, ROLE, PUBLIC principal types
  - Granular permissions (read, write, delete, share)
  - Temporary access (expires_at)

- [ ] **Revoke Permission** works
  - `DELETE /api/dms/documents/acl/{aclId}` removes ACL entry

- [ ] **Permission Check** enforced
  - Download endpoint checks `can_read`
  - Upload version endpoint checks `can_write`
  - Delete endpoint checks `can_delete`
  - Share endpoint checks `can_share`
  - Access denied returns HTTP 403

- [ ] **Expired ACLs** cleaned up
  - Scheduled job runs `cleanup_expired_acls()`

---

## 📦 Deliverables

1. ✅ Database migration: `V4__document_acl.sql`
2. ✅ Java entities: `DocumentAcl.java`, `DocumentAclRepository.java`
3. ✅ Permission service: `DocumentPermissionService.java`
4. ✅ REST API: 3 endpoints (grant, list, revoke)

---

**Ready for Implementation** ✅  
**Estimated Completion:** 0.5 dne (~400 LOC)
