---
id: DMS-001-T4
story: DMS-001
title: "REST API Endpoints for Version Management"
status: todo
assignee: ""
estimate: "3 hours"
created: 2025-11-08
updated: 2025-11-08
---

# DMS-001-T4: REST API Endpoints for Version Management

> **Parent Story:** [DMS-001: Document Versioning](../README.md)  
> **Status:** todo | **Estimate:** 3 hours

## 🎯 Subtask Goal

Add 5 REST endpoints to `DocumentController` for version management: upload new version, list versions, get version metadata, download version, rollback to version.

## ✅ Acceptance Criteria

- [ ] `POST /api/dms/documents/{id}/versions` - Upload new version (multipart)
- [ ] `GET /api/dms/documents/{id}/versions` - List all versions
- [ ] `GET /api/dms/documents/{id}/versions/{v}` - Get version metadata
- [ ] `GET /api/dms/documents/{id}/versions/{v}/download` - Download URL
- [ ] `POST /api/dms/documents/{id}/rollback/{v}` - Rollback
- [ ] All endpoints secured with `@PreAuthorize`
- [ ] Integration tests passing

## 📂 Files to Modify

- `backend/src/main/java/cz/muriel/core/document/DocumentController.java`

## 🔧 Implementation (5 Endpoints)

```java
@RestController
@RequestMapping("/api/dms/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    
    @PostMapping("/{documentId}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentVersion> uploadNewVersion(
        @PathVariable UUID documentId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "changeComment", required = false) String changeComment,
        Principal principal
    ) throws IOException {
        return ResponseEntity.ok(
            documentService.uploadNewVersion(documentId, file, changeComment, principal.getName())
        );
    }
    
    @GetMapping("/{documentId}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentVersion>> listVersions(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.listVersions(documentId));
    }
    
    @GetMapping("/{documentId}/versions/{versionNumber}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<DocumentVersion> getVersion(
        @PathVariable UUID documentId,
        @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(documentService.getVersion(documentId, versionNumber));
    }
    
    @GetMapping("/{documentId}/versions/{versionNumber}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<Map<String, String>> downloadVersion(
        @PathVariable UUID documentId,
        @PathVariable int versionNumber
    ) {
        String url = documentService.getVersionDownloadUrl(documentId, versionNumber);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }
    
    @PostMapping("/{documentId}/rollback/{versionNumber}")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentVersion> rollbackToVersion(
        @PathVariable UUID documentId,
        @PathVariable int versionNumber,
        Principal principal
    ) {
        return ResponseEntity.ok(
            documentService.rollbackToVersion(documentId, versionNumber, principal.getName())
        );
    }
}
```

## 🧪 Testing

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DocumentControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(authorities = "DOCUMENT_WRITE")
    void testUploadNewVersion() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "v2.pdf", "application/pdf", "content".getBytes());
        
        mockMvc.perform(multipart("/api/dms/documents/" + docId + "/versions")
                .file(file)
                .param("changeComment", "Updated"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionNumber").value(2));
    }
}
```

## ✅ Definition of Done

- [ ] 5 endpoints implemented
- [ ] Security annotations applied
- [ ] Integration tests passing
- [ ] Swagger docs updated
- [ ] Committed: `feat(DMS-001-T4): Add version REST endpoints`
