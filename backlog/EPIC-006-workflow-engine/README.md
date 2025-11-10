# EPIC-006: Workflow Orchestration (Internal Engine + External n8n)

> 🔄 **Unified Architecture:** Single EPIC s 2 vrstvami - Internal metamodel-driven engine + External n8n orchestration hub

**Status:** � **70% COMPLETE** (Phase 1 done, Phase 2 in progress)  
**Implementováno:** Říjen 2024 - Leden 2025 (W1-W12), **Listopad 2025+ (WF12-WF19, N8N1-N8N10)**  
**LOC:** ~18,000 (existing) + ~10,600 (planned: Phase 2 ~6.2k + Phase 3 Multi-Tenant n8n ~4.4k)  
**Tests:** 119 unit + 15 integration (existing)  
**Dokumentace:** [`WORKFLOW_UNIFIED_ARCHITECTURE.md`](../WORKFLOW_UNIFIED_ARCHITECTURE.md), [`EPIC-011-n8n-workflow-automation`](../EPIC-011-n8n-workflow-automation/README.md)

---

## 🎯 Vision

- Interní workflow engine je primární orchestrátor pro všechny Core procesy (entity lifecycle, approvaly, SLA, integrace do metamodelu).
- n8n funguje jako rozšiřující integrační a automation vrstva, nikdy nenahrazuje interní engine.
- Komunikace mezi vrstvami probíhá přes EXTERNAL_TASK executor (Core → n8n → Core), nikdy přímým zápisem n8n do databáze.

**2-vrstvá workflow orchestrace:**

```
┌─────────────────────────────────────────────────────────────┐
│  LAYER 1: INTERNAL ENGINE (metamodel-driven)                │
│  → LAYER 1 = pravda o stavu a pravidlech                    │
│  - Core state machine (states, transitions, guards)         │
│  - Typed executors: APPROVAL, REST_SYNC, KAFKA_COMMAND      │
│  - Sequential step orchestration                            │
│  - Scope: Core business procesy (Order, Invoice, Contract) │
└────────────────────┬────────────────────────────────────────┘
                     │ EXTERNAL_TASK executor
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  LAYER 2: EXTERNAL n8n (visual orchestration)               │
│  → LAYER 2 = integrace, enrichment, AI, ETL, notifikace     │
│  - Integrace: Jira, Confluence, Trello, M365, Google        │
│  - AI/ML pipelines, ETL jobs                                │
│  - 400+ built-in nodes (no custom connectors needed!)       │
│  - Scope: External integrace + heavy data processing        │
└─────────────────────────────────────────────────────────────┘
```

### Business Goals
- **Automatizace procesů**: Core workflows + external integrations
- **Vizualizace stavu**: Real-time přehled (internal + n8n)
- **Auditovatelnost**: Kompletní historie změn
- **Flexibilita**: Změny bez code deployment
- **Plug & Play**: Leverage n8n 400+ nodes (Jira, Confluence, atd.)

---

## 📋 Stories Overview

### ✅ Phase 1: Foundation (W1-W12) - COMPLETE

| ID | Story | Status | LOC | Tests | Value |
|----|-------|--------|-----|-------|-------|
| W1 | Definition Model | ✅ DONE | ~1,200 | 15 | JSON state machine |
| W2 | Persistence Layer | ✅ DONE | ~800 | 12 | PostgreSQL storage |
| W3-W4 | Designer UI | ✅ DONE | ~2,500 | 18 | React Flow editor |
| W5 | Runtime Foundation | ✅ DONE | ~3,000 | 44 | State transitions |
| W6 | Frontend UX | ✅ DONE | ~2,000 | 36 | Graph + Timeline |
| W7 | Executors Framework | ⚠️ PARTIAL | ~2,500 | 24 | Interface OK, **typed executors CHYBÍ** |
| W8 | Timers & SLA | ✅ DONE | ~600 | - | Auto-transitions |
| W9 | Versioning | ✅ DONE | ~1,800 | 8 | Schema evolution |
| W10 | Studio UI | ⚠️ UI ONLY | ~2,200 | 11 | **Backend mock, no storage** |
| W11 | Testing/Simulation | ✅ DONE | ~900 | - | Dry-run mode |
| W12 | Monitoring | ⚠️ METRICS ONLY | ~1,500 | - | **Primární: Prometheus + Loki, runtime UX v FE** |

**Subtotal:** 12/12 stories, ~18,000 LOC, 119 tests

---

### 🔨 Phase 2: Typed Executors & Runtime (WF12-WF19) - IN PROGRESS

| ID | Story | Status | LOC | Priority | Dependencies |
|----|-------|--------|-----|----------|--------------|
| [WF12](stories/WF12-approval-executor/) | APPROVAL Executor | ✅ SPEC READY | 800 | 🔴 HIGH | W5, W7 |
| WF13 | REST_SYNC Executor | ⏳ TODO | 1,200 | 🔴 HIGH | W5, W7 |
| WF14 | KAFKA_COMMAND Executor | ⏳ TODO | 600 | 🔴 HIGH | W5, W7, Kafka |
| WF15 | EXTERNAL_TASK Executor | ⏳ TODO | 800 | 🔴 HIGH | W5, W7, N8N6 |
| WF16 | TIMER/DELAY Executor | ⏳ TODO | 400 | 🟡 MEDIUM | W5, W8 |
| WF17 | Workflow Instance Runtime | ⏳ TODO | 1,500 | 🔴 HIGH | WF12-16 |
| WF18 | Workflow Steps Schema | ⏳ TODO | 600 | 🔴 HIGH | META, W10 |
| WF19 | Admin Grafana Dashboards | ⏳ TODO | 300 | 🟡 MEDIUM | W12, Grafana |

**WF15 / EXTERNAL_TASK handshake (Phase 2 priority):**
1. Core vytvoří `pending external task` (instance reference + payload + `correlationId`) a čeká na výsledek.
2. n8n worker skrze oficiální Core-connector node vyzvedne task, zpracuje jej ve vlastním flow a pošle výsledek zpět přes Core API.
3. Workflow engine naváže na odpověď přes `onSuccess`/`onError` větve a případné kompenzační kroky.

**Požadavky:** idempotence řízená `correlationId`, centrální timeout + retry politika, audit log (kdo/kdy/jaký flow), schopnost dohledat průběh v Loki/W-OPS přes stejné `correlationId`, a nulový přímý zápis n8n do Core DB (vše přes Core API).

**Subtotal:** 1/8 specs ready, ~6,200 LOC planned

---

### 🚀 Phase 3: Multi-Tenant n8n Integration Layer (N8N1-N8N10) - TODO

> 📖 **Kompletní specifikace:** See [`EPIC-011-n8n-workflow-automation`](../EPIC-011-n8n-workflow-automation/README.md)

| ID | Story | Status | LOC | Priority | Dependencies |
|----|-------|--------|-----|----------|--------------|
| N8N1 | Platform Deployment | ⏳ TODO | 400 | 🔴 HIGH | PostgreSQL |
| N8N2 | Keycloak SSO (Multi-Realm) | ⏳ TODO | 300 | 🔴 HIGH | Keycloak, N8N1 |
| N8N3 | Nginx Proxy (Audit Headers) | ⏳ TODO | 200 | 🔴 HIGH | Nginx, N8N1 |
| N8N4 | Workflow Templates | ⏳ TODO | 500 | 🟡 MEDIUM | N8N1-3 |
| N8N5 | Monitoring | ⏳ TODO | 400 | 🟡 MEDIUM | Grafana, N8N1 |
| N8N6 | Testing & Quality Gates | ⏳ TODO | 600 | 🔴 HIGH | N8N1-5 |
| **N8N7** | **n8n Provisioning Service** | ⏳ TODO | 600 | 🔴 HIGH | N8N1-3 |
| **N8N8** | **Multi-Tenant SSO & Routing** | ⏳ TODO | 500 | 🔴 HIGH | N8N7 |
| **N8N9** | **Tenant Isolation & Audit** | ⏳ TODO | 400 | 🔴 HIGH | N8N8 |
| **N8N10** | **Core API Connector Node** | ⏳ TODO | 300 | 🟡 MEDIUM | N8N1-3 |

