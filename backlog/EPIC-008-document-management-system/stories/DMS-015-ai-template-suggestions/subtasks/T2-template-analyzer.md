---
id: DMS-015-T2
story: DMS-015
title: "MCP Integration (Context-Aware)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-015-T2: MCP Integration (Context-Aware)

> **Parent Story:** [DMS-015](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

MCP Integration (Context-Aware)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

**See parent story [`../README.md`](../README.md) for exact file paths.**

## 🔧 Implementation Details

### Code Example 1 (java)

```java
@Service
public class TemplateMCPService {
    
    @Autowired MCPClient mcpClient;
    
    public TemplateSuggestion suggestWithContext(String entityType) {
        // MCP provides full metamodel context
        Map<String, Object> context = mcpClient.getResource("metamodel://" + entityType);
        
        String prompt = """
            Using metamodel context for %s:
            %s
            
            Suggest a professional document template with:
            - Header/footer layout
            - Field placeholders
            - Table structures for nested entities
            - Signature block
            """.formatted(entityType, context);
        
        return mcpClient.complete(prompt, TemplateSuggestion.class);
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
- [ ] Committed with message: `feat(DMS-015-T2): MCP Integration (Context-Aware)`
