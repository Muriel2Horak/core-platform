# DMS-001: Document Versioning System

**Epic:** [EPIC-008 Document Management System](../README.md)  
**Priority:** 🔴 P1 (KRITICKÉ - Core First-Class Component)  
**Status:** 📋 Not Started  
**Effort:** 1 den (~600 LOC)  
**Dependencies:** None (prvni story Phase 1)

---

## 🎯 Story Goal

Implementovat **full version history tracking** pro dokumenty, umožnit:
- Upload nové verze dokumentu **bez smazání starých**
- **Rollback** na libovolnou předchozí verzi
- **Version metadata** (kdo, kdy, change comment, signature)
- **Storage optimization** (delta storage pro podobné verze)

---

## 📊 Current State vs. Target

### ❌ SOUČASNÝ STAV
```sql
-- V1__init.sql má pouze VERSION FLAG (ne history)
document (
  version INTEGER DEFAULT 1,  -- ❌ Pouze aktuální verze, žádná historie!
  ...
)
```

**Problém:** Při uploadu nové verze se `version++` ale stará data **SE ZTRATÍ** (přepíše se `storage_key`).

### ✅ TARGET STAV
```sql
-- Nová tabulka: document_version
document_version (
  id UUID PRIMARY KEY,
  document_id UUID REFERENCES document(id),
  version_number INTEGER NOT NULL,
  storage_key TEXT NOT NULL,
  checksum_sha256 TEXT NOT NULL,
  size_bytes BIGINT NOT NULL,
  mime_type TEXT NOT NULL,
  created_by TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  change_comment TEXT,
  -- Signature metadata (pro signed versions)
  signed_by TEXT,
  signed_at TIMESTAMPTZ,
  signature_hash TEXT,
  signature_method TEXT,  -- BANKID | EID | INTERNAL
  UNIQUE (document_id, version_number)
);

-- Index pro rychlé načtení version history
CREATE INDEX idx_document_version_document ON document_version(document_id, version_number DESC);
CREATE INDEX idx_document_version_created_at ON document_version(created_at);
```

---

## 🛠️ Implementation Tasks

### Task 1: Database Migration (1h)

**File:** `backend/src/main/resources/db/migration/V2__document_versioning.sql`

```sql
-- ===================================================================
-- V2: Document Versioning System
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_version table
CREATE TABLE document_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    
    -- Storage metadata
    storage_key TEXT NOT NULL,
    checksum_sha256 TEXT NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    mime_type TEXT NOT NULL,
    
    -- Audit metadata
    created_by TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    change_comment TEXT,
    
    -- Signature metadata (optional)
    signed_by TEXT,
    signed_at TIMESTAMPTZ,
    signature_hash TEXT,
    signature_method TEXT CHECK (signature_method IN ('BANKID', 'EID', 'INTERNAL')),
    
    UNIQUE (document_id, version_number)
);

-- 2. Indexes for performance
CREATE INDEX idx_document_version_document ON document_version(document_id, version_number DESC);
CREATE INDEX idx_document_version_created_at ON document_version(created_at);
CREATE INDEX idx_document_version_signed_by ON document_version(signed_by) WHERE signed_by IS NOT NULL;

-- 3. Migrate existing documents to version 1
INSERT INTO document_version (
    document_id, version_number, storage_key, checksum_sha256,
    size_bytes, mime_type, created_by, created_at
)
SELECT 
    id AS document_id,
    1 AS version_number,
    storage_key,
    checksum_sha256,
    size_bytes,
    mime_type,
    uploaded_by AS created_by,
    uploaded_at AS created_at
FROM document
WHERE storage_key IS NOT NULL;  -- Only migrate documents with actual files

-- 4. Add current_version_id column to document table
ALTER TABLE document ADD COLUMN current_version_id UUID REFERENCES document_version(id);

-- 5. Update current_version_id to point to version 1
UPDATE document d
SET current_version_id = dv.id
FROM document_version dv
WHERE dv.document_id = d.id AND dv.version_number = 1;

-- 6. Create function to auto-increment version number
CREATE OR REPLACE FUNCTION get_next_version_number(p_document_id UUID)
RETURNS INTEGER AS $$
DECLARE
    v_max_version INTEGER;
BEGIN
    SELECT COALESCE(MAX(version_number), 0) + 1
    INTO v_max_version
    FROM document_version
    WHERE document_id = p_document_id;
    
    RETURN v_max_version;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE document_version IS 'Version history for documents - stores all past versions with metadata';
COMMENT ON COLUMN document_version.signature_hash IS 'SHA-256 hash of digital signature (BankID/eID certificate)';
```

