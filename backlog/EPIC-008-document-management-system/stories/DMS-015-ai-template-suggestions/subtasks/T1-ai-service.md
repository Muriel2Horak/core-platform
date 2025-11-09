---
id: DMS-015-T1
story: DMS-015
title: "AI Template Service"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-015-T1: AI Template Service

> **Parent Story:** [DMS-015](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

AI Template Service

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/dms/ai/TemplateAIService.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
@Service
public class TemplateAIService {
    
    @Autowired OpenAIClient openAIClient;
    @Autowired MetamodelService metamodelService;
    
    public TemplateSuggestion suggestTemplate(String entityType) {
        MetamodelEntity entity = metamodelService.getEntity(entityType);
        
        String prompt = buildPrompt(entity);
        String response = openAIClient.complete(prompt);
        
        return parseResponse(response);
    }
    
    private String buildPrompt(MetamodelEntity entity) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a DOCX template structure for entity: ").append(entity.getName()).append("\n");
        prompt.append("Entity fields:\n");
        
        entity.getFields().forEach(field -> {
            prompt.append("- ").append(field.getName())
                  .append(" (").append(field.getType()).append(")\n");
        });
        
        prompt.append("\nGenerate:\n");
        prompt.append("1. Template structure (paragraphs, tables)\n");
        prompt.append("2. Field mapping suggestions (${entity.fieldName})\n");
        prompt.append("3. Sample content layout\n");
        
        return prompt.toString();
    }
    
    private TemplateSuggestion parseResponse(String response) {
        // Parse AI response into structured format
        return TemplateSuggestion.builder()
            .structure(extractStructure(response))
            .fieldMappings(extractFieldMappings(response))
            .sampleContent(extractSampleContent(response))
            .build();
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

This subtask is part of DMS-015. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-015-T1): AI Template Service`