Jedna instance n8n obsluhuje celý Core cluster; SSO a provisioning (včetně per-tenant účtů `tenant-xxx`) zajišťuje EPIC-011 a probíhá výhradně přes Core FE. Tenant admini přistupují do n8n přes SSO, Core admini mají globální n8n účet, veřejná URL bez SSO není dostupná. EXTERNAL_TASK executor komunikuje pouze s těmito oficiálně provázanými účty a jejich eventy jsou auditované.

**Subtotal:** 0/10 implemented, ~4,400 LOC planned (base ~2,600 + multi-tenant ~1,800)

**Multi-Tenant Features:**
- Tenant model = Keycloak realm = subdoména (`{tenant}.${DOMAIN}`) pro Core i n8n routing (náš finální model).
- Workflow definice jsou buď shared (`system`), nebo per-tenant (uložené s `tenantId/realm` metadaty).
- Všechna runtime API (graph/state/history/forecast) čtou tenant/realm z JWT, filtrují definice i instance a blokují cross-tenant přístup.
- Všechny workflow eventy logujeme do Loki s `tenant`, `realm`, `user` a `correlationId` (audit trail pro interní i EXTERNAL_TASK kroky).
- Per-tenant n8n účty (`tenant-xxx`) + globální n8n admin vznikají přes n8nProvisioningService; konektor auto-injectuje `X-Core-Tenant` a další auditní hlavičky.
- Přístup k n8n designeru jde přes Core FE (SSO), ne veřejnou URL; požadavky n8n→Core používají jen oficiálně svázané účty.

### Workflow Studio vs n8n Studio
- **Workflow Designer / Studio (EPIC-006 + EPIC-005):** definuje interní workflow nad metamodelovými entitami, deployment přes approve/publish, plná integrace s RBAC, auditingem a Core UI.
- **n8n Studio:** integrační nástroj pro tenant adminy / vybrané uživatele; využívá EXTERNAL_TASK + Core API konektor, staví automace jen nad exponovaným a omezeným Core API, nikdy nepřistupuje přímo k DB.

---

### 🛡️ Phase W-OPS: Workflow Operations Console - TODO

| ID | Story | Status | LOC | Priority | Dependencies |
|----|-------|--------|-----|----------|--------------|
| W-OPS | Workflow Operations Console | ⏳ TODO | 1,800 | 🔴 HIGH | W5, W6, W7, WF12-19, W12, WF19 |

**Notes:** W-OPS přináší interní Camunda-style konzoli a je první volbou pro monitoring i řízení workflow. Grafana zůstává volitelným doplňkem pro admin tenant, ale nikdy nenahrazuje Workflow Operations Console ani business UX.

---

### 📊 Total Progress

| Phase | Stories | Status | LOC | Tests |
|-------|---------|--------|-----|-------|
| **Phase 1 (W1-W12)** | 12/12 | ✅ DONE | ~18,000 | 119 |
| **Phase 2 (WF12-WF19)** | 1/8 | 🔨 IN PROGRESS | ~6,200 | TBD |
| **Phase 3 (N8N1-N8N10)** | 0/10 | ⏳ TODO | ~4,400 | TBD |
| **Phase W-OPS (Console)** | 0/1 | ⏳ TODO | ~1,800 | TBD |
| **TOTAL** | **13/31** | **42%** | **~30,400** | **119+** |

---

## 📖 Detailed Stories

### WORK-001: Workflow Definition Model (W1)

**Status:** ✅ **DONE**  
**Implementation:** Říjen 2024  
**LOC:** ~1,200

#### Description
JSON-based workflow definition schema pro reprezentaci state machine s guards, actions, a metadata.

#### Key Features
- **State Machine Model**: States, transitions, guards, actions
- **Node Types**: Start, State, Decision, Loop, Wait, End
- **Validation**: JSON Schema validation
- **Metadata**: Versioning, author, timestamps

#### Implementation
```json
{
  "id": "approval-workflow",
  "version": "1.0.0",
  "states": [
    {
      "id": "draft",
      "type": "STATE",
      "name": "Draft",
      "transitions": [
        {
          "to": "pending_approval",
          "event": "SUBMIT",
          "guards": ["hasRequiredFields"],
          "actions": ["notifyApprovers"]
        }
      ]
    },
    {
      "id": "pending_approval",
      "type": "STATE",
      "name": "Pending Approval",
      "transitions": [
        {
          "to": "approved",
          "event": "APPROVE",
          "guards": ["isApprover"],
          "actions": ["sendApprovalEmail"]
        },
        {
          "to": "rejected",
          "event": "REJECT",
          "guards": ["isApprover"],
          "actions": ["sendRejectionEmail"]
        }
      ]
    }
  ]
}
```

#### Components
- `WorkflowDefinition.java` - Core model
- `WorkflowState.java` - State representation
- `WorkflowTransition.java` - Transition model
- `WorkflowValidator.java` - JSON schema validation

#### Tests
- 15 unit tests covering validation, serialization, edge cases

#### Value
- **Declarative workflows**: Business users can understand JSON
- **Version control**: Workflow definitions in Git
- **Validation**: Catch errors before deployment

---

### WORK-002: Workflow Persistence (W2)

**Status:** ✅ **DONE**  
**Implementation:** Říjen 2024  
**LOC:** ~800

#### Description
PostgreSQL schema a CRUD API pro workflow definitions storage.

#### Key Features
- **DB Schema**: `workflow_definitions` table
- **CRUD Endpoints**: Create, Read, Update, Delete, List
- **Validation**: Pre-save JSON schema validation
- **Audit**: Created/updated timestamps

#### Database Schema
```sql
CREATE TABLE workflow_definitions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_id VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  version VARCHAR(50) NOT NULL,
  definition JSONB NOT NULL,
  status VARCHAR(50) DEFAULT 'DRAFT',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255)
);

CREATE INDEX idx_workflow_status ON workflow_definitions(status);
CREATE INDEX idx_workflow_id ON workflow_definitions(workflow_id);
```

#### REST API
```java
// WorkflowDefinitionController.java
@GetMapping("/api/workflows/definitions")
List<WorkflowDefinitionDTO> listWorkflows();

@GetMapping("/api/workflows/definitions/{id}")
WorkflowDefinitionDTO getWorkflow(@PathVariable UUID id);

@PostMapping("/api/workflows/definitions")
WorkflowDefinitionDTO createWorkflow(@RequestBody WorkflowDefinitionDTO dto);

@PutMapping("/api/workflows/definitions/{id}")
WorkflowDefinitionDTO updateWorkflow(@PathVariable UUID id, @RequestBody WorkflowDefinitionDTO dto);

@DeleteMapping("/api/workflows/definitions/{id}")
void deleteWorkflow(@PathVariable UUID id);
```

#### Tests
- 12 unit tests (CRUD operations, validation, error handling)

#### Value
- **Persistent storage**: Workflows survive restarts
- **RESTful API**: Frontend integration
- **Validation**: Invalid definitions rejected

---

### WORK-003: Designer UI (W3-W4)

**Status:** ✅ **DONE**  
**Implementation:** Říjen 2024  
**LOC:** ~2,500

#### Description
React Flow based visual workflow designer s drag-and-drop, node palette, a live preview.

#### Key Features
- **Drag-and-Drop**: Node palette → canvas
- **Node Types**: State, Decision, Loop, Wait, Start, End
- **Edge Configuration**: Guards, actions, labels
- **Live Preview**: Real-time graph visualization
- **Export/Import**: JSON workflow definition

#### Components
```typescript
// WorkflowDesigner.tsx
export const WorkflowDesigner: React.FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  
  const nodeTypes = {
    state: StateNode,
    decision: DecisionNode,
    loop: LoopNode,
    wait: WaitNode,
    start: StartNode,
    end: EndNode
  };
  
  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      fitView
    >
      <Controls />
      <MiniMap />
      <Background />
      <Panel position="top-left">
        <NodePalette />
      </Panel>
    </ReactFlow>
  );
};
```

#### Node Palette
- **Start Node** (green circle): Entry point
- **State Node** (blue rectangle): Business state
- **Decision Node** (yellow diamond): Conditional routing
- **Loop Node** (purple hexagon): Iteration
- **Wait Node** (orange octagon): Delay/timer
- **End Node** (red circle): Terminal state

