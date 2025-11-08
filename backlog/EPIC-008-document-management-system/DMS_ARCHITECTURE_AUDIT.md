# DMS Architecture Audit - Comprehensive Scope Analysis

**Datum:** 8. listopadu 2025  
**Účel:** Definovat KOMPLETNÍ scope DMS jako First-Class komponenty vedle Metamodelu a Workflow

---

## 📋 EXECUTIVE SUMMARY

### Současný Stav (Co MÁME implementováno)

**✅ HOTOVO (80%):**

1. **MinIO Storage Backend** (~800 LOC)
   - MinioClient konfigurace
   - Bucket management (per-tenant)
   - Presigned URLs (upload/download)
   - Server-side encryption

2. **Core Document API** (~1,200 LOC)
   - `POST /api/documents/upload` - MultipartFile upload
   - `GET /api/documents/{id}` - Metadata retrieval
   - `GET /api/documents/{id}/download` - Presigned URL redirect
   - `DELETE /api/documents/{id}` - Document deletion
   - `GET /api/documents?entityType=X&entityId=Y` - List documents for entity

3. **Database Schema** (V1__init.sql)
   ```sql
   document (
     id UUID,
     tenant_id TEXT,
     entity_type TEXT,  -- Vazba na metamodel entity
     entity_id TEXT,
     filename TEXT,
     mime_type TEXT,
     size_bytes BIGINT,
     storage_key TEXT,  -- MinIO object path
     checksum_sha256 TEXT,
     version INTEGER DEFAULT 1,
     uploaded_by TEXT,
     uploaded_at TIMESTAMPTZ
   )
   
   document_index (
     document_id UUID FK,
     extracted_text TEXT,  -- Tika extraction
     search_vector TSVECTOR,  -- Fulltext search
     indexed_at TIMESTAMPTZ
   )
   ```

4. **Fulltext Search Integration** (~500 LOC)
   - Apache Tika text extraction
   - PostgreSQL `tsvector` indexing
   - Trigram search support
   - Search API: `GET /api/search?q=contract&types=Document`

5. **Frontend File Manager** (~1,000 LOC)
   - React Dropzone upload component
   - File grid view
   - Download links
   - Basic metadata display

**❌ CHYBÍ (Tvůj Požadavek):**

1. **First-Class Documents Koncept**
   - Documents jako samostatná služba (vedle Metamodelu, Workflow)
   - Jednotné DMS API (ne fragmentované endpointy)
   - Document lifecycle stavy (draft, active, archived, locked, signed)

2. **Document Versioning** (CRITICAL)
   - `document_version` tabulka
   - Historie změn (kdo, kdy, diff)
   - Rollback capability
   - Version lock (kdo edituje)

3. **Document Links (Entity Vazby)**
   - `document_link` tabulka
   - entity (entityType, entityId) → documentId mapping
   - Role vazby (primary, attachment, contract, evidence)
   - M:N vztahy (1 dokument může být u více entit)

4. **Document Templates & Generování**
   - `document_template` tabulka
   - Mapování na metamodel fields: `${entity.field}`, `${now}`, `${user.name}`
   - GUI editor šablon (ve Studiu)
   - Generování dokumentů z šablon (DOCX, PDF)

5. **Multi-Storage Backend Support**
   - StorageService abstrakce (interface)
   - MinIO implementation (default) ✅
   - M365/SharePoint implementation ❌
   - Google Drive implementation ❌
   - Tenant-specific config (per-tenant storage mode)

6. **WebDAV-Like Edit (Office Integration)**
   - Lock mechanism (kdo edituje)
   - Secure link generování (Office může otevřít)
   - Auto-save → nová verze
   - Unlock při zavření

7. **Podpisy & eID Integrace**
   - Signovací workflow
   - BankID / eID integrace
   - Audit trail podpisů (kdo, kdy, hash)
   - Trustworthy archive (NBU-level eventual)

8. **Metamodel Integration**
   - `entity.features.documents.enabled = true/false`
   - Konfigurace:
     - Povolené typy dokumentů
     - Max velikost
     - Povinné přílohy (např. smlouva musí mít contract_pdf)
     - Permissions (kdo může upload/download)
   - FE: Generický Documents tab u každé entity

9. **Workflow Integration**
   - Dokument jako podmínka přechodu workflow
   - Generování dokumentů jako workflow step
   - Signovací krok v workflow
   - Archivace dokumentů po dokončení procesu

10. **AI/MCP/n8n Napojení**
    - AI: Návrh šablon podle metamodelu
    - AI: Sumarizace dokumentů
    - AI: Anonymizace citlivých údajů
    - n8n: Orchestrace (přeposílání, klasifikace)

---

## 🎯 NOVÝ SCOPE: DMS jako First-Class Component

### 1️⃣ Core Koncept: Documents jako První-Class Občan

#### Entity Model

