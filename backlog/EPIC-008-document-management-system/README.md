# EPIC-008: Document Management System (DMS)

> **Strategic Initiative:** Document Management jako First-Class Citizen vedle Metamodelu a Workflow

---

## 🎯 Vision

Vytvořit **kompletní DMS** jako samostatnou službu, která:
- Sedí **vedle Metamodelu a Workflow** (ne jako append)
- Je **přísně multi-tenant** (bucket/prefix isolation)
- Poskytuje **jednotné API** pro práci s dokumenty
- Podporuje **různé storage backendy** (MinIO, M365, Google Drive)
- Drží **security, audit, verzování a vazby na entity**

---

## 📊 SOUČASNÝ STAV (Co MÁME)

### ✅ IMPLEMENTOVÁNO (80%)

**1. MinIO Storage Backend** (~800 LOC)
- MinioClient konfigurace
- Bucket management (per-tenant)
- Presigned URLs (upload/download)
- Server-side encryption

**2. Core Document API** (~1,200 LOC)
```
POST   /api/documents/upload
GET    /api/documents/{id}
GET    /api/documents/{id}/download
DELETE /api/documents/{id}
GET    /api/documents?entityType=X&entityId=Y
```

**3. Database Schema** (V1__init.sql)
```sql
document (
  id, tenant_id, entity_type, entity_id,
  filename, mime_type, size_bytes, storage_key,
  checksum_sha256, version, uploaded_by, uploaded_at
)

document_index (
  document_id, extracted_text, search_vector, indexed_at
)
```

**4. Fulltext Search** (~500 LOC)
- Apache Tika text extraction
- PostgreSQL tsvector indexing
- Search API: `GET /api/search?q=contract&types=Document`

**5. Frontend File Manager** (~1,000 LOC)
- React Dropzone upload component
- File grid view
- Download links

---

## ❌ CO CHYBÍ (20% - KRITICKÉ)

### Phase 1: Core First-Class Components (KRITICKÉ)
1. **Document Versioning** (document_version tabulka)
2. **Document Links** (M:N vazby entity → documents)
3. **Document ACL** (fine-grained permissions)
4. **Audit Trail** (document_audit tabulka)

### Phase 2: Advanced Features
5. **Document Templates** (template engine + GUI editor)
6. **WebDAV Editing** (lock + Office integration)
7. **Share Links** (public sharing s expirací)

### Phase 3: Multi-Storage
8. **StorageService abstrakce**
9. **SharePoint implementation** (M365)
10. **Google Drive implementation**

### Phase 4: Workflow & Signatures
11. **Workflow integration** (GENERATE_DOCUMENT, SIGN steps)
12. **Podpisy** (BankID/eID integrace)

### Phase 5: Metamodel Integration
13. **entity.features.documents config**
14. **Generický Documents tab** (FE)

---

## 📋 STORIES (15 stories, 10 dní implementace)

> **Detailní architektura:** [DMS_ARCHITECTURE_AUDIT.md](DMS_ARCHITECTURE_AUDIT.md)

### 🏗️ Phase 1: Core First-Class Components (3 dny)

#### DMS-001: Document Versioning
**Status:** 📋 Not Started  
**Effort:** 1 den (~600 LOC)

**Scope:**
- `document_version` tabulka (version_number, storage_key, checksum, created_by, signed_by)
- API: `POST /api/dms/documents/{id}/versions` (upload new version)
- API: `GET /api/dms/documents/{id}/versions` (list all versions)
- API: `POST /api/dms/documents/{id}/rollback/{v}` (rollback to version)
- FE: Version history timeline

**Acceptance Criteria:**
- [ ] Upload nové verze dokumentu bez smazání starých
- [ ] Rollback na libovolnou verzi
- [ ] Version metadata (kdo, kdy, change comment)
- [ ] Signature metadata per version (signed_by, signature_hash)

---

#### DMS-002: Document Links (Entity Vazby)
**Status:** � Not Started  
**Effort:** 1 den (~500 LOC)

