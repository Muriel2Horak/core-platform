---
id: DMS-001-T2
story: DMS-001
title: "DocumentVersion Entity & Repository"
status: todo
assignee: ""
estimate: "3 hours"
created: 2025-11-08
updated: 2025-11-08
---

# DMS-001-T2: DocumentVersion Entity & Repository

> **Parent Story:** [DMS-001: Document Versioning](../README.md)  
> **Status:** todo | **Estimate:** 3 hours

## 🎯 Subtask Goal

Implementovat JPA entitu `DocumentVersion` a JDBC repository s custom queries pro version history tracking. Entita mapuje tabulku `document_version` s podporou pro digitální podpisy (metadata, ne implementace).

## ✅ Acceptance Criteria

Tento subtask je hotový pokud:

- [ ] `DocumentVersion.java` entita s Lombok anotacemi (`@Builder`, `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`)
- [ ] Všechna pole z DB tabulky namapována (15+ columns)
- [ ] `isSigned()` helper metoda pro detekci podepsaných verzí
- [ ] `SignatureMethod` enum (BANKID, EID, INTERNAL)
- [ ] `DocumentVersionRepository.java` s JDBC Template (NOT JPA)
- [ ] Custom queries: `save()`, `findByDocumentId()`, `findByDocumentIdAndVersion()`, `getNextVersionNumber()`, `findLatestVersion()`
- [ ] `RowMapper` pro mapping ResultSet → Entity
- [ ] Kód prošel code review
- [ ] Všechny testy procházejí
- [ ] Commit message: `feat(DMS-001-T2): Add DocumentVersion entity and repository`

## 📂 Files to Modify/Create

### Create New Files

- `backend/src/main/java/cz/muriel/core/document/DocumentVersion.java`
- `backend/src/main/java/cz/muriel/core/document/DocumentVersionRepository.java`

### Related Files (for context)

- `backend/src/main/resources/db/migration/V2__document_versioning.sql` (T1 - already exists)

## 🔧 Implementation Steps

