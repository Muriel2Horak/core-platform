---
id: DMS-010-T1
story: DMS-010
title: "Google Drive Storage Service"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-010-T1: Google Drive Storage Service

> **Parent Story:** [DMS-010](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Google Drive Storage Service

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/dms/storage/GoogleDriveStorageService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

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

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-010. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-010-T1): Google Drive Storage Service`