---

### Task 2: Java Entity & Repository (1.5h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentVersion.java`

```java
package cz.muriel.core.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Document version entity - stores full history of document versions.
 * Each upload creates new version, old versions preserved for rollback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {
    private UUID id;
    private UUID documentId;
    private Integer versionNumber;
    
    // Storage metadata
    private String storageKey;
    private String checksumSha256;
    private Long sizeBytes;
    private String mimeType;
    
    // Audit metadata
    private String createdBy;
    private Instant createdAt;
    private String changeComment;
    
    // Signature metadata (optional)
    private String signedBy;
    private Instant signedAt;
    private String signatureHash;
    private SignatureMethod signatureMethod;
    
    public enum SignatureMethod {
        BANKID,
        EID,
        INTERNAL
    }
    
    /**
     * Check if this version is digitally signed.
     */
    public boolean isSigned() {
        return signedBy != null && signatureHash != null;
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentVersionRepository.java`

```java
package cz.muriel.core.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentVersionRepository {
    private final JdbcTemplate jdbcTemplate;
    
    private static final RowMapper<DocumentVersion> ROW_MAPPER = (rs, rowNum) -> 
        DocumentVersion.builder()
            .id(UUID.fromString(rs.getString("id")))
            .documentId(UUID.fromString(rs.getString("document_id")))
            .versionNumber(rs.getInt("version_number"))
            .storageKey(rs.getString("storage_key"))
            .checksumSha256(rs.getString("checksum_sha256"))
            .sizeBytes(rs.getLong("size_bytes"))
            .mimeType(rs.getString("mime_type"))
            .createdBy(rs.getString("created_by"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .changeComment(rs.getString("change_comment"))
            .signedBy(rs.getString("signed_by"))
            .signedAt(rs.getTimestamp("signed_at") != null 
                ? rs.getTimestamp("signed_at").toInstant() : null)
            .signatureHash(rs.getString("signature_hash"))
            .signatureMethod(rs.getString("signature_method") != null 
                ? DocumentVersion.SignatureMethod.valueOf(rs.getString("signature_method")) 
                : null)
            .build();
    
    /**
     * Create new version for document.
     */
    public DocumentVersion save(DocumentVersion version) {
        if (version.getId() == null) {
            version.setId(UUID.randomUUID());
        }
        
        String sql = """
            INSERT INTO document_version (
                id, document_id, version_number, storage_key, checksum_sha256,
                size_bytes, mime_type, created_by, change_comment
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER,
            version.getId(),
            version.getDocumentId(),
            version.getVersionNumber(),
            version.getStorageKey(),
            version.getChecksumSha256(),
            version.getSizeBytes(),
            version.getMimeType(),
            version.getCreatedBy(),
            version.getChangeComment()
        );
    }
    
    /**
     * List all versions for document (newest first).
     */
    public List<DocumentVersion> findByDocumentId(UUID documentId) {
        String sql = """
            SELECT * FROM document_version
            WHERE document_id = ?
            ORDER BY version_number DESC
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, documentId);
    }
    
    /**
     * Get specific version by number.
     */
    public Optional<DocumentVersion> findByDocumentIdAndVersion(UUID documentId, int versionNumber) {
        String sql = """
            SELECT * FROM document_version
            WHERE document_id = ? AND version_number = ?
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, documentId, versionNumber)
            .stream().findFirst();
    }
    
    /**
     * Get next version number for document.
     */
    public int getNextVersionNumber(UUID documentId) {
        String sql = "SELECT get_next_version_number(?)";
        return jdbcTemplate.queryForObject(sql, Integer.class, documentId);
    }
    
    /**
     * Get current (latest) version.
     */
    public Optional<DocumentVersion> findLatestVersion(UUID documentId) {
        String sql = """
            SELECT * FROM document_version
            WHERE document_id = ?
            ORDER BY version_number DESC
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, documentId)
            .stream().findFirst();
    }
}
```