#### Tests
- 18 unit tests (Vitest + RTL): drag-drop, node creation, edge connections, export/import

#### Value
- **Visual design**: No code required
- **Intuitive UX**: Familiar diagram metaphor
- **Fast iteration**: Drag, connect, test

---

### WORK-004: Runtime Foundation (W5)

**Status:** ✅ **DONE**  
**Implementation:** Říjen 2024  
**LOC:** ~3,000  
**Tests:** 44 unit + integration tests

#### Description
Execution engine pro workflow instances s DB persistence, Kafka events, a Prometheus metrics.

#### Key Features
- **Instance Management**: Start, pause, resume, cancel
- **State Transitions**: Validate guards, execute actions
- **Event Publishing**: Kafka events pro každý transition
- **Metrics**: Prometheus metrics (durations, error-rate, SLA)
- **History Tracking**: Timeline všech state changes

#### Database Schema
```sql
CREATE TABLE workflow_instances (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_id VARCHAR(255) NOT NULL,
  entity_type VARCHAR(255) NOT NULL,
  entity_id UUID NOT NULL,
  current_state VARCHAR(255) NOT NULL,
  status VARCHAR(50) DEFAULT 'RUNNING',
  context JSONB DEFAULT '{}',
  started_at TIMESTAMP DEFAULT NOW(),
  completed_at TIMESTAMP,
  created_by VARCHAR(255)
);

CREATE TABLE workflow_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  instance_id UUID REFERENCES workflow_instances(id),
  event_type VARCHAR(50) NOT NULL,
  from_state VARCHAR(255),
  to_state VARCHAR(255),
  actor VARCHAR(255),
  metadata JSONB,
  occurred_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE workflow_timers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  instance_id UUID REFERENCES workflow_instances(id),
  state VARCHAR(255) NOT NULL,
  trigger_at TIMESTAMP NOT NULL,
  sla_deadline TIMESTAMP,
  action VARCHAR(255),
  status VARCHAR(50) DEFAULT 'PENDING'
);
```

#### REST API
```java
// WorkflowRuntimeController.java

// Vizualizace grafu s current state
@GetMapping("/api/workflows/{entity}/{id}/graph")
WorkflowGraphDTO getGraph(@PathVariable String entity, @PathVariable UUID id);

// Allowed/blocked transitions + "why not" explanations
@GetMapping("/api/workflows/{entity}/{id}/state")
WorkflowStateDTO getState(@PathVariable String entity, @PathVariable UUID id);

// Timeline s event history
@GetMapping("/api/workflows/{entity}/{id}/history")
List<WorkflowEventDTO> getHistory(@PathVariable String entity, @PathVariable UUID id);

// Forecast: next steps, timers, SLA deadlines
@GetMapping("/api/workflows/{entity}/{id}/forecast")
WorkflowForecastDTO getForecast(@PathVariable String entity, @PathVariable UUID id);
```

#### Kafka Events
```json
{
  "eventType": "ENTER_STATE",
  "instanceId": "uuid",
  "workflowId": "approval-workflow",
  "state": "pending_approval",
  "actor": "user@example.com",
  "timestamp": "2025-01-14T10:30:00Z",
  "metadata": {
    "duration_ms": 1500,
    "previous_state": "draft"
  }
}
```

#### Prometheus Metrics
```java
// Micrometer metrics
workflowMetrics.recordTransition(workflowId, fromState, toState, durationMs);
workflowMetrics.incrementErrorCount(workflowId, state);
workflowMetrics.recordSLABreach(workflowId, state, delayMs);
```

#### Tests
- **Unit**: `WorkflowRuntimeServiceTest.java` (graph, state, history, forecast)
- **Integration**: 
  - `WorkflowApiIT.java` (Testcontainers PostgreSQL)
  - `WorkflowEventsKafkaIT.java` (Testcontainers Kafka)

#### Value
- **WHERE AM I**: Current state visualization
- **WHAT HAPPENED**: Complete audit trail
- **WHAT'S NEXT**: Forecast with timers and SLA

---

### WORK-005: Frontend UX (W6)

**Status:** ✅ **DONE**  
**Implementation:** Leden 2025  
**LOC:** ~2,000  
**Tests:** 36 unit tests

#### Description
React komponenty pro workflow visualization: graph, timeline panel, actions bar.

#### Key Features

##### WorkflowGraph Component
- **React Flow visualization** s current state highlighting
- **Allowed edges**: Zelené, animated (guard pass)
- **Blocked edges**: Šedé, static (guard fail)
- **"Why not" tooltips**: Vysvětlení proč transition blokován
- **Layout toggle**: ELK (hierarchical) / Dagre (compact)
- **Legend**: Visual indicators guide

```typescript
// WorkflowGraph.tsx
export const WorkflowGraph: React.FC<Props> = ({ instanceId }) => {
  const { data: graph } = useWorkflowGraph(instanceId);
  const [layout, setLayout] = useState<'elk' | 'dagre'>('elk');
  
  const nodes = graph.states.map(state => ({
    id: state.id,
    type: 'workflow-state',
    data: {
      label: state.name,
      isCurrent: state.id === graph.currentState,
      isVisited: graph.visitedStates.includes(state.id)
    },
    style: state.id === graph.currentState 
      ? { borderColor: '#1976d2', background: '#e3f2fd' }
      : {}
  }));
  
  const edges = graph.transitions.map(transition => ({
    id: `${transition.from}-${transition.to}`,
    source: transition.from,
    target: transition.to,
    animated: transition.allowed,
    style: {
      stroke: transition.allowed ? '#4caf50' : '#bdbdbd'
    },
    label: transition.event,
    labelStyle: {
      fill: transition.allowed ? '#2e7d32' : '#757575'
    }
  }));
  
  return (
    <ReactFlow nodes={nodes} edges={edges} fitView>
      <Controls />
      <LayoutToggle layout={layout} onChange={setLayout} />
      <WorkflowLegend />
    </ReactFlow>
  );
};
```

##### TimelinePanel Component
- **MUI Timeline** s event history
- **Duration formatting**: `5m 30s`, `2h 15m` (human-readable)
- **SLA badges**: OK (green), WARN (yellow), BREACH (red)
- **Actor tracking**: Kdo provedl akci
- **Relative timestamps**: `date-fns` formatting

```typescript
// TimelinePanel.tsx
export const TimelinePanel: React.FC<Props> = ({ instanceId }) => {
  const { data: history } = useWorkflowTimeline(instanceId);
  
  return (
    <Timeline>
      {history.map(event => (
        <TimelineItem key={event.id}>
          <TimelineSeparator>
            <TimelineDot color={getSLAColor(event.sla)} />
            <TimelineConnector />
          </TimelineSeparator>
          <TimelineContent>
            <Typography variant="h6">{event.toState}</Typography>
            <Typography variant="body2" color="text.secondary">
              {formatDuration(event.durationMs)} • {event.actor}
            </Typography>
            <Chip
              size="small"
              label={event.sla}
              color={getSLAColor(event.sla)}
              icon={<SLAIcon sla={event.sla} />}
            />
          </TimelineContent>
        </TimelineItem>
      ))}
    </Timeline>
  );
};
```

##### ActionsBar Component
- **Context-aware buttons**: Pouze allowed actions pro current state
- **Read-only mode**: Pokud workflow locked (Kafka signal)
- **Stale→Fresh refresh**: 30s timeout před apply action
- **Disabled tooltips**: "Why not" vysvětlení

```typescript
// ActionsBar.tsx
export const ActionsBar: React.FC<Props> = ({ instanceId }) => {
  const { data: state } = useWorkflowState(instanceId);
  const [applyTransition] = useApplyTransition();
  
  return (
    <Box sx={{ display: 'flex', gap: 2 }}>
      {state.allowedTransitions.map(transition => (
        <Tooltip key={transition.event} title={transition.label}>
          <Button
            variant="contained"
            disabled={state.isLocked}
            onClick={() => applyTransition(instanceId, transition.event)}
          >
            {transition.event}
          </Button>
        </Tooltip>
      ))}
      
      {state.blockedTransitions.map(transition => (
        <Tooltip key={transition.event} title={transition.whyNot}>
          <span>
            <Button variant="outlined" disabled>
              {transition.event}
            </Button>
          </span>
        </Tooltip>
      ))}
    </Box>
  );
};
```

