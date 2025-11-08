# META-015: Loki Integration

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED**  
**Priorita:** P2 (Medium)  
**Estimated LOC:** ~800 řádků  
**Effort:** 2 týdny (80 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **query Loki logs z metamodelu**, abych **mohl zobrazit logy jako read-only entity**.

---

## 🎯 Business Value

**HIGH-LEVEL požadavek:**
> 9️⃣ Loki / log data jako "read-only entita": Loki je chápán jako další read-only datasource v metamodelu (entita typu `logSource`, pole mapovaná na labels/JSON fields, filtry a časové rozsahy definované v meta). FE používá stejné generické komponenty (tabulka nad logy, grafy time-series, export). RBAC, tenanti a query limity opět řízené metamodel pravidly.

---

## 🎯 Acceptance Criteria

### AC1: Log Entity Definition

```yaml
entity: ApplicationLog
storageType: log
logSource: loki

fields:
  - name: timestamp
    type: datetime
    logMapping: __timestamp__
  
  - name: level
    type: enum
    values: [DEBUG, INFO, WARN, ERROR]
    logMapping: level  # Loki label
  
  - name: message
    type: text
    logMapping: msg    # JSON field
  
  - name: service
    type: string
    logMapping: service  # Loki label
```

### AC2: LogQL Query Builder

- **GIVEN** filters: `level=ERROR`, `service=backend`
- **THEN** generuje LogQL:

```logql
{service="backend"} 
  | json 
  | level="ERROR" 
  | __timestamp__ > 2025-11-08T00:00:00Z
```

### AC3: UI Log Viewer

- **GIVEN** ApplicationLog entity
- **THEN** Generic Table zobrazí logy
- **Filtry**: Time range, level, service
- **Export**: CSV obsahující pouze visible fields

---

## 🏗️ Implementation

```java
@Service
public class LokiQueryService {
    
    private final RestTemplate lokiRestTemplate;
    
    public List<LogEntry> query(EntitySchema schema, Map<String, Object> filters) {
        String logQL = buildLogQL(schema, filters);
        
        String url = String.format("%s/loki/api/v1/query_range?query=%s&start=%s&end=%s",
            lokiBaseUrl, URLEncoder.encode(logQL), startTime, endTime);
        
        LokiResponse response = lokiRestTemplate.getForObject(url, LokiResponse.class);
        
        return response.getData().getResult().stream()
            .flatMap(stream -> stream.getValues().stream())
            .map(value -> parseLogEntry(value, schema))
            .collect(Collectors.toList());
    }
    
    private String buildLogQL(EntitySchema schema, Map<String, Object> filters) {
        // {service="backend"} | json | level="ERROR"
        List<String> labelFilters = new ArrayList<>();
        List<String> jsonFilters = new ArrayList<>();
        
        filters.forEach((fieldName, value) -> {
            FieldSchema field = schema.getField(fieldName);
            if (field.getLogMapping() != null) {
                if (isLokiLabel(field)) {
                    labelFilters.add(field.getLogMapping() + "=\"" + value + "\"");
                } else {
                    jsonFilters.add(field.getLogMapping() + "=\"" + value + "\"");
                }
            }
        });
        
        StringBuilder logQL = new StringBuilder("{");
        logQL.append(String.join(", ", labelFilters));
        logQL.append("} | json");
        
        if (!jsonFilters.isEmpty()) {
            logQL.append(" | ").append(String.join(" | ", jsonFilters));
        }
        
        return logQL.toString();
    }
}
```

---

**Story Owner:** Backend Team  
**Priority:** P2  
**Effort:** 2 týdny
