---
id: DMS-011-T3
story: DMS-011
title: "Sign Document Step Handler"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-011-T3: Sign Document Step Handler

> **Parent Story:** [DMS-011](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Sign Document Step Handler

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/workflow/handlers/SignDocumentStepHandler.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
@Component("SIGN_DOCUMENT")
public class SignDocumentStepHandler implements WorkflowStepHandler {
    
    @Autowired SigningService signingService;
    
    public void execute(WorkflowInstance instance, WorkflowStep step) {
        // Step config: {"documentField": "contractDocumentId", "signerEmail": "${entity.client.email}"}
        UUID documentId = UUID.fromString(instance.getVariables().get(
            step.getConfig().get("documentField").asText()
        ));
        
        String signerEmail = resolveVariable(
            step.getConfig().get("signerEmail").asText(),
            instance
        );
        
        // Create signing request
        SigningRequest request = signingService.createSigningRequest(
            documentId,
            signerEmail,
            SigningMethod.BANKID
        );
        
        // Wait for signature (async - workflow paused)
        instance.setStatus(WorkflowStatus.WAITING_SIGNATURE);
        instance.getVariables().put("signingRequestId", request.getId().toString());
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

This subtask is part of DMS-011. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-011-T3): Sign Document Step Handler`