**Scope:**
- `document_link` tabulka (document_id, entity_type, entity_id, link_role, display_order)
- API: `POST /api/dms/documents/{id}/links` (link document to entity)
- API: `DELETE /api/dms/documents/{id}/links/{linkId}` (unlink)
- API: `GET /api/dms/entities/{type}/{id}/documents` (list all documents for entity)
- M:N vazby (1 dokument může být u více entit)

**Acceptance Criteria:**
- [ ] Link document k entity (Contract, Case, UserProfile...)
- [ ] Role vazby (primary, attachment, contract, evidence)
- [ ] Unlinkování dokumentu (soft delete link)
- [ ] Display order pro seřazení příloh

---

#### DMS-003: Document ACL (Access Control)
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~400 LOC)

**Scope:**
- `document_acl` tabulka (principal_type, principal_id, can_read/write/delete/share)
- API: `GET /api/dms/documents/{id}/acl` (list ACL entries)
- API: `POST /api/dms/documents/{id}/acl` (grant permission)
- API: `DELETE /api/dms/documents/{id}/acl/{aclId}` (revoke permission)
- Permission check middleware

**Acceptance Criteria:**
- [ ] Grant permissions (USER, ROLE, PUBLIC)
- [ ] Fine-grained: can_read, can_write, can_delete, can_share
- [ ] Expires_at support (temporary access)
- [ ] Permission check před download/edit/delete

---

#### DMS-004: Document Audit Trail
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~300 LOC)

**Scope:**
- `document_audit` tabulka (action, user_id, ip_address, performed_at)
- API: `GET /api/dms/documents/{id}/audit` (audit log pro dokument)
- API: `GET /api/dms/audit` (global audit log - admin only)
- Audit events: UPLOAD, DOWNLOAD, VIEW, EDIT, DELETE, LOCK, UNLOCK, SIGN, SHARE

**Acceptance Criteria:**
- [ ] Audit log pro všechny operace
- [ ] IP address + user agent tracking
- [ ] Filtrace: by document, by user, by action, by date range
- [ ] Export audit logu (CSV, JSON)

---

### � Phase 2: Advanced Features (2 dny)

#### DMS-005: Document Templates
**Status:** 📋 Not Started  
**Effort:** 1 den (~800 LOC)

**Scope:**
- `document_template` tabulka (template_type, template_file_id, field_mappings JSONB)
- API: `GET /api/dms/templates` (list templates)
- API: `POST /api/dms/templates` (create template)
- API: `POST /api/dms/templates/{id}/generate` (generate document from template)
- Template engine: DOCX processing (Apache POI), placeholder replacement `${entity.field}`

**Acceptance Criteria:**
- [ ] Upload template file (DOCX, ODT)
- [ ] Field mappings: `${entity.name}`, `${now}`, `${user.email}`
- [ ] Generate document from template + entity data
- [ ] Template versioning (version column)

---

#### DMS-006: WebDAV Editing (Office Integration)
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~500 LOC)

**Scope:**
- Lock mechanism: `POST /api/dms/documents/{id}/lock` (acquire lock)
- `DELETE /api/dms/documents/{id}/lock` (release lock)
- `POST /api/dms/documents/{id}/webdav-url` (generate secure WebDAV URL)
- Office protocol handler: `ms-word:ofe|u|<webdav-url>`
- Auto-save → nová verze při uložení v Office

**Acceptance Criteria:**
- [ ] Lock dokument při otevření v Office
- [ ] Secure WebDAV URL (expiry 1h)
- [ ] Auto-save vytvoří novou verzi
- [ ] Unlock při zavření Office
- [ ] Prevent concurrent edits (lock enforcement)

---

#### DMS-007: Share Links (Public Sharing)
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~400 LOC)

**Scope:**
- `document_share_link` tabulka (share_token, password_hash, max_downloads, expires_at)
- API: `POST /api/dms/documents/{id}/share` (create share link)
- API: `GET /api/dms/share/{token}` (access shared document - public endpoint)
- API: `GET /api/dms/share/{token}/download` (download via share link)
- Password protection (optional)

