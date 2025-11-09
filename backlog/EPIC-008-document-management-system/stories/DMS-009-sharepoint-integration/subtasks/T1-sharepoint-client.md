---
id: DMS-009-T1
story: DMS-009
title: "SharePoint Storage Service"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-009-T1: SharePoint Storage Service

> **Parent Story:** [DMS-009](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

SharePoint Storage Service

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/dms/storage/SharePointStorageService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "SHAREPOINT")
public class SharePointStorageService implements StorageService {
    
    @Value("${sharepoint.tenant-id}")
    private String tenantId;
    
    @Value("${sharepoint.client-id}")
    private String clientId;
    
    @Value("${sharepoint.client-secret}")
    private String clientSecret;
    
    public String upload(String siteId, String path, InputStream data, long size, String contentType) {
        // Microsoft Graph API: PUT /sites/{siteId}/drive/root:/{path}:/content
        String accessToken = getAccessToken();
        HttpEntity<byte[]> entity = new HttpEntity<>(data.readAllBytes(), headers);
        
        String url = String.format("https://graph.microsoft.com/v1.0/sites/%s/drive/root:/%s:/content",
            siteId, path);
        
        ResponseEntity<DriveItem> response = restTemplate.exchange(url, HttpMethod.PUT, entity, DriveItem.class);
        return response.getBody().getWebUrl();
    }
    
    private String getAccessToken() {
        // OAuth2 client credentials flow
        String url = String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("scope", "https://graph.microsoft.com/.default");
        params.add("grant_type", "client_credentials");
        
        TokenResponse token = restTemplate.postForObject(url, params, TokenResponse.class);
        return token.getAccessToken();
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

This subtask is part of DMS-009. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-009-T1): SharePoint Storage Service`