---

### Task 3: Document Service Extensions (2h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

```java
// ===== ADD TO EXISTING DocumentService.java =====

@RequiredArgsConstructor
@Service
public class DocumentService {
    private final DocumentVersionRepository versionRepository;
    // ... existing fields ...
    
    /**
     * Upload NEW VERSION of existing document.
     * Old versions are preserved in document_version table.
     */
    public DocumentVersion uploadNewVersion(
        UUID documentId,
        MultipartFile file,
        String changeComment,
        String userId
    ) throws IOException {
        // 1. Verify document exists and user has access
        Document document = getDocument(documentId, getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        
        // 2. Get next version number
        int nextVersion = versionRepository.getNextVersionNumber(documentId);
        
        // 3. Upload file to storage
        String storageKey = String.format("%s/%s/v%d/%s",
            getCurrentTenantId(),
            documentId,
            nextVersion,
            file.getOriginalFilename()
        );
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(getBucketName())
                    .object(storageKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        }
        
        // 4. Calculate checksum
        String checksum = calculateSHA256(file.getBytes());
        
        // 5. Create version record
        DocumentVersion newVersion = DocumentVersion.builder()
            .documentId(documentId)
            .versionNumber(nextVersion)
            .storageKey(storageKey)
            .checksumSha256(checksum)
            .sizeBytes(file.getSize())
            .mimeType(file.getContentType())
            .createdBy(userId)
            .changeComment(changeComment)
            .build();
        
        DocumentVersion savedVersion = versionRepository.save(newVersion);
        
        // 6. Update document.current_version_id
        jdbcTemplate.update(
            "UPDATE document SET current_version_id = ? WHERE id = ?",
            savedVersion.getId(), documentId
        );
        
        // 7. Extract text for search (async)
        extractTextAsync(documentId, storageKey);
        
        log.info("Uploaded version {} for document {} (tenant {})",
            nextVersion, documentId, getCurrentTenantId());
        
        return savedVersion;
    }
    
    /**
     * List all versions for document.
     */
    public List<DocumentVersion> listVersions(UUID documentId) {
        // Verify access
        getDocument(documentId, getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found"));
        
        return versionRepository.findByDocumentId(documentId);
    }
    
    /**
     * Get specific version.
     */
    public DocumentVersion getVersion(UUID documentId, int versionNumber) {
        // Verify access
        getDocument(documentId, getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found"));
        
        return versionRepository.findByDocumentIdAndVersion(documentId, versionNumber)
            .orElseThrow(() -> new NotFoundException(
                String.format("Version %d not found for document %s", versionNumber, documentId)
            ));
    }
    
    /**
     * Rollback document to previous version.
     * Creates NEW version (copy of old version) to preserve history.
     */
    public DocumentVersion rollbackToVersion(
        UUID documentId,
        int targetVersionNumber,
        String userId
    ) {
        // 1. Get target version
        DocumentVersion targetVersion = getVersion(documentId, targetVersionNumber);
        
        // 2. Get next version number
        int nextVersion = versionRepository.getNextVersionNumber(documentId);
        
        // 3. Copy file in storage
        String newStorageKey = String.format("%s/%s/v%d/%s",
            getCurrentTenantId(),
            documentId,
            nextVersion,
            extractFilename(targetVersion.getStorageKey())
        );
        
        try {
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(getBucketName())
                    .object(newStorageKey)
                    .source(CopySource.builder()
                        .bucket(getBucketName())
                        .object(targetVersion.getStorageKey())
                        .build())
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy file during rollback", e);
        }
        
        // 4. Create new version (rollback)
        DocumentVersion rollbackVersion = DocumentVersion.builder()
            .documentId(documentId)
            .versionNumber(nextVersion)
            .storageKey(newStorageKey)
            .checksumSha256(targetVersion.getChecksumSha256())
            .sizeBytes(targetVersion.getSizeBytes())
            .mimeType(targetVersion.getMimeType())
            .createdBy(userId)
            .changeComment(String.format("Rollback to version %d", targetVersionNumber))
            .build();
        
        DocumentVersion savedVersion = versionRepository.save(rollbackVersion);
        
        // 5. Update current_version_id
        jdbcTemplate.update(
            "UPDATE document SET current_version_id = ? WHERE id = ?",
            savedVersion.getId(), documentId
        );
        
        log.info("Rolled back document {} to version {} (tenant {})",
            documentId, targetVersionNumber, getCurrentTenantId());
        
        return savedVersion;
    }
    
    /**
     * Get download URL for specific version.
     */
    public String getVersionDownloadUrl(UUID documentId, int versionNumber) {
        DocumentVersion version = getVersion(documentId, versionNumber);
        
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(getBucketName())
                    .object(version.getStorageKey())
                    .expiry(7, TimeUnit.DAYS)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }
    
    private String extractFilename(String storageKey) {
        return storageKey.substring(storageKey.lastIndexOf('/') + 1);
    }
}
```

