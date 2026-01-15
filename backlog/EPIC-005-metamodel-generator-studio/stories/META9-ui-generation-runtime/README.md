---
id: META-009
epic: EPIC-005-metamodel-generator-studio
title: "UI Generation Runtime"
priority: P1
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "160 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META9-ui-generation-runtime/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-009: UI Generation Runtime

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🔴 **CRITICAL** - Klíčová funkcionalita  
**Priorita:** P1 (High Priority)  
**Estimated LOC:** ~2,500 řádků  
**Effort:** 4 týdny (160 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **automaticky generovat React UI komponenty z YAML metamodelu**, abych **eliminoval ručně psané formuláře a tabulky a zajistil konzistentní UX napříč entitami**.

---

## 🎯 Business Value

**Proč je to důležité:**
- **20x rychlejší FE vývoj**: Nová entita → kompletní CRUD UI za minuty
- **Konzistentní UX**: Všechny entity mají stejný look & feel
- **Metadata-driven**: Změna v YAML → automatická aktualizace UI
- **Zero boilerplate**: Žádné ručně psané formuláře, tabulky, validace

**HIGH-LEVEL požadavek:**
> 4️⃣ Generování UI: Deklarativní UI spec na základě metamodelu - default list view (sloupce, filtry, quick actions), detail view (záložky, groups, read-only fields), inline edit / bulk edit (jen pro povolená pole), default pohledy, user-saved pohledy, komponenty stavěné genericky: tabulka, detail, graf, timeline.

---

## 🎯 Acceptance Criteria

### AC1: Generic List View Component
- **GIVEN** YAML entity s fields
- **WHEN** uživatel naviguje na `/app/products`
- **THEN** zobrazí se:
  - Tabulka s sloupci z `fields` označených `visible: true`
  - Filtry pro `filterable` pole
  - Třídění pro `sortable` pole
  - Stránkování
  - Quick actions (Edit, Delete)

### AC2: Detail View with Tabs & Groups
- **GIVEN** entity s konfigurace UI:
  ```yaml
  ui:
    detail:
      tabs:
        - name: "Basic Info"
          groups:
            - name: "Product Details"
              fields: [name, price, category]
        - name: "Documents"
          component: DocumentTab
  ```
- **WHEN** uživatel klikne na detail
- **THEN** zobrazí se:
  - Záložkové rozhraní (Basic Info, Documents)
  - Pole seskupená podle `groups`
  - Read-only pole označená `editable: false`

### AC3: Form Generation from Schema
- **GIVEN** field s typem a constraints:
  ```yaml
  - name: email
    type: email
    required: true
    pattern: "^[a-z@.]+"
  ```
- **WHEN** zobrazím create/edit formulář
- **THEN** vygeneruje se:
  - Input type="email"
  - Required validace (červená hvězdička)
  - Pattern validace (chybová hláška)
  - Auto-focus na první pole

### AC4: Inline Edit (Table Cells)
- **GIVEN** pole označené `inlineEditable: true`
- **WHEN** uživatel double-click na buňku v tabulce
- **THEN**:
  - Buňka se změní na input
  - Změna se odešle na API po blur
  - Zobrazí se loading spinner
  - Success/error feedback

### AC5: Bulk Edit
- **GIVEN** multi-select v tabulce
- **WHEN** uživatel vybere 5 řádků a klikne "Bulk Edit"
- **THEN**:
  - Otevře se dialog s polem "Category" (editovatelné pole)
  - Po save se změní kategorie u všech 5 produktů
  - Progress bar zobrazí 1/5, 2/5, ..., 5/5

### AC6: User-Saved Views
- **GIVEN** uživatel nastaví filtry + sloupce
- **WHEN** klikne "Save View"
- **THEN**:
  - View se uloží do localStorage/backend
  - Zobrazí se v dropdown "My Views"
  - Ostatní uživatelé vidí "Shared Views" (pokud sdíleno)

### AC7: Field Types Rendering
- **GIVEN** různé field types
- **THEN** vyrenderuje se:
  - `string` → `<input type="text">`
  - `number` → `<input type="number">`
  - `email` → `<input type="email">`
  - `date` → `<DatePicker>`
  - `boolean` → `<Checkbox>`
  - `enum` → `<Select options={...}>`
  - `ref` (foreign key) → `<Autocomplete entity={...}>`
  - `json` → `<CodeEditor mode="json">`

### AC8: Relationship Navigation
- **GIVEN** pole typu `ref`:
  ```yaml
  - name: category
    type: ref
    target: Category
  ```
- **WHEN** zobrazím v detailu
- **THEN**:
  - Zobrazí se jako link: `<a href="/app/categories/123">Electronics</a>`
  - Klik otevře detail Category entity

---

## 🏗️ Implementation Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│ Metamodel YAML                                          │
│   entity: Product                                       │
│   ui: { list, detail, form }                            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ MetamodelUILoader (Frontend)                            │
│  - fetchSchema() from /api/admin/metamodel              │
│  - parseUiConfig()                                      │
│  - buildComponentTree()                                 │
└────────────────┬────────────────────────────────────────┘
                 │
    ┌────────────┼────────────┬────────────┐
    ▼            ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐
│ Generic │ │ Generic  │ │ Generic │ │ Field    │
│ Table   │ │ Detail   │ │ Form    │ │ Renderers│
└─────────┘ └──────────┘ └─────────┘ └──────────┘
```

### Core Components

**1. MetamodelUILoader (React Context)**
```tsx
// frontend/src/contexts/MetamodelContext.tsx
export const MetamodelProvider: React.FC = ({ children }) => {
  const [schemas, setSchemas] = useState<Record<string, EntitySchema>>({});
  
  useEffect(() => {
    // Fetch all entity schemas on app load
    fetch('/api/admin/metamodel/schemas')
      .then(res => res.json())
      .then(data => {
        const schemaMap = {};
        data.forEach(schema => {
          schemaMap[schema.entity] = schema;
        });
        setSchemas(schemaMap);
      });
  }, []);
  
  const getSchema = (entityName: string) => schemas[entityName];
  
  return (
    <MetamodelContext.Provider value={{ schemas, getSchema }}>
      {children}
    </MetamodelContext.Provider>
  );
};

export const useMetamodel = (entityName: string) => {
  const { getSchema } = useContext(MetamodelContext);
  return getSchema(entityName);
};
```

**2. GenericTable Component**
```tsx
// frontend/src/components/generic/GenericTable.tsx
interface GenericTableProps {
  entityName: string;
  filters?: Record<string, any>;
  onRowClick?: (id: string) => void;
}

export const GenericTable: React.FC<GenericTableProps> = ({ 
  entityName, filters, onRowClick 
}) => {
  const schema = useMetamodel(entityName);
  const [data, setData] = useState([]);
  const [pagination, setPagination] = useState({ page: 1, size: 20 });
  
  // Fetch data from API
  useEffect(() => {
    const queryString = buildQueryString(filters, pagination);
    fetch(`/api/${schema.table}?${queryString}`)
      .then(res => res.json())
      .then(result => {
        setData(result.data);
        setPagination(result.pagination);
      });
  }, [schema, filters, pagination]);
  
  // Get visible columns from schema
  const columns = schema.fields
    .filter(f => f.ui?.list?.visible !== false)
    .map(field => ({
      key: field.name,
      label: field.label || field.name,
      sortable: field.sortable,
      render: (value, row) => renderFieldValue(value, field, row)
    }));
  
  return (
    <DataGrid
      columns={columns}
      data={data}
      pagination={pagination}
      onPageChange={page => setPagination(prev => ({ ...prev, page }))}
      onRowClick={row => onRowClick?.(row.id)}
      sortable
      filterable
    />
  );
};
```

**3. GenericDetail Component**
```tsx
// frontend/src/components/generic/GenericDetail.tsx
interface GenericDetailProps {
  entityName: string;
  id: string;
}

export const GenericDetail: React.FC<GenericDetailProps> = ({ entityName, id }) => {
  const schema = useMetamodel(entityName);
  const [entity, setEntity] = useState(null);
  
  useEffect(() => {
    fetch(`/api/${schema.table}/${id}`)
      .then(res => res.json())
      .then(data => setEntity(data));
  }, [schema, id]);
  
  if (!entity) return <Skeleton />;
  
  // Render tabs from UI config
  const tabs = schema.ui?.detail?.tabs || [
    { name: "Details", fields: schema.fields.map(f => f.name) }
  ];
  
  return (
    <Tabs>
      {tabs.map(tab => (
        <Tab key={tab.name} label={tab.name}>
          <TabPanel>
            {renderTabContent(tab, entity, schema)}
          </TabPanel>
        </Tab>
      ))}
    </Tabs>
  );
};

function renderTabContent(tab, entity, schema) {
  if (tab.component === "DocumentTab") {
    return <DocumentTab entityId={entity.id} entityType={schema.entity} />;
  }
  
  // Render field groups
  return tab.groups?.map(group => (
    <FieldGroup key={group.name} title={group.name}>
      {group.fields.map(fieldName => {
        const field = schema.fields.find(f => f.name === fieldName);
        return (
          <FieldDisplay
            key={fieldName}
            label={field.label}
            value={entity[fieldName]}
            field={field}
          />
        );
      })}
    </FieldGroup>
  ));
}
```

**4. GenericForm Component**
```tsx
// frontend/src/components/generic/GenericForm.tsx
interface GenericFormProps {
  entityName: string;
  id?: string; // undefined = create, string = edit
  onSave?: (entity: any) => void;
}

export const GenericForm: React.FC<GenericFormProps> = ({ entityName, id, onSave }) => {
  const schema = useMetamodel(entityName);
  const { register, handleSubmit, formState: { errors } } = useForm();
  
  // Load existing entity if editing
  useEffect(() => {
    if (id) {
      fetch(`/api/${schema.table}/${id}`)
        .then(res => res.json())
        .then(data => reset(data));
    }
  }, [id, schema]);
  
  const onSubmit = (data) => {
    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/${schema.table}/${id}` : `/api/${schema.table}`;
    
    fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
      .then(res => res.json())
      .then(result => {
        toast.success('Saved successfully');
        onSave?.(result);
      })
      .catch(err => toast.error(err.message));
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {schema.fields
        .filter(f => f.ui?.form?.visible !== false)
        .map(field => (
          <FormField
            key={field.name}
            field={field}
            register={register}
            errors={errors}
          />
        ))}
      
      <Button type="submit">Save</Button>
    </form>
  );
};
```

**5. Field Renderers**
```tsx
// frontend/src/components/generic/FieldRenderers.tsx
export const renderFieldValue = (value: any, field: FieldSchema, row: any) => {
  switch (field.type) {
    case 'string':
    case 'text':
      return <span>{value}</span>;
    
    case 'number':
    case 'decimal':
      return <span className="text-right">{formatNumber(value)}</span>;
    
    case 'date':
      return <span>{formatDate(value)}</span>;
    
    case 'datetime':
      return <span>{formatDateTime(value)}</span>;
    
    case 'boolean':
      return <Checkbox checked={value} disabled />;
    
    case 'enum':
      return <Chip label={value} color={getEnumColor(field, value)} />;
    
    case 'ref':
      // Foreign key reference
      const targetEntity = field.targetEntity;
      const targetId = value;
      return (
        <Link to={`/app/${targetEntity.toLowerCase()}/${targetId}`}>
          {row[field.name + '_display'] || targetId}
        </Link>
      );
    
    case 'json':
      return (
        <IconButton onClick={() => showJsonDialog(value)}>
          <JsonIcon />
        </IconButton>
      );
    
    default:
      return <span>{String(value)}</span>;
  }
};

export const FormField: React.FC<{ field: FieldSchema }> = ({ field, register, errors }) => {
  const validation = buildValidationRules(field);
  
  switch (field.type) {
    case 'string':
      return (
        <TextField
          {...register(field.name, validation)}
          label={field.label}
          error={!!errors[field.name]}
          helperText={errors[field.name]?.message}
          required={field.required}
        />
      );
    
    case 'email':
      return (
        <TextField
          {...register(field.name, { ...validation, pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/ })}
          type="email"
          label={field.label}
          error={!!errors[field.name]}
        />
      );
    
    case 'number':
      return (
        <TextField
          {...register(field.name, validation)}
          type="number"
          label={field.label}
          inputProps={{ min: field.min, max: field.max }}
        />
      );
    
    case 'date':
      return (
        <DatePicker
          {...register(field.name, validation)}
          label={field.label}
        />
      );
    
    case 'boolean':
      return (
        <FormControlLabel
          control={<Checkbox {...register(field.name)} />}
          label={field.label}
        />
      );
    
    case 'enum':
      return (
        <Select
          {...register(field.name, validation)}
          label={field.label}
        >
          {field.enumValues.map(val => (
            <MenuItem key={val} value={val}>{val}</MenuItem>
          ))}
        </Select>
      );
    
    case 'ref':
      return (
        <AutocompleteField
          {...register(field.name, validation)}
          label={field.label}
          entityName={field.targetEntity}
          displayField={field.displayField || 'name'}
        />
      );
    
    case 'json':
      return (
        <CodeEditor
          {...register(field.name, validation)}
          mode="json"
          label={field.label}
        />
      );
  }
};
```

**6. Inline Edit Handler**
```tsx
// frontend/src/components/generic/InlineEditCell.tsx
export const InlineEditCell: React.FC<{ 
  value: any; 
  field: FieldSchema; 
  row: any;
  onSave: (newValue: any) => Promise<void>;
}> = ({ value, field, row, onSave }) => {
  const [editing, setEditing] = useState(false);
  const [tempValue, setTempValue] = useState(value);
  const [saving, setSaving] = useState(false);
  
  const handleSave = async () => {
    setSaving(true);
    try {
      await onSave(tempValue);
      setEditing(false);
    } catch (err) {
      toast.error('Save failed');
    } finally {
      setSaving(false);
    }
  };
  
  if (!field.inlineEditable) {
    return renderFieldValue(value, field, row);
  }
  
  if (!editing) {
    return (
      <div onDoubleClick={() => setEditing(true)} className="cursor-pointer hover:bg-gray-100">
        {renderFieldValue(value, field, row)}
      </div>
    );
  }
  
  return (
    <div className="flex items-center gap-2">
      <TextField
        value={tempValue}
        onChange={e => setTempValue(e.target.value)}
        onBlur={handleSave}
        autoFocus
        size="small"
      />
      {saving && <CircularProgress size={16} />}
    </div>
  );
};
```

---

## 🔧 YAML Schema Extensions for UI

```yaml
entity: Product
table: products

fields:
  - name: name
    type: string
    label: "Product Name"
    
    # UI Configuration
    ui:
      list:
        visible: true        # Show in table
        width: 200           # Column width
        sortable: true
        filterable: true
      detail:
        visible: true
        editable: true
        group: "Basic Info"  # Field grouping
      form:
        visible: true
        placeholder: "Enter product name"
        helpText: "Unique identifier for the product"
    
    # Inline edit
    inlineEditable: true
    
  - name: internal_notes
    type: text
    
    ui:
      list:
        visible: false       # Hidden from table
      detail:
        visible: true
        editable: true
        group: "Internal"
        visibleFor: ["admin", "manager"]  # Role-based visibility

# UI Layout Configuration
ui:
  list:
    defaultSort:
      - field: name
        direction: asc
    defaultFilters:
      - field: status
        value: "active"
    quickActions:
      - label: "Edit"
        icon: "edit"
        action: "openEditDialog"
      - label: "Delete"
        icon: "delete"
        action: "confirmDelete"
        confirmMessage: "Are you sure?"
  
  detail:
    tabs:
      - name: "Basic Info"
        icon: "info"
        groups:
          - name: "Product Details"
            fields: [name, price, category, status]
          - name: "Inventory"
            fields: [stock, warehouse_location]
      
      - name: "Documents"
        component: "DocumentTab"
        props:
          allowUpload: true
          allowedTypes: ["pdf", "docx"]
      
      - name: "History"
        component: "AuditLogTab"
  
  form:
    layout: "grid"  # or "vertical"
    columns: 2
    sections:
      - title: "Basic Information"
        fields: [name, category, price]
      - title: "Additional Details"
        fields: [description, tags]
```

---

## 📦 Deliverables

1. **Core Context & Hooks** (~300 LOC)
   - `MetamodelContext.tsx`
   - `useMetamodel.ts`
   - `useEntityData.ts`

2. **Generic Components** (~1,200 LOC)
   - `GenericTable.tsx`
   - `GenericDetail.tsx`
   - `GenericForm.tsx`
   - `InlineEditCell.tsx`
   - `BulkEditDialog.tsx`

3. **Field Renderers** (~500 LOC)
   - `FieldDisplay.tsx`
   - `FieldInput.tsx`
   - `AutocompleteField.tsx` (ref fields)
   - `CodeEditor.tsx` (json fields)

4. **UI Utilities** (~300 LOC)
   - `buildValidationRules.ts`
   - `formatters.ts`
   - `queryBuilder.ts`

5. **Tests** (~500 LOC)
   - Unit tests pro každou komponentu
   - Integration tests (Playwright)

---

## 🔗 Dependencies

**Blocking:**
- ❌ META-008 (API Generation) - UI konzumuje API

**Integrates With:**
- META-012 (Workflow) - workflow buttons v UI
- META-014 (DMS) - DocumentTab component
- META-016 (RBAC) - field visibility based on roles

---

## 🎯 Success Metrics

- New entity → Full CRUD UI in < 5 minutes
- Zero hand-coded forms/tables
- Consistent UX across all entities
- 90%+ user adoption (prefer generated UI over custom)

---

**Story Owner:** Frontend Team  
**Priority:** P1 - High  
**Effort:** 4 týdny
