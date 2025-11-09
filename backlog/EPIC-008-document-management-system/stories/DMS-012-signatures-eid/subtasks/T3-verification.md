---
id: DMS-012-T3
story: DMS-012
title: "BankID Integration"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-012-T3: BankID Integration

> **Parent Story:** [DMS-012](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

BankID Integration

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/dms/signing/BankIDService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

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
- [ ] Committed with message: `feat(DMS-012-T3): BankID Integration`
