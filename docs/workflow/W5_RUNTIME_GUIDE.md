# 🔄 W5: Workflow Runtime - Implementation Guide

## 📋 Overview

W5 implements the **Workflow Runtime** layer, providing visualization, state tracking, history timeline, and forecast capabilities for workflow-enabled entities.

**Status**: ✅ Complete  
**Version**: 1.0.0  
**Date**: 2025-10-14

---

## 🎯 Scope & DoD

### Scope
- ✅ DB migration: `workflow_instances`, `workflow_events`, `workflow_timers` (+ indexes, retention views)
- ✅ API endpoints:
  - `GET /api/workflows/{entity}/{id}/graph` - Visual graph with current state highlight
  - `GET /api/workflows/{entity}/{id}/state` - Allowed/blocked transitions with "why not" reasons
  - `GET /api/workflows/{entity}/{id}/history` - Timeline with durations and SLA status
  - `GET /api/workflows/{entity}/{id}/forecast` - Next steps and active timers/SLA
- ✅ Kafka events: `ENTER_STATE`, `EXIT_STATE`, `ACTION_APPLIED`, `ERROR`
- ✅ Metrics (Micrometer): durations, error-rate, transition counts, SLA breaches

### Definition of Done (DoD)
- ✅ FE can load history and forecast from API
- ✅ Events/metrics visible in Prometheus/Grafana
- ✅ OpenAPI documented endpoints
- ✅ Unit + Integration + E2E tests passing
- ✅ 3 UX goals met: **WHERE AM I / WHAT HAPPENED / WHAT'S NEXT**

---

## 🗄️ Database Schema

### Tables

#### `workflow_instances`
Runtime workflow execution context tracking.

```sql
CREATE TABLE workflow_instances (
    id UUID PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    workflow_version_id UUID,
    current_state_code TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL, -- RUNNING, COMPLETED, FAILED, CANCELLED
    error_message TEXT,
    context JSONB,
    created_by TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**Indexes**:
- `idx_workflow_instances_tenant` on `tenant_id`
- `idx_workflow_instances_entity` on `entity_type, entity_id`
- `idx_workflow_instances_status` on `status`
- `idx_workflow_instances_started` on `started_at`
- GIN index on `context`

---

#### `workflow_events`
Immutable event log for all workflow state changes.

```sql
CREATE TABLE workflow_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE, -- For deduplication
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    workflow_instance_id UUID REFERENCES workflow_instances(id),
    event_type TEXT NOT NULL, -- ENTER_STATE, EXIT_STATE, ACTION_APPLIED, TIMER_FIRED, ERROR
    from_state_code TEXT,
    to_state_code TEXT,
    transition_code TEXT,
    actor TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    duration_ms BIGINT,
    metadata JSONB,
    error_details TEXT
);
```

**Indexes**:
- `idx_workflow_events_entity` on `entity_type, entity_id, tenant_id, timestamp DESC`
- `idx_workflow_events_type` on `event_type`
- `idx_workflow_events_timestamp` on `timestamp DESC`

**Retention Policy**: Events older than 6 months can be archived (future enhancement).

---

#### `workflow_timers`
Scheduled timers for delays and SLA tracking.

```sql
CREATE TABLE workflow_timers (
    id UUID PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    workflow_instance_id UUID REFERENCES workflow_instances(id),
    timer_type TEXT NOT NULL, -- DELAY, SLA_WARNING, SLA_BREACH, ESCALATION
    state_code TEXT NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    fired_at TIMESTAMPTZ,
    status TEXT NOT NULL, -- PENDING, FIRED, CANCELLED
    action TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL
);
```

**Indexes**:
- `idx_workflow_timers_scheduled` on `scheduled_at` WHERE `status = 'PENDING'`
- `idx_workflow_timers_pending` on `status, scheduled_at`

---

### Views

#### `v_active_workflows`
Active workflows with duration metrics.

```sql
CREATE VIEW v_active_workflows AS
SELECT 
    wi.id,
    wi.tenant_id,
    wi.entity_type,
    wi.entity_id,
    wi.current_state_code,
    wi.status,
    wi.started_at,
    EXTRACT(EPOCH FROM (NOW() - wi.started_at))::BIGINT * 1000 AS duration_ms,
    es.since AS current_state_since,
    EXTRACT(EPOCH FROM (NOW() - es.since))::BIGINT * 1000 AS state_duration_ms,
    es.sla_minutes
FROM workflow_instances wi
LEFT JOIN entity_state es ON 
    wi.entity_type = es.entity_type AND 
    wi.entity_id = es.entity_id
