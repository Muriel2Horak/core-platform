# N8N5: n8n Monitoring & Observability

**Typ:** TASK  
**Epic:** EPIC-011 (n8n External Orchestration Layer)  
**Fase:** Phase 3 (n8n Deployment)  
**Priorita:** MEDIUM  
**Effort:** 400 LOC, 1 den  
**Dependencies:** N8N1 (Platform), WF19 (Grafana)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Implementovat **monitoring & observability** pro n8n:
- Prometheus metrics export z n8n
- Grafana dashboard (workflow executions, errors, latency)
- Alert rules (execution failures, high error rate)
- Loki log aggregation

---

## 📋 Požadavky

### Funkční Požadavky

1. **Metrics Export**
   - n8n internal metrics → Prometheus scrape
   - Custom metrics: `n8n_workflow_executions_total`, `n8n_workflow_duration_ms`

2. **Grafana Dashboard**
   - Panels: Active executions, Success rate, Duration p99
   - Filters: workflow name, status (success/error)

3. **Alerting**
   - High error rate (>10% failed executions)
   - Workflow timeout (>5 min)
   - n8n pod down

---

## 🔧 Implementace

### 1. n8n Prometheus Metrics Exporter

**n8n má built-in Prometheus endpoint:** `GET /metrics`

**File:** `docker/docker-compose.yml` (enable metrics)

```yaml
services:
  n8n:
    environment:
      # ... existing vars ...
      
      # Prometheus metrics
      - N8N_METRICS=true
      - N8N_METRICS_INCLUDE_WORKFLOW_ID_LABEL=true
      - N8N_METRICS_INCLUDE_NODE_TYPE_LABEL=true
      - N8N_METRICS_INCLUDE_CREDENTIAL_TYPE_LABEL=false  # Security: no credential exposure
```

---

### 2. Prometheus Scrape Config

**File:** `monitoring/prometheus/prometheus.yml` (add n8n scrape job)

```yaml
scrape_configs:
  # ... existing jobs ...
  
  - job_name: 'n8n'
    scrape_interval: 15s
    scrape_timeout: 10s
    static_configs:
      - targets: ['n8n:5678']
    metrics_path: '/metrics'
    scheme: http
```

---

### 3. Grafana Dashboard JSON

**File:** `monitoring/grafana/dashboards/n8n-overview.json`

```json
{
  "dashboard": {
    "title": "n8n Workflow Automation",
    "uid": "n8n-overview",
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Active Executions",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(n8n_workflow_executions_total{status=\"running\"})",
            "legendFormat": "Active"
          }
        ],
        "gridPos": { "h": 4, "w": 6, "x": 0, "y": 0 }
      },
      {
        "id": 2,
        "title": "Success Rate (Last 1h)",
        "type": "gauge",
        "targets": [
          {
            "expr": "sum(rate(n8n_workflow_executions_total{status=\"success\"}[1h])) / sum(rate(n8n_workflow_executions_total[1h])) * 100",
            "legendFormat": "Success %"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                { "value": 0, "color": "red" },
                { "value": 80, "color": "yellow" },
                { "value": 95, "color": "green" }
              ]
            },
            "unit": "percent"
          }
        },
        "gridPos": { "h": 4, "w": 6, "x": 6, "y": 0 }
      },
      {
        "id": 3,
        "title": "Workflow Executions Over Time",
        "type": "timeseries",
        "targets": [
          {
            "expr": "sum by (status) (rate(n8n_workflow_executions_total[5m]))",
            "legendFormat": "{{ status }}"
          }
        ],
        "gridPos": { "h": 8, "w": 12, "x": 0, "y": 4 }
      },
      {
        "id": 4,
        "title": "Workflow Duration (p99)",
        "type": "timeseries",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, sum(rate(n8n_workflow_execution_duration_bucket[5m])) by (le, workflow_name))",
            "legendFormat": "{{ workflow_name }}"
          }
        ],
        "fieldConfig": {
          "defaults": { "unit": "ms" }
        },
        "gridPos": { "h": 8, "w": 12, "x": 12, "y": 4 }
      },
      {
        "id": 5,
        "title": "Top Failed Workflows (Last 24h)",
        "type": "table",
        "targets": [
          {
            "expr": "topk(10, sum by (workflow_name) (increase(n8n_workflow_executions_total{status=\"error\"}[24h])))",
            "format": "table",
            "instant": true
          }
        ],
        "gridPos": { "h": 6, "w": 12, "x": 0, "y": 12 }
      }
    ],
    "templating": {
      "list": [
        {
          "name": "workflow",
          "type": "query",
          "query": "label_values(n8n_workflow_executions_total, workflow_name)",
          "multi": true,
          "includeAll": true
        }
      ]
    }
  }
}
```

