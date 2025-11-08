# META-005: Visual Studio UI (React Schema Designer)

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED** (UI mockups exist)  
**Priorita:** P1 (High-value feature)  
**Estimated LOC:** ~800 řádků (React + backend API)  

---

## 📋 Story Description

Jako **low-code user**, chci **visual editor pro tvorbu entit**, abych **mohl designovat schema bez znalosti YAML syntaxe pomocí drag-and-drop UI**.

---

## 🎯 Acceptance Criteria

### AC1: Visual Entity Designer
- **GIVEN** prázdný canvas
- **WHEN** kliknu "New Entity"
- **THEN** zobrazí modal s formulářem:
  - Entity name (text input)
  - Table name (auto-generated, editovatelné)
  - Fields (list)
  - Save / Cancel buttons

### AC2: Field Editor
- **GIVEN** entita s 3 fields
- **WHEN** přidám nový field
- **THEN** zobrazí field editor:
  - Field name (text)
  - Type (dropdown: string, integer, decimal, boolean, date, text)
  - Properties:
    - [ ] Nullable
    - [ ] Unique
    - Default value (optional)
  - Add / Remove buttons

### AC3: Relationship Designer
- **GIVEN** 2 entity "User" a "Tenant"
- **WHEN** přidám relationship "User.tenant → Tenant"
- **THEN** visual arrow mezi entitami
  - Type: Many-to-One / One-to-Many / Many-to-Many
  - FK column auto-generated (`tenant_id`)
  - Bidirectional option

### AC4: YAML Preview & Sync
- **GIVEN** visual design změny
- **WHEN** kliknu "Preview YAML"
- **THEN** zobrazí split view:
  - Left: Visual designer
  - Right: Generated YAML (live preview)
- **AND** tlačítko "Apply Changes" → volá META-002 reload API

---

## 🏗️ Planned Implementation

### UI Mockup (Design)

```
┌─────────────────────────────────────────────────────────────┐
│ Metamodel Studio                              [+ New Entity] │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│   ┌──────────┐                  ┌──────────┐                │
│   │  User    │                  │  Tenant  │                │
│   ├──────────┤                  ├──────────┤                │
│   │ id       │───────────────→  │ id       │                │
│   │ name     │    tenant_id     │ name     │                │
│   │ email    │                  │ slug     │                │
│   │ created  │                  └──────────┘                │
│   └──────────┘                                               │
│                                                               │
│   [Field Properties Panel]                                   │
│   Name: email                                                │
│   Type: [string ▼]                                           │
│   ☑ Unique   ☐ Nullable                                      │
│   Default: (empty)                                           │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### React Components (Planned)

```typescript
// MetamodelStudio.tsx (main component)
export function MetamodelStudio() {
  const [entities, setEntities] = useState<Entity[]>([]);
  const [selectedEntity, setSelectedEntity] = useState<Entity | null>(null);
  
  return (
    <div className="metamodel-studio">
      <Toolbar onNewEntity={handleNewEntity} />
      <Canvas entities={entities} onSelectEntity={setSelectedEntity} />
      <PropertyPanel entity={selectedEntity} onChange={handleEntityChange} />
      <YamlPreview entities={entities} />
    </div>
  );
}

// EntityCard.tsx (draggable entity card)
export function EntityCard({ entity, onSelect }: EntityCardProps) {
  return (
    <div className="entity-card" onClick={() => onSelect(entity)}>
      <h3>{entity.name}</h3>
      <ul>
        {entity.fields.map(f => (
          <li key={f.name}>
            {f.name}: {f.type} {f.unique && '🔒'}
          </li>
        ))}
      </ul>
    </div>
  );
}

// FieldEditor.tsx (field properties form)
export function FieldEditor({ field, onChange }: FieldEditorProps) {
  return (
    <div className="field-editor">
      <input
        value={field.name}
        onChange={e => onChange({ ...field, name: e.target.value })}
        placeholder="Field name"
      />
      <select
        value={field.type}
        onChange={e => onChange({ ...field, type: e.target.value as FieldType })}
      >
        <option value="string">String</option>
        <option value="integer">Integer</option>
        <option value="decimal">Decimal</option>
        <option value="boolean">Boolean</option>
        <option value="date">Date</option>
        <option value="text">Text</option>
      </select>
      <label>
        <input
          type="checkbox"
          checked={field.unique}
          onChange={e => onChange({ ...field, unique: e.target.checked })}
        />
        Unique
      </label>
      <label>
        <input
          type="checkbox"
          checked={field.nullable}
          onChange={e => onChange({ ...field, nullable: e.target.checked })}
        />
        Nullable
      </label>
    </div>
  );
}

