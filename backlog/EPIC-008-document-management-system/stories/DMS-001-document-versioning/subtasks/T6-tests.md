---
id: DMS-001-T6
story: DMS-001
title: "Integration & E2E Tests for Document Versioning"
status: todo
assignee: ""
estimate: "4 hours"
created: 2025-11-08
updated: 2025-11-08
---

# DMS-001-T6: Integration & E2E Tests for Document Versioning

> **Parent Story:** [DMS-001: Document Versioning](../README.md)  
> **Status:** todo | **Estimate:** 4 hours

## 🎯 Subtask Goal

Write comprehensive tests for version management: unit tests (JUnit), integration tests (Testcontainers), E2E tests (Playwright). Ensure upload, list, download, rollback workflows work end-to-end.

## ✅ Acceptance Criteria

- [ ] Unit tests for `DocumentVersionRepository` (5+ test cases)
- [ ] Unit tests for `DocumentService` version methods (5+ test cases)
- [ ] Integration tests for REST endpoints (5+ test cases)
- [ ] E2E test for complete versioning workflow (upload → list → rollback → download)
- [ ] Test coverage >80% for new code
- [ ] All tests passing in CI/CD
- [ ] Test data cleanup (rollback DB changes)

## 📂 Files to Create

### Backend Tests

- `backend/src/test/java/cz/muriel/core/document/DocumentVersionRepositoryTest.java`
- `backend/src/test/java/cz/muriel/core/document/DocumentServiceTest.java`
- `backend/src/test/java/cz/muriel/core/document/DocumentControllerTest.java`

### E2E Tests

- `e2e/specs/dms/document-versioning.spec.ts`

## 🔧 Implementation

### Unit Tests: DocumentVersionRepository

```java
@SpringBootTest
@Transactional
class DocumentVersionRepositoryTest {
    @Autowired
    private DocumentVersionRepository repository;
    
    @Test
    void testSaveAndFindById() {
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
        
        DocumentVersion saved = repository.save(version);
        
        assertNotNull(saved.getId());
        assertEquals(1, saved.getVersionNumber());
    }
    
    @Test
    void testFindByDocumentId_OrderedByVersionDesc() {
        UUID documentId = UUID.randomUUID();
        repository.save(createVersion(documentId, 1));
        repository.save(createVersion(documentId, 2));
        
        List<DocumentVersion> versions = repository.findByDocumentId(documentId);
        
        assertEquals(2, versions.size());
        assertEquals(2, versions.get(0).getVersionNumber()); // Newest first
        assertEquals(1, versions.get(1).getVersionNumber());
    }
    
    @Test
    void testGetNextVersionNumber() {
        UUID documentId = UUID.randomUUID();
        
        // No versions exist
        assertEquals(1, repository.getNextVersionNumber(documentId));
        
        // Version 1 exists
        repository.save(createVersion(documentId, 1));
        assertEquals(2, repository.getNextVersionNumber(documentId));
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

### Integration Tests: DocumentService

```java
@SpringBootTest
@Transactional
class DocumentServiceTest {
    @Autowired
    private DocumentService service;
    
    @Test
    void testUploadNewVersion() throws IOException {
        // Upload initial document (version 1)
        UUID documentId = uploadInitialDocument();
        
        // Upload version 2
        MockMultipartFile file2 = new MockMultipartFile(
            "file", "contract-v2.pdf", "application/pdf", "version 2 content".getBytes()
        );
        
        DocumentVersion v2 = service.uploadNewVersion(documentId, file2, "Updated pricing", "user1");
        
        assertEquals(2, v2.getVersionNumber());
        assertEquals("Updated pricing", v2.getChangeComment());
        assertTrue(v2.getStorageKey().contains("/v2/"));
        
        // Both versions exist
        List<DocumentVersion> versions = service.listVersions(documentId);
        assertEquals(2, versions.size());
    }
    