WHERE wi.status = 'RUNNING';
```

---

## 🔌 API Endpoints

### 1. GET /api/workflows/{entity}/{id}/graph

**Description**: Returns workflow graph visualization with nodes, edges, current state.

**Response**:
```json
{
  "entityType": "Order",
  "entityId": "order-123",
  "currentState": "APPROVED",
  "nodes": [
    {
      "id": "PENDING",
      "code": "PENDING",
      "label": "Pending",
      "type": "state",
      "current": false,
      "metadata": {}
    },
    {
      "id": "APPROVED",
      "code": "APPROVED",
      "label": "Approved",
      "type": "state",
      "current": true,
      "metadata": {}
    }
  ],
  "edges": [
    {
      "id": "approve",
      "source": "PENDING",
      "target": "APPROVED",
      "label": "Approve",
      "transitionCode": "approve",
      "allowed": false,
      "whyNot": "Current state is APPROVED, requires PENDING",
      "slaMinutes": 30
    },
    {
      "id": "ship",
      "source": "APPROVED",
      "target": "SHIPPED",
      "label": "Ship",
      "transitionCode": "ship",
      "allowed": true,
      "whyNot": null,
      "slaMinutes": 60
    }
  ]
}
```

**Use Case**: React Flow visualization, auto-layout (elkjs/dagre).

---

### 2. GET /api/workflows/{entity}/{id}/state

**Description**: Returns current state with allowed/blocked transitions.

**Response**:
```json
{
  "currentState": {
    "entityType": "Order",
    "entityId": "order-123",
    "tenantId": "tenant-1",
    "stateCode": "APPROVED",
    "since": "2025-10-14T10:00:00Z"
  },
  "allowedTransitions": [
    {
      "code": "ship",
      "label": "Ship",
      "toState": "SHIPPED",
      "slaMinutes": 60
    }
  ],
  "blockedTransitions": [
    {
      "code": "approve",
      "label": "Approve",
      "toState": "APPROVED",
      "reason": "Current state is APPROVED, requires PENDING"
    }
  ],
  "slaStatus": "OK",
  "stateAgeMs": 120000
}
```

**Use Case**: Actions bar, tooltips for blocked transitions.

---

### 3. GET /api/workflows/{entity}/{id}/history

**Description**: Returns workflow history timeline with durations.

**Response**:
```json
{
  "entityType": "Order",
  "entityId": "order-123",
  "entries": [
    {
      "eventType": "ACTION_APPLIED",
      "fromState": "PENDING",
      "toState": "APPROVED",
      "transitionCode": "approve",
      "timestamp": "2025-10-14T10:00:00Z",
      "durationMs": 120000,
      "actor": "user-1",
      "slaStatus": "OK"
    },
    {
      "eventType": "ACTION_APPLIED",
      "fromState": null,
      "toState": "PENDING",
      "transitionCode": "create",
      "timestamp": "2025-10-14T09:58:00Z",
      "durationMs": 60000,
      "actor": "user-1",
      "slaStatus": "OK"
    }
  ],
  "totalDurationMs": 180000
}
```

**Use Case**: Timeline panel with state durations.

---

### 4. GET /api/workflows/{entity}/{id}/forecast

**Description**: Returns next possible steps and active timers/SLA.

**Response**:
```json
{
  "currentState": "APPROVED",
  "nextSteps": [
    {
      "transitionCode": "ship",
      "label": "Ship",
      "toState": "SHIPPED",
      "automatic": false,
      "estimatedSlaMinutes": 60
    }
  ],
  "pendingTimers": [
    {
      "id": "timer-uuid",
      "type": "SLA_WARNING",
      "scheduledAt": "2025-10-14T11:00:00Z",
      "action": "notify",
      "remainingMs": 3600000
    }
  ]
}
```

**Use Case**: Forecast panel showing what's next, SLA countdown.

---

## 📨 Kafka Events

### Topic: `workflow.events`

**Event Types**:
- `ENTER_STATE`: Entity entered a new state
- `EXIT_STATE`: Entity exited a state (with duration)
- `ACTION_APPLIED`: Transition executed
- `TIMER_FIRED`: Scheduled timer fired
- `ERROR`: Workflow error occurred

**Payload Schema**:
```json
{
  "eventId": "uuid",
  "tenantId": "tenant-1",
  "entityType": "Order",
  "entityId": "order-123",
  "workflowInstanceId": "instance-uuid",
  "eventType": "ENTER_STATE",
  "fromStateCode": "PENDING",
  "toStateCode": "APPROVED",
  "transitionCode": "approve",
  "actor": "user-1",
  "timestamp": "2025-10-14T10:00:00Z",
  "durationMs": 5000,
  "errorDetails": null
}
```

**Ordering**: Events for same `{entityType}:{entityId}` are ordered (same partition key).

---

## 📊 Metrics (Micrometer)

### Counters
- `workflow.transition.count{entity_type, transition, status}` - Number of transitions
- `workflow.transition.error{entity_type, transition, error_type}` - Transition errors
- `workflow.sla.breach{entity_type, state}` - SLA breaches
- `workflow.sla.warning{entity_type, state}` - SLA warnings (80% threshold)
- `workflow.completion{entity_type, status}` - Workflow completions

### Timers
- `workflow.transition.duration{entity_type, from_state, to_state}` - Transition durations
- `workflow.state.duration{entity_type, state}` - Time spent in states
- `workflow.total.duration{entity_type, status}` - Total workflow execution time

### Gauges
- `workflow.active.count{entity_type}` - Number of active workflows

**Export**: Prometheus format at `/actuator/prometheus`

---

## 🧪 Testing

### Unit Tests (`WorkflowRuntimeServiceTest`)
- ✅ Graph generation with current state highlight
- ✅ Allowed/blocked transitions with reasons
- ✅ History timeline with durations
- ✅ Forecast with pending timers
- ✅ SLA status calculation

### Integration Tests
- ✅ `WorkflowApiIT`: All 4 endpoints (Testcontainers PostgreSQL)
- ✅ `WorkflowEventsKafkaIT`: Event publishing and ordering (Testcontainers Kafka)

### E2E Tests (`pre/05_workflow_runtime_smoke.spec.ts`)
- ✅ Timeline panel visibility and content
- ✅ Forecast panel with next steps
- ✅ "Why not" tooltips for blocked transitions
- ✅ SLA badges and state age
- ✅ Full UX flow (3 goals verification)

**Coverage**: BE 80/70 (line/branch), FE 80/70 (target met)

---

## 🎨 Frontend Integration

### Timeline Panel
```tsx
// Fetch history
const { data: history } = useQuery(['workflow-history', entityId], () =>
  fetch(`/api/workflows/${entityType}/${entityId}/history`).then(r => r.json())
);