```sql
-- ========================================
-- 1. DOCUMENT (Core entity)
-- ========================================
CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    
    -- Document identity
    name TEXT NOT NULL,
    description TEXT,
    document_type TEXT NOT NULL,  -- contract, letter, evidence, attachment, template
    
    -- File info
    mime_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 TEXT NOT NULL,
    
    -- Lifecycle state
    state TEXT NOT NULL DEFAULT 'draft',  -- draft, active, archived, deleted, locked, signed
    locked_by TEXT,  -- User ID kdo má lock
    locked_at TIMESTAMPTZ,
    lock_expires_at TIMESTAMPTZ,
    
    -- Versioning
    current_version INTEGER NOT NULL DEFAULT 1,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by TEXT,
    
    -- Validity period
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    
    -- Metadata (JSONB for flexible attributes)
    metadata JSONB DEFAULT '{}'::jsonb,
    
    CONSTRAINT chk_document_state CHECK (state IN ('draft', 'active', 'archived', 'deleted', 'locked', 'signed'))
);

CREATE INDEX idx_document_tenant ON document(tenant_id);
CREATE INDEX idx_document_type ON document(tenant_id, document_type);
CREATE INDEX idx_document_state ON document(tenant_id, state);
CREATE INDEX idx_document_locked ON document(locked_by) WHERE locked_by IS NOT NULL;


-- ========================================
-- 2. DOCUMENT_VERSION (Version history)
-- ========================================
CREATE TABLE document_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL,
    
    -- Version info
    version_number INTEGER NOT NULL,
    
    -- Storage
    storage_backend TEXT NOT NULL DEFAULT 'MINIO',  -- MINIO, SHAREPOINT, GOOGLE_DRIVE
    storage_key TEXT NOT NULL,  -- MinIO object path / SharePoint URL / Drive file ID
    version_id TEXT,  -- Backend-specific version ID
    
    -- File info (může se měnit mezi verzemi)
    mime_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 TEXT NOT NULL,
    
    -- Change tracking
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL,
    change_comment TEXT,
    
    -- Signature info (pokud podepsáno)
    signed_by TEXT,
    signed_at TIMESTAMPTZ,
    signature_method TEXT,  -- BANKID, EID, MANUAL, NONE
    signature_hash TEXT,
    
    UNIQUE(document_id, version_number)
);

CREATE INDEX idx_docversion_document ON document_version(document_id, version_number DESC);
CREATE INDEX idx_docversion_tenant ON document_version(tenant_id);
CREATE INDEX idx_docversion_created ON document_version(created_at DESC);


-- ========================================
-- 3. DOCUMENT_LINK (Entity vazby)
-- ========================================
CREATE TABLE document_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL,
    
    -- Entity vazba
    entity_type TEXT NOT NULL,  -- UserProfile, Contract, Case, Invoice...
    entity_id TEXT NOT NULL,
    
    -- Role vazby
    link_role TEXT NOT NULL DEFAULT 'attachment',  -- primary, attachment, contract, evidence, template
    
    -- Order (pro seřazení příloh)
    display_order INTEGER DEFAULT 0,
    
    -- Metadata
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    linked_by TEXT NOT NULL,
    
    UNIQUE(document_id, entity_type, entity_id, link_role)
);

CREATE INDEX idx_doclink_entity ON document_link(entity_type, entity_id, tenant_id);
CREATE INDEX idx_doclink_document ON document_link(document_id);


-- ========================================
-- 4. DOCUMENT_ACL (Access Control)
-- ========================================
CREATE TABLE document_acl (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL,
    
    -- Principal (user or role)
    principal_type TEXT NOT NULL,  -- USER, ROLE, PUBLIC
    principal_id TEXT,  -- User ID, Role name, NULL for PUBLIC
    
    -- Permissions
    can_read BOOLEAN DEFAULT FALSE,
    can_write BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    can_share BOOLEAN DEFAULT FALSE,
    
    -- Grant info
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    
    CONSTRAINT chk_acl_principal CHECK (
        (principal_type = 'PUBLIC' AND principal_id IS NULL) OR
        (principal_type IN ('USER', 'ROLE') AND principal_id IS NOT NULL)
    )
);

CREATE INDEX idx_docacl_document ON document_acl(document_id);
CREATE INDEX idx_docacl_principal ON document_acl(principal_type, principal_id, tenant_id);


-- ========================================
-- 5. DOCUMENT_TEMPLATE (Šablony)
-- ========================================
CREATE TABLE document_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    
    -- Template identity
    name TEXT NOT NULL,
    description TEXT,
    template_type TEXT NOT NULL,  -- contract, letter, statement, invoice
    
    -- Supported formats
    output_format TEXT NOT NULL DEFAULT 'PDF',  -- PDF, DOCX, ODT, HTML
    
    -- Template file
    template_file_id UUID NOT NULL REFERENCES document(id),  -- Stored as document itself
    
    -- Field mapping (JSONB)
    field_mappings JSONB NOT NULL DEFAULT '[]'::jsonb,
    -- [
    --   {"placeholder": "${entity.name}", "metamodelPath": "name", "type": "text"},
    --   {"placeholder": "${now}", "expression": "NOW()", "type": "date"},
    --   {"placeholder": "${user.name}", "source": "context.user.name", "type": "text"}
    -- ]
    
    -- Validation
    required_fields TEXT[] DEFAULT '{}',
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by TEXT,
    
    -- Version
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_doctemplate_tenant ON document_template(tenant_id, is_active);
CREATE INDEX idx_doctemplate_type ON document_template(template_type);


-- ========================================
-- 6. DOCUMENT_AUDIT (Audit trail)
-- ========================================
CREATE TABLE document_audit (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL,
    tenant_id TEXT NOT NULL,
    
    -- Action
    action TEXT NOT NULL,  -- UPLOAD, DOWNLOAD, VIEW, EDIT, DELETE, LOCK, UNLOCK, SIGN, SHARE
    
    -- Actor
    user_id TEXT NOT NULL,
    ip_address INET,
    user_agent TEXT,
    
    -- Details
    version_number INTEGER,
    details JSONB DEFAULT '{}'::jsonb,
    
    -- Timestamp
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_docaudit_document ON document_audit(document_id, performed_at DESC);
CREATE INDEX idx_docaudit_tenant ON document_audit(tenant_id, performed_at DESC);
CREATE INDEX idx_docaudit_user ON document_audit(user_id, performed_at DESC);
CREATE INDEX idx_docaudit_action ON document_audit(action);


-- ========================================
-- 7. DOCUMENT_SHARE_LINK (Public sharing)
-- ========================================
CREATE TABLE document_share_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL,
    
    -- Share token (secure random)
    share_token TEXT NOT NULL UNIQUE,
    
    -- Access control
    password_hash TEXT,  -- Bcrypt hash pokud je heslo
    max_downloads INTEGER,
    current_downloads INTEGER DEFAULT 0,
    
    -- Expiration
    expires_at TIMESTAMPTZ NOT NULL,
    
    -- Permissions
    can_download BOOLEAN DEFAULT TRUE,
    can_preview BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT NOT NULL,
    last_accessed_at TIMESTAMPTZ
);

CREATE INDEX idx_docshare_document ON document_share_link(document_id);
CREATE INDEX idx_docshare_token ON document_share_link(share_token);
CREATE INDEX idx_docshare_expires ON document_share_link(expires_at) WHERE expires_at > NOW();
```

