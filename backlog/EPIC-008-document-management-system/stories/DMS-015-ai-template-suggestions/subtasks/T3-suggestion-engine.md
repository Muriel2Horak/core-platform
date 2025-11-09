---
id: DMS-015-T3
story: DMS-015
title: "Frontend Template Builder with AI"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-015-T3: Frontend Template Builder with AI

> **Parent Story:** [DMS-015](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Frontend Template Builder with AI

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

### Code Example 1 (tsx)

```tsx
export function TemplateBuilder({ entityType }: Props) {
    const [suggestion, setSuggestion] = useState<TemplateSuggestion>();
    const [isLoading, setIsLoading] = useState(false);
    
    const handleAISuggest = async () => {
        setIsLoading(true);
        const result = await api.post('/api/dms/templates/ai-suggest', { entityType });
        setSuggestion(result.data);
        setIsLoading(false);
    };
    
    return (
        <Box>
            <Button onClick={handleAISuggest} disabled={isLoading}>
                <AutoAwesome /> AI Suggest Template
            </Button>
            
            {suggestion && (
                <Card>
                    <CardContent>
                        <Typography variant="h6">AI Suggestion</Typography>
                        <Typography variant="subtitle2">Structure:</Typography>
                        <pre>{suggestion.structure}</pre>
                        
                        <Typography variant="subtitle2">Field Mappings:</Typography>
                        <List>
                            {suggestion.fieldMappings.map(mapping => (
                                <ListItem key={mapping.field}>
                                    <ListItemText 
                                        primary={`\${entity.${mapping.field}}`}
                                        secondary={mapping.description}
                                    />
                                    <Button size="small" onClick={() => insertMapping(mapping)}>
                                        Insert
                                    </Button>
                                </ListItem>
                            ))}
                        </List>
                    </CardContent>
                    <CardActions>
                        <Button onClick={() => applyTemplateStructure(suggestion)}>
                            Apply Structure
                        </Button>
                    </CardActions>
                </Card>
            )}
        </Box>
    );
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
- [ ] Committed with message: `feat(DMS-015-T3): Frontend Template Builder with AI`