**Acceptance Criteria:**
- [ ] Generate public share link s random token
- [ ] Password protection (Bcrypt hash)
- [ ] Max downloads limit (current_downloads counter)
- [ ] Expires_at enforcement
- [ ] Download count tracking

---

### � Phase 3: Multi-Storage (2 dny)

#### DMS-008: StorageService Abstraction
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~400 LOC)

**Scope:**
- `StorageService` interface (upload, download, delete, getPresignedUrl)
- `MinioStorageService` implementation (refactor existing code)
- Tenant-specific storage config (MINIO | SHAREPOINT | GOOGLE_DRIVE)
- Storage backend selection based on `document_version.storage_backend` column

**Acceptance Criteria:**
- [ ] StorageService interface definován
- [ ] MinioStorageService implementace (existing code refactor)
- [ ] Tenant config: storage.backend = MINIO | SHAREPOINT | GOOGLE_DRIVE
- [ ] Storage backend selection při upload/download

---

#### DMS-009: SharePoint Storage Implementation
**Status:** 📋 Not Started  
**Effort:** 1 den (~600 LOC)

**Scope:**
- `SharePointStorageService` implementation (Microsoft Graph API)
- SSO delegated access (Keycloak/AAD federation)
- Upload to tenant's SharePoint site
- Download via Graph API
- Presigned URL via sharing links

**Acceptance Criteria:**
- [ ] Upload document to SharePoint site/drive
- [ ] Download from SharePoint via Graph API
- [ ] Presigned URL generation (sharing link)
- [ ] Tenant-specific config (siteId, driveId, clientId, clientSecret)
- [ ] SSO authentication (delegated access)

---

#### DMS-010: Google Drive Storage Implementation
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~500 LOC)

**Scope:**
- `GoogleDriveStorageService` implementation (Google Drive API)
- SSO via Google OIDC
- Upload to tenant's Google Drive
- Download via Drive API
- Presigned URL via sharing links

**Acceptance Criteria:**
- [ ] Upload document to Google Drive
- [ ] Download from Drive via API
- [ ] Presigned URL generation (shareable link)
- [ ] Tenant-specific config (driveId, clientId, clientSecret)
- [ ] SSO authentication (Google OIDC)

---

### 🔄 Phase 4: Workflow & Signatures (2 dny)

#### DMS-011: Workflow Integration
**Status:** 📋 Not Started  
**Effort:** 1 den (~600 LOC)

**Scope:**
- Workflow step types: GENERATE_DOCUMENT, SIGN_DOCUMENT, ARCHIVE_DOCUMENT
- `GenerateDocumentStepHandler` (generate from template)
- `SignDocumentStepHandler` (create signing request)
- `ArchiveDocumentStepHandler` (set document state = archived)
- Document jako podmínka workflow přechodu (validation)

**Acceptance Criteria:**
- [ ] GENERATE_DOCUMENT step vygeneruje dokument z template
- [ ] SIGN_DOCUMENT step vytvoří signing request
- [ ] ARCHIVE_DOCUMENT step nastaví state = archived
- [ ] Workflow validation: dokument required pro přechod

---

#### DMS-012: Podpisy & eID Integrace
**Status:** 📋 Not Started  
**Effort:** 1 den (~700 LOC)

**Scope:**
- `SigningRequest` entity (signing_token, signer_email, signature_method, status)
- API: `POST /api/dms/documents/{id}/sign` (create signing request)
- API: `POST /api/dms/sign/{token}/complete` (complete signing after BankID auth)
- BankID integration (signature verification)
- Public signing page: `/public/sign/{token}`

**Acceptance Criteria:**
- [ ] Create signing request s secure token
- [ ] Email invitation s signing link
- [ ] BankID authentication flow
- [ ] Signature verification (certificate validation)
- [ ] Signed version metadata (signed_by, signed_at, signature_hash)

---

### 🧩 Phase 5: Metamodel Integration (1 den)

#### DMS-013: Metamodel Features Configuration
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~400 LOC)

**Scope:**
- Metamodel schema extension: `entity.features.documents.enabled = true`
- Config: allowedTypes, maxFileSize, allowedMimeTypes, requiredDocuments
- Permissions: upload.roles, download.roles, delete.roles
- Validation: required documents check před workflow transition