#### Tests
- **Unit (Vitest + RTL)**: 36 tests total
  - `WorkflowGraph.test.tsx`: nodes, edges, highlighting, tooltips, layout toggle
  - `TimelinePanel.test.tsx`: durations, SLA badges, actors, empty state
  - `ActionsBar.test.tsx`: allowed/blocked buttons, tooltips, read-only mode

#### Value
- **WHERE AM I**: Visual graph s current state
- **WHAT HAPPENED**: Timeline s audit trail
- **WHAT CAN I DO**: Context-aware actions

---

### WORK-006: Executors (W7)

**Status:** ✅ **DONE**  
**Implementation:** Leden 2025  
**LOC:** ~2,500  
**Tests:** 24 unit tests

#### Description
Executor framework pro action execution: HTTP calls, script execution, human tasks.

#### Key Features

##### Abstract Executor Framework
```java
// WorkflowExecutor.java
public interface WorkflowExecutor {
  ExecutionResult execute(WorkflowExecutionContext context);
  boolean supports(String actionType);
}

// ExecutorRegistry.java
@Component
public class ExecutorRegistry {
  private final List<WorkflowExecutor> executors;
  
  public WorkflowExecutor getExecutor(String actionType) {
    return executors.stream()
      .filter(e -> e.supports(actionType))
      .findFirst()
      .orElseThrow(() -> new UnsupportedActionException(actionType));
  }
}
```

##### HTTP Executor
- **REST API calls**: GET, POST, PUT, DELETE
- **Template variables**: `{{variable}}` substitution from context
- **Retry logic**: Exponential backoff
- **Timeout**: Configurable per action

```java
@Component
public class HttpExecutor implements WorkflowExecutor {
  private final RestTemplate restTemplate;
  
  @Override
  public ExecutionResult execute(WorkflowExecutionContext context) {
    HttpActionConfig config = context.getActionConfig(HttpActionConfig.class);
    
    String url = replaceVariables(config.getUrl(), context.getVariables());
    HttpMethod method = config.getMethod();
    Object body = buildRequestBody(config.getBody(), context.getVariables());
    
    try {
      ResponseEntity<String> response = restTemplate.exchange(
        url, method, new HttpEntity<>(body), String.class
      );
      
      return ExecutionResult.success()
        .withOutput("response", response.getBody())
        .withOutput("status", response.getStatusCode().value());
    } catch (Exception e) {
      return ExecutionResult.failure(e.getMessage());
    }
  }
  
  @Override
  public boolean supports(String actionType) {
    return "HTTP".equals(actionType);
  }
}
```

##### Script Executor
- **Languages**: Groovy, JavaScript (Nashorn/GraalVM)
- **Context injection**: Workflow variables available as globals
- **Sandboxing**: SecurityManager restrictions
- **Result capture**: Return value → workflow context

```java
@Component
public class ScriptExecutor implements WorkflowExecutor {
  private final ScriptEngineManager scriptEngineManager;
  
  @Override
  public ExecutionResult execute(WorkflowExecutionContext context) {
    ScriptActionConfig config = context.getActionConfig(ScriptActionConfig.class);
    
    ScriptEngine engine = scriptEngineManager.getEngineByName(config.getLanguage());
    Bindings bindings = engine.createBindings();
    bindings.putAll(context.getVariables());
    
    try {
      Object result = engine.eval(config.getScript(), bindings);
      return ExecutionResult.success().withOutput("result", result);
    } catch (ScriptException e) {
      return ExecutionResult.failure(e.getMessage());
    }
  }
  
  @Override
  public boolean supports(String actionType) {
    return "SCRIPT".equals(actionType);
  }
}
```

##### Human Task Executor
- **Task creation**: Assign task to user/group
- **Notification**: Email/Slack notification
- **Completion callback**: User completes task → workflow continues
- **Timeout**: SLA deadline for task completion

```java
@Component
public class HumanTaskExecutor implements WorkflowExecutor {
  private final TaskService taskService;
  private final NotificationService notificationService;
  
  @Override
  public ExecutionResult execute(WorkflowExecutionContext context) {
    HumanTaskConfig config = context.getActionConfig(HumanTaskConfig.class);
    
    Task task = Task.builder()
      .title(config.getTitle())
      .description(config.getDescription())
      .assignee(config.getAssignee())
      .dueDate(config.getDueDate())
      .workflowInstanceId(context.getInstanceId())
      .build();
    
    taskService.create(task);
    notificationService.notifyAssignee(task);
    
    return ExecutionResult.success().withOutput("taskId", task.getId());
  }
  
  @Override
  public boolean supports(String actionType) {
    return "HUMAN_TASK".equals(actionType);
  }
}
```

#### Tests
- 24 unit tests:
  - `HttpExecutorTest.java`: GET/POST, template vars, retry, timeout
  - `ScriptExecutorTest.java`: Groovy/JS execution, context injection, error handling
  - `HumanTaskExecutorTest.java`: task creation, notification, completion

#### Value
- **HTTP Integration**: Call external APIs
- **Business Logic**: Custom scripts for complex decisions
- **Human Approval**: Manual approval steps

---

### WORK-007: Timers & SLA (W8)

**Status:** ✅ **DONE**  
**Implementation:** Leden 2025  
**LOC:** ~600

#### Description
Scheduled timers pro auto-transitions a SLA deadline monitoring.

#### Key Features
- **@Scheduled checks**: Every 30 seconds check pending timers
- **Auto-transitions**: Execute action when timer fires
- **SLA violations**: Detect breaches, publish Kafka event
- **Metrics**: Record SLA breach durations

```java
@Component
public class WorkflowTimerService {
  
  @Scheduled(fixedRate = 30000) // Every 30 seconds
  public void checkPendingTimers() {
    List<WorkflowTimer> pending = timerRepository.findPendingTimers(Instant.now());
    
    for (WorkflowTimer timer : pending) {
      try {
        executeTimerAction(timer);
        timer.setStatus(TimerStatus.FIRED);
        timerRepository.save(timer);
      } catch (Exception e) {
        log.error("Timer execution failed: {}", timer.getId(), e);
        timer.setStatus(TimerStatus.FAILED);
        timerRepository.save(timer);
      }
    }
  }
  
  @Scheduled(fixedRate = 60000) // Every 1 minute
  public void checkSLAViolations() {
    List<WorkflowTimer> violations = timerRepository.findSLAViolations(Instant.now());
    
    for (WorkflowTimer timer : violations) {
      long delayMs = Duration.between(timer.getSlaDeadline(), Instant.now()).toMillis();
      
      eventPublisher.publishSLABreach(
        timer.getInstanceId(), timer.getState(), delayMs
      );
      
      metricsService.recordSLABreach(
        timer.getWorkflowId(), timer.getState(), delayMs
      );
    }
  }
}
```

#### Configuration Example
```json
{
  "state": "pending_approval",
  "timers": [
    {
      "name": "reminder",
      "delay": "2d",
      "action": "sendReminderEmail"
    },
    {
      "name": "auto_approve",
      "delay": "7d",
      "action": "autoApprove"
    }
  ],
  "sla": {
    "deadline": "3d",
    "action": "escalateToManager"
  }
}
```

#### Value
- **Automation**: No manual intervention needed
- **SLA Compliance**: Track and enforce deadlines
- **Escalation**: Auto-escalate delayed workflows

---

### WORK-008: Versioning (W9)

**Status:** ✅ **DONE**  
**Implementation:** Leden 2025  
**LOC:** ~1,800  
**Tests:** 8 unit tests

#### Description
Schema evolution s verzováním workflow definitions a migrace existujících instances.

#### Key Features
- **Version Management**: CRUD pro workflow versions
- **Activation**: Activate specific version (becomes default)
- **Instance Migration**: IMMEDIATE, LAZY, MANUAL strategies
- **Backward Compatibility**: Old instances continue running

