---
id: DMS-011-T2
story: DMS-011
title: "Generate Document Step Handler"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-011-T2: Generate Document Step Handler

> **Parent Story:** [DMS-011](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Generate Document Step Handler

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/workflow/handlers/GenerateDocumentStepHandler.java`

## 🔧 Implementation Details

### Code Example 1 (java)

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
- [ ] Committed with message: `feat(DMS-011-T2): Generate Document Step Handler`
