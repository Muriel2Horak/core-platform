---
id: DMS-001-T3
story: DMS-001
title: "Document Service Extensions - Version Management"
status: todo
assignee: ""
estimate: "4 hours"
created: 2025-11-08
updated: 2025-11-08
---

# DMS-001-T3: Document Service Extensions - Version Management

> **Parent Story:** [DMS-001: Document Versioning](../README.md)  
> **Status:** todo | **Estimate:** 4 hours

## 🎯 Subtask Goal

Extend existing `DocumentService.java` with version management methods: `uploadNewVersion()`, `listVersions()`, `getVersion()`, `rollbackToVersion()`, `getVersionDownloadUrl()`. Handles MinIO storage, checksum calculation, version numbering.

## ✅ Acceptance Criteria

- [ ] Method `uploadNewVersion(documentId, file, changeComment, userId)` creates new version
- [ ] Method `listVersions(documentId)` returns all versions (newest first)
- [ ] Method `getVersion(documentId, versionNumber)` retrieves specific version
- [ ] Method `rollbackToVersion(documentId, targetVersionNumber, userId)` creates rollback version
- [ ] Method `getVersionDownloadUrl(documentId, versionNumber)` returns presigned MinIO URL
- [ ] Storage key format: `{tenantId}/{documentId}/v{versionNumber}/{filename}`
- [ ] Checksum calculated with SHA-256
- [ ] `document.current_version_id` updated after upload/rollback
- [ ] Access control: User must have read access to document
- [ ] Async text extraction triggered for new versions
- [ ] Code review approved
- [ ] All tests passing

## 📂 Files to Modify

### Modify Existing Files

- `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

### Dependencies

- `DocumentVersionRepository` (T2 - must be complete first)
- `MinioClient` (existing)
- `JdbcTemplate` (existing)

## 🔧 Implementation Steps

### Step 1: Add uploadNewVersion() Method

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

Add this method to existing `DocumentService` class:

```java
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
```

### Step 2: Add listVersions() Method

```java
/**
 * List all versions for document.
 */
public List<DocumentVersion> listVersions(UUID documentId) {
    // Verify access
    getDocument(documentId, getCurrentTenantId())
        .orElseThrow(() -> new NotFoundException("Document not found"));
    
    return versionRepository.findByDocumentId(documentId);
}
```

### Step 3: Add getVersion() Method

```java
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
```

### Step 4: Add rollbackToVersion() Method

```java
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
```

### Step 5: Add getVersionDownloadUrl() Method

```java
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
```

### Step 6: Add Dependency Injection

At the top of `DocumentService` class, add:

```java
@RequiredArgsConstructor
@Service
public class DocumentService {
    private final DocumentVersionRepository versionRepository; // ADD THIS
    private final JdbcTemplate jdbcTemplate; // ADD THIS
    // ... existing fields ...
}
```

## 🧪 Testing

### Unit Tests

**File:** `backend/src/test/java/cz/muriel/core/document/DocumentServiceTest.java`

```java
@SpringBootTest
@Transactional
class DocumentServiceTest {
    @Autowired
    private DocumentService service;
    
    @Test
    void testUploadNewVersion() throws IOException {
        // Given: Document with version 1
        UUID documentId = uploadInitialDocument();
        MockMultipartFile file2 = new MockMultipartFile(
            "file", "contract-v2.pdf", "application/pdf", "version 2 content".getBytes()
        );
        
        // When: Upload version 2
        DocumentVersion v2 = service.uploadNewVersion(documentId, file2, "Updated pricing", "user1");
        
        // Then
        assertEquals(2, v2.getVersionNumber());
        assertEquals("Updated pricing", v2.getChangeComment());
        assertTrue(v2.getStorageKey().contains("/v2/"));
        
        // Verify both versions exist
        List<DocumentVersion> versions = service.listVersions(documentId);
        assertEquals(2, versions.size());
    }
    
    @Test
    void testRollbackToVersion() {
        // Given: Document with 3 versions
        UUID documentId = uploadInitialDocument();
        service.uploadNewVersion(documentId, file2, "v2", "user1");
        service.uploadNewVersion(documentId, file3, "v3", "user1");
        
        // When: Rollback to version 1
        DocumentVersion v4 = service.rollbackToVersion(documentId, 1, "user1");
        
        // Then
        assertEquals(4, v4.getVersionNumber());
        assertEquals("Rollback to version 1", v4.getChangeComment());
        
        // Checksum matches version 1
        DocumentVersion v1 = service.getVersion(documentId, 1);
        assertEquals(v1.getChecksumSha256(), v4.getChecksumSha256());
    }
}
```

## 📝 Notes

- **Async Text Extraction:** Call `extractTextAsync()` after version upload (don't block upload)
- **Storage Cost:** Each version = full file copy (delta storage in Phase 2)
- **Rollback Strategy:** Creates new version (preserves history) instead of hard rollback
- **Presigned URLs:** Valid for 7 days, no authentication needed after generation

## ✅ Definition of Done

- [ ] 5 methods added to `DocumentService.java`
- [ ] `DocumentVersionRepository` injected
- [ ] Storage key format correct: `{tenant}/{docId}/v{N}/{filename}`
- [ ] SHA-256 checksum calculated for uploads
- [ ] `current_version_id` updated after upload/rollback
- [ ] Access control enforced (calls `getDocument()` first)
- [ ] Async text extraction triggered
- [ ] Unit tests written (5+ test cases)
- [ ] All tests passing
- [ ] Code review approved
- [ ] Committed: `feat(DMS-001-T3): Add version management to DocumentService`