// YamlPreview.tsx (live YAML generation)
export function YamlPreview({ entities }: YamlPreviewProps) {
  const yaml = useMemo(() => {
    return entities.map(e => generateYaml(e)).join('\n---\n');
  }, [entities]);
  
  return (
    <div className="yaml-preview">
      <h3>YAML Preview</h3>
      <pre><code className="language-yaml">{yaml}</code></pre>
      <button onClick={handleApplyChanges}>Apply Changes</button>
    </div>
  );
}

function generateYaml(entity: Entity): string {
  return `
entity: ${entity.name}
table: ${entity.table}
fields:
${entity.fields.map(f => `  - name: ${f.name}
    type: ${f.type}${f.unique ? '\n    unique: true' : ''}${f.nullable ? '\n    nullable: true' : ''}`).join('\n')}
  `.trim();
}
```

### Backend API (Planned)

```java
@RestController
@RequestMapping("/api/admin/metamodel/studio")
public class MetamodelStudioController {
    
    @GetMapping("/entities")
    public List<EntitySchema> getAllEntities() {
        return yamlLoader.loadAll();
    }
    
    @PostMapping("/entities")
    public ResponseEntity<EntitySchema> createEntity(@RequestBody EntitySchema schema) {
        // 1. Validate schema
        validator.validate(schema);
        
        // 2. Save to YAML file
        yamlWriter.save(schema);
        
        // 3. Trigger hot reload (META-002)
        reloadService.reload();
        
        return ResponseEntity.ok(schema);
    }
    
    @PutMapping("/entities/{name}")
    public ResponseEntity<EntitySchema> updateEntity(
        @PathVariable String name,
        @RequestBody EntitySchema schema
    ) {
        yamlWriter.update(name, schema);
        reloadService.reload();
        return ResponseEntity.ok(schema);
    }
    
    @DeleteMapping("/entities/{name}")
    public ResponseEntity<Void> deleteEntity(@PathVariable String name) {
        yamlWriter.delete(name);
        reloadService.reload();
        return ResponseEntity.noContent().build();
    }
}
```

---

## 🧪 Planned Tests

```typescript
// MetamodelStudio.test.tsx
describe('MetamodelStudio', () => {
  it('should create new entity via UI', async () => {
    render(<MetamodelStudio />);
    
    // Click "New Entity"
    await userEvent.click(screen.getByText('New Entity'));
    
    // Fill form
    await userEvent.type(screen.getByLabelText('Entity Name'), 'Product');
    await userEvent.type(screen.getByLabelText('Table Name'), 'products');
    
    // Add field
    await userEvent.click(screen.getByText('Add Field'));
    await userEvent.type(screen.getByLabelText('Field Name'), 'name');
    await userEvent.selectOptions(screen.getByLabelText('Type'), 'string');
    await userEvent.click(screen.getByLabelText('Unique'));
    
    // Save
    await userEvent.click(screen.getByText('Save'));
    
    // Verify API call
    expect(mockApi.post).toHaveBeenCalledWith('/api/admin/metamodel/studio/entities', {
      name: 'Product',
      table: 'products',
      fields: [
        { name: 'name', type: 'string', unique: true }
      ]
    });
  });
  
  it('should show live YAML preview', () => {
    const entities = [
      { name: 'User', table: 'users', fields: [{ name: 'email', type: 'string', unique: true }] }
    ];
    
    render(<YamlPreview entities={entities} />);
    
    expect(screen.getByText(/entity: User/)).toBeInTheDocument();
    expect(screen.getByText(/table: users/)).toBeInTheDocument();
    expect(screen.getByText(/unique: true/)).toBeInTheDocument();
  });
});
```

---

## 💡 Expected Value

### Benefits
- **Low-Code UX**: Non-developers can design entities
- **Visual Clarity**: See entity relationships graphically
- **YAML Learning**: Live preview teaches YAML syntax
- **Fast Prototyping**: 5 min to design 3-entity schema (vs 20 min manual YAML)

### Use Cases
- **Product Manager**: Design `Product`, `Category`, `Tag` entities visually
- **Consultant**: Demo platform capabilities to client
- **Developer**: Rapid prototyping of new modules

---

## 🔗 Related

- **Depends On:** 
  - [META-001 (Schema Diff)](META-001.md) - YAML parser
  - [META-002 (Hot Reload)](META-002.md) - Apply changes API
- **Enables:** EPIC-007 S10 (Metamodel Studio full feature)
- **Inspired By:** Strapi Content-Type Builder, Hasura Console

---

## 📚 References

- **Design Mockups:** `docs/metamodel/studio-ui-mockups.png` (TODO)
- **React Flow:** https://reactflow.dev/ (for visual editor)
- **YAML.js:** https://github.com/eemeli/yaml (for live preview)
