package cz.muriel.core.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 📄 Document Models
 */
public class DocumentModels {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private UUID id;
        private String tenantId;
        private String entityType;
        private String entityId;
        private String filename;
        private String contentType;
        private Long sizeBytes;
        private String storageKey;
        private String versionId;
        private String uploadedBy;
        private Instant uploadedAt;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentIndex {
        private UUID documentId;
        private String tenantId;
        private String contentTsv;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRequest {
        private String entityType;
        private String entityId;
        private String filename;
        private String contentType;
        private byte[] content;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResult {
        private UUID documentId;
        private String downloadUrl;
        private String message;
    }
}
