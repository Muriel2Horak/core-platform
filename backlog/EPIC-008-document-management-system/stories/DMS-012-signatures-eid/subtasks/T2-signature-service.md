---
id: DMS-012-T2
story: DMS-012
title: "Signing Service"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-012-T2: Signing Service

> **Parent Story:** [DMS-012](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Signing Service

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/dms/signing/SigningService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

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

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-012. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-012-T2): Signing Service`