#### API Endpoints (Kompletní DMS API)

```
# ========================================
# DOCUMENT CRUD
# ========================================
POST   /api/dms/documents                    # Create document (upload new)
GET    /api/dms/documents/{id}               # Get metadata
PUT    /api/dms/documents/{id}               # Update metadata
DELETE /api/dms/documents/{id}               # Delete document
GET    /api/dms/documents                    # List documents (filter by entity/type/state)

# ========================================
# DOCUMENT VERSIONS
# ========================================
POST   /api/dms/documents/{id}/versions      # Upload new version
GET    /api/dms/documents/{id}/versions      # List all versions
GET    /api/dms/documents/{id}/versions/{v}  # Get specific version
POST   /api/dms/documents/{id}/rollback/{v}  # Rollback to version
GET    /api/dms/documents/{id}/diff/{v1}/{v2} # Compare versions (text files)

# ========================================
# DOCUMENT DOWNLOADS
# ========================================
GET    /api/dms/documents/{id}/download               # Download current version
GET    /api/dms/documents/{id}/download/{version}     # Download specific version
GET    /api/dms/documents/{id}/preview                # Preview (inline, not download)
GET    /api/dms/documents/{id}/thumbnail              # Thumbnail image

# ========================================
# DOCUMENT LOCKS (WebDAV-like editing)
# ========================================
POST   /api/dms/documents/{id}/lock          # Acquire edit lock
DELETE /api/dms/documents/{id}/lock          # Release lock
GET    /api/dms/documents/{id}/lock          # Check lock status
POST   /api/dms/documents/{id}/webdav-url    # Generate WebDAV URL for Office

# ========================================
# DOCUMENT LINKS (Entity Vazby)
# ========================================
POST   /api/dms/documents/{id}/links         # Link document to entity
DELETE /api/dms/documents/{id}/links/{linkId} # Unlink
GET    /api/dms/entities/{type}/{id}/documents # List all documents for entity

# ========================================
# DOCUMENT ACCESS CONTROL
# ========================================
GET    /api/dms/documents/{id}/acl           # List ACL entries
POST   /api/dms/documents/{id}/acl           # Grant permission
DELETE /api/dms/documents/{id}/acl/{aclId}   # Revoke permission

# ========================================
# DOCUMENT SHARING
# ========================================
POST   /api/dms/documents/{id}/share         # Create public share link
GET    /api/dms/share/{token}                # Access shared document
GET    /api/dms/share/{token}/download       # Download via share link

# ========================================
# DOCUMENT TEMPLATES
# ========================================
GET    /api/dms/templates                    # List templates
POST   /api/dms/templates                    # Create template
PUT    /api/dms/templates/{id}               # Update template
POST   /api/dms/templates/{id}/generate      # Generate document from template

# ========================================
# DOCUMENT AUDIT
# ========================================
GET    /api/dms/documents/{id}/audit         # Audit trail for document
GET    /api/dms/audit                        # Global audit log (admin)

# ========================================
# DOCUMENT SIGNATURES
# ========================================
POST   /api/dms/documents/{id}/sign          # Create signing request
GET    /api/dms/documents/{id}/signatures    # List signatures
POST   /api/dms/sign/{token}                 # External signing (BankID/eID)
```

