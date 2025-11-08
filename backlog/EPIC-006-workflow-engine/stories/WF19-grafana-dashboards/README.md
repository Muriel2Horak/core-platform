# WF19: Grafana Dashboards - Workflow Monitoring

**Typ:** TASK  
**Epic:** EPIC-006 (Workflow Engine - Internal Layer)  
**Fase:** Phase 2 (Typed Executors)  
**Priorita:** LOW (monitoring enhancement)  
**Effort:** 300 LOC, 1 den  
**Dependencies:** W12 (Metrics), WF12-WF17 (Executors & Runtime)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Vytvořit **Grafana dashboardy** pro workflow monitoring:
- Workflow health (instances, completion rate, error rate)
- SLA compliance metrics
- Bottleneck analysis (slowest steps, timeout rate)
- Executor performance (per executor type)
- Alert rules (Prometheus)

---

## 📋 Dashboards

### 1. Workflow Overview Dashboard

**Panels:**

1. **Workflow Instances Total** (Counter)
   - Metric: `workflow_instances_total`
   - Breakdown: status (RUNNING, COMPLETED, FAILED)

2. **Completion Rate** (Gauge)
   - Formula: `COMPLETED / (COMPLETED + FAILED) * 100`

3. **Active Workflows** (Graph)
   - Metric: `workflow_instances_active`
   - Time series

4. **Error Rate** (Graph)
   - Metric: `workflow_instances_failed_total`
   - Rate per minute

---

### 2. Step Execution Dashboard

**Panels:**

1. **Step Duration (p50, p95, p99)** (Heatmap)
   - Metric: `workflow_step_duration_ms`
   - Breakdown: step_type

2. **Step Success Rate** (Table)
   - Columns: step_type, total_executions, success_rate, error_rate

3. **Slowest Steps** (Bar chart)
   - Top 10 steps by p99 latency

4. **Retry Rate** (Graph)
   - Metric: `workflow_step_retries_total`
   - Breakdown: step_type

---

### 3. SLA Compliance Dashboard

**Panels:**

1. **SLA Breaches** (Counter)
   - Metric: `workflow_sla_breaches_total`

2. **SLA Compliance %** (Gauge)
   - Formula: `(instances - sla_breaches) / instances * 100`

3. **SLA Breach Trend** (Graph)
   - Time series per entity_type

---

### 4. Executor Performance Dashboard

**Panels:**

1. **Executor Call Rate** (Graph)
   - Metric: `workflow_executor_calls_total`
   - Breakdown: executor_type

2. **Executor Latency** (Heatmap)
   - Metric: `workflow_executor_duration_ms`
   - p50/p95/p99 per executor

3. **Circuit Breaker State** (Table)
   - Metric: `workflow_executor_circuit_breaker_state`
   - Values: CLOSED (0), OPEN (1), HALF_OPEN (0.5)

---

## 🔧 Implementace

### 1. Prometheus Metrics (Already Exists from W12)

**Ensure these metrics exist:**

```java
// Workflow instances
workflow_instances_total{status="COMPLETED|FAILED|RUNNING"}
workflow_instances_active

// Step executions
workflow_step_executions_total{step_type, status}
workflow_step_duration_ms{step_type}
workflow_step_retries_total{step_type}

// SLA
workflow_sla_breaches_total{entity_type}

// Executors
workflow_executor_calls_total{executor_type}
workflow_executor_duration_ms{executor_type}
workflow_executor_circuit_breaker_state{executor_type}
```

---

### 2. Grafana Dashboard JSON

**File:** `docker/grafana/dashboards/workflow-overview.json`

```json
{
  "dashboard": {
    "title": "Workflow Engine - Overview",
    "panels": [
      {
        "id": 1,
        "title": "Active Workflows",
        "type": "stat",
        "targets": [
          {
            "expr": "workflow_instances_active",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {"mode": "thresholds"},
            "thresholds": {
              "steps": [
                {"value": 0, "color": "green"},
                {"value": 50, "color": "yellow"},
                {"value": 100, "color": "red"}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Completion Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "sum(workflow_instances_total{status='COMPLETED'}) / (sum(workflow_instances_total{status='COMPLETED'}) + sum(workflow_instances_total{status='FAILED'})) * 100",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "min": 0,
            "max": 100,
            "thresholds": {
              "steps": [
                {"value": 0, "color": "red"},
                {"value": 90, "color": "yellow"},
                {"value": 95, "color": "green"}
              ]
            }
          }
        }
      },
      {
        "id": 3,
        "title": "Workflow Instances Over Time",
        "type": "timeseries",
        "targets": [
          {
            "expr": "sum(rate(workflow_instances_total[5m])) by (status)",
            "legendFormat": "{{status}}",
            "refId": "A"
          }
        ]
      },
      {
        "id": 4,
        "title": "Step Duration (p99)",
        "type": "timeseries",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, workflow_step_duration_ms) by (step_type)",
            "legendFormat": "{{step_type}}",
            "refId": "A"
          }
        ]
      }
    ]
  }
}
```

---

### 3. Alert Rules

**File:** `docker/prometheus/alerts/workflow-alerts.yml`

```yaml
groups:
  - name: workflow_alerts
    interval: 30s
    rules:
      - alert: WorkflowHighErrorRate
        expr: rate(workflow_instances_total{status="FAILED"}[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High workflow error rate"
          description: "Error rate {{ $value }} instances/s over last 5 minutes"
      
      - alert: WorkflowSLABreach
        expr: workflow_sla_breaches_total > 10
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Workflow SLA breaches detected"
          description: "{{ $value }} SLA breaches in entity {{ $labels.entity_type }}"
      
      - alert: WorkflowStepTimeout
        expr: rate(workflow_step_executions_total{status="TIMEOUT"}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High step timeout rate"
          description: "Step {{ $labels.step_type }} timeout rate {{ $value }}/s"
      
      - alert: WorkflowCircuitBreakerOpen
        expr: workflow_executor_circuit_breaker_state{state="OPEN"} == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker open"
          description: "Executor {{ $labels.executor_type }} circuit breaker is OPEN"
```

---

### 4. Provisioning

**File:** `docker/grafana/provisioning/dashboards/workflow.yml`

```yaml
apiVersion: 1

providers:
  - name: 'Workflow Dashboards'
    type: file
    options:
      path: /etc/grafana/dashboards
```

**File:** `docker-compose.yml` (Grafana volumes)

```yaml
grafana:
  volumes:
    - ./docker/grafana/dashboards:/etc/grafana/dashboards:ro
    - ./docker/grafana/provisioning:/etc/grafana/provisioning:ro
```

---

## ✅ Acceptance Criteria

1. **Dashboards:**
   - [ ] Workflow Overview dashboard
   - [ ] Step Execution dashboard
   - [ ] SLA Compliance dashboard
   - [ ] Executor Performance dashboard

2. **Alerts:**
   - [ ] High error rate alert
   - [ ] SLA breach alert
   - [ ] Step timeout alert
   - [ ] Circuit breaker alert

3. **Visualization:**
   - [ ] Time series graphs (5m rate)
   - [ ] Gauges (completion rate, SLA compliance)
   - [ ] Tables (step performance)

---

**Related Stories:**
- W12: Monitoring (Prometheus metrics)
- WF12-WF17: Executors & Runtime (metric sources)