// Render timeline
<Timeline entries={history.entries} totalDuration={history.totalDurationMs} />
```

### Forecast Panel
```tsx
// Fetch forecast
const { data: forecast } = useQuery(['workflow-forecast', entityId], () =>
  fetch(`/api/workflows/${entityType}/${entityId}/forecast`).then(r => r.json())
);

// Render next steps
<Forecast nextSteps={forecast.nextSteps} pendingTimers={forecast.pendingTimers} />
```

### Graph Visualization (React Flow)
```tsx
const { data: graph } = useQuery(['workflow-graph', entityId], () =>
  fetch(`/api/workflows/${entityType}/${entityId}/graph`).then(r => r.json())
);

<ReactFlow 
  nodes={graph.nodes.map(n => ({ ...n, className: n.current ? 'current' : '' }))}
  edges={graph.edges.map(e => ({ 
    ...e, 
    style: e.allowed ? {} : { opacity: 0.5 },
    label: e.whyNot ? `${e.label} (${e.whyNot})` : e.label
  }))}
/>
```

---

## 🔧 Configuration

### application.yml
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

workflow:
  events:
    topic: workflow.events
    retention-days: 180 # 6 months
  timers:
    poll-interval: 60s # Check pending timers every minute
  sla:
    warning-threshold: 0.8 # 80% of SLA time
```

---

## 📈 Monitoring & Observability

### Grafana Dashboard
Create dashboard with panels:
1. **Active Workflows** (`workflow.active.count`)
2. **Transition Duration (P95)** (`workflow.transition.duration`)
3. **SLA Breaches** (`workflow.sla.breach`)
4. **Error Rate** (`workflow.transition.error`)

### Alerts
- **SLA Breach**: `increase(workflow_sla_breach_total[5m]) > 0`
- **High Error Rate**: `rate(workflow_transition_error_total[5m]) > 0.1`
- **DLQ Growth**: (future W7)

---

## 🚀 Deployment

### Migration
```bash
# Applied automatically by Flyway
# V3__workflow_runtime.sql
```

### Feature Flag (optional)
```yaml
features:
  workflow-runtime: true
```

### Rollback Plan
If issues occur:
1. Disable feature flag
2. Events still logged to `workflow_events` (no data loss)
3. Revert to V2 schema if needed

---

## 📚 Related Documentation
- [W6: Frontend UX](./W6_FRONTEND_UX.md) (next phase)
- [W7: Workflow Executors](./W7_EXECUTORS.md) (automatic steps)
- [Workflow Analysis](../WORKFLOW_ANALYSIS.md) (original design)

---

## ✅ Completion Checklist

- [x] DB migration with indexes
- [x] 4 API endpoints implemented
- [x] Kafka event publishing
- [x] Micrometer metrics
- [x] Unit tests (WorkflowRuntimeServiceTest)
- [x] Integration tests (API + Kafka)
- [x] E2E smoke test
- [x] OpenAPI documentation
- [x] DoD verified
- [x] Tagged: `studio-workflow-W5`

---

**Status**: ✅ **W5 Complete - Ready for W6 (Frontend UX)**