---

## 2️⃣ Integrace s Metamodelem

### Metamodel Schema Extension

```yaml
# metamodel_entities/Contract.yaml (příklad)
entity:
  name: Contract
  features:
    documents:
      enabled: true
      config:
        # Povolené typy dokumentů
        allowedTypes:
          - contract         # Smlouva (PDF)
          - attachment       # Přílohy
          - evidence         # Dokladová evidence
        
        # Validace
        maxFileSize: 10485760  # 10 MB
        allowedMimeTypes:
          - application/pdf
          - image/png
          - image/jpeg
          - application/vnd.openxmlformats-officedocument.wordprocessingml.document
        
        # Povinné dokumenty (validace před workflow přechodem)
        requiredDocuments:
          - type: contract
            min: 1
            max: 1
            description: "Signed contract PDF"
        
        # Permissions (kdo může co)
        permissions:
          upload:
            roles: [ADMIN, CONTRACT_MANAGER]
            condition: "${entity.state != 'LOCKED'}"
          
          download:
            roles: [ADMIN, CONTRACT_MANAGER, CONTRACT_VIEWER]
          
          delete:
            roles: [ADMIN]
            condition: "${entity.state == 'DRAFT'}"
```

### Frontend: Generický Documents Tab

```typescript
// FE: components/DocumentsTab.tsx

interface DocumentsTabProps {
  entityType: string;
  entityId: string;
  config: EntityDocumentsConfig;  // Z metamodelu
}

function DocumentsTab({ entityType, entityId, config }: DocumentsTabProps) {
  const { data: documents } = useQuery(
    ['documents', entityType, entityId],
    () => api.get(`/api/dms/entities/${entityType}/${entityId}/documents`)
  );
  
  const canUpload = useMemo(() => {
    return config.permissions.upload.roles.includes(currentUser.role);
  }, [config, currentUser]);
  
  return (
    <div>
      {canUpload && (
        <DocumentUpload
          allowedTypes={config.allowedTypes}
          maxSize={config.maxFileSize}
          onUpload={(file) => uploadDocument(entityType, entityId, file)}
        />
      )}
      
      <DocumentGrid
        documents={documents}
        onDownload={downloadDocument}
        onDelete={canDelete ? deleteDocument : undefined}
      />
    </div>
  );
}
```

---

## 3️⃣ Storage Backends (Multi-Provider)

### Storage Abstraction Interface

```java
package cz.muriel.core.dms.storage;

public interface StorageService {
    
    /**
     * Upload document to storage
     */
    UploadResult upload(StorageUploadRequest request) throws StorageException;
    
    /**
     * Download document from storage
     */
    InputStream download(String storageKey) throws StorageException;
    
    /**
     * Generate presigned URL for download
     */
    String getPresignedUrl(String storageKey, Duration expiry) throws StorageException;
    
    /**
     * Delete document from storage
     */
    void delete(String storageKey) throws StorageException;
    
    /**
     * Check if storage supports versioning
     */
    boolean supportsVersioning();
    
    /**
     * List versions of a document
     */
    List<StorageVersion> listVersions(String storageKey) throws StorageException;
}
```

### Implementation: MinIO (Default)

```java
@Service
@ConditionalOnProperty(name = "dms.storage.backend", havingValue = "MINIO", matchIfMissing = true)
public class MinioStorageService implements StorageService {
    
    private final MinioClient minioClient;
    private final MinIOProperties minioProperties;
    
    @Override
    public UploadResult upload(StorageUploadRequest request) throws StorageException {
        String bucketName = getBucketForTenant(request.getTenantId());
        
        try {
            // Upload with SHA256 checksum
            ObjectWriteResponse response = minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(request.getStorageKey())
                    .stream(request.getInputStream(), request.getSize(), -1)
                    .contentType(request.getMimeType())
                    .build()
            );
            
            return UploadResult.builder()
                .storageKey(request.getStorageKey())
                .versionId(response.versionId())
                .etag(response.etag())
                .build();
                
        } catch (Exception e) {
            throw new StorageException("MinIO upload failed", e);
        }
    }
    
    // ... download, delete, presigned URL implementations
}
```

### Implementation: SharePoint (M365)