**Acceptance Criteria:**
- [ ] Metamodel YAML: `features.documents.enabled = true/false`
- [ ] allowedTypes: [contract, attachment, evidence]
- [ ] maxFileSize + allowedMimeTypes validation
- [ ] requiredDocuments validation (min/max count)
- [ ] Permissions config: upload/download/delete roles

---

#### DMS-014: Generický Documents Tab (Frontend)
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~500 LOC)

**Scope:**
- `<DocumentsTab>` React component (generic pro všechny entity)
- Upload area (dle permissions z metamodelu)
- Document grid (list dokumentů pro entity)
- Download / Delete actions (dle permissions)
- Version history view

**Acceptance Criteria:**
- [ ] Generický Documents tab viditelný u všech entit s `features.documents.enabled = true`
- [ ] Upload button visible pouze pokud `permissions.upload.roles` includes current user role
- [ ] Document list filtrovaný podle entity (entityType + entityId)
- [ ] Download / Delete buttons podle permissions
- [ ] Version history timeline

---

### 🤖 Phase 6: AI/MCP/n8n Napojení (BONUS)

#### DMS-015: AI Template Suggestions
**Status:** 📋 Not Started  
**Effort:** 0.5 dne (~300 LOC)

**Scope:**
- AI service: suggest template from metamodel schema
- Prompt: "Create contract template for entity Contract with fields: name, amount, validFrom"
- Response: template structure + field mappings
- MCP integration: context-aware template suggestions

**Acceptance Criteria:**
- [ ] AI suggest template name + structure
- [ ] Field mappings: which entity fields to include
- [ ] Placeholder syntax: `${entity.fieldName}`
- [ ] Preview template before save

---

## 📅 Timeline & Dependencies

**Total:** 10 dní implementace, 15 stories

### Phase 1: Core (3 dny) - KRITICKÉ
- DMS-001: Versioning (1d)
- DMS-002: Links (1d)
- DMS-003: ACL (0.5d)
- DMS-004: Audit (0.5d)

### Phase 2: Advanced (2 dny)
- DMS-005: Templates (1d)
- DMS-006: WebDAV (0.5d)
- DMS-007: Share Links (0.5d)

### Phase 3: Multi-Storage (2 dny)
- DMS-008: Abstraction (0.5d)
- DMS-009: SharePoint (1d)
- DMS-010: Google Drive (0.5d)

### Phase 4: Workflow (2 dny)
- DMS-011: Workflow Integration (1d)
- DMS-012: Signatures (1d)

### Phase 5: Metamodel (1 den)
- DMS-013: Metamodel Config (0.5d)
- DMS-014: Documents Tab (0.5d)

### Phase 6: AI (BONUS)
- DMS-015: AI Suggestions (0.5d)

---

## 🎯 Success Criteria

- [ ] Documents jako First-Class Citizen (samostatná služba)
- [ ] Versioning s rollback capability
- [ ] M:N vazby entity → documents
- [ ] Multi-storage support (MinIO, SharePoint, Google Drive)
- [ ] WebDAV editing (Office integration)
- [ ] Podpisy (BankID/eID)
- [ ] Metamodel integration (generický Documents tab)
- [ ] Workflow integration (GENERATE, SIGN steps)
- [ ] Audit trail (compliance-ready)

---

## 📖 Dokumentace

