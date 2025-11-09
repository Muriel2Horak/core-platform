# DMS-009: SharePoint Integration

**Epic:** EPIC-008 Document Management System  
**Phase:** 3 - Multi-Storage  
**Estimate:** 1 day  
**LOC:** ~600

## Story

**AS** tenant admin  
**I WANT** documents stored in SharePoint  
**SO THAT** they integrate with existing Microsoft 365 workflows

## Implementation

### 1. SharePoint Storage Service

**File:** `backend/src/main/java/cz/muriel/core/dms/storage/SharePointStorageService.java`

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

### 2. SSO Delegated Access

**Integration:** User's Azure AD token (via Keycloak federation)

```java
public String uploadWithDelegatedAccess(String userToken, String siteId, String path, InputStream data) {
    // Use user's AAD token instead of client credentials
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(userToken);
    // ... upload as user
}
```

### 3. Tenant Config

**Example storage_config:**

```json
{
  "sharepoint": {
    "tenantId": "contoso.onmicrosoft.com",
    "siteId": "contoso.sharepoint.com,abc123,def456",
    "driveId": "b!xyz789",
    "clientId": "app-registration-client-id",
    "clientSecret": "secret-from-azure-portal"
  }
}
```

### 4. Dependencies

**pom.xml:**

```xml
<dependency>
    <groupId>com.microsoft.graph</groupId>
    <artifactId>microsoft-graph</artifactId>
    <version>6.0.0</version>
</dependency>
```

## API

GET `/api/dms/storage/sharepoint/sites` - List available SharePoint sites  
POST `/api/dms/storage/test-connection` - Verify SharePoint credentials

## Acceptance Criteria

- [ ] SharePointStorageService implements StorageService
- [ ] Microsoft Graph API integration works
- [ ] OAuth2 client credentials flow
- [ ] Tenant config supports SharePoint
- [ ] E2E: Upload document → stored in SharePoint → download works

## Deliverables

- `SharePointStorageService.java`
- `MicrosoftGraphClient.java` (Graph API wrapper)
- `SharePointConfig.java`
- Storage config UI (tenant settings)
- Integration tests with Microsoft Graph mock