---

### Task 4: REST API Endpoints (1.5h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentController.java`

```java
// ===== ADD TO EXISTING DocumentController.java =====

@RestController
@RequestMapping("/api/dms/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    
    // ===== VERSION MANAGEMENT ENDPOINTS =====
    
    /**
     * Upload new version of existing document.
     */
    @PostMapping("/{documentId}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentVersion> uploadNewVersion(
        @PathVariable UUID documentId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "changeComment", required = false) String changeComment,
        Principal principal
    ) throws IOException {
        DocumentVersion version = documentService.uploadNewVersion(
            documentId,
            file,
            changeComment,
            principal.getName()
        );
        
        return ResponseEntity.ok(version);
    }
    
    /**
     * List all versions for document (newest first).
     */
    @GetMapping("/{documentId}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentVersion>> listVersions(
        @PathVariable UUID documentId
    ) {
        List<DocumentVersion> versions = documentService.listVersions(documentId);
        return ResponseEntity.ok(versions);
    }
    
    /**
     * Get specific version metadata.
     */
    @GetMapping("/{documentId}/versions/{versionNumber}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<DocumentVersion> getVersion(
        @PathVariable UUID documentId,
        @PathVariable int versionNumber
    ) {
        DocumentVersion version = documentService.getVersion(documentId, versionNumber);
        return ResponseEntity.ok(version);
    }
    
    /**
     * Download specific version (returns presigned URL).
     */
    @GetMapping("/{documentId}/versions/{versionNumber}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<Map<String, String>> downloadVersion(
        @PathVariable UUID documentId,
        @PathVariable int versionNumber
    ) {
        String downloadUrl = documentService.getVersionDownloadUrl(documentId, versionNumber);
        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }
    
    /**
     * Rollback document to previous version.
     * Creates new version (copy of old) to preserve history.
     */
    @PostMapping("/{documentId}/rollback/{versionNumber}")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentVersion> rollbackToVersion(
        @PathVariable UUID documentId,
        @PathVariable int versionNumber,
        Principal principal
    ) {
        DocumentVersion newVersion = documentService.rollbackToVersion(
            documentId,
            versionNumber,
            principal.getName()
        );
        
        return ResponseEntity.ok(newVersion);
    }
}
```

---

### Task 5: Frontend Version History Component (2h)

**File:** `frontend/src/components/DocumentVersionHistory.tsx`

