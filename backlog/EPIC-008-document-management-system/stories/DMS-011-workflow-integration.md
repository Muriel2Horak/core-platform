# DMS-011: Workflow Integration

**Epic:** EPIC-008 Document Management System  
**Phase:** 4 - Workflow & Signatures  
**Estimate:** 1 day  
**LOC:** ~600

## Story

**AS** workflow designer  
**I WANT** workflow steps that generate/sign/archive documents  
**SO THAT** document lifecycle is automated

## Implementation

### 1. Workflow Step Types

**Database:** Add to `workflow_step_type` enum

```sql
ALTER TYPE workflow_step_type ADD VALUE 'GENERATE_DOCUMENT';
ALTER TYPE workflow_step_type ADD VALUE 'SIGN_DOCUMENT';
ALTER TYPE workflow_step_type ADD VALUE 'ARCHIVE_DOCUMENT';
```

### 2. Generate Document Step Handler

**File:** `backend/src/main/java/cz/muriel/core/workflow/handlers/GenerateDocumentStepHandler.java`

```java
@Component("GENERATE_DOCUMENT")
public class GenerateDocumentStepHandler implements WorkflowStepHandler {
    
    @Autowired DocumentTemplateService templateService;
    
    public void execute(WorkflowInstance instance, WorkflowStep step) {
        // Step config: {"templateId": "uuid", "outputField": "contractDocumentId"}
        UUID templateId = UUID.fromString(step.getConfig().get("templateId").asText());
        String outputField = step.getConfig().get("outputField").asText();
        
        // Generate document from entity data
        byte[] document = templateService.generateDocument(
            templateId,
            instance.getEntityType(),
            instance.getEntityId()
        );
        
        // Upload generated document
        Document doc = documentService.upload(
            instance.getTenantId(),
            "generated-contract.docx",
            new ByteArrayInputStream(document),
            document.length,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        
        // Link document to entity
        documentLinkService.linkDocumentToEntity(
            doc.getId(),
            instance.getEntityType(),
            instance.getEntityId(),
            LinkRole.PRIMARY
        );
        
        // Store document ID in workflow variables
        instance.getVariables().put(outputField, doc.getId().toString());
    }
}
```

### 3. Sign Document Step Handler

**File:** `backend/src/main/java/cz/muriel/core/workflow/handlers/SignDocumentStepHandler.java`

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

### 4. Workflow Transition Validation

**Add document validation to transitions:**

```java
public void validateTransition(WorkflowInstance instance, String toState) {
    // Check required documents exist
    List<UUID> requiredDocs = metamodelService.getRequiredDocuments(
        instance.getEntityType(),
        toState
    );
    
    for (UUID docType : requiredDocs) {
        if (!documentLinkService.hasDocument(instance.getEntityId(), docType)) {
            throw new ValidationException("Missing required document: " + docType);
        }
    }
}
```

## API

No new endpoints - extends existing workflow API.

**Workflow step config examples:**

```yaml
steps:
  - type: GENERATE_DOCUMENT
    config:
      templateId: "contract-template-uuid"
      outputField: "contractDocumentId"
  
  - type: SIGN_DOCUMENT
    config:
      documentField: "contractDocumentId"
      signerEmail: "${entity.client.email}"
      method: "BANKID"
  
  - type: ARCHIVE_DOCUMENT
    config:
      documentField: "contractDocumentId"
      archiveLocation: "SHAREPOINT"
      retentionYears: 10
```

## Acceptance Criteria

- [ ] GENERATE_DOCUMENT step handler implemented
- [ ] SIGN_DOCUMENT step handler implemented
- [ ] Document validation in transitions
- [ ] Workflow pauses on signature request
- [ ] Signature webhook resumes workflow
- [ ] E2E: Workflow generates contract → sends for signature → completes

## Deliverables

- `GenerateDocumentStepHandler.java`
- `SignDocumentStepHandler.java`
- `ArchiveDocumentStepHandler.java`
- Workflow step type migrations
- Integration tests
