# W10: Workflow Studio UI - Implementation Complete ✅

**Datum:** 2025-01-14  
**Status:** ✅ Core Implementation Complete

## 📋 Přehled

W10 zavádí **drag-and-drop visual workflow editor**, který umožňuje:

- ✅ Vizuální tvorbu workflow jako state machine
- ✅ Paleta nodů (State, Decision, End)
- ✅ Konfiguraci nodů (actions, guards, metadata)
- ✅ Konfiguraci transitions (labels, conditions)
- ✅ Real-time validaci
- ✅ Export/import definic
- ✅ React Flow canvas s zoom/pan

---

## 🎨 Frontend Component

### `WorkflowStudio.tsx`

Hlavní editor component postavený na **React Flow**.

#### Props

```typescript
interface WorkflowStudioProps {
  entityType: string;                           // Typ entity (ORDER, INVOICE, ...)
  initialDefinition?: any;                      // Načtení existující definice
  onSave: (definition: any) => Promise<void>;   // Callback při uložení
  onValidate?: (definition: any) => Promise<ValidationResult>; // Validace
}
```

#### Features

**1. Node Palette** (levý sidebar)
- **State Node** - běžný stav s actions a guards
- **Decision Node** - rozhodovací bod (žlutý border)
- **End Node** - koncový stav (červený)

**2. React Flow Canvas**
- Drag & drop nodů
- Kreslení transitions (edges)
- Zoom, pan, minimap
- Background grid

**3. Toolbar** (horní lišta)
- **Validate** - validace workflow struktury
- **Export** - export do JSON souboru
- **Save** - uložení definice přes `onSave` callback

**4. Node Configuration Dialog**
Double-click na node otevře dialog s:
- **Label** - název stavu
- **Actions** - seznam akcí (chip management)
- **Guards** - seznam guardů (chip management)

**5. Edge Configuration Dialog**
Double-click na edge otevře dialog s:
- **Label** - název transitions (e.g., "submit", "approve")
- **Condition** - podmínka (e.g., `status == 'APPROVED'`)

---

## 🔧 Implementation Details

### Node Types

```typescript
interface NodeData {
  label: string;
  type: 'state' | 'decision' | 'end';
  actions?: string[];     // Pro type='state'
  guards?: string[];      // Pro všechny typy
  metadata?: Record<string, any>;
}
```

**Příklad node:**
```json
{
  "id": "state-1",
  "type": "default",
  "data": {
    "label": "Submitted",
    "type": "state",
    "actions": ["sendEmail", "notifyAdmin"],
    "guards": ["isAuthenticated", "hasPermission"]
  },
  "position": { "x": 250, "y": 100 }
}
```

### Edge Types

```typescript
interface EdgeData {
  label?: string;
  condition?: string;
  guards?: string[];
}
```

**Příklad edge:**
```json
{
  "id": "e1",
  "source": "state-1",
  "target": "state-2",
  "label": "approve",
  "data": {
    "condition": "status == 'APPROVED' && amount < 1000"
  }
}
```

### Export Format

Při uložení/exportu se generuje definice:

```json
{
  "entityType": "ORDER",
  "nodes": [
    {
      "id": "start",
      "type": "state",
      "label": "Start",
      "position": { "x": 250, "y": 0 },
      "actions": [],
      "guards": []
    },
    {
      "id": "state-1",
      "type": "state",
      "label": "Submitted",
      "position": { "x": 250, "y": 150 },
      "actions": ["sendEmail", "updateDatabase"],
      "guards": ["isAuthenticated"]
    },
    {
      "id": "end",
      "type": "end",
      "label": "End",
      "position": { "x": 250, "y": 300 }
    }
  ],
  "edges": [
    {
      "id": "e1",
      "source": "start",
      "target": "state-1",
      "label": "submit",
      "condition": null
    },
    {
      "id": "e2",
      "source": "state-1",
      "target": "end",
      "label": "complete",
      "condition": "isCompleted == true"
    }
  ]
}
```

---

## 🧪 Testy

### Unit Tests: `WorkflowStudio.test.tsx`

**Testované scénáře:**

1. ✅ **Rendering** - zobrazení header s entity type
2. ✅ **Node Palette** - tlačítka pro přidání nodů
3. ✅ **Add Node** - kliknutí na State/Decision/End přidá node
4. ✅ **Validate Disabled** - validate button je disabled když není onValidate
5. ✅ **Validate Enabled** - validate button je enabled když je onValidate
6. ✅ **onSave Callback** - volá se s definicí
7. ✅ **Validation Success** - zobrazí "Valid workflow"
8. ✅ **Validation Errors** - zobrazí chyby a warnings
9. ✅ **Initial Definition** - načte existující definici
10. ✅ **Helper Text** - zobrazí nápovědu o double-click
11. ✅ **Export Button** - má export tlačítko

