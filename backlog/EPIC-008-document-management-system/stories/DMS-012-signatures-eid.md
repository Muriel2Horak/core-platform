# DMS-012: Digital Signatures & eID Integration

**Epic:** EPIC-008 Document Management System  
**Phase:** 4 - Workflow & Signatures  
**Estimate:** 1 day  
**LOC:** ~700

## Story

**AS** user  
**I WANT** documents signed via BankID/eID  
**SO THAT** signatures are legally binding

## Implementation

### 1. Signing Request Entity

**Database:** `V8__signing.sql`

```sql
CREATE TABLE signing_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id UUID NOT NULL REFERENCES document_version(id),
    signing_token VARCHAR(64) UNIQUE NOT NULL,
    signer_name VARCHAR(255) NOT NULL,
    signer_email VARCHAR(255) NOT NULL,
    signer_personal_id VARCHAR(20),
    signing_method VARCHAR(20) NOT NULL, -- BANKID, EID, MANUAL
    status VARCHAR(20) NOT NULL, -- PENDING, SIGNED, EXPIRED, CANCELLED
    signature_data TEXT,
    certificate_data TEXT,
    signed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_signing_request_token ON signing_request(signing_token);
CREATE INDEX idx_signing_request_status ON signing_request(status);
```

### 2. Signing Service

**File:** `backend/src/main/java/cz/muriel/core/dms/signing/SigningService.java`

```java
@Service
public class SigningService {
    
    public SigningRequest createSigningRequest(UUID documentVersionId, String signerEmail, SigningMethod method) {
        DocumentVersion version = versionRepo.findById(documentVersionId).orElseThrow();
        
        SigningRequest request = new SigningRequest();
        request.setDocumentVersionId(documentVersionId);
        request.setSigningToken(generateSecureToken(64));
        request.setSignerEmail(signerEmail);
        request.setSigningMethod(method);
        request.setStatus(SigningStatus.PENDING);
        request.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        
        signingRequestRepo.save(request);
        
        // Send email with signing link
        String signingUrl = String.format("https://admin.core-platform.local/public/sign/%s", 
            request.getSigningToken());
        emailService.sendSigningInvitation(signerEmail, signingUrl);
        
        return request;
    }
    
    public void processSignature(String token, String signatureData, String certificateData) {
        SigningRequest request = signingRequestRepo.findBySigningToken(token)
            .orElseThrow(() -> new NotFoundException("Invalid signing token"));
        
        if (request.getStatus() != SigningStatus.PENDING) {
            throw new IllegalStateException("Signing request already processed");
        }
        
        // Verify signature (BankID integration)
        boolean valid = bankIdService.verifySignature(signatureData, certificateData);
        if (!valid) {
            throw new ValidationException("Invalid signature");
        }
        
        // Update signing request
        request.setSignatureData(signatureData);
        request.setCertificateData(certificateData);
        request.setStatus(SigningStatus.SIGNED);
        request.setSignedAt(Instant.now());
        
        // Update document version with signature metadata
        DocumentVersion version = versionRepo.findById(request.getDocumentVersionId()).orElseThrow();
        version.setSignature(signatureData);
        version.setSignedBy(request.getSignerEmail());
        version.setSignedAt(request.getSignedAt());
        versionRepo.save(version);
        
        // Trigger workflow continuation (if waiting for signature)
        workflowService.resumeAfterSignature(request.getId());
    }
}
```

### 3. BankID Integration

**File:** `backend/src/main/java/cz/muriel/core/dms/signing/BankIDService.java`

```java
@Service
public class BankIDService {
    
    @Value("${bankid.endpoint}")
    private String bankIdEndpoint;
    
    @Value("${bankid.client-id}")
    private String clientId;
    
    public String initiateSignature(byte[] documentData, String personalId) {
        // BankID API call: POST /sign/initiate
        Map<String, Object> request = Map.of(
            "documentData", Base64.getEncoder().encodeToString(documentData),
            "personalId", personalId,
            "returnUrl", "https://admin.core-platform.local/api/dms/signing/callback"
        );
        
        BankIDResponse response = restTemplate.postForObject(
            bankIdEndpoint + "/sign/initiate",
            request,
            BankIDResponse.class
        );
        
        return response.getTransactionId();
    }
    
    public boolean verifySignature(String signatureData, String certificateData) {
        // Verify certificate chain against BankID root CA
        // Verify signature matches document hash
        return true; // Placeholder
    }
}
```

### 4. Public Signing Page

**Frontend:** `/public/sign/{token}`

```tsx
export function PublicSigningPage() {
    const { token } = useParams();
    const [request, setRequest] = useState<SigningRequest>();
    
    useEffect(() => {
        api.get(`/api/dms/signing/${token}`).then(res => setRequest(res.data));
    }, [token]);
    
    const handleSign = async () => {
        // Redirect to BankID
        const bankIdUrl = await api.post(`/api/dms/signing/${token}/initiate`);
        window.location.href = bankIdUrl.data.redirectUrl;
    };
    
    return (
        <Container maxWidth="md">
            <Typography variant="h4">Sign Document</Typography>
            <Typography>Document: {request?.documentName}</Typography>
            <Typography>Signer: {request?.signerEmail}</Typography>
            <Button onClick={handleSign}>Sign with BankID</Button>
        </Container>
    );
}
```

## API

POST `/api/dms/signing/request` - Create signing request  
GET `/api/dms/signing/{token}` - Get signing request (public)  
POST `/api/dms/signing/{token}/initiate` - Initiate BankID flow  
POST `/api/dms/signing/callback` - BankID callback webhook

## Acceptance Criteria

- [ ] Signing request entity created
- [ ] BankID integration works
- [ ] Public signing page accessible
- [ ] Email notification sent
- [ ] Signature data stored in document_version
- [ ] Workflow resumes after signature
- [ ] E2E: Create signing request → Sign with BankID → Document marked signed

## Deliverables

- `SigningRequest.java` entity
- `SigningService.java`
- `BankIDService.java`
- `V8__signing.sql` migration
- Public signing page component
- BankID integration guide