```tsx
import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Timeline,
  TimelineItem,
  TimelineSeparator,
  TimelineConnector,
  TimelineContent,
  TimelineDot,
  TimelineOppositeContent
} from '@mui/lab';
import {
  Typography,
  Paper,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Download,
  History,
  VerifiedUser,
  Comment,
  Restore
} from '@mui/icons-material';
import { format } from 'date-fns';
import axios from 'axios';

interface DocumentVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  storageKey: string;
  checksumSha256: string;
  sizeBytes: number;
  mimeType: string;
  createdBy: string;
  createdAt: string;
  changeComment?: string;
  signedBy?: string;
  signedAt?: string;
  signatureHash?: string;
  signatureMethod?: 'BANKID' | 'EID' | 'INTERNAL';
}

export const DocumentVersionHistory: React.FC = () => {
  const { documentId } = useParams<{ documentId: string }>();
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [rollbackDialogOpen, setRollbackDialogOpen] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null);
  const [changeComment, setChangeComment] = useState('');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  
  useEffect(() => {
    loadVersions();
  }, [documentId]);
  
  const loadVersions = async () => {
    try {
      const response = await axios.get(`/api/dms/documents/${documentId}/versions`);
      setVersions(response.data);
    } catch (error) {
      console.error('Failed to load versions:', error);
    } finally {
      setLoading(false);
    }
  };
  
  const handleUploadNewVersion = async () => {
    if (!uploadFile) return;
    
    const formData = new FormData();
    formData.append('file', uploadFile);
    if (changeComment) {
      formData.append('changeComment', changeComment);
    }
    
    try {
      await axios.post(`/api/dms/documents/${documentId}/versions`, formData);
      setUploadDialogOpen(false);
      setUploadFile(null);
      setChangeComment('');
      loadVersions();
    } catch (error) {
      console.error('Failed to upload new version:', error);
    }
  };
  
  const handleRollback = async () => {
    if (selectedVersion === null) return;
    
    try {
      await axios.post(`/api/dms/documents/${documentId}/rollback/${selectedVersion}`);
      setRollbackDialogOpen(false);
      setSelectedVersion(null);
      loadVersions();
    } catch (error) {
      console.error('Failed to rollback:', error);
    }
  };
  
  const handleDownloadVersion = async (versionNumber: number) => {
    try {
      const response = await axios.get(
        `/api/dms/documents/${documentId}/versions/${versionNumber}/download`
      );
      window.open(response.data.downloadUrl, '_blank');
    } catch (error) {
      console.error('Failed to download version:', error);
    }
  };
  
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };
  
  if (loading) {
    return <Typography>Loading version history...</Typography>;
  }
  
  return (
    <Paper sx={{ p: 3 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">
          <History sx={{ mr: 1, verticalAlign: 'middle' }} />
          Version History ({versions.length} versions)
        </Typography>
        
        <Button
          variant="contained"
          onClick={() => setUploadDialogOpen(true)}
          startIcon={<Download />}
        >
          Upload New Version
        </Button>
      </div>
      
      <Timeline position="alternate">
        {versions.map((version, index) => (
          <TimelineItem key={version.id}>
            <TimelineOppositeContent color="text.secondary">
              {format(new Date(version.createdAt), 'dd.MM.yyyy HH:mm')}
            </TimelineOppositeContent>
            
            <TimelineSeparator>
              <TimelineDot color={index === 0 ? 'primary' : 'grey'}>
                {version.signedBy ? <VerifiedUser /> : null}
              </TimelineDot>
              {index < versions.length - 1 && <TimelineConnector />}
            </TimelineSeparator>
            
            <TimelineContent>
              <Paper elevation={3} sx={{ p: 2 }}>
                <Typography variant="h6">
                  Version {version.versionNumber}
                  {index === 0 && <Chip label="Current" color="primary" size="small" sx={{ ml: 1 }} />}
                  {version.signedBy && (
                    <Chip
                      label={`Signed (${version.signatureMethod})`}
                      color="success"
                      size="small"
                      icon={<VerifiedUser />}
                      sx={{ ml: 1 }}
                    />
                  )}
                </Typography>
                
                <Typography variant="body2" color="text.secondary">
                  Uploaded by: <strong>{version.createdBy}</strong>
                </Typography>
                
                <Typography variant="body2" color="text.secondary">
                  Size: {formatFileSize(version.sizeBytes)}
                </Typography>
                
                {version.changeComment && (
                  <Typography variant="body2" sx={{ mt: 1, fontStyle: 'italic' }}>
                    <Comment fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
                    {version.changeComment}
                  </Typography>
                )}
                
                {version.signedBy && (
                  <Typography variant="body2" color="success.main" sx={{ mt: 1 }}>
                    <VerifiedUser fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
                    Signed by {version.signedBy} on {format(new Date(version.signedAt!), 'dd.MM.yyyy HH:mm')}
                  </Typography>
                )}
                
                <div style={{ marginTop: 8 }}>
                  <Tooltip title="Download this version">
                    <IconButton
                      size="small"
                      color="primary"
                      onClick={() => handleDownloadVersion(version.versionNumber)}
                    >
                      <Download />
                    </IconButton>
                  </Tooltip>
                  
                  {index > 0 && (
                    <Tooltip title="Rollback to this version">
                      <IconButton
                        size="small"
                        color="warning"
                        onClick={() => {
                          setSelectedVersion(version.versionNumber);
                          setRollbackDialogOpen(true);
                        }}
                      >
                        <Restore />
                      </IconButton>
                    </Tooltip>
                  )}
                </div>
              </Paper>
            </TimelineContent>
          </TimelineItem>
        ))}
      </Timeline>
      
      {/* Upload New Version Dialog */}
      <Dialog open={uploadDialogOpen} onClose={() => setUploadDialogOpen(false)}>
        <DialogTitle>Upload New Version</DialogTitle>
        <DialogContent>
          <input
            type="file"
            onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
            style={{ marginBottom: 16 }}
          />
          
          <TextField
            label="Change Comment (optional)"
            fullWidth
            multiline
            rows={3}
            value={changeComment}
            onChange={(e) => setChangeComment(e.target.value)}
            placeholder="Describe what changed in this version..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleUploadNewVersion}
            variant="contained"
            disabled={!uploadFile}
          >
            Upload
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Rollback Confirmation Dialog */}
      <Dialog open={rollbackDialogOpen} onClose={() => setRollbackDialogOpen(false)}>
        <DialogTitle>Confirm Rollback</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to rollback to version {selectedVersion}?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            This will create a new version (copy of version {selectedVersion}).
            All version history will be preserved.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRollbackDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleRollback} variant="contained" color="warning">
            Rollback
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};
```