### Step 1: Create DocumentVersion Entity

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentVersion.java`

```java
package cz.muriel.core.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a version of a document.
 * Each document can have multiple versions (full history tracking).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {
    private UUID id;
    private UUID documentId;
    
    /**
     * Version number (incremental per document).
     * Version 1 = initial upload, Version 2 = first update, etc.
     */
    private Integer versionNumber;
    
    /**
     * MinIO storage key for this version's file.
     * Format: {tenantId}/{documentId}/v{versionNumber}/{filename}
     */
    private String storageKey;
    
    /**
     * SHA-256 checksum of file content.
     * Used for integrity checks and deduplication.
     */
    private String checksumSha256;
    
    /**
     * File size in bytes.
     */
    private Long sizeBytes;
    
    /**
     * MIME type (e.g., application/pdf, image/png).
     */
    private String mimeType;
    
    /**
     * Username who uploaded this version.
     */
    private String createdBy;
    
    /**
     * Timestamp when version was uploaded.
     */
    private Instant createdAt;
    
    /**
     * Optional comment describing what changed in this version.
     */
    private String changeComment;
    
    /**
     * Username who digitally signed this version (if signed).
     */
    private String signedBy;
    
    /**
     * Timestamp when version was signed.
     */
    private Instant signedAt;
    
    /**
     * Cryptographic signature hash (implementation in DMS-012).
     */
    private String signatureHash;
    
    /**
     * Signature method used.
     */
    private SignatureMethod signatureMethod;
    
    /**
     * Signature methods supported by platform.
     */
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

### Step 2: Create DocumentVersionRepository

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

### Step 3: Build and Verify

```bash
# 1. Build backend
cd backend
./mvnw clean compile

# 2. Verify no compilation errors
./mvnw test-compile

# 3. Run application to verify runtime
./mvnw spring-boot:run
# Check logs: "Started CorePlatformApplication in X.X seconds"
```

## 🧪 Testing

### Unit Tests (JUnit)

**File:** `backend/src/test/java/cz/muriel/core/document/DocumentVersionRepositoryTest.java`

```java
@SpringBootTest
@Transactional
class DocumentVersionRepositoryTest {
    @Autowired
    private DocumentVersionRepository repository;
    
    @Test
    void testSaveAndFindById() {
        // Given
        UUID documentId = UUID.randomUUID();
        DocumentVersion version = DocumentVersion.builder()
            .documentId(documentId)
            .versionNumber(1)
            .storageKey("tenant/doc/v1/file.pdf")
            .checksumSha256("abc123")
            .sizeBytes(1024L)
            .mimeType("application/pdf")
            .createdBy("user1")
            .build();
        
        // When
        DocumentVersion saved = repository.save(version);
        
        // Then
        assertNotNull(saved.getId());
        assertEquals(1, saved.getVersionNumber());
        assertEquals("user1", saved.getCreatedBy());
    }
    
    @Test
    void testFindByDocumentId() {
        // Given
        UUID documentId = UUID.randomUUID();
        repository.save(createVersion(documentId, 1));
        repository.save(createVersion(documentId, 2));
        
        // When
        List<DocumentVersion> versions = repository.findByDocumentId(documentId);
        
        // Then
        assertEquals(2, versions.size());
        assertEquals(2, versions.get(0).getVersionNumber()); // Newest first
        assertEquals(1, versions.get(1).getVersionNumber());
    }
    
    @Test
    void testGetNextVersionNumber() {
        // Given
        UUID documentId = UUID.randomUUID();
        
        // When - no versions exist
        int nextVersion = repository.getNextVersionNumber(documentId);
        
        // Then
        assertEquals(1, nextVersion);
        
        // When - version 1 exists
        repository.save(createVersion(documentId, 1));
        nextVersion = repository.getNextVersionNumber(documentId);
        
        // Then
        assertEquals(2, nextVersion);
    }
    
    private DocumentVersion createVersion(UUID documentId, int versionNumber) {
        return DocumentVersion.builder()
            .documentId(documentId)
            .versionNumber(versionNumber)
            .storageKey(String.format("tenant/doc/v%d/file.pdf", versionNumber))
            .checksumSha256("checksum" + versionNumber)
            .sizeBytes(1024L)
            .mimeType("application/pdf")
            .createdBy("user1")
            .build();
    }
}
```

### Manual Verification

```bash
# 1. Create test document version
psql -U core -d core

INSERT INTO document_version (
    id, document_id, version_number, storage_key,
    checksum_sha256, size_bytes, mime_type, created_by
) VALUES (
    gen_random_uuid(),
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    1,
    'tenant1/doc1/v1/test.pdf',
    'sha256hash',
    1024,
    'application/pdf',
    'test-user'
);

# 2. Verify entity mapping in Java
# Start app, hit breakpoint in repository.findByDocumentId()
# Check: All fields populated correctly, SignatureMethod null (not signed)

# 3. Verify isSigned() logic
UPDATE document_version SET 
    signed_by = 'signer1', 
    signature_hash = 'hash123',
    signature_method = 'BANKID'
WHERE version_number = 1;

# Java: version.isSigned() should return true
```

## 📝 Notes

- **JDBC Template vs JPA:** Using JDBC for performance (version queries are read-heavy)
- **Row Mapper:** Handles nullable signature fields correctly
- **Signature Support:** Metadata columns ready, actual signing implementation in DMS-012
- **Version Number:** Managed by `get_next_version_number()` function (atomicity guaranteed)

## ✅ Definition of Done

- [ ] `DocumentVersion.java` created with 15+ fields
- [ ] `SignatureMethod` enum defined
- [ ] `isSigned()` helper method implemented
- [ ] `DocumentVersionRepository.java` created with JDBC Template
- [ ] 5 repository methods implemented
- [ ] `RowMapper` correctly handles nullable fields
- [ ] Unit tests written (5+ test cases)
- [ ] All tests passing (`./mvnw test`)
- [ ] Code compiled successfully
- [ ] Code review approved
- [ ] Committed with message: `feat(DMS-001-T2): Add DocumentVersion entity and repository`