```java
@Service
@ConditionalOnProperty(name = "dms.storage.backend", havingValue = "SHAREPOINT")
public class SharePointStorageService implements StorageService {
    
    private final GraphServiceClient graphClient;
    private final SharePointConfig sharePointConfig;
    
    @Override
    public UploadResult upload(StorageUploadRequest request) throws StorageException {
        try {
            // Upload to tenant's SharePoint
            String siteId = sharePointConfig.getSiteId(request.getTenantId());
            String driveId = sharePointConfig.getDriveId(request.getTenantId());
            
            DriveItem uploadedItem = graphClient
                .sites(siteId)
                .drives(driveId)
                .items()
                .byId(request.getStorageKey())
                .content()
                .put(request.getInputStream());
            
            return UploadResult.builder()
                .storageKey(uploadedItem.getId())
                .versionId(uploadedItem.getETag())
                .build();
                
        } catch (Exception e) {
            throw new StorageException("SharePoint upload failed", e);
        }
    }
    
    @Override
    public String getPresignedUrl(String storageKey, Duration expiry) throws StorageException {
        // Create sharing link via Graph API
        Permission permission = graphClient
            .drives(driveId)
            .items(storageKey)
            .createLink()
            .post(new CreateLinkPostRequestBody() {{
                setType(LinkType.View);
                setScope(LinkScope.Anonymous);
            }});
        
        return permission.getLink().getWebUrl();
    }
}
```

### Tenant-Specific Storage Config

```yaml
# tenant_config/tenant_001.yaml
tenant:
  id: tenant_001
  storage:
    backend: SHAREPOINT  # MINIO | SHAREPOINT | GOOGLE_DRIVE
    config:
      siteId: "contoso.sharepoint.com,abc123"
      driveId: "b!xyz789"
      clientId: "${SHAREPOINT_CLIENT_ID}"
      clientSecret: "${SHAREPOINT_CLIENT_SECRET}"
```

---

## 4️⃣ WebDAV-Like Editing (Office Integration)

### Lock Mechanism

```java
@Service
public class DocumentLockService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Transactional
    public LockResult acquireLock(UUID documentId, String userId, Duration lockDuration) {
        // Check current lock
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
        
        if (doc.getLockedBy() != null && !doc.getLockedBy().equals(userId)) {
            if (doc.getLockExpiresAt().isAfter(Instant.now())) {
                throw new DocumentLockedException(
                    "Document locked by " + doc.getLockedBy() + 
                    " until " + doc.getLockExpiresAt()
                );
            }
        }
        
        // Acquire lock
        Instant expiresAt = Instant.now().plus(lockDuration);
        
        jdbcTemplate.update(
            "UPDATE document SET locked_by = ?, locked_at = ?, lock_expires_at = ? WHERE id = ?",
            userId, Instant.now(), expiresAt, documentId
        );
        
        return LockResult.builder()
            .documentId(documentId)
            .lockedBy(userId)
            .expiresAt(expiresAt)
            .build();
    }
    
    @Transactional
    public void releaseLock(UUID documentId, String userId) {
        jdbcTemplate.update(
            "UPDATE document SET locked_by = NULL, locked_at = NULL, lock_expires_at = NULL " +
            "WHERE id = ? AND locked_by = ?",
            documentId, userId
        );
    }
}
```

### WebDAV URL Generation (Office Open)

```java
@RestController
@RequestMapping("/api/dms/documents")
public class DocumentWebDavController {
    
    @PostMapping("/{id}/webdav-url")
    public ResponseEntity<Map<String, String>> generateWebDavUrl(
        @PathVariable UUID id,
        @RequestParam String action  // EDIT | VIEW
    ) {
        Document doc = documentService.getDocument(id);
        
        // Acquire lock for EDIT
        if ("EDIT".equals(action)) {
            documentLockService.acquireLock(id, getCurrentUserId(), Duration.ofHours(1));
        }
        
        // Generate secure WebDAV URL
        String webdavUrl = webdavService.generateUrl(doc, action);
        
        return ResponseEntity.ok(Map.of(
            "webdavUrl", webdavUrl,
            "protocol", "ms-word:ofe|u|" + webdavUrl,  // Office protocol handler
            "expiresAt", Instant.now().plus(Duration.ofHours(1)).toString()
        ));
    }
}
```

### Frontend: Open in Office Button

```typescript
function DocumentRow({ document }: { document: Document }) {
  const openInOffice = async () => {
    const response = await api.post(
      `/api/dms/documents/${document.id}/webdav-url`,
      { action: 'EDIT' }
    );
    
    // Open in Office (protocol handler)
    window.location.href = response.data.protocol;
    
    // Poll for unlock (when user closes Office)
    const pollInterval = setInterval(async () => {
      const lockStatus = await api.get(`/api/dms/documents/${document.id}/lock`);
      
      if (!lockStatus.data.isLocked) {
        clearInterval(pollInterval);
        toast.success('Document saved and unlocked');
      }
    }, 5000);
  };
  
  return (
    <button onClick={openInOffice}>
      📝 Open in Word
    </button>
  );
}
```

---

## 5️⃣ Document Templates & Generování

### Template Engine