#### Database Schema
```sql
CREATE TABLE workflow_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_id VARCHAR(255) NOT NULL,
  version VARCHAR(50) NOT NULL,
  definition JSONB NOT NULL,
  is_active BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT NOW(),
  created_by VARCHAR(255),
  UNIQUE(workflow_id, version)
);

CREATE TABLE workflow_migrations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  instance_id UUID REFERENCES workflow_instances(id),
  from_version VARCHAR(50),
  to_version VARCHAR(50),
  strategy VARCHAR(50) NOT NULL,
  status VARCHAR(50) DEFAULT 'PENDING',
  migrated_at TIMESTAMP,
  error_message TEXT
);
```

#### REST API
```java
// WorkflowVersionController.java

@PostMapping("/api/workflows/{workflowId}/versions")
WorkflowVersionDTO createVersion(
  @PathVariable String workflowId,
  @RequestBody CreateVersionRequest request
);

@PutMapping("/api/workflows/{workflowId}/versions/{version}/activate")
void activateVersion(
  @PathVariable String workflowId,
  @PathVariable String version
);

@PostMapping("/api/workflows/{workflowId}/versions/{version}/migrate")
MigrationJobDTO migrateInstances(
  @PathVariable String workflowId,
  @PathVariable String version,
  @RequestParam MigrationStrategy strategy
);
```

#### Migration Strategies
```java
public enum MigrationStrategy {
  IMMEDIATE,  // Migrate all instances now
  LAZY,       // Migrate on next state transition
  MANUAL      // User must trigger migration
}

// Example: IMMEDIATE migration
public void migrateImmediate(String workflowId, String toVersion) {
  List<WorkflowInstance> instances = instanceRepository
    .findRunningInstances(workflowId);
  
  for (WorkflowInstance instance : instances) {
    try {
      WorkflowDefinition newDef = versionRepository
        .findByWorkflowIdAndVersion(workflowId, toVersion);
      
      // Map old state to new state (if exists)
      String newState = mapState(instance.getCurrentState(), newDef);
      instance.setCurrentState(newState);
      instance.setVersion(toVersion);
      
      instanceRepository.save(instance);
      
      migrationRepository.save(Migration.success(instance.getId(), toVersion));
    } catch (Exception e) {
      migrationRepository.save(Migration.failure(instance.getId(), e.getMessage()));
    }
  }
}
```

#### Tests
- 8 unit tests: version CRUD, activation, migration strategies, error handling

#### Value
- **Schema Evolution**: Change workflow without breaking running instances
- **Gradual Rollout**: Test new version before full migration
- **Rollback**: Revert to previous version if needed

---

### WORK-009: Studio UI (W10)

**Status:** ✅ **DONE**  
**Implementation:** Leden 2025  
**LOC:** ~2,200  
**Tests:** 11 unit tests

#### Description
React Flow visual workflow builder s drag-and-drop, property editors, a live validation.

#### Key Features
- **Drag-and-Drop Editor**: Node palette → canvas
- **Property Panels**: Configure node properties
- **Guard Editor**: Visual expression builder
- **Action Editor**: Configure HTTP/Script/Human actions
- **Live Validation**: Real-time error highlighting
- **Export/Import**: JSON workflow definition

```typescript
// WorkflowStudio.tsx
export const WorkflowStudio: React.FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  
  // Live validation
  useEffect(() => {
    const validationErrors = validateWorkflow(nodes, edges);
    setErrors(validationErrors);
  }, [nodes, edges]);
  
  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* Left Panel: Node Palette */}
      <NodePalette />
      
      {/* Center: Canvas */}
      <Box sx={{ flex: 1 }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onNodeClick={(e, node) => setSelectedNode(node)}
        >
          <Controls />
          <MiniMap />
          <Background />
        </ReactFlow>
        
        {/* Validation Errors */}
        {errors.length > 0 && (
          <Alert severity="error">
            {errors.map(err => <div key={err.id}>{err.message}</div>)}
          </Alert>
        )}
      </Box>
      
      {/* Right Panel: Property Editor */}
      {selectedNode && (
        <PropertyPanel
          node={selectedNode}
          onChange={updateNodeProperties}
        />
      )}
    </Box>
  );
};
```

#### Property Panel Components
```typescript
// StateNodeProperties.tsx
export const StateNodeProperties: React.FC<Props> = ({ node, onChange }) => {
  return (
    <Box>
      <TextField
        label="State Name"
        value={node.data.name}
        onChange={e => onChange({ ...node, data: { ...node.data, name: e.target.value }})}
      />
      
      <FormControlLabel
        control={<Checkbox checked={node.data.isInitial} />}
        label="Initial State"
      />
      
      <Typography variant="h6">Timers</Typography>
      <TimerEditor timers={node.data.timers} onChange={updateTimers} />
      
      <Typography variant="h6">SLA</Typography>
      <SLAEditor sla={node.data.sla} onChange={updateSLA} />
    </Box>
  );
};

// TransitionProperties.tsx
export const TransitionProperties: React.FC<Props> = ({ edge, onChange }) => {
  return (
    <Box>
      <TextField label="Event" value={edge.data.event} onChange={updateEvent} />
      
      <Typography variant="h6">Guards</Typography>
      <GuardEditor guards={edge.data.guards} onChange={updateGuards} />
      
      <Typography variant="h6">Actions</Typography>
      <ActionEditor actions={edge.data.actions} onChange={updateActions} />
    </Box>
  );
};
```

#### Tests
- 11 unit tests: drag-drop, property editing, validation, export/import

#### Value
- **Visual Design**: Business users can design workflows
- **Live Feedback**: Errors caught before save
- **Fast Iteration**: Design → Test → Deploy in minutes

---

### WORK-010: Testing/Simulation (W11)

**Status:** ✅ **DONE**  
**Implementation:** Leden 2025  
**LOC:** ~900

#### Description
Dry-run mode pro workflow testing bez side effects.

#### Key Features
- **Simulation Mode**: Execute workflow with mocked actions
- **Mock Data Injection**: Override action results
- **Guard Verification**: Test all conditional paths
- **Test Scenarios**: Pre-defined test cases

```java
@RestController
public class WorkflowTestingController {
  
  @PostMapping("/api/workflows/{workflowId}/simulate")
  public SimulationResult simulate(
    @PathVariable String workflowId,
    @RequestBody SimulationRequest request
  ) {
    WorkflowDefinition definition = definitionService.get(workflowId);
    
    // Create test instance
    WorkflowInstance testInstance = new WorkflowInstance();
    testInstance.setWorkflowId(workflowId);
    testInstance.setCurrentState(definition.getInitialState());
    testInstance.setContext(request.getContext());
    
    // Execute with mocked actions
    SimulationExecutor executor = new SimulationExecutor(request.getMocks());
    List<SimulationStep> steps = new ArrayList<>();
    
    for (String event : request.getEvents()) {
      SimulationStep step = executor.applyTransition(testInstance, event);
      steps.add(step);
      
      if (!step.isSuccess()) {
        break; // Stop on error
      }
    }
    
    return SimulationResult.builder()
      .steps(steps)
      .finalState(testInstance.getCurrentState())
      .errors(collectErrors(steps))
      .build();
  }
}
```

#### Simulation Request Example
```json
{
  "context": {
    "amount": 5000,
    "requester": "user@example.com"
  },
  "events": ["SUBMIT", "APPROVE"],
  "mocks": {
    "checkBudget": { "result": true },
    "sendEmail": { "success": true }
  }
}
```

#### Value
- **Safe Testing**: No side effects
- **Coverage**: Test all paths
- **Debugging**: Step-through execution

---

### WORK-011: Monitoring (W12)

**Status:** ✅ **DONE**  
**Implementation:** Leden 2025  
**LOC:** ~1,500

#### Description
Primární monitoring workflow engine běží na Prometheus metrikách (durace instancí, počty, chybovost) a Loki streamech (`WF_EVENTS`, `WF_ACTIONS`, `EXTERNAL_TASK_CALLS`). Tato data živí Workflow Operations Console (W-OPS) jako hlavní nástroj pro dohled; runtime UX (stav workflow, timeline, SLA forecast) zobrazujeme přímo v našem FE a embedovat Grafana Scenes již neplánujeme. WF19 řeší jen volitelný admin-only dashboard postavený nad stejnými metrikami. Pokud je Grafana nasazena, může zobrazovat agregované metriky (Prometheus), ale není náhradou za Workflow Operations Console.

