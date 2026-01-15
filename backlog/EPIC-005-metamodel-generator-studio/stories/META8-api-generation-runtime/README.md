---
id: META-008
epic: EPIC-005-metamodel-generator-studio
title: "API Generation Runtime"
priority: P1
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "120 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META8-api-generation-runtime/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-008: API Generation Runtime

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🔴 **CRITICAL** - Klíčová funkcionalita  
**Priorita:** P1 (High Priority)  
**Estimated LOC:** ~1,500 řádků  
**Effort:** 3 týdny (120 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **automaticky generovat REST API endpoints z YAML metamodelu**, abych **eliminoval boilerplate CRUD kontrolery a zajistil konzistentní API napříč entitami**.

---

## 🎯 Business Value

**Proč je to důležité:**
- **10x rychlejší vývoj**: Nové entity → API za minuty, ne dny
- **Konzistence**: Všechny entity mají stejnou strukturu API (filtry, stránkování, třídění)
- **Maintainability**: Změna v metamodelu → automatická aktualizace API
- **RLS enforcement**: Tenant filtering vynucen na API vrstvě

**HIGH-LEVEL požadavek:**
> 4️⃣ Generování API & UI: Metamodel generuje CRUD endpointy (včetně filtrů, stránkování, třídění), Search DSL (z polí označených jako searchable), RLS / tenant filtr, Validace (constraints z metamodelu → BE validační logika), Export (CSV/Excel/JSON) podle povolených polí.

---

## 🎯 Acceptance Criteria

### AC1: Generic CRUD Endpoints Generation
- **GIVEN** YAML entity definition:
  ```yaml
  entity: Product
  table: products
  tenant_aware: true
  
  fields:
    - name: name
      type: string
      searchable: true
      filterable: true
    - name: price
      type: decimal
      sortable: true
  ```
- **WHEN** metamodel je načten
- **THEN** automaticky vytvoří REST endpoints:
  - `GET /api/products` - list s filtrováním
  - `GET /api/products/{id}` - detail
  - `POST /api/products` - create
  - `PUT /api/products/{id}` - update
  - `DELETE /api/products/{id}` - delete

### AC2: Filter DSL Support
- **GIVEN** pole označené `filterable: true`
- **WHEN** volám `GET /api/products?filter=price>100&filter=name~'laptop'`
- **THEN** API vrátí:
  - Produkty s `price > 100` AND `name` obsahující 'laptop'
  - Filter syntax podporuje: `=`, `!=`, `>`, `<`, `>=`, `<=`, `~` (LIKE), `IN`, `NOT IN`

### AC3: Search DSL (Full-text)
- **GIVEN** pole označené `searchable: true`
- **WHEN** volám `GET /api/products?search=laptop dell`
- **THEN** API vrátí:
  - Full-text search přes všechna `searchable` pole
  - Relevance scoring (PostgreSQL `ts_rank`)
  - Highlight matched terms

### AC4: Pagination & Sorting
- **GIVEN** jakákoli entita
- **WHEN** volám `GET /api/products?page=2&size=20&sort=price,desc&sort=name,asc`
- **THEN** API vrátí:
  - Stránku 2 (offset 20)
  - 20 items per page
  - Seřazeno: primárně `price DESC`, sekundárně `name ASC`
  - Response obsahuje: `totalElements`, `totalPages`, `currentPage`, `hasNext`

### AC5: RLS (Row-Level Security) Auto-injection
- **GIVEN** entity s `tenant_aware: true`
- **WHEN** volám API s `X-Tenant-ID: 123` header
- **THEN** automaticky přidá do všech queries:
  ```sql
  WHERE tenant_id = 123
  ```
- **AND** pokud chybí tenant header → vrátí `403 Forbidden`

### AC6: Validation from Metamodel Constraints
- **GIVEN** field s constraints:
  ```yaml
  - name: email
    type: string
    constraints:
      - type: pattern
        value: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
      - type: required
  ```
- **WHEN** volám `POST /api/users` s invalidním emailem
- **THEN** API vrátí `400 Bad Request`:
  ```json
  {
    "errors": [
      {
        "field": "email",
        "constraint": "pattern",
        "message": "Email format is invalid"
      }
    ]
  }
  ```

### AC7: Export Endpoints (CSV/Excel/JSON)
- **GIVEN** entita s polem označeným `exportable: true`
- **WHEN** volám `GET /api/products/export?format=csv&filter=price>100`
- **THEN** API vrátí:
  - CSV soubor s filtrovanými daty
  - Pouze `exportable` pole (respektuje field-level permissions)
  - Content-Disposition header: `attachment; filename="products-2025-11-08.csv"`

### AC8: API Response Format Consistency
- **GIVEN** jakákoli entita
- **WHEN** volám list endpoint
- **THEN** response má strukturu:
  ```json
  {
    "data": [...],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 145,
      "totalPages": 8
    },
    "filters": {
      "applied": ["price>100"],
      "available": ["name", "price", "category"]
    }
  }
  ```

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | CRUD runtime router + metamodel endpoint registry | 40h | META-006 |
| 2 | Filter/search DSL parser + query builder | 30h | T1, META-020 |
| 3 | Pagination/sorting + export endpoints + response schema | 25h | T1 |
| 4 | Validation + tenant/field-level enforcement | 15h | META-007, META-016 |
| 5 | Testy (API + security + export) + docs | 10h | T1, T2, T3, T4 |

---

## 🏗️ Implementation Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│ Metamodel YAML                                          │
│   entity: Product                                       │
│   fields: [name, price, category]                       │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ MetamodelApiGenerator                                   │
│  - parseEntitySchema()                                  │
│  - generateEndpoints()                                  │
│  - registerRoutes()                                     │
└────────────────┬────────────────────────────────────────┘
                 │
    ┌────────────┼────────────┬────────────┐
    ▼            ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐
│ CRUD    │ │ Filter   │ │ Search  │ │ Export   │
│ Handler │ │ DSL      │ │ Engine  │ │ Service  │
└─────────┘ └──────────┘ └─────────┘ └──────────┘
    │            │            │            │
    └────────────┴────────────┴────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ Generic Repository Layer                                │
│  - JPA Criteria API                                     │
│  - Specification Builder                                │
│  - RLS Filter Injector                                  │
└─────────────────────────────────────────────────────────┘
```

### Core Components

**1. MetamodelApiGenerator**
```java
@Component
public class MetamodelApiGenerator {
    
    private final MetamodelLoader metamodelLoader;
    private final RequestMappingHandlerMapping handlerMapping;
    
    @PostConstruct
    public void registerDynamicEndpoints() {
        List<EntitySchema> schemas = metamodelLoader.loadAll();
        
        for (EntitySchema schema : schemas) {
            registerCrudEndpoints(schema);
            registerSearchEndpoint(schema);
            registerExportEndpoint(schema);
        }
    }
    
    private void registerCrudEndpoints(EntitySchema schema) {
        String basePath = "/api/" + schema.getTable();
        
        // GET /api/products
        registerEndpoint("GET", basePath, 
            request -> handleList(schema, request));
        
        // GET /api/products/{id}
        registerEndpoint("GET", basePath + "/{id}", 
            request -> handleGet(schema, request));
        
        // POST /api/products
        registerEndpoint("POST", basePath, 
            request -> handleCreate(schema, request));
        
        // PUT /api/products/{id}
        registerEndpoint("PUT", basePath + "/{id}", 
            request -> handleUpdate(schema, request));
        
        // DELETE /api/products/{id}
        registerEndpoint("DELETE", basePath + "/{id}", 
            request -> handleDelete(schema, request));
    }
}
```

**2. FilterDslParser**
```java
@Component
public class FilterDslParser {
    
    /**
     * Parse filter expressions like:
     * - price>100
     * - name~'laptop'
     * - category IN ('electronics','books')
     */
    public Specification<Object> parseFilter(String filterExpr, EntitySchema schema) {
        // Tokenize: field, operator, value
        FilterToken token = tokenize(filterExpr);
        
        // Validate field exists in schema
        FieldSchema field = schema.getField(token.field);
        if (!field.isFilterable()) {
            throw new IllegalArgumentException("Field not filterable: " + token.field);
        }
        
        // Build JPA Specification
        return (root, query, cb) -> {
            switch (token.operator) {
                case ">":
                    return cb.greaterThan(root.get(token.field), token.value);
                case ">=":
                    return cb.greaterThanOrEqualTo(root.get(token.field), token.value);
                case "<":
                    return cb.lessThan(root.get(token.field), token.value);
                case "<=":
                    return cb.lessThanOrEqualTo(root.get(token.field), token.value);
                case "=":
                    return cb.equal(root.get(token.field), token.value);
                case "!=":
                    return cb.notEqual(root.get(token.field), token.value);
                case "~": // LIKE
                    return cb.like(root.get(token.field), "%" + token.value + "%");
                case "IN":
                    return root.get(token.field).in(parseList(token.value));
                default:
                    throw new UnsupportedOperationException("Operator: " + token.operator);
            }
        };
    }
}
```

**3. SearchEngine**
```java
@Component
public class SearchEngine {
    
    /**
     * Full-text search across searchable fields
     * Uses PostgreSQL to_tsvector & ts_rank
     */
    public Page<Object> search(String query, EntitySchema schema, Pageable pageable) {
        // Get all searchable fields
        List<FieldSchema> searchableFields = schema.getFields().stream()
            .filter(FieldSchema::isSearchable)
            .collect(Collectors.toList());
        
        if (searchableFields.isEmpty()) {
            throw new IllegalStateException("No searchable fields in entity: " + schema.getEntity());
        }
        
        // Build full-text search query
        String tsQuery = String.format(
            "to_tsvector('english', %s) @@ to_tsquery('english', ?)",
            searchableFields.stream()
                .map(f -> "COALESCE(" + f.getName() + ", '')")
                .collect(Collectors.joining(" || ' ' || "))
        );
        
        // Execute with ranking
        String sql = String.format(
            "SELECT *, ts_rank(to_tsvector('english', %s), to_tsquery('english', ?)) AS rank " +
            "FROM %s " +
            "WHERE %s " +
            "ORDER BY rank DESC",
            // ... field concatenation
            schema.getTable(),
            tsQuery
        );
        
        return jdbcTemplate.query(sql, new Object[]{query, query}, rowMapper);
    }
}
```

**4. RlsFilterInjector**
```java
@Component
public class RlsFilterInjector implements SpecificationCustomizer {
    
    @Override
    public <T> Specification<T> customize(Specification<T> spec, EntitySchema schema, HttpServletRequest request) {
        if (!schema.isTenantAware()) {
            return spec; // No RLS needed
        }
        
        // Extract tenant from header
        String tenantHeader = request.getHeader("X-Tenant-ID");
        if (tenantHeader == null) {
            throw new ForbiddenException("Missing X-Tenant-ID header for tenant-aware entity");
        }
        
        Long tenantId = Long.parseLong(tenantHeader);
        
        // Inject tenant filter
        Specification<T> tenantSpec = (root, query, cb) -> 
            cb.equal(root.get("tenantId"), tenantId);
        
        // Combine with existing spec
        return spec != null ? spec.and(tenantSpec) : tenantSpec;
    }
}
```

**5. ValidationEngine**
```java
@Component
public class MetamodelValidator {
    
    public void validate(Object entity, EntitySchema schema) {
        List<ValidationError> errors = new ArrayList<>();
        
        for (FieldSchema field : schema.getFields()) {
            Object value = getFieldValue(entity, field.getName());
            
            // Required constraint
            if (field.isRequired() && value == null) {
                errors.add(new ValidationError(field.getName(), "required", "Field is required"));
            }
            
            // Pattern constraint (regex)
            if (field.getPattern() != null && value != null) {
                if (!value.toString().matches(field.getPattern())) {
                    errors.add(new ValidationError(field.getName(), "pattern", "Invalid format"));
                }
            }
            
            // Min/Max length
            if (field.getMinLength() != null && value != null) {
                if (value.toString().length() < field.getMinLength()) {
                    errors.add(new ValidationError(field.getName(), "minLength", 
                        "Minimum length: " + field.getMinLength()));
                }
            }
            
            // Custom constraints
            for (Constraint constraint : field.getConstraints()) {
                validateConstraint(value, constraint, errors);
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
```

**6. ExportService**
```java
@Service
public class ExportService {
    
    public StreamingResponseBody exportCsv(EntitySchema schema, Specification<Object> filters) {
        return outputStream -> {
            // Get exportable fields
            List<FieldSchema> exportableFields = schema.getFields().stream()
                .filter(FieldSchema::isExportable)
                .collect(Collectors.toList());
            
            // CSV Writer
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream));
            
            // Header row
            String[] headers = exportableFields.stream()
                .map(FieldSchema::getLabel)
                .toArray(String[]::new);
            writer.writeNext(headers);
            
            // Data rows (stream to avoid memory issues)
            repository.findAll(filters).forEach(entity -> {
                String[] row = exportableFields.stream()
                    .map(f -> getFieldValue(entity, f.getName()))
                    .map(Object::toString)
                    .toArray(String[]::new);
                writer.writeNext(row);
            });
            
            writer.close();
        };
    }
}
```

---

## 🔧 YAML Schema Extensions

### Field-Level Metadata for API Generation

```yaml
entity: Product
table: products
tenant_aware: true

fields:
  - name: name
    type: string
    label: "Product Name"
    
    # API Generation flags
    searchable: true      # Include in full-text search
    filterable: true      # Allow filtering (?filter=name~'laptop')
    sortable: true        # Allow sorting (?sort=name,asc)
    exportable: true      # Include in CSV/Excel exports
    
    # Constraints (for validation)
    constraints:
      - type: required
      - type: minLength
        value: 3
      - type: maxLength
        value: 255
  
  - name: price
    type: decimal
    label: "Price"
    
    filterable: true
    sortable: true
    exportable: true
    
    constraints:
      - type: required
      - type: min
        value: 0.01
      - type: max
        value: 999999.99
  
  - name: internal_notes
    type: text
    
    # NOT exportable (internal only)
    searchable: false
    filterable: false
    exportable: false     # Hidden from exports
    
    visibility: adminOnly  # Field-level security
```

---

## 📊 API Examples

### List with Filters
```bash
GET /api/products?filter=price>100&filter=category='electronics'&sort=price,desc&page=1&size=20

Response:
{
  "data": [
    {
      "id": 42,
      "name": "Laptop Dell XPS",
      "price": 1299.99,
      "category": "electronics"
    }
  ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 145,
    "totalPages": 8
  },
  "filters": {
    "applied": ["price>100", "category='electronics'"],
    "available": ["name", "price", "category", "brand"]
  }
}
```

### Full-text Search
```bash
GET /api/products?search=laptop dell xps

Response:
{
  "data": [
    {
      "id": 42,
      "name": "Laptop Dell XPS 13",
      "description": "High-performance laptop with Dell quality",
      "_searchRank": 0.893  # Relevance score
    }
  ]
}
```

### Export
```bash
GET /api/products/export?format=csv&filter=price>500

Response Headers:
Content-Type: text/csv
Content-Disposition: attachment; filename="products-2025-11-08.csv"

Response Body:
Name,Price,Category
"Laptop Dell XPS",1299.99,"electronics"
"MacBook Pro",2499.99,"electronics"
```

---

## 🧪 Testing Strategy

### Unit Tests
```java
@Test
void shouldGenerateListEndpoint() {
    EntitySchema schema = createProductSchema();
    
    MetamodelApiGenerator generator = new MetamodelApiGenerator();
    generator.registerCrudEndpoints(schema);
    
    // Verify endpoint registered
    assertTrue(handlerMapping.hasEndpoint("GET", "/api/products"));
}

@Test
void shouldParseFilterDsl() {
    String filter = "price>100";
    
    Specification<Product> spec = filterDslParser.parseFilter(filter, productSchema);
    
    // Verify SQL generated
    String sql = specificationToSql(spec);
    assertEquals("WHERE price > 100", sql);
}

@Test
void shouldInjectRlsFilter() {
    HttpServletRequest request = mockRequest("X-Tenant-ID", "123");
    
    Specification<Product> spec = rlsInjector.customize(null, productSchema, request);
    
    String sql = specificationToSql(spec);
    assertTrue(sql.contains("tenant_id = 123"));
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
class ApiGenerationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldFilterProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("filter", "price>100")
                .header("X-Tenant-ID", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].price").value(greaterThan(100)));
    }
    
    @Test
    void shouldRejectMissingTenantHeader() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Missing X-Tenant-ID header"));
    }
}
```

---

## 📦 Deliverables

1. **Core Components** (~600 LOC)
   - `MetamodelApiGenerator.java`
   - `DynamicEndpointRegistry.java`

2. **Filter & Search** (~400 LOC)
   - `FilterDslParser.java`
   - `SearchEngine.java`
   - `SpecificationBuilder.java`

3. **Security & Validation** (~300 LOC)
   - `RlsFilterInjector.java`
   - `MetamodelValidator.java`
   - `ConstraintEvaluator.java`

4. **Export Service** (~200 LOC)
   - `ExportService.java`
   - `CsvExporter.java`
   - `ExcelExporter.java`
   - `JsonExporter.java`

5. **Tests** (~400 LOC)
   - Unit tests pro všechny komponenty
   - Integration tests pro API endpoints

6. **Documentation**
   - API specification (OpenAPI/Swagger)
   - Filter DSL reference
   - RLS security guide

---

## 🔗 Dependencies

**Blocking:**
- ❌ META-001 (Schema Diff) - DONE ✅
- ❌ META-007 (Validation) - PLANNED (validace constraintů)

**Blocked By:**
- META-016 (RBAC) - field-level permissions
- META-020 (Search DSL) - advanced search features

**Integrates With:**
- META-009 (UI Generation) - API konzumuje frontend
- META-011 (Streaming) - API publishuje eventy do Kafky
- META-013 (Reporting) - export data pro reporty

---

## 🎯 Success Metrics

**Functionality:**
- ✅ Všechny CRUD operace fungují pro novou entitu
- ✅ Filtry, třídění, stránkování podporují 100% use cases
- ✅ RLS automaticky aplikován na všechny queries
- ✅ Validation z metamodelu funguje na API vrstvě

**Performance:**
- List endpoint: < 100ms (bez filtrů)
- Filtered query: < 200ms
- Export: streaming (žádné OOM na 1M+ rows)

**Code Quality:**
- Zero duplicitní CRUD kód v projektu
- 80%+ test coverage pro generátor
- Swagger docs auto-generated z metamodelu

---

**Story Owner:** Backend Team  
**Estimated Start:** Q1 2026  
**Priority:** P1 - High (klíčová funkcionalita platformy)  
**Risk:** Medium (komplexní dynamické routing, JPA Criteria API edge cases)