```java
@Service
public class DocumentTemplateService {
    
    private final MetamodelService metamodelService;
    private final DocumentRepository documentRepository;
    
    /**
     * Generate document from template
     */
    @Transactional
    public UUID generateDocument(
        UUID templateId,
        String entityType,
        String entityId,
        Map<String, Object> context
    ) throws Exception {
        
        DocumentTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
        
        // 1. Fetch entity data from metamodel
        Map<String, Object> entityData = metamodelService.getEntityData(entityType, entityId);
        
        // 2. Merge with context
        Map<String, Object> templateContext = new HashMap<>();
        templateContext.put("entity", entityData);
        templateContext.put("user", context.get("user"));
        templateContext.put("now", Instant.now());
        templateContext.putAll(context);
        
        // 3. Process template (DOCX example)
        Document templateDoc = documentRepository.findById(template.getTemplateFileId())
            .orElseThrow();
        
        InputStream templateStream = storageService.download(templateDoc.getStorageKey());
        
        // Use Apache POI for DOCX processing
        XWPFDocument docx = new XWPFDocument(templateStream);
        
        // Replace placeholders
        for (XWPFParagraph para : docx.getParagraphs()) {
            for (XWPFRun run : para.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    text = replacePlaceholders(text, templateContext);
                    run.setText(text, 0);
                }
            }
        }
        
        // 4. Save generated document
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        docx.write(out);
        docx.close();
        
        MultipartFile generatedFile = new MockMultipartFile(
            "generated.docx",
            "generated.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            out.toByteArray()
        );
        
        // Upload as new document
        DocumentModels.UploadResult result = documentService.uploadDocument(
            generatedFile,
            entityType,
            entityId,
            context.get("userId").toString()
        );
        
        return result.getDocumentId();
    }
    
    private String replacePlaceholders(String text, Map<String, Object> context) {
        // ${entity.name} → "John Doe"
        // ${now} → "2025-11-08T10:30:00Z"
        // ${user.email} → "admin@example.com"
        
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = resolvePlaceholder(placeholder, context);
            matcher.appendReplacement(result, value != null ? value.toString() : "");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
```

### Template Editor (Studio GUI)

```typescript
// FE: Studio > Templates > TemplateEditor

interface TemplateField {
  placeholder: string;        // ${entity.name}
  metamodelPath?: string;     // "name" from entity
  expression?: string;        // "NOW()" for dynamic values
  source?: string;            // "context.user.name"
  type: 'text' | 'date' | 'number';
}

function TemplateEditor({ templateId }: { templateId: string }) {
  const [template, setTemplate] = useState<DocumentTemplate>();
  const [fieldMappings, setFieldMappings] = useState<TemplateField[]>([]);
  
  const addField = () => {
    setFieldMappings([
      ...fieldMappings,
      {
        placeholder: '${entity.}',
        type: 'text'
      }
    ]);
  };
  
  return (
    <div>
      <h2>Template: {template.name}</h2>
      
      <FileUpload
        label="Upload Template File (DOCX)"
        accept=".docx"
        onChange={uploadTemplateFile}
      />
      
      <h3>Field Mappings</h3>
      <Table>
        <thead>
          <tr>
            <th>Placeholder</th>
            <th>Source</th>
            <th>Type</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {fieldMappings.map((field, idx) => (
            <tr key={idx}>
              <td>
                <Input
                  value={field.placeholder}
                  onChange={e => updateField(idx, 'placeholder', e.target.value)}
                  placeholder="${entity.name}"
                />
              </td>
              <td>
                <Select
                  value={field.metamodelPath || field.expression}
                  onChange={e => updateField(idx, 'metamodelPath', e.target.value)}
                >
                  <option value="">-- Select Field --</option>
                  <optgroup label="Entity Fields">
                    {metamodelFields.map(f => (
                      <option key={f} value={f}>{f}</option>
                    ))}
                  </optgroup>
                  <optgroup label="System">
                    <option value="NOW()">Current Date/Time</option>
                    <option value="user.name">Current User Name</option>
                  </optgroup>
                </Select>
              </td>
              <td>
                <Select
                  value={field.type}
                  onChange={e => updateField(idx, 'type', e.target.value)}
                >
                  <option value="text">Text</option>
                  <option value="date">Date</option>
                  <option value="number">Number</option>
                </Select>
              </td>
              <td>
                <button onClick={() => removeField(idx)}>🗑️</button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      
      <button onClick={addField}>➕ Add Field</button>
      <button onClick={saveTemplate}>💾 Save Template</button>
    </div>
  );
}
```

---

## 6️⃣ Podpisy & eID Integrace

### Signing Workflow

