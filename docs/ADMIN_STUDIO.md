# 🎨 Metamodel Studio - Admin Documentation

## Overview

Metamodel Studio je admin GUI pro správu aplikačního metamodelu. Umožňuje validovat, upravovat a schvalovat změny v entitách, polích, relacích a workflow krocích.

## Role & Access

**Required Role:** `CORE_ADMIN_STUDIO`

Pouze uživatelé s touto rolí mají přístup k `/core-admin/studio`.

## Workflow: Validate → Propose → Approve → Publish

### 1. **View Mode (Read-only)**
- Zobrazení všech entit v metamodelu
- Vyhledávání a filtrování
- Detail entity s poli, validacemi, policies

### 2. **Edit Mode**
- Editace entity (název, tabulka, pole)
- Přidání/odebrání polí
- Nastavení flags (required, unique)
- JSON editor pro pokročilé případy (S10-F)

### 3. **Validate**
- Client-side validation (PascalCase, snake_case)
- Server-side validation (POST `/api/admin/studio/validate`)
- Zobrazení chyb s path v JSON

Příklad validačních pravidel:
- Entity name: `^[A-Z][a-zA-Z0-9]*$` (PascalCase)
- Table name: `^[a-z][a-z0-9_]*$` (snake_case)
- Fields: min 1 field required
- Field name & type required

### 4. **Diff (S10-D)**
- Side-by-side porovnání current vs draft
- Highlight změn (added/removed/modified)

### 5. **Propose (S10-D)**
- Vytvoření change request (CR)
- Uložení snapshotu změn
- Přiřazení autora a popisu

### 6. **Approve (S10-D)**
- Schválení CR
- Bump `specVersion++`
- Aplikace změn na metamodel

### 7. **Publish (Hot Reload)**
- Volání `/api/admin/metamodel/reload`
- Invalidace UI cache
- Změny viditelné v hlavním GUI bez redeploye

## API Endpoints

### Studio API (`/api/admin/studio`)

#### Export Entities
```http
GET /api/admin/studio/entities
Authorization: Bearer <token>
X-Tenant-Id: <tenant>

Response:
{
  "status": "success",
  "entitiesCount": 5,
  "entities": [
    {
      "name": "User",
      "entity": "User",
      "table": "users_directory",
      "fields": [...],
      "accessPolicy": {...},
      "ui": {...}
    }
  ]
}
```

#### Get Single Entity
```http
GET /api/admin/studio/entities/{entity}

Response:
{
  "name": "User",
  "entity": "User",
  "table": "users_directory",
  "fields": [...]
}
```

#### Validate Entity Draft
```http
POST /api/admin/studio/validate
Content-Type: application/json

{
  "entity": "TestEntity",
  "table": "test_entities",
  "fields": [
    { "name": "id", "type": "bigint", "required": true },
    { "name": "name", "type": "string", "required": true }
  ]
}

Response (valid):
{
  "status": "valid",
  "errors": []
}

Response (invalid):
{
  "status": "invalid",
  "errors": [
    {
      "field": "entity",
      "message": "Entity name must start with capital letter",
      "severity": "error"
    }
  ]
}
```

## UI Components

### ModelTree
- Načítá všechny entity z BE
- Vyhledávání (search box)
- Filtrování
- Kliknutím vybere entitu pro detail

### EntityDetail (Read-only)
- Zobrazení metadata (entity, table, idField, versionField)
- Tabulka polí (name, type, flags)
- Access Policy (JSON)
- UI Config (JSON)
- Navigation Config (JSON)

### EntityEditor (Draft mode)
- Editace entity metadata
- Přidání/odebrání polí
- Toggle flags (Required, Unique)
- Validate button → volá BE validaci
- Save Draft button (disabled dokud není valid)

## Fallback: JSON Editor (S10-F)

Pro edge cases, které GUI nepodporuje, je k dispozici raw JSON editor (Monaco):
- Export current entity → JSON
- Edit JSON manually
- Import → validate → propose

## Limitations

### Current Phase (S10-C)
- ✅ Read-only viewer
- ✅ Entity editor (basic fields)
- ✅ Validation (client + server)
- ❌ Diff viewer (S10-D TODO)
- ❌ Propose/Approve workflow (S10-D TODO)
- ❌ Workflow steps editor (S10-E TODO)
- ❌ Undo/Redo (S10-F TODO)

### Not Supported (Yet)
- Relations editor (S10-C extended)
- Validation rules editor (business rules, CEL/SpEL)
- Workflow step input/output mapping
- Correlation ID config
- OpenAPI/AsyncAPI references

## Testing

### Unit Tests
```bash
# Frontend
cd frontend && npm test -- MetamodelStudioPage.test.tsx --run
cd frontend && npm test -- ModelTree.test.tsx --run

# Backend
cd backend && ./mvnw test -Dtest=StudioAdminControllerIT
```

### E2E Tests
```bash
npx playwright test e2e/pre/08_studio_rbac.spec.ts
```

### Integration Test Scenario
1. Login as studio-admin@muriel.cz
2. Navigate to `/core-admin/studio`
3. Select entity "User"
4. Click "Edit" button
5. Add new field: `bio` (type: text)
6. Click "Validate" → should pass
7. Click "Save Draft" → success message
8. (S10-D) Click "Propose" → creates CR
9. (S10-D) Click "Approve" → applies changes
10. Verify new field appears in main GUI

## Security

### RBAC
- All endpoints protected with `@PreAuthorize("hasAuthority('CORE_ADMIN_STUDIO')")`
- FE route guard checks `user.roles.includes('CORE_ADMIN_STUDIO')`
- Non-admin users see "Přístup odepřen" message

### Audit Logging
- All changes logged to audit trail
- Author, timestamp, description
- Before/after snapshots

## Troubleshooting

### "Přístup odepřen"
→ Missing `CORE_ADMIN_STUDIO` role. Contact admin to assign role.

### "Validation failed"
→ Check entity name (PascalCase), table name (snake_case), fields (min 1 required).

### "Failed to load entities"
→ Check BE is running, `/api/admin/studio/entities` returns 200.

### "Save Draft disabled"
→ Run "Validate" first. Fix all errors before saving.

## Roadmap

### S10-D (Next)
- Diff viewer (side-by-side)
- Propose/Approve workflow
- specVersion bumping
- Hot reload integration

### S10-E
- Workflow steps editor
- Input/output mapping
- Dry-run validator

### S10-F
- Undo/Redo
- Autosave (debounced)
- Export/Import draft JSON
- Quick actions (Duplicate, Jump to relation)

---

**Status:** S10-C Complete ✅  
**Next:** S10-D (Diff/Propose/Approve)
