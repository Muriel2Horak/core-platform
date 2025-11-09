# DMS-015: AI Template Suggestions (BONUS)

**Epic:** EPIC-008 Document Management System  
**Phase:** 6 - AI Features (BONUS)  
**Estimate:** 0.5 day  
**LOC:** ~300

## Story

**AS** metamodel designer  
**I WANT** AI-generated document template suggestions  
**SO THAT** template creation is faster

## Implementation

### 1. AI Template Service

**File:** `backend/src/main/java/cz/muriel/core/dms/ai/TemplateAIService.java`

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

### 2. MCP Integration (Context-Aware)

**Using MCP for metamodel context:**

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

### 3. Frontend Template Builder with AI

**Component:** Template editor with AI assistant

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

### 4. Example AI Prompt/Response

**Input (for Contract entity):**

```
Create a DOCX template structure for entity: Contract
Entity fields:
- contractNumber (string)
- clientName (string)
- startDate (date)
- endDate (date)
- totalAmount (decimal)
- paymentTerms (text)

Generate:
1. Template structure
2. Field mapping suggestions
3. Sample content layout
```

**AI Response:**

```json
{
  "structure": "HEADER: Logo + Contract Title\nPARAGRAPH: Contract Number, Date\nTABLE: Client Information\nPARAGRAPH: Terms and Conditions\nTABLE: Payment Schedule\nFOOTER: Signature Block",
  "fieldMappings": [
    {"field": "contractNumber", "placeholder": "${entity.contractNumber}", "label": "Contract #"},
    {"field": "clientName", "placeholder": "${entity.clientName}", "label": "Client"},
    {"field": "totalAmount", "placeholder": "${entity.totalAmount}", "format": "currency"}
  ],
  "sampleContent": "Contract for ${entity.clientName}\nEffective ${entity.startDate} - ${entity.endDate}\nTotal Value: ${entity.totalAmount}"
}
```

## API

POST `/api/dms/templates/ai-suggest` - Generate template suggestion  
POST `/api/dms/templates/mcp-suggest` - Context-aware suggestion (MCP)

## Acceptance Criteria

- [ ] AI service generates template suggestions
- [ ] MCP integration provides metamodel context
- [ ] Frontend shows AI suggestions
- [ ] User can apply suggested structure
- [ ] E2E: Select Contract entity → AI Suggest → Apply template

## Deliverables

- `TemplateAIService.java`
- `TemplateMCPService.java`
- `TemplateBuilder.tsx` (with AI assistant)
- OpenAI integration config
- MCP integration example