```java
@Service
public class DocumentSigningService {
    
    private final BankIdService bankIdService;
    private final DocumentRepository documentRepository;
    
    /**
     * Create signing request
     */
    @Transactional
    public SigningRequest createSigningRequest(
        UUID documentId,
        String signerEmail,
        SignatureMethod method  // BANKID, EID, MANUAL
    ) throws Exception {
        
        Document doc = documentRepository.findById(documentId)
            .orElseThrow();
        
        // Generate secure signing token
        String signingToken = generateSecureToken();
        
        // Create signing request entity
        SigningRequest request = SigningRequest.builder()
            .documentId(documentId)
            .signerEmail(signerEmail)
            .signatureMethod(method)
            .signingToken(signingToken)
            .status("PENDING")
            .expiresAt(Instant.now().plus(Duration.ofDays(7)))
            .build();
        
        signingRequestRepository.save(request);
        
        // Send email with signing link
        emailService.sendSigningInvitation(
            signerEmail,
            doc.getName(),
            generateSigningUrl(signingToken)
        );
        
        return request;
    }
    
    /**
     * Complete signing (after BankID auth)
     */
    @Transactional
    public void completeSign(String signingToken, BankIdAuthResult authResult) throws Exception {
        
        SigningRequest request = signingRequestRepository.findBySigningToken(signingToken)
            .orElseThrow(() -> new SigningRequestNotFoundException(signingToken));
        
        if (request.getExpiresAt().isBefore(Instant.now())) {
            throw new SigningRequestExpiredException();
        }
        
        Document doc = documentRepository.findById(request.getDocumentId())
            .orElseThrow();
        
        // Verify BankID signature
        boolean valid = bankIdService.verifySignature(
            authResult.getSignature(),
            authResult.getCertificate()
        );
        
        if (!valid) {
            throw new InvalidSignatureException();
        }
        
        // Create new version with signature metadata
        DocumentVersion signedVersion = DocumentVersion.builder()
            .documentId(doc.getId())
            .versionNumber(doc.getCurrentVersion() + 1)
            .signedBy(authResult.getPersonalNumber())
            .signedAt(Instant.now())
            .signatureMethod("BANKID")
            .signatureHash(authResult.getSignature())
            .build();
        
        documentVersionRepository.save(signedVersion);
        
        // Update document state
        jdbcTemplate.update(
            "UPDATE document SET state = 'signed', current_version = ? WHERE id = ?",
            signedVersion.getVersionNumber(),
            doc.getId()
        );
        
        // Audit log
        auditService.log(
            doc.getId(),
            "SIGN",
            authResult.getPersonalNumber(),
            Map.of("method", "BANKID", "certificate", authResult.getCertificate())
        );
    }
}
```

### External Signing Page

```typescript
// FE: public/sign/{token}

function DocumentSigningPage({ token }: { token: string }) {
  const [signingRequest, setSigningRequest] = useState<SigningRequest>();
  const [bankIdAuth, setBankIdAuth] = useState<BankIdAuth>();
  
  useEffect(() => {
    api.get(`/api/dms/sign/${token}`).then(setSigningRequest);
  }, [token]);
  
  const initiateSign = async () => {
    // Start BankID auth flow
    const authResponse = await api.post(`/api/dms/sign/${token}/bankid/init`);
    
    setBankIdAuth(authResponse.data);
    
    // Poll for BankID completion
    const pollInterval = setInterval(async () => {
      const status = await api.get(`/api/dms/sign/${token}/bankid/status`);
      
      if (status.data.status === 'COMPLETE') {
        clearInterval(pollInterval);
        
        // Complete signing
        await api.post(`/api/dms/sign/${token}/complete`, {
          authResult: status.data.authResult
        });
        
        toast.success('Document signed successfully');
      }
    }, 2000);
  };
  
  return (
    <div>
      <h1>Sign Document</h1>
      <p>Document: <strong>{signingRequest?.documentName}</strong></p>
      <p>Requested by: {signingRequest?.requesterName}</p>
      
      <DocumentPreview documentId={signingRequest?.documentId} />
      
      <button onClick={initiateSign}>
        🔒 Sign with BankID
      </button>
      
      {bankIdAuth && (
        <div>
          <QRCode value={bankIdAuth.autoStartToken} />
          <p>Scan with BankID mobile app</p>
        </div>
      )}
    </div>
  );
}
```

---

## 7️⃣ Workflow Integration

### Workflow Step: Generování Dokumentu

```yaml
# workflow_definitions/contract_approval.yaml

workflow:
  name: Contract Approval
  steps:
    - id: draft_contract
      type: USER_TASK
      name: Draft Contract
      
    - id: generate_contract_pdf
      type: GENERATE_DOCUMENT
      name: Generate Contract PDF
      config:
        templateId: "contract_template_001"
        entityType: "Contract"
        entityId: "${workflow.entityId}"
        documentType: "contract"
        outputFormat: "PDF"
      
    - id: sign_contract
      type: SIGN_DOCUMENT
      name: Sign Contract
      config:
        documentId: "${step.generate_contract_pdf.documentId}"
        signatureMethod: "BANKID"
        signers:
          - email: "${entity.clientEmail}"
            role: "CLIENT"
          - email: "${entity.managerEmail}"
            role: "MANAGER"
      
    - id: archive_contract
      type: ARCHIVE_DOCUMENT
      name: Archive Signed Contract
      config:
        documentId: "${step.generate_contract_pdf.documentId}"
        archiveType: "PERMANENT"
        retention: "10y"
```