---

### 4. Prometheus Alert Rules

**File:** `monitoring/prometheus/alerts/n8n-alerts.yml`

```yaml
groups:
  - name: n8n
    interval: 30s
    rules:
      # High error rate
      - alert: N8nHighErrorRate
        expr: |
          (sum(rate(n8n_workflow_executions_total{status="error"}[5m])) 
          / sum(rate(n8n_workflow_executions_total[5m]))) > 0.1
        for: 5m
        labels:
          severity: warning
          component: n8n
        annotations:
          summary: "n8n high error rate (>10%)"
          description: "{{ $value | humanizePercentage }} of workflows failing"
      
      # Workflow stuck (long running)
      - alert: N8nWorkflowTimeout
        expr: |
          n8n_workflow_execution_duration_ms > 300000
        for: 2m
        labels:
          severity: warning
          component: n8n
        annotations:
          summary: "n8n workflow timeout (>5 min)"
          description: "Workflow {{ $labels.workflow_name }} running for {{ $value }}ms"
      
      # n8n service down
      - alert: N8nServiceDown
        expr: up{job="n8n"} == 0
        for: 1m
        labels:
          severity: critical
          component: n8n
        annotations:
          summary: "n8n service is down"
          description: "n8n has been unreachable for 1 minute"
      
      # No executions (stale)
      - alert: N8nNoExecutions
        expr: |
          rate(n8n_workflow_executions_total[10m]) == 0
        for: 30m
        labels:
          severity: info
          component: n8n
        annotations:
          summary: "n8n has no workflow executions"
          description: "No workflows executed in last 30 minutes (might be expected)"
```

---

### 5. Loki Log Collection

**File:** `docker/docker-compose.yml` (add n8n logging labels)

```yaml
services:
  n8n:
    logging:
      driver: loki
      options:
        loki-url: "http://loki:3100/loki/api/v1/push"
        loki-batch-size: "400"
        labels: "service,environment"
    labels:
      logging: "promtail"
      logging_jobname: "n8n"
```

---

### 6. Health Check Script

**File:** `scripts/n8n-health-check.sh` (already created in N8N1, extend)

```bash
#!/bin/bash
set -e

N8N_URL="${N8N_URL:-https://admin.core-platform.local/n8n}"

echo "🔍 n8n Health Check..."

# 1. Health endpoint
curl -k -f -s "${N8N_URL}/healthz" > /dev/null && echo "✅ n8n healthy" || (echo "❌ n8n unhealthy" && exit 1)

# 2. Metrics endpoint
METRICS=$(curl -k -s "${N8N_URL}/metrics")
if echo "$METRICS" | grep -q "n8n_workflow_executions_total"; then
  echo "✅ Metrics OK"
else
  echo "⚠️ Metrics not available"
fi

# 3. Check Prometheus scrape
PROM_TARGETS=$(curl -s http://localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] | select(.labels.job=="n8n") | .health')
if [ "$PROM_TARGETS" == "up" ]; then
  echo "✅ Prometheus scrape OK"
else
  echo "⚠️ Prometheus not scraping n8n"
fi

echo "🎉 Health check complete!"
```

---

## ✅ Acceptance Criteria

1. **Metrics:**
   - [ ] n8n exposes `/metrics` endpoint
   - [ ] Prometheus scrapes n8n metrics (15s interval)
   - [ ] Metrics include: `n8n_workflow_executions_total`, `n8n_workflow_execution_duration_ms`

2. **Dashboard:**
   - [ ] Grafana dashboard "n8n Workflow Automation" exists
   - [ ] Panels show: Active executions, Success rate, Duration p99, Top failed workflows
   - [ ] Template variable `$workflow` filters by workflow name

3. **Alerting:**
   - [ ] 4 alert rules configured (high error rate, timeout, service down, no executions)
   - [ ] Alerts fire correctly (test by stopping n8n)

4. **Logs:**
   - [ ] n8n logs visible in Loki
   - [ ] LogQL query: `{job="n8n"} |= "error"` shows errors

5. **Tests:**
   - [ ] `scripts/n8n-health-check.sh` passes
   - [ ] Metrics visible: `curl http://localhost:9090/api/v1/query?query=n8n_workflow_executions_total`

---

**Related Stories:**
- WF19: Grafana Dashboards (shared monitoring infrastructure)
- N8N1: Platform Deployment (n8n service)
