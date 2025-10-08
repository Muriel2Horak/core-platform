package cz.muriel.core.document;

import cz.muriel.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 📄 Document REST Controller
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload document
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String entityId,
        Authentication auth
    ) {
        try {
            String tenantId = TenantContext.getTenantKey();
            
            DocumentModels.UploadResult result = documentService.uploadDocument(
                auth, tenantId, entityType, entityId, file
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "documentId", result.getDocumentId().toString(),
                "downloadUrl", result.getDownloadUrl(),
                "message", result.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Document upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Upload failed: " + e.getMessage()
                ));
        }
    }

    /**
     * Get document metadata
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentModels.Document> getDocument(
        @PathVariable UUID documentId
    ) {
        try {
            DocumentModels.Document doc = documentService.getDocument(documentId);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            log.error("Failed to get document: {}", documentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Download document (redirect to presigned URL)
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Void> downloadDocument(
        @PathVariable UUID documentId
    ) {
        try {
            String tenantId = TenantContext.getTenantKey();
            String downloadUrl = documentService.getDownloadUrl(documentId, tenantId);
            
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
                
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Failed to generate download URL: {}", documentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete document
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
        @PathVariable UUID documentId
    ) {
        try {
            String tenantId = TenantContext.getTenantKey();
            documentService.deleteDocument(documentId, tenantId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document deleted"
            ));
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "Access denied"));
        } catch (Exception e) {
            log.error("Failed to delete document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * List documents for entity
     */
    @GetMapping
    public ResponseEntity<List<DocumentModels.Document>> listDocuments(
        @RequestParam String entityType,
        @RequestParam String entityId
    ) {
        try {
            String tenantId = TenantContext.getTenantKey();
            List<DocumentModels.Document> docs = documentService.listDocuments(tenantId, entityType, entityId);
            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            log.error("Failed to list documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
