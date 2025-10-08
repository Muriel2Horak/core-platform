package cz.muriel.core.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.config.MinIOProperties;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 📄 Document Service - MinIO upload/download + Tika extraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final MinioClient minioClient;
    private final MinIOProperties minioProperties;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Tika tika = new Tika();

    /**
     * Upload document
     */
    @Transactional
    public DocumentModels.UploadResult uploadDocument(
        Authentication auth,
        String tenantId,
        String entityType,
        String entityId,
        MultipartFile file
    ) throws Exception {
        
        String userId = getUserId(auth);
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long sizeBytes = file.getSize();
        
        // Ensure bucket exists
        String bucketName = getBucketName(tenantId);
        ensureBucketExists(bucketName);
        
        // Storage key: <entityType>/<entityId>/<uuid>-<filename>
        String objectId = UUID.randomUUID().toString();
        String storageKey = String.format("%s/%s/%s-%s", entityType, entityId, objectId, filename);
        
        // Upload to MinIO
        InputStream inputStream = file.getInputStream();
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(storageKey)
                .stream(inputStream, sizeBytes, -1)
                .contentType(contentType)
                .build()
        );
        
        // Get version ID (if versioning enabled)
        String versionId = null;
        try {
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storageKey)
                    .build()
            );
            versionId = stat.versionId();
        } catch (Exception e) {
            log.warn("Could not get version ID: {}", e.getMessage());
        }
        
        // Save metadata to DB
        UUID documentId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO document (id, tenant_id, entity_type, entity_id, filename, content_type, " +
            "size_bytes, storage_key, version_id, uploaded_by, uploaded_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            documentId, tenantId, entityType, entityId, filename, contentType,
            sizeBytes, storageKey, versionId, userId, Instant.now()
        );
        
        // Extract text for fulltext search (async in production)
        extractAndIndexText(documentId, tenantId, file.getBytes());
        
        // Generate presigned download URL
        String downloadUrl = getPresignedDownloadUrl(bucketName, storageKey);
        
        log.info("Document uploaded: id={}, tenant={}, entity={}/{}, size={}, user={}",
            documentId, tenantId, entityType, entityId, sizeBytes, userId);
        
        return DocumentModels.UploadResult.builder()
            .documentId(documentId)
            .downloadUrl(downloadUrl)
            .message("Document uploaded successfully")
            .build();
    }

    /**
     * Get document metadata
     */
    public DocumentModels.Document getDocument(UUID documentId) {
        String sql = "SELECT * FROM document WHERE id = ?";
        List<DocumentModels.Document> docs = jdbcTemplate.query(sql, new DocumentRowMapper(), documentId);
        
        if (docs.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        
        return docs.get(0);
    }

    /**
     * Get presigned download URL
     */
    public String getDownloadUrl(UUID documentId, String tenantId) throws Exception {
        DocumentModels.Document doc = getDocument(documentId);
        
        // Verify tenant
        if (!doc.getTenantId().equals(tenantId)) {
            throw new SecurityException("Access denied to document");
        }
        
        String bucketName = getBucketName(tenantId);
        return getPresignedDownloadUrl(bucketName, doc.getStorageKey());
    }

    /**
     * Delete document
     */
    @Transactional
    public void deleteDocument(UUID documentId, String tenantId) throws Exception {
        DocumentModels.Document doc = getDocument(documentId);
        
        if (!doc.getTenantId().equals(tenantId)) {
            throw new SecurityException("Access denied to document");
        }
        
        // Delete from MinIO
        String bucketName = getBucketName(tenantId);
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(doc.getStorageKey())
                .build()
        );
        
        // Delete from DB (cascade deletes document_index)
        jdbcTemplate.update("DELETE FROM document WHERE id = ?", documentId);
        
        log.info("Document deleted: id={}, tenant={}", documentId, tenantId);
    }

    /**
     * List documents for entity
     */
    public List<DocumentModels.Document> listDocuments(String tenantId, String entityType, String entityId) {
        String sql = "SELECT * FROM document WHERE tenant_id = ? AND entity_type = ? AND entity_id = ? " +
            "ORDER BY uploaded_at DESC";
        
        return jdbcTemplate.query(sql, new DocumentRowMapper(), tenantId, entityType, entityId);
    }

    // --- Private helpers ---

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build()
        );
        
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            );
            
            // Enable versioning if configured
            if (minioProperties.isVersioningEnabled()) {
                try {
                    minioClient.setBucketVersioning(
                        SetBucketVersioningArgs.builder()
                            .bucket(bucketName)
                            .config(new io.minio.messages.VersioningConfiguration(
                                io.minio.messages.VersioningConfiguration.Status.ENABLED, null))
                            .build()
                    );
                    log.info("Versioning enabled for bucket: {}", bucketName);
                } catch (Exception e) {
                    log.warn("Could not enable versioning for bucket {}: {}", bucketName, e.getMessage());
                }
            }
            
            log.info("Created MinIO bucket: {}", bucketName);
        }
    }

    private String getBucketName(String tenantId) {
        return minioProperties.getBucketPrefix() + "-" + tenantId.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

    private String getPresignedDownloadUrl(String bucketName, String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .method(io.minio.http.Method.GET)
                .expiry(minioProperties.getPresignedUrlExpirySeconds(), TimeUnit.SECONDS)
                .build()
        );
    }

    private void extractAndIndexText(UUID documentId, String tenantId, byte[] content) {
        try {
            // Extract text with Tika
            String text = tika.parseToString(new ByteArrayInputStream(content));
            
            if (text != null && !text.trim().isEmpty()) {
                // Insert into fulltext index
                jdbcTemplate.update(
                    "INSERT INTO document_index (document_id, tenant_id, content_tsv) " +
                    "VALUES (?, ?, to_tsvector('english', ?))",
                    documentId, tenantId, text
                );
                
                log.debug("Indexed document text: id={}, length={}", documentId, text.length());
            }
        } catch (Exception e) {
            log.error("Failed to extract/index text for document {}: {}", documentId, e.getMessage(), e);
        }
    }

    private String getUserId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getSubject();
        }
        return auth != null ? auth.getName() : "system";
    }

    // Row mapper
    
    private class DocumentRowMapper implements RowMapper<DocumentModels.Document> {
        @Override
        public DocumentModels.Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, Object> metadata = null;
            String metadataJson = rs.getString("metadata");
            if (metadataJson != null) {
                try {
                    metadata = objectMapper.readValue(metadataJson, Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse metadata JSON: {}", metadataJson);
                }
            }
            
            return DocumentModels.Document.builder()
                .id((UUID) rs.getObject("id"))
                .tenantId(rs.getString("tenant_id"))
                .entityType(rs.getString("entity_type"))
                .entityId(rs.getString("entity_id"))
                .filename(rs.getString("filename"))
                .contentType(rs.getString("content_type"))
                .sizeBytes(rs.getLong("size_bytes"))
                .storageKey(rs.getString("storage_key"))
                .versionId(rs.getString("version_id"))
                .uploadedBy(rs.getString("uploaded_by"))
                .uploadedAt(rs.getTimestamp("uploaded_at").toInstant())
                .metadata(metadata)
                .build();
        }
    }
}