    @Test
    void testRollbackToVersion() {
        // Upload 3 versions
        UUID documentId = uploadInitialDocument();
        service.uploadNewVersion(documentId, file2, "v2", "user1");
        service.uploadNewVersion(documentId, file3, "v3", "user1");
        
        // Rollback to version 1
        DocumentVersion v4 = service.rollbackToVersion(documentId, 1, "user1");
        
        assertEquals(4, v4.getVersionNumber());
        assertEquals("Rollback to version 1", v4.getChangeComment());
        
        // Checksum matches version 1
        DocumentVersion v1 = service.getVersion(documentId, 1);
        assertEquals(v1.getChecksumSha256(), v4.getChecksumSha256());
    }
}
```

### Integration Tests: REST API

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DocumentControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(authorities = "DOCUMENT_WRITE")
    void testUploadNewVersion() throws Exception {
        UUID documentId = createTestDocument();
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "contract-v2.pdf", "application/pdf", "new content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/dms/documents/" + documentId + "/versions")
                .file(file)
                .param("changeComment", "Updated pricing"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionNumber").value(2))
            .andExpect(jsonPath("$.changeComment").value("Updated pricing"));
    }
    
    @Test
    @WithMockUser(authorities = "DOCUMENT_READ")
    void testListVersions() throws Exception {
        UUID documentId = createTestDocumentWithVersions();
        
        mockMvc.perform(get("/api/dms/documents/" + documentId + "/versions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].versionNumber").value(2)); // Newest first
    }
    
    @Test
    @WithMockUser(authorities = "DOCUMENT_WRITE")
    void testRollback() throws Exception {
        UUID documentId = createTestDocumentWithVersions();
        
        mockMvc.perform(post("/api/dms/documents/" + documentId + "/rollback/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionNumber").value(3))
            .andExpect(jsonPath("$.changeComment").value("Rollback to version 1"));
    }
    
    @Test
    @WithMockUser(authorities = "DOCUMENT_READ")
    void testDownloadVersion() throws Exception {
        UUID documentId = createTestDocumentWithVersions();
        
        mockMvc.perform(get("/api/dms/documents/" + documentId + "/versions/1/download"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.downloadUrl").isNotEmpty());
    }
}
```

### E2E Test: Playwright

**File:** `e2e/specs/dms/document-versioning.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test('document versioning workflow', async ({ page }) => {
  // Upload initial document
  await page.goto('/documents');
  await page.click('button:has-text("Upload Document")');
  await page.setInputFiles('input[type="file"]', 'fixtures/contract-v1.pdf');
  await page.click('button:has-text("Upload")');
  
  // Click on document to open details
  await page.click('text=contract-v1.pdf');
  
  // Navigate to Version History tab
  await page.click('text=Version History');
  await expect(page.locator('text=Version 1')).toBeVisible();
  await expect(page.locator('text=Current')).toBeVisible();
  
  // Upload new version
  await page.click('button:has-text("Upload New Version")');
  await page.setInputFiles('input[type="file"]', 'fixtures/contract-v2.pdf');
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
  
  // Download version 1
  const downloadPromise = page.waitForEvent('download');
  await page.click('text=Version 1').locator('[aria-label="Download this version"]').click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain('.pdf');
});
```

## 🧪 Running Tests

```bash
# Backend unit tests
cd backend
./mvnw test

# Backend integration tests (Testcontainers)
./mvnw verify

# E2E tests
cd e2e
npx playwright test specs/dms/document-versioning.spec.ts

# Coverage report
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html
```

## ✅ Definition of Done

- [ ] 5+ unit tests for DocumentVersionRepository
- [ ] 5+ unit tests for DocumentService
- [ ] 5+ integration tests for REST endpoints
- [ ] 1 complete E2E test workflow
- [ ] All tests passing locally
- [ ] All tests passing in CI/CD
- [ ] Test coverage >80% for new code
- [ ] Test data cleanup working (@Transactional rollback)
- [ ] Committed: `test(DMS-001-T6): Add versioning tests (unit, integration, E2E)`