---

## ✅ Acceptance Criteria

- [ ] **Database Migration V2** runs successfully
  - `document_version` table created
  - Existing documents migrated to version 1
  - Indexes created for performance
  - `get_next_version_number()` function works

- [ ] **Upload New Version** works
  - `POST /api/dms/documents/{id}/versions` accepts file
  - New version created without deleting old versions
  - Storage key includes version number: `{tenant}/{docId}/v{N}/{filename}`
  - `current_version_id` updated to latest version

- [ ] **List Versions** works
  - `GET /api/dms/documents/{id}/versions` returns all versions (newest first)
  - Response includes: versionNumber, createdBy, createdAt, changeComment, sizeBytes

- [ ] **Download Version** works
  - `GET /api/dms/documents/{id}/versions/{v}/download` returns presigned URL
  - URL valid for 7 days
  - Can download any historical version

- [ ] **Rollback** works
  - `POST /api/dms/documents/{id}/rollback/{v}` creates new version (copy of old)
  - Preserves history (doesn't delete versions)
  - Updates `current_version_id` to rollback version
  - Includes changeComment: "Rollback to version {v}"

- [ ] **Frontend Version History** works
  - Timeline view shows all versions (newest at top)
  - "Current" badge on latest version
  - Download button for each version
  - Rollback button (disabled for current version)
  - Upload new version dialog with changeComment field

- [ ] **Signature Metadata** supported
  - `signed_by`, `signed_at`, `signature_hash`, `signature_method` columns exist
  - Frontend shows "Signed" badge for signed versions
  - DMS-012 story will implement actual signing workflow

---

## 🧪 Testing Plan

### Unit Tests (JUnit)

```java
@Test
void testUploadNewVersion() {
    // Upload version 1
    DocumentVersion v1 = service.uploadDocument(tenantId, file1, "user1");
    assertEquals(1, v1.getVersionNumber());
    
    // Upload version 2
    DocumentVersion v2 = service.uploadNewVersion(v1.getDocumentId(), file2, "Updated content", "user1");
    assertEquals(2, v2.getVersionNumber());
    
    // Both versions exist
    List<DocumentVersion> versions = service.listVersions(v1.getDocumentId());
    assertEquals(2, versions.size());
}

@Test
void testRollbackToVersion() {
    // Upload 3 versions
    DocumentVersion v1 = service.uploadDocument(tenantId, file1, "user1");
    DocumentVersion v2 = service.uploadNewVersion(v1.getDocumentId(), file2, "v2", "user1");
    DocumentVersion v3 = service.uploadNewVersion(v1.getDocumentId(), file3, "v3", "user1");
    
    // Rollback to v1
    DocumentVersion v4 = service.rollbackToVersion(v1.getDocumentId(), 1, "user1");
    assertEquals(4, v4.getVersionNumber());
    assertEquals("Rollback to version 1", v4.getChangeComment());
    assertEquals(v1.getChecksumSha256(), v4.getChecksumSha256());
}
```

### Integration Tests (Playwright E2E)

```typescript
test('document versioning workflow', async ({ page }) => {
  // Upload initial document
  await page.goto('/documents');
  await page.click('button:has-text("Upload Document")');
  await page.setInputFiles('input[type="file"]', 'contract-v1.pdf');
  await page.click('button:has-text("Upload")');
  
  // Click on document to open details
  await page.click('text=contract-v1.pdf');
  
  // Navigate to Version History tab
  await page.click('text=Version History');
  await expect(page.locator('text=Version 1')).toBeVisible();
  await expect(page.locator('text=Current')).toBeVisible();
  
  // Upload new version
  await page.click('button:has-text("Upload New Version")');
  await page.setInputFiles('input[type="file"]', 'contract-v2.pdf');
  await page.fill('textarea[placeholder*="Describe what changed"]', 'Updated pricing section');
  await page.click('button:has-text("Upload")');
  
  // Verify version 2 exists
  await expect(page.locator('text=Version 2')).toBeVisible();
  await expect(page.locator('text=Updated pricing section')).toBeVisible();
  
  // Rollback to version 1
  await page.click('[aria-label="Rollback to this version"]').first();
  await page.click('button:has-text("Rollback")');
  
  // Verify version 3 created (rollback)
  await expect(page.locator('text=Version 3')).toBeVisible();
  await expect(page.locator('text=Rollback to version 1')).toBeVisible();
});
```

---

## 📦 Deliverables

1. ✅ Database migration: `V2__document_versioning.sql`
2. ✅ Java entities: `DocumentVersion.java`, `DocumentVersionRepository.java`
3. ✅ Service methods: `uploadNewVersion()`, `listVersions()`, `rollbackToVersion()`
4. ✅ REST API: 5 new endpoints (`POST /versions`, `GET /versions`, `GET /versions/{v}`, `GET /versions/{v}/download`, `POST /rollback/{v}`)
5. ✅ Frontend: `DocumentVersionHistory.tsx` component s Timeline view
6. ✅ Unit tests: 5+ test cases
7. ✅ E2E test: Complete versioning workflow

---

## 🔗 Dependencies

- **Depends on:** None (first story in Phase 1)
- **Blocks:** DMS-012 (Signatures) - needs version metadata columns
- **Related:** DMS-004 (Audit Trail) - audit version uploads

---

## 📝 Notes

- **Storage Cost:** Each version stores full file copy (delta storage v Phase 2)
- **Signature Support:** Schema includes signature columns, implementation in DMS-012
- **Performance:** Index on `(document_id, version_number DESC)` for fast latest version lookup
- **Security:** Version access inherits document permissions (checked in service layer)
- **Cleanup:** Consider retention policy (archive old versions after N months) - Phase 3

---

**Ready for Implementation** ✅  
**Estimated Completion:** 1 den (~600 LOC)