- **[Architecture Audit](DMS_ARCHITECTURE_AUDIT.md)** - Kompletní analýza (co MÁME vs. co CHYBÍ)
- **[Database Schema](DMS_ARCHITECTURE_AUDIT.md#1%EF%B8%8F%E2%83%A3-core-koncept-documents-jako-prvn%C3%AD-class-ob%C4%8Dan)** - Entity model (7 tabulek)
- **[API Endpoints](DMS_ARCHITECTURE_AUDIT.md#api-endpoints-kompletn%C3%AD-dms-api)** - Kompletní DMS API specification
- **[Workflow Integration](DMS_ARCHITECTURE_AUDIT.md#7%EF%B8%8F%E2%83%A3-workflow-integration)** - GENERATE_DOCUMENT, SIGN_DOCUMENT steps

---

**Next Step:** Vytvoř stories DMS-001 až DMS-015 podle této architektury 🚀
   - Image thumbnails (Thumbnailator)
   - Office doc preview (LibreOffice headless)
   - Video thumbnail extraction (FFmpeg)
   - Preview cache (Redis)
   - **Estimate:** 3 SP (~600 LOC)

## 📈 Success Metrics

### Performance KPIs
- **Upload throughput:** >100 MB/s (multi-part uploads)
- **Download latency:** P95 <200ms (CDN-backed)
- **Preview generation:** <2s for 90% of documents
- **Storage efficiency:** >30% compression ratio
- **Cache hit rate:** >85% pro thumbnails

### Business KPIs
- **Document volume:** 100,000+ documents managed
- **Monthly uploads:** 10,000+ files
- **Storage:** 500GB+ with <5% growth monthly
- **Availability:** 99.9% uptime
- **Security:** 0 unauthorized access incidents

## 🔗 Dependencies

### Upstream Dependencies
- **EPIC-007:** Platform Hardening (multi-tenancy, security)
- **EPIC-003:** Monitoring (metrics, alerts)
- **.env configuration:** MinIO/S3 credentials

### Downstream Dependencies
- **EPIC-006:** Workflow Engine (document attachments)
- **EPIC-010:** Search (full-text indexing) - future

## 📚 Technical Architecture

### Storage Layer
```
┌─────────────────────────────────────────────────┐
│         DocumentController (REST API)          │
├─────────────────────────────────────────────────┤
│            DocumentService (Business Logic)     │
├─────────────────────────────────────────────────┤
│          StorageService (Abstraction)           │
├──────────┬──────────┬──────────┬───────────────┤
│ S3Storage│MinIOStorage│LocalFS  │ Future: GCS   │
└──────────┴──────────┴──────────┴───────────────┘
```

### Preview Generation Pipeline
```
Document Upload → Virus Scan → Storage → Async Preview Job
                                              ↓
                                    PreviewGenerator Service
                                    ├─ PDF: PDFBox
                                    ├─ Images: Thumbnailator
                                    ├─ Office: LibreOffice
                                    └─ Video: FFmpeg
                                              ↓
                                    Preview Cache (Redis)
```

### Security Model
```
┌─────────────────────────────────────────────────┐
│ JWT Token (tenantId, userId, roles)            │
├─────────────────────────────────────────────────┤
│ Tenant Isolation Filter (Spring Security)      │
├─────────────────────────────────────────────────┤
│ Document Permissions (ACL per document)        │
├─────────────────────────────────────────────────┤
│ Storage Encryption (at-rest: AES-256)          │
└─────────────────────────────────────────────────┘
```

## 🧪 Testing Strategy

- **Unit Tests:** StorageService implementations, preview generators (100% coverage)
- **Integration Tests:** Full upload→storage→preview flow
- **E2E Tests:** Playwright tests pro upload/download/preview UI
- **Load Tests:** 1,000 concurrent uploads, 10,000 downloads/sec
- **Security Tests:** Unauthorized access attempts, tenant isolation

## 📝 Documentation

- **API Documentation:** OpenAPI spec pro DocumentController
- **Storage Configuration:** How to configure S3/MinIO/local backends
- **Preview Troubleshooting:** Common FFmpeg/LibreOffice issues
- **Performance Tuning:** CDN setup, cache optimization

## 🚀 Deployment

- **Feature Flags:** `dms.preview.enabled`, `dms.virus-scan.enabled`
- **Rollout:** Blue/green deployment s storage backend migration
- **Monitoring:** Grafana dashboards pro upload/download metrics
- **Alerting:** Storage quota warnings, preview failures

---

**Epic Owner:** Backend Team  
**Created:** 7. listopadu 2025  
**Last Updated:** 7. listopadu 2025  
**Status:** ⏳ In Progress (0/5 stories complete)
