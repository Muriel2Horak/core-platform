# DMS-010: Google Drive Integration

**Epic:** EPIC-008 Document Management System  
**Phase:** 3 - Multi-Storage  
**Estimate:** 0.5 day  
**LOC:** ~500

## Story

**AS** tenant admin  
**I WANT** documents stored in Google Drive  
**SO THAT** they integrate with Google Workspace

## Implementation

### 1. Google Drive Storage Service

**File:** `backend/src/main/java/cz/muriel/core/dms/storage/GoogleDriveStorageService.java`

```java
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "GOOGLE_DRIVE")
public class GoogleDriveStorageService implements StorageService {
    
    private Drive driveService;
    
    @PostConstruct
    public void init() throws IOException {
        GoogleCredential credential = GoogleCredential
            .fromStream(new FileInputStream(serviceAccountKeyPath))
            .createScoped(Collections.singleton(DriveScopes.DRIVE));
        
        driveService = new Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("CorePlatform DMS").build();
    }
    
    public String upload(String folderId, String filename, InputStream data, long size, String contentType) {
        File fileMetadata = new File()
            .setName(filename)
            .setParents(Collections.singletonList(folderId));
        
        InputStreamContent content = new InputStreamContent(contentType, data);
        File file = driveService.files().create(fileMetadata, content)
            .setFields("id, webViewLink")
            .execute();
        
        return file.getWebViewLink();
    }
    
    public InputStream download(String fileId) {
        return driveService.files().get(fileId).executeMediaAsInputStream();
    }
}
```

### 2. SSO via Google OIDC

**Integration:** User's Google token (via Keycloak Google IDP)

```java
public String uploadWithUserToken(String googleAccessToken, String filename, InputStream data) {
    Drive userDrive = new Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(),
        new GoogleCredential().setAccessToken(googleAccessToken)
    ).build();
    // ... upload with user's Drive
}
```

### 3. Tenant Config

**Example storage_config:**

```json
{
  "google_drive": {
    "projectId": "core-platform-123456",
    "serviceAccountEmail": "dms@core-platform.iam.gserviceaccount.com",
    "serviceAccountKeyPath": "/secrets/google-service-account.json",
    "rootFolderId": "1a2b3c4d5e6f7g8h9i"
  }
}
```

### 4. Dependencies

**pom.xml:**

```xml
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-drive</artifactId>
    <version>v3-rev20230822-2.0.0</version>
</dependency>
```

## API

GET `/api/dms/storage/google/folders` - List Drive folders  
POST `/api/dms/storage/test-connection` - Verify Google credentials

## Acceptance Criteria

- [ ] GoogleDriveStorageService implements StorageService
- [ ] Service account authentication works
- [ ] Upload creates file in shared Drive
- [ ] Download retrieves file content
- [ ] Tenant config supports Google Drive

## Deliverables

- `GoogleDriveStorageService.java`
- `GoogleDriveConfig.java`
- Service account setup guide
- Integration tests with Drive mock