**Spuštění:**
```bash
cd frontend
npm test -- WorkflowStudio.test.tsx
```

---

## 📊 Use Cases

### UC1: Vytvoření nového workflow

1. Otevři WorkflowStudio pro entity type `ORDER`

2. Přidej nody z palety:
   - State: "Draft"
   - State: "Submitted"
   - State: "Approved"
   - End: "End"

3. Nakresli transitions:
   - Draft → Submitted (event: "submit")
   - Submitted → Approved (event: "approve")
   - Approved → End (event: "complete")

4. Double-click na "Submitted" node:
   - Přidej action: `sendEmail`
   - Přidej action: `notifyManager`
   - Přidej guard: `isAuthenticated`

5. Double-click na transition "approve":
   - Nastav condition: `amount < 10000`

6. Klikni **Validate** - zkontroluje strukturu

7. Klikni **Save** - uloží definici

### UC2: Editace existujícího workflow

```tsx
<WorkflowStudio
  entityType="ORDER"
  initialDefinition={existingWorkflow}  // Načte z DB
  onSave={async (def) => {
    await api.updateWorkflow(workflowId, def);
  }}
  onValidate={async (def) => {
    return await api.validateWorkflow(def);
  }}
/>
```

### UC3: Export pro verzování

1. Vytvoř workflow v editoru

2. Klikni **Export**

3. Stáhne se `workflow-ORDER-{timestamp}.json`

4. Tento soubor lze použít pro:
   - Vytvoření nové verze přes W9 API
   - Backup
   - Code review v gitu

---

## 🎯 Klíčové vlastnosti

### ✅ Co máme

1. **React Flow Integration** - profesionální canvas s zoom/pan
2. **Node Palette** - 3 typy nodů (state, decision, end)
3. **Drag & Drop** - intuitivní umístění nodů
4. **Node Configuration** - dialog s actions, guards, label
5. **Edge Configuration** - dialog s label, condition
6. **Real-time Validation** - volitelná validace před uložením
7. **Export** - JSON soubor ke stažení
8. **Initial Definition** - načítání existujících workflow
9. **MiniMap** - přehledná miniatura canvasu
10. **Unit Tests** - 11 test cases pokrývající UI interakce

### 🔜 Co zbývá (pro finalizaci)

- [ ] Integration test (načtení, editace, uložení)
- [ ] E2E test (vytvoření workflow end-to-end)
- [ ] Custom node renderers (vizuální styling pro decision nodes)
- [ ] Guard/action autocomplete (suggestions z existujících)
- [ ] Undo/Redo funkcionalita
- [ ] Keyboard shortcuts (Ctrl+S pro save, Delete pro smazání nodu)

---

## 🎨 Styling & UX

### Node Styly

- **State Node** - defaultní modrý border
- **Decision Node** - oranžový border (`borderColor: '#ff9800'`)
- **End Node** - červený output node

### Layout

```
┌─────────────────────────────────────────────────────────┐
│ Workflow Studio - ORDER     [Validate] [Export] [Save]  │
├──────────┬──────────────────────────────────────────────┤
│          │                                               │
│  Node    │                                               │
│  Palette │           React Flow Canvas                  │
│          │                                               │
│  [State] │         (Drag & drop nodes here)             │
│          │                                               │
│ [Decision│                                               │
│          │                                               │
│  [End]   │                                               │
│          │                                               │
│  ───────│                                               │
│          │                                               │
│ Validation│         [MiniMap]                           │
│  Result  │                                               │
│          │                                               │
│  Help    │         [Controls]                           │
│  Text    │                                               │
└──────────┴──────────────────────────────────────────────┘
```

---

## 🔗 Integrace s W9 Versioning

WorkflowStudio vygeneruje definici, která se může uložit jako nová verze:

```typescript
// V aplikaci:
const handleSaveWorkflow = async (definition: any) => {
  // 1. Ulož jako nová verze
  const versionId = await api.post('/api/v1/workflows/versions', {
    entityType: definition.entityType,
    schemaDefinition: definition,  // nodes + edges
    createdBy: currentUser,
    notes: "Created via Workflow Studio"
  });

  // 2. Aktivuj verzi
  await api.post(`/api/v1/workflows/versions/${versionId}/activate`);

  toast.success('Workflow saved and activated!');
};

<WorkflowStudio
  entityType="ORDER"
  onSave={handleSaveWorkflow}
  onValidate={validateWorkflowLogic}
/>
```

