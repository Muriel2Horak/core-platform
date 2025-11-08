# S5: Monitoring & Alerting Integration

> **Observability:** Integrate n8n with Grafana dashboards and alerting

## 📋 Story

**As a** platform administrator  
**I want** n8n metrics in Grafana with alerting  
**So that** I can monitor workflow health and respond to failures

## 🎯 Acceptance Criteria

**GIVEN** n8n is running with workflows  
**WHEN** accessing Grafana dashboard  
**THEN** n8n metrics are displayed (executions, failures, duration)  
**AND** alerts trigger on execution failures >10%  
**AND** logs are searchable in Loki

## 🏗️ Implementation

### 1. Grafana Dashboard

**File:** `monitoring/dashboards/n8n-workflows.json`

```json
{
  "dashboard": {
    "title": "n8n Workflow Automation",
    "panels": [
      {
        "id": 1,
        "title": "Total Workflow Executions",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(n8n_workflow_executions_total)",
            "legendFormat": "Executions"
          }
        ]
      },
      {
        "id": 2,
        "title": "Execution Success Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "sum(n8n_workflow_executions_success) / sum(n8n_workflow_executions_total) * 100",
            "legendFormat": "Success %"
          }
        ],
        "fieldConfig": {
          "defaults": {
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
        "title": "Execution Duration (p95)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, n8n_workflow_execution_duration_seconds_bucket)",
            "legendFormat": "{{workflow_name}}"
          }
        ]
      },
      {
        "id": 4,
        "title": "Active Workflows",
        "type": "stat",
        "targets": [
          {
            "expr": "count(n8n_active_workflows)",
            "legendFormat": "Active"
          }
        ]
      },
      {
        "id": 5,
        "title": "Execution Failures (24h)",
        "type": "table",
        "targets": [
          {
            "expr": "topk(10, sum by (workflow_name) (increase(n8n_workflow_executions_failed[24h])))",
            "format": "table"
          }
        ]
      }
    ]
  }
}
```

### 2. Prometheus Metrics Scraper

**File:** `monitoring/prometheus/n8n-scrape.yml`

```yaml
scrape_configs:
  - job_name: 'n8n'
    static_configs:
      - targets: ['n8n:5678']
    metrics_path: '/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s
```

### 3. Alerting Rules

**File:** `monitoring/alerts/n8n-alerts.yml`

```yaml
groups:
  - name: n8n_alerts
    interval: 1m
    rules:
      - alert: N8nHighFailureRate
        expr: |
          (sum(rate(n8n_workflow_executions_failed[5m])) / 
           sum(rate(n8n_workflow_executions_total[5m]))) * 100 > 10
        for: 5m
        labels:
          severity: warning
          component: n8n
        annotations:
          summary: "n8n workflow failure rate above 10%"
          description: "{{ $value | humanizePercentage }} of workflows are failing"

      - alert: N8nWorkflowStuck
        expr: |
          max(n8n_workflow_execution_duration_seconds) > 3600
        for: 10m
        labels:
          severity: critical
          component: n8n
        annotations:
          summary: "n8n workflow execution taking >1 hour"
          description: "Workflow {{ $labels.workflow_name }} stuck for {{ $value | humanizeDuration }}"

      - alert: N8nDown
        expr: up{job="n8n"} == 0
        for: 2m
        labels:
          severity: critical
          component: n8n
        annotations:
          summary: "n8n service is down"
          description: "n8n has been unreachable for 2 minutes"
```

### 4. Loki Log Integration

**File:** `docker/docker-compose.yml` (n8n service update)

```yaml
services:
  n8n:
    logging:
      driver: loki
      options:
        loki-url: "http://loki:3100/loki/api/v1/push"
        loki-pipeline-stages: |
          - json:
              expressions:
                level: level
                workflow: workflow
                execution_id: executionId
        labels: |
          job=n8n,component=workflow-automation
```

### 5. Grafana Data Source

**File:** `monitoring/grafana/datasources.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true

  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    jsonData:
      derivedFields:
        - datasourceUid: loki
          matcherRegex: "execution_id=(\\w+)"
          name: ExecutionID
          url: "https://admin.core-platform.local/n8n/execution/${__value.raw}"
```

## ✅ Testing

```bash
# 1. Import dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @monitoring/dashboards/n8n-workflows.json

# 2. Test metrics scraping
curl http://localhost:5678/metrics
# Expected: Prometheus metrics output

# 3. Trigger test alert (execute failing workflow)
curl -X POST https://admin.core-platform.local/n8n/api/v1/workflows/test-failure/execute

# 4. Check logs in Loki
curl -G http://localhost:3100/loki/api/v1/query_range \
  --data-urlencode 'query={job="n8n"}' \
  --data-urlencode 'limit=10'

# 5. View dashboard
# Open: https://admin.core-platform.local/grafana/d/n8n-workflows
# Verify: Panels show data, no errors
```

## 🎯 Acceptance Checklist

- [x] Grafana dashboard created (5 panels)
- [x] Prometheus scraping n8n metrics
- [x] 3 alert rules configured (failure rate, stuck workflows, down)
- [x] Loki capturing n8n logs
- [x] Dashboard accessible via Grafana UI

---

**Effort**: ~4 hours  
**LOC**: ~300 lines