#### Key Features

##### Primary Telemetry
- Prometheus: `workflow_instance_duration_seconds`, `workflow_instances_total{status}`, `workflow_executor_errors_total{executor}`, `workflow_external_task_pending`.
- Prometheus: `workflow_sla_breaches_total`, `workflow_transition_latency_seconds`, `workflow_external_task_retry_total`.
- Loki: streamy `WF_EVENTS`, `WF_ACTIONS`, `EXTERNAL_TASK_CALLS` s tenant + user + correlationId pro audit trace.

##### Monitoring API
```java
@RestController
public class WorkflowMonitoringController {
  
  @GetMapping("/api/workflows/monitoring/stats")
  public WorkflowStats getStats() {
    return WorkflowStats.builder()
      .totalInstances(instanceRepository.count())
      .runningInstances(instanceRepository.countByStatus(RUNNING))
      .completedToday(instanceRepository.countCompletedToday())
      .averageDuration(calculateAverageDuration())
      .slaCompliance(calculateSLACompliance())
      .build();
  }
  
  @GetMapping("/api/workflows/monitoring/bottlenecks")
  public List<StateBottleneck> getBottlenecks() {
    return stateAnalyzer.findBottlenecks();
  }
  
  @GetMapping("/api/workflows/monitoring/errors")
  public List<WorkflowError> getRecentErrors(@RequestParam int hours) {
    return errorRepository.findRecent(hours);
  }
}
```

##### Prometheus Alerts
```yaml
# alerts/workflow-alerts.yml
groups:
  - name: workflow
    rules:
      - alert: HighErrorRate
        expr: rate(workflow_errors_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High workflow error rate"
      
      - alert: SLABreachRate
        expr: rate(workflow_sla_breaches_total[1h]) > 0.2
        for: 15m
        labels:
          severity: critical
        annotations:
          summary: "SLA breach rate above 20%"
      
      - alert: LongRunningWorkflow
        expr: workflow_instance_duration_seconds > 3600
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Workflow running >1 hour"
```

##### Frontend Dashboard Component
Tento FE komponent vizualizuje stav workflow přímo v Core FE (žádné embedované Grafana Scenes).
```typescript
// WorkflowMonitoring.tsx
export const WorkflowMonitoring: React.FC = () => {
  const { data: stats } = useWorkflowStats();
  const { data: bottlenecks } = useWorkflowBottlenecks();
  
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={3}>
        <StatCard
          title="Total Instances"
          value={stats.totalInstances}
          icon={<WorkflowIcon />}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <StatCard
          title="Running"
          value={stats.runningInstances}
          color="primary"
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <StatCard
          title="SLA Compliance"
          value={`${stats.slaCompliance}%`}
          color={stats.slaCompliance > 90 ? 'success' : 'warning'}
        />
      </Grid>
      
      <Grid item xs={12} md={3}>
        <StatCard
          title="Avg Duration"
          value={formatDuration(stats.averageDuration)}
        />
      </Grid>
      
      <Grid item xs={12}>
        <BottlenecksTable data={bottlenecks} />
      </Grid>
    </Grid>
  );
};
```

#### Value
- **Visibility**: Real-time workflow health
- **Proactive**: Alerts before problems escalate
- **Optimization**: Identify bottlenecks

---

## 🆕 Phase 2: NEW Stories (WF12-WF19)

### WF12: APPROVAL Executor

**Status:** ✅ **SPEC READY**  
**Estimate:** 3 days, 800 LOC  
**Priority:** � HIGH  
**Dependencies:** W5 (WorkflowService), W7 (executor framework)

#### Description
APPROVAL step type s podporou 4 approval modes: SINGLE, ALL_OF, ANY_OF, QUORUM.

#### Key Features
- **Approval Types**:
  - SINGLE: 1 approver stačí
  - ALL_OF: všichni musí schválit
  - ANY_OF: alespoň 1 ze seznamu
  - QUORUM: N z M approverů
- **Role-based access**: Approvers z Keycloak rolí
- **SLA tracking**: Escalations po deadlinu
- **Notifications**: Email + Slack alerts

#### Database Schema
```sql
CREATE TABLE workflow_approval_requests (
  id UUID PRIMARY KEY,
  workflow_instance_id UUID,
  approval_type VARCHAR(50),  -- SINGLE, ALL_OF, ANY_OF, QUORUM
  required_approvals INTEGER,
  approvers TEXT[],
  due_at TIMESTAMP,
  escalated BOOLEAN,
  status VARCHAR(50)  -- PENDING, APPROVED, REJECTED, ESCALATED
);

CREATE TABLE workflow_approval_responses (
  id UUID PRIMARY KEY,
  approval_request_id UUID REFERENCES workflow_approval_requests(id),
  user_id VARCHAR(100),
  response VARCHAR(50),  -- APPROVE, REJECT
  comment TEXT,
  responded_at TIMESTAMP,
  UNIQUE (approval_request_id, user_id)
);
```

#### API Endpoints
```java
// ApprovalController.java
POST /api/workflows/approvals/{requestId}/respond
Body: { "response": "APPROVE", "comment": "LGTM" }

GET /api/workflows/approvals/pending  // My pending approvals
```

**Detail:** [stories/WF12-approval-executor/README.md](stories/WF12-approval-executor/README.md)

---

### WF13: REST_SYNC Executor

**Status:** ⏳ **TODO**  
**Estimate:** 5 days, 1,200 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** W5, W7

#### Description
REST_SYNC step type pro synchronní HTTP volání s OpenAPI support, retry logic, circuit breaker.

#### Key Features
- OpenAPI spec parsing (extract endpoint, method, schema)
- HTTP client (Spring WebClient)
- Retry logic (exponential backoff)
- Circuit breaker (Resilience4j)
- `${variable}` substitution v URL, headers, body
- Idempotence via correlation ID

---

### WF14: KAFKA_COMMAND Executor

**Status:** ⏳ **TODO**  
**Estimate:** 2 days, 600 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** W5, W7, Kafka

#### Description
KAFKA_COMMAND step type s request-reply pattern, correlation ID tracking, timeout → DLQ.

---

### WF15: EXTERNAL_TASK Executor ⚡ **KLÍČOVÝ PRO n8n**

**Status:** ⏳ **TODO**  
**Estimate:** 3 days, 800 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** W5, W7, N8N6 (BFF API)

#### Description
EXTERNAL_TASK step type - most mezi Internal workflow engine a External n8n orchestrator.

#### Key Features
- Poll & complete pattern pro external workers
- Worker registration + heartbeat
- Timeout detection (scheduled job)
- Callback API: `/external-tasks/{id}/complete`

#### Database Schema
```sql
CREATE TABLE workflow_external_tasks (
  id UUID PRIMARY KEY,
  workflow_instance_id UUID,
  task_type VARCHAR(100),  -- "n8n-jira-create-issue"
  worker_id VARCHAR(100),
  status VARCHAR(50),  -- PENDING, CLAIMED, COMPLETED, FAILED, TIMEOUT
  input JSONB,
  output JSONB,
  timeout_at TIMESTAMP,
  last_heartbeat_at TIMESTAMP
);
```

**Integration Pattern:**
```
Core Workflow → ExternalTaskExecutor → BFF API → n8n → Jira
                      ↓                                  ↓
              external_task (PENDING)              callback
                      ↓                                  ↓
              Timeout checker ← ← ← ← ← ← /complete endpoint
```

#### End-to-End Flow
1. Core engine uloží `pending external task` s kontextem instance, payloadem a `correlationId` (+ očekávaným timeoutem).
2. n8n flow přes Core-connector node periodicky polluje `/external-tasks`, claimne task a provede potřebné kroky (např. Jira integrace).
3. Výsledek se vrací přes Core API (`/external-tasks/{id}/complete|/fail`) spolu s `correlationId`, aby engine věděl, kterou instanci odblokovat.
4. Runtime pokračuje přes `onSuccess`/`onError` větve, případně aktivuje retry/compensation logiku WF17.