### Workflow Engine Integration

```java
@Component
public class GenerateDocumentStepHandler implements WorkflowStepHandler {
    
    private final DocumentTemplateService templateService;
    
    @Override
    public StepResult execute(WorkflowStep step, WorkflowContext context) {
        GenerateDocumentConfig config = step.getConfig(GenerateDocumentConfig.class);
        
        try {
            UUID documentId = templateService.generateDocument(
                config.getTemplateId(),
                config.getEntityType(),
                config.getEntityId(),
                Map.of("user", context.getCurrentUser())
            );
            
            return StepResult.success(Map.of(
                "documentId", documentId.toString()
            ));
            
        } catch (Exception e) {
            return StepResult.failure("Failed to generate document: " + e.getMessage());
        }
    }
}
```

---

## 8️⃣ AI/MCP/n8n Napojení

### AI: Template Suggestions

```java
@Service
public class AiTemplateService {
    
    private final OpenAiService openAiService;
    private final MetamodelService metamodelService;
    
    /**
     * Generate template suggestion from metamodel
     */
    public TemplateSuggestion suggestTemplate(String entityType, String templateType) {
        // Get metamodel schema
        EntitySchema schema = metamodelService.getSchema(entityType);
        
        // Build AI prompt
        String prompt = String.format(
            """
            Create a %s template for entity %s.
            
            Entity fields:
            %s
            
            Generate:
            1. Template name
            2. Template structure (DOCX outline)
            3. Field mappings (which entity fields to include)
            4. Placeholder syntax (${entity.fieldName})
            
            Format as JSON.
            """,
            templateType,
            entityType,
            formatFields(schema.getFields())
        );
        
        String aiResponse = openAiService.complete(prompt);
        
        return parseTemplateSuggestion(aiResponse);
    }
}
```

### n8n: Document Anonymization Workflow

```json
{
  "name": "Document Anonymization",
  "nodes": [
    {
      "type": "n8n-nodes-base.webhook",
      "name": "Document Uploaded",
      "webhookId": "document-uploaded"
    },
    {
      "type": "n8n-nodes-base.httpRequest",
      "name": "Download Document",
      "url": "{{$json['downloadUrl']}}"
    },
    {
      "type": "n8n-nodes-base.openAi",
      "name": "Detect PII",
      "operation": "detect-pii",
      "prompt": "Find all personal data (names, emails, SSN) in this document"
    },
    {
      "type": "n8n-nodes-base.function",
      "name": "Anonymize Text",
      "code": "return items.map(item => ({ ...item, text: anonymize(item.text) }))"
    },
    {
      "type": "n8n-nodes-base.httpRequest",
      "name": "Upload Anonymized",
      "method": "POST",
      "url": "{{$env.API_BASE}}/api/dms/documents/{{$json['documentId']}}/versions"
    }
  ]
}
```

---

## 📊 ZÁVĚR: Co MÁME vs. Co CHYBÍ

### ✅ MÁME (80% implementováno)

1. MinIO storage backend
2. Basic upload/download API
3. Document metadata v DB (entity_type, entity_id vazba)
4. Fulltext search (Tika + tsvector)
5. Frontend upload component

### ❌ CHYBÍ (20% - KRITICKÉ pro First-Class koncept)

1. **Document Versioning** (document_version tabulka)
2. **Document Links** (M:N vazby entity → documents)
3. **Document Templates** (template engine, GUI editor)
4. **Multi-Storage** (SharePoint, Google Drive)
5. **WebDAV Editing** (lock mechanism, Office integration)
6. **Podpisy** (BankID/eID integrace)
7. **Metamodel Features** (entity.features.documents config)
8. **Workflow Integration** (GENERATE_DOCUMENT, SIGN_DOCUMENT steps)
9. **ACL** (fine-grained permissions)
10. **Audit Trail** (document_audit tabulka)
11. **Share Links** (public sharing s expirací)
12. **AI/MCP** (template suggestions, anonymizace)

---

## 🎯 DOPORUČENÝ IMPLEMENTAČNÍ PLÁN

### Phase 1: Core First-Class Components (3 dny)
- Document Versioning (document_version)
- Document Links (M:N vazby)
- Document ACL (permissions)
- Audit Trail (document_audit)

### Phase 2: Advanced Features (2 dny)
- Document Templates (template engine + GUI)
- WebDAV Editing (lock + Office integration)
- Share Links (public sharing)

### Phase 3: Multi-Storage (2 dny)
- StorageService abstrakce
- SharePoint implementation
- Google Drive implementation

### Phase 4: Workflow & AI (2 dny)
- Workflow integration (GENERATE_DOCUMENT, SIGN)
- Podpisy (BankID/eID)
- AI template suggestions

### Phase 5: Metamodel Integration (1 den)
- entity.features.documents config
- Generický Documents tab (FE)

**Total: 10 dní implementace**

---

**Připraven vytvořit EPIC-008 stories podle této architektury?** 🚀
