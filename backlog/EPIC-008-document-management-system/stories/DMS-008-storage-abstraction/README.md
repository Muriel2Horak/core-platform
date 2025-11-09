# DMS-008: StorageService Abstraction

**Epic:** EPIC-008 Document Management System  
**Phase:** 3 - Multi-Storage  
**Estimate:** 0.5 day  
**LOC:** ~400

## Story

**AS** administrator  
**I WANT** pluggable storage backends (MinIO, SharePoint, Google Drive)  
**SO THAT** tenants can choose their preferred storage provider

## Current State

Hard-coded MinIO integration in DocumentService - cannot switch providers per tenant.

## Implementation

### 1. Storage Interface

**File:** `backend/src/main/java/cz/muriel/core/dms/storage/StorageService.java`

```java
public interface StorageService {
    String upload(String bucket, String key, InputStream data, long size, String contentType);
    InputStream download(String bucket, String key);
    void delete(String bucket, String key);
    String getPresignedUrl(String bucket, String key, Duration expiry);
    boolean exists(String bucket, String key);
}

public enum StorageProvider {
    MINIO, SHAREPOINT, GOOGLE_DRIVE
}
```

### 2. MinIO Implementation Refactor

**File:** `backend/src/main/java/cz/muriel/core/dms/storage/MinioStorageService.java`

```java
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "MINIO")
public class MinioStorageService implements StorageService {
    // Existing MinioClient logic moved here
}
```

### 3. Tenant Storage Config

**Database:** `V7__storage_config.sql`

```sql
ALTER TABLE tenant ADD COLUMN storage_provider VARCHAR(20) DEFAULT 'MINIO';
ALTER TABLE tenant ADD COLUMN storage_config JSONB;

COMMENT ON COLUMN tenant.storage_config IS 'Provider-specific config:
  MinIO: {endpoint, accessKey, secretKey, bucket}
  SharePoint: {tenantId, siteId, driveId, clientId, clientSecret}
  Google: {projectId, bucket, serviceAccount}';
```

### 4. Storage Factory

**File:** `backend/src/main/java/cz/muriel/core/dms/storage/StorageServiceFactory.java`

```java
@Component
public class StorageServiceFactory {
    public StorageService getStorageService(String tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
        return switch (tenant.getStorageProvider()) {
            case MINIO -> minioStorageService;
            case SHAREPOINT -> sharePointStorageService;
            case GOOGLE_DRIVE -> googleDriveStorageService;
        };
    }
}
```

### 5. DocumentService Integration

```java
@Service
public class DocumentService {
    @Autowired StorageServiceFactory storageFactory;
    
    public Document upload(...) {
        StorageService storage = storageFactory.getStorageService(tenantId);
        String url = storage.upload(bucket, key, inputStream, size, contentType);
        // ...
    }
}
```

## API

No new endpoints - internal refactor only.

## Acceptance Criteria

- [ ] StorageService interface defined
- [ ] MinioStorageService implements interface
- [ ] Tenant storage_provider config stored
- [ ] StorageServiceFactory routes requests
- [ ] All existing DMS tests pass with MinIO

## Deliverables

- `StorageService.java` interface
- `MinioStorageService.java` (refactored)
- `StorageServiceFactory.java`
- `V7__storage_config.sql`
- Unit tests for factory