#### Requirements
- **Idempotence:** `correlationId` + `taskId` blokují duplicitní dokončení (n8n může provést retry).
- **Timeout + Retry:** centrální politika (WF15 + WF17) hlídá SLA, zvyšuje retry counter a může přepnout na fallback.
- **Audit Log:** každý claim/complete jde do Loki (`WF_EVENTS`, `EXTERNAL_TASK_CALLS`) s tenant + user + flow ID.
- **Isolation:** n8n nikdy nepíše do Core DB; používá pouze Core API konektor a service account navázaný na tenant.

---

### WF16: TIMER/DELAY Executor

**Status:** ⏳ **TODO**  
**Estimate:** 2 days, 400 LOC  
**Priority:** 🟡 MEDIUM  
**Dependencies:** W5, W8

#### Description
TIMER step type pro delayed actions, reminders, deadline enforcement.

---

### WF17: Workflow Instance Runtime ⚡ **KRITICKÝ - ORCHESTRATOR**

**Status:** ⏳ **TODO**  
**Estimate:** 6 days, 1,500 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** WF12-WF16 (all executors)

#### Description
Runtime orchestration pro sequential step-by-step execution s context management.

#### Key Features
- Sequential execution: step 1 → wait → step 2 → wait → step 3
- Runtime context: variables `${entityId}`, `${step-1.output.jiraKey}`
- Error handling: onError routing
- Compensation: rollback on failure

#### Database Schema
```sql
CREATE TABLE workflow_instances (
  id UUID PRIMARY KEY,
  entity_type VARCHAR(100),
  entity_id VARCHAR(100),
  workflow_version_id BIGINT,
  status VARCHAR(50),  -- RUNNING, COMPLETED, FAILED, CANCELLED
  current_step_id VARCHAR(100),
  context JSONB,  -- runtime variables
  created_at TIMESTAMP,
  completed_at TIMESTAMP
);

CREATE TABLE workflow_step_executions (
  id UUID PRIMARY KEY,
  workflow_instance_id UUID REFERENCES workflow_instances(id),
  step_id VARCHAR(100),
  step_type VARCHAR(50),  -- APPROVAL, REST_SYNC, KAFKA_COMMAND, etc.
  status VARCHAR(50),  -- PENDING, RUNNING, SUCCESS, FAILED
  input JSONB,
  output JSONB,
  error TEXT,
  retry_count INTEGER,
  started_at TIMESTAMP,
  completed_at TIMESTAMP
);
```

#### API Endpoints
```java
POST /api/workflows/instances/{entityId}/start
GET /api/workflows/instances/{instanceId}
POST /api/workflows/instances/{instanceId}/cancel
```

---

### WF18: Workflow Steps Schema v Metamodel

**Status:** ⏳ **TODO**  
**Estimate:** 3 days, 600 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** META (metamodel CRUD), W10 (UI editor)

#### Description
Workflow steps jako součást entity definition v metamodelu (JSONB storage).

#### Implementation
```java
// EntityDefinition.java
@JsonProperty("workflowSteps")
private List<WorkflowStep> workflowSteps;

record WorkflowStep(
  String id,
  StepType type,  // APPROVAL, REST_SYNC, KAFKA_COMMAND, EXTERNAL_TASK, TIMER
  String label,
  Map<String, Object> config,
  String onSuccess,
  String onError,
  RetryPolicy retry
) {}
```

---

### WF19: Admin Grafana Dashboards

**Status:** ⏳ **TODO**  
**Estimate:** 1 day, 300 LOC  
**Priority:** 🟡 MEDIUM  
**Dependencies:** W12 (metrics), Grafana

#### Description
Volitelný Grafana dashboard dostupný pouze pro admin tenant (SSO přes Keycloak). Čerpá metriky a logy z W12, nesdílí se do FE ani se neembeduje pomocí Grafana Scenes; slouží jen pro operátory a SRE tým. Pokud je Grafana nasazena, může zobrazovat agregované metriky (Prometheus), ale není náhradou za Workflow Operations Console.

#### Access & Security
- Keycloak SSO → `admin` realm, role `workflow-observability`.
- n8n/Core admini mají read-only přístup, žádná veřejná URL.
- Dashboard se zobrazuje jen v Grafaně (SSO), runtime UX (stav/timeline) zůstává v Core FE.

#### Panels
- Active workflow instances (by status)
- Step executions/hour (by type)
- Approval pending time (avg, p95, p99)
- External task timeout rate
- Executor retry rate

---

## 🚀 Phase 3: External n8n Layer (N8N1-N8N6)

### N8N1: n8n Platform Deployment

**Status:** ⏳ **TODO**  
**Estimate:** 1 day, 400 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** PostgreSQL

#### Description
n8n Community Edition Docker service s PostgreSQL backend.

#### Docker Compose
```yaml
n8n:
  image: n8nio/n8n:latest
  environment:
    - N8N_BASIC_AUTH_ACTIVE=false  # SSO via Keycloak
    - DB_TYPE=postgresdb
    - DB_POSTGRESDB_DATABASE=n8n
    - DB_POSTGRESDB_HOST=core-db
    - WEBHOOK_URL=https://admin.core-platform.local/n8n/webhook
  ports:
    - "5678"  # Internal only
```

---

### N8N2: Keycloak SSO Integration

**Status:** ⏳ **TODO**  
**Estimate:** 1 day, 300 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** Keycloak, N8N1

#### Description
n8n authentication via Keycloak OIDC.

#### Keycloak Client Config
- Client ID: `n8n-client`
- Redirect URIs: `https://admin.core-platform.local/n8n/*`
- Roles: `n8n-users`, `n8n-admins`

---

### N8N3: Nginx Reverse Proxy

**Status:** ⏳ **TODO**  
**Estimate:** 0.5 day, 200 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** Nginx, N8N1, N8N2

#### Description
`/n8n/*` routing s SSL termination a Keycloak auth.

---

### N8N4: Workflow Templates

**Status:** ⏳ **TODO**  
**Estimate:** 2 days, 500 LOC  
**Priority:** 🟡 MEDIUM  
**Dependencies:** N8N1-N8N3

#### Description
Pre-built n8n workflows pro common use cases.

#### Templates
1. `jira-create-issue.json` - Webhook → Jira create → callback
2. `confluence-sync.json` - Webhook → Confluence update
3. `trello-automation.json` - Webhook → Trello create card
4. `ai-summarization.json` - Webhook → OpenAI → callback

---

### N8N5: n8n Monitoring

**Status:** ⏳ **TODO**  
**Estimate:** 1 day, 400 LOC  
**Priority:** 🟡 MEDIUM  
**Dependencies:** Grafana, N8N1

#### Description
Grafana dashboards pro n8n execution monitoring.

---

### N8N6: Backend BFF API ⚡ **MOST PRO INTEGRACI**

**Status:** ⏳ **TODO**  
**Estimate:** 3 days, 800 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** N8N1-N8N3, Spring Security, Redis

#### Description
Spring Boot proxy pro n8n REST API s JWT validation, tenant filtering, rate limiting.

#### API Endpoints
```java
@RestController
@RequestMapping("/api/n8n")
public class N8nBffController {
  @PostMapping("/workflows/{workflowId}/execute")
  public ResponseEntity<ExecutionResult> executeWorkflow(
    @PathVariable String workflowId,
    @RequestBody Map<String, Object> input,
    @AuthenticationPrincipal Jwt jwt
  ) {
    // 1. Validate JWT + extract tenant
    // 2. Rate limit check
    // 3. Audit log
    // 4. Call n8n REST API
    return webClient.post()
      .uri("http://n8n:5678/api/v1/workflows/{id}/execute", workflowId)
      .bodyValue(input)
      .retrieve()
      .toEntity(ExecutionResult.class);
  }
}
```

---

## 🛡️ Workflow Operations Console (W-OPS)

**Status:** ⏳ **TODO**  
**Estimate:** 1,800 LOC  
**Priority:** 🔴 HIGH  
**Dependencies:** W5 runtime tables (`workflow_instances`, `workflow_events`, `workflow_step_executions`, `workflow_timers`), WF12-17 executors, W12 metrics, WF19 (optional Grafana), Loki BFF, Keycloak multi-tenant auth.

### Functional Scope