---

## 🧩 Validace

Příklad validační funkce:

```typescript
const validateWorkflow = async (definition: any): Promise<ValidationResult> => {
  const errors: string[] = [];
  const warnings: string[] = [];

  // Zkontroluj že existuje end node
  const hasEndNode = definition.nodes.some(n => n.type === 'end');
  if (!hasEndNode) {
    errors.push('No end node found. Every workflow must have an end state.');
  }

  // Zkontroluj že všechny nody jsou propojené
  const connectedNodes = new Set<string>();
  definition.edges.forEach(e => {
    connectedNodes.add(e.source);
    connectedNodes.add(e.target);
  });

  definition.nodes.forEach(node => {
    if (!connectedNodes.has(node.id) && node.id !== 'start') {
      warnings.push(`Node "${node.label}" is disconnected.`);
    }
  });

  // Zkontroluj že decision nodes mají alespoň 2 výstupy
  definition.nodes
    .filter(n => n.type === 'decision')
    .forEach(decision => {
      const outgoingEdges = definition.edges.filter(e => e.source === decision.id);
      if (outgoingEdges.length < 2) {
        errors.push(`Decision node "${decision.label}" must have at least 2 outgoing transitions.`);
      }
    });

  return {
    valid: errors.length === 0,
    errors,
    warnings
  };
};
```

---

## 📝 Příklad Workflow Definition

Komplexní ORDER workflow:

```json
{
  "entityType": "ORDER",
  "nodes": [
    {
      "id": "start",
      "type": "state",
      "label": "Start",
      "position": { "x": 250, "y": 0 },
      "actions": [],
      "guards": []
    },
    {
      "id": "draft",
      "type": "state",
      "label": "Draft",
      "position": { "x": 250, "y": 100 },
      "actions": ["initializeOrder"],
      "guards": []
    },
    {
      "id": "submitted",
      "type": "state",
      "label": "Submitted",
      "position": { "x": 250, "y": 200 },
      "actions": ["sendEmail", "notifyWarehouse"],
      "guards": ["isAuthenticated", "hasItems"]
    },
    {
      "id": "approval-check",
      "type": "decision",
      "label": "Approval Check",
      "position": { "x": 250, "y": 300 },
      "guards": ["requiresApproval"]
    },
    {
      "id": "approved",
      "type": "state",
      "label": "Approved",
      "position": { "x": 100, "y": 400 },
      "actions": ["processPayment", "allocateInventory"],
      "guards": []
    },
    {
      "id": "auto-approved",
      "type": "state",
      "label": "Auto-Approved",
      "position": { "x": 400, "y": 400 },
      "actions": ["processPayment"],
      "guards": []
    },
    {
      "id": "end",
      "type": "end",
      "label": "End",
      "position": { "x": 250, "y": 500 }
    }
  ],
  "edges": [
    {
      "id": "e1",
      "source": "start",
      "target": "draft",
      "label": "create"
    },
    {
      "id": "e2",
      "source": "draft",
      "target": "submitted",
      "label": "submit",
      "condition": "items.length > 0"
    },
    {
      "id": "e3",
      "source": "submitted",
      "target": "approval-check",
      "label": "check"
    },
    {
      "id": "e4",
      "source": "approval-check",
      "target": "approved",
      "label": "needs-approval",
      "condition": "amount >= 1000"
    },
    {
      "id": "e5",
      "source": "approval-check",
      "target": "auto-approved",
      "label": "auto-approve",
      "condition": "amount < 1000"
    },
    {
      "id": "e6",
      "source": "approved",
      "target": "end",
      "label": "complete"
    },
    {
      "id": "e7",
      "source": "auto-approved",
      "target": "end",
      "label": "complete"
    }
  ]
}
```

---

## 📚 Závěr

**W10 Studio UI je funkční** a připravený pro:
- Vizuální tvorbu workflow jako state machines
- Konfiguraci nodů a transitions
- Real-time validaci
- Export/import definic
- Integraci s W9 versioning API

Zbývá doplnit IT/E2E testy, custom renderers a pokročilé UX features pro production-ready stav.

**Next:** W11 Workflow Testing (simulation mode)