#### Instance List
- Implicitní filtr na tenant/realm dle JWT; volitelné filtry: `workflowType`, stav, SLA breach flag, businessKey, časové okno (absolute / relative) a EXTERNAL_TASK status.
- Fulltext search (instanceId, entityId, correlationId) + stránkování.
- Export aktuálního výběru do CSV/JSON (audit-safe, pouze tenant data).

#### Instance Detail
- React Flow vizualizace aktuální instance (z workflow definice + historie), zvýrazňuje aktivní stav i dokončené/failed kroky.
- Timeline kroků s informacemi kdo/kdy/co + klient (FE, API, n8n) a SLA zbývající čas / breach indikace.
- Odkaz na související metamodelovou entitu (detail entity) a deep-link do Loki logů s předvyplněným `correlationId` a `tenant`.

#### Heatmapy & Analytika
- Kombinuje Micrometer/Prometheus metriky (doby ve stavech, počty přechodů, error rate) s event logy (`workflow_events`).
- Vykresluje heatmapu na workflow diagram: barvy uzlů = počet instancí / průměrná doba / SLA breach %, hrany = frekvence přechodů.
- Časové filtry 24h / 7d / 30d + možnost overlay pro businessKey.

#### Operace & Řízení
- Role `CORE_ADMIN_WORKFLOW` a `TENANT_WORKFLOW_ADMIN` mohou: retry konkrétní step, cancel instance, re-run EXTERNAL_TASK (nový poll), manuálně provést force transition pokud to definice dovoluje.
- Každá operace se loguje do Loki (`WF_ACTIONS`) s tenant, user, instanceId, krokem a parametry; UI jen volá REST API, logika a validace běží v backendu.

#### Chyby & DLQ
- Dedicated přehled failed kroků (executor type, topic, error message, retry count, poslední výskyt) s možností Retry / Ignore / Reopen.
- Přímý link na Loki logy (používá correlationId + stepId) a na entitu workflow instance.

### Data Sources & Telemetrie
- Runtime data z `workflow_instances`, `workflow_events`, `workflow_step_executions`, `workflow_timers` (rozšíření W5/WF17) poskytuje backend API.
- Metriky využívají Micrometer → Prometheus; backend přepočítává SLA statusy a heatmap agregace (FE dostane hotová data).
- Logy dotazuje pouze backend přes Loki BFF; FE nikdy nevolá Loki přímo.
- Všechny API automaticky filtrují podle tenant/realm claimu; cross-tenant odpovědi jsou blokované.

### Integrace s n8n / EXTERNAL_TASK
- EXTERNAL_TASK executor (WF15) zůstává jediným rozhraním pro n8n integrace; W-OPS zobrazuje stav těchto kroků (pending / in-progress / failed / timeout).
- Každý external task musí mít `correlationId` + `externalWorkerId`, aby bylo možné dohledat běh v Loki a v n8n logu.
- n8n nikdy neprovádí přímý zápis do Core DB; všechny změny procházejí Core API a jsou viditelné ve workflow_events.

### Security, FE/BE Stack
- FE používá stávající workflow/metamodel stack (React, MUI, React Query, React Flow) s generickými komponentami, které lze později využít i v dalších modulech.
- Backend řeší kompletní logiku: filtry, SLA výpočty, allowed operations, enforcement RBAC + audit logy.
- Tenant = Keycloak realm = subdoména zůstává povinný model; W-OPS respektuje i subdoménové routování a SSO session.

## 📊 Overall Impact

### Metrics
- **Development Speed**: 10x faster workflow creation (vs code)
- **Business Agility**: Workflow changes in hours (vs weeks)
- **Audit Compliance**: 100% event tracking
- **SLA Adherence**: 95%+ compliance rate
- **Integration Coverage**: 400+ external systems via n8n (Jira, Confluence, Trello, M365, Google, AI)

### Business Value
- **Cost Savings**: Reduced manual process overhead + no custom connector maintenance
- **Faster Time-to-Market**: New processes + integrations deployed quickly
- **Better Visibility**: Real-time process monitoring (internal + external)
- **Compliance**: Full audit trail
- **Plug & Play**: Leverage n8n's 400+ built-in nodes (no custom code)

---

## 🎯 Roadmap (5 týdnů)

### Week 1: Core Executors
- WF12 APPROVAL Executor
- WF14 KAFKA_COMMAND Executor

### Week 2: Integration Executors
- WF13 REST_SYNC Executor
- WF15 EXTERNAL_TASK Executor ← **Klíčový pro n8n**
- WF16 TIMER/DELAY Executor

### Week 3: Workflow Runtime
- WF17 Workflow Instance Runtime (orchestrator)
- WF18 Workflow Steps Schema (metamodel)

### Week 4: n8n Deployment
- N8N1 Platform Deployment
- N8N2 Keycloak SSO + N8N3 Nginx Proxy
- N8N6 Backend BFF API
- N8N4 Workflow Templates
- N8N5 Monitoring

### Week 5: Observability + E2E
- WF19 Grafana Dashboards
- E2E integration tests (Order approval, Jira sync, timeout scenarios)

---

**For detailed architecture, see:**
- [`WORKFLOW_UNIFIED_ARCHITECTURE.md`](../WORKFLOW_UNIFIED_ARCHITECTURE.md) - Complete 2-layer design
- `WORKFLOW_EPIC_W5_W12_COMPLETE.md` - Phase 1 implementation summary
- `docs/workflow/W5_RUNTIME_GUIDE.md` - Runtime API guide

## ✅ Definition of Done (EPIC-006)

- **Interní engine**
  - Definuje, verzuje a spouští workflow nad metamodelovými entitami.
  - Poskytuje REST API pro graph/state/history/forecast dotazy.
- **UX**
  - Uživatel v detailu entity vidí:
    - aktuální stav,
    - možné akce včetně „why not“ důvodů,
    - historii kroků,
    - očekávané další kroky / SLA a forecast.
- **Executors**
  - APPROVAL, REST_SYNC, KAFKA_COMMAND a TIMER běží end-to-end.
  - EXTERNAL_TASK executor je integrován s n8n podle EPIC-011 (oficiální konektor, audit, idempotence).
- **Multi-tenancy**
  - Všechny operace respektují realm/subdoménu z JWT, cross-tenant přístup je blokovaný.
- **Observabilita**
  - Prometheus metriky + Loki logy poskytují audit trail pro interní kroky i EXTERNAL_TASK integrace.

## 🧪 Testing Requirements

- **Unit tests**
  - Výpočet allowed actions (RBAC + definice) pro Workflow Operations Console.
  - SLA výpočty, zbývající čas, breach flagy a heatmap agregace nad metrikami/eventy.
  - Serializace/export (CSV/JSON) a korektní tvorba Loki query (correlationId + tenant).
- **Integration tests**
  - Změna stavu → záznam ve `workflow_events` → data dostupná přes W-OPS API & timeline.
  - EXTERNAL_TASK lifecycle (pending → claimed → complete/fail) promítne status do W-OPS a do audit logu.
  - Backend filtrování podle tenant/realm; jiný tenant nedostane žádná data (HTTP 403/empty set).
- **E2E scénáře**
  - Vytvoření instance → vizuální zobrazení v W-OPS včetně grafu a timeline.
  - Simulace SLA breach → indikace v dashboardu + heatmap.
  - Retry/force action → ověření zápisu ve workflow DB + Loki audit logu.
  - Multi-tenant UI: přepnutí na jiný realm neumožní čtení cizích instancí ani logů.

## ⚙️ Technical Guidelines

- **Frontend**
  - Reuse existující metamodel/workflow FE stack (React, MUI, React Query, React Flow) a tvoř generické komponenty (instance list, heatmap overlay, operations panel) pro budoucí moduly.
  - Žádná business logika ani filtr/SLA výpočty na FE; UI jen zobrazuje data a spouští akce přes REST API.
- **Backend**
  - Veškerá logika (filtering, allowed operations, SLA, heatmap aggregace) běží v servisách chráněných RBAC, včetně audit logů do Loki (`WF_ACTIONS`, `WF_EVENTS`).
  - Loki API je dostupné pouze přes náš BFF; FE nemá přímý přístup.
  - Audit log je povinný pro každou změnu stavu/operaci (user, tenant, instanceId, action, payload hash).
