---
id: INF-012
epic: EPIC-007-infrastructure-deployment
title: "Comprehensive Monitoring & Alerting"
priority: P1
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-012-monitoring-alerting/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-012: Comprehensive Monitoring & Alerting

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** HIGH  
**Effort:** 3 dny, ~700 LOC  
**Owner:** Platform + Ops Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```yaml
# Máme monitoring stack:
- Grafana (dashboards)
- Loki (log aggregation)
- Prometheus (metrics - MISSING!)

# ALE CHYBÍ:
- Alerting rules
- Notification channels
- SLA monitoring
- Business metrics
```

**Issues:**
- Service DOWN → zjištěno až kdy user complains
- Performance degradation undetected
- Žádné proactive alerts

### Goal

**Complete monitoring:**

```yaml
# Metrics Collection
prometheus:
  - Backend JVM metrics
  - Database performance
  - Kafka lag
  - SSL expiry

# Alerting Rules
alerts:
  - Service down > 1 min → PagerDuty
  - CPU > 80% for 5 min → Slack
  - DB connections > 90% → Email
  - SSL expires < 7 days → Slack
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Prometheus Integration**
   - Scrape: Backend (/actuator/prometheus)
   - Scrape: Postgres Exporter
   - Scrape: Kafka Exporter
   - Retention: 30 days

2. ✅ **Alert Rules**
   - Service availability (uptime)
   - Resource usage (CPU, memory, disk)
   - Application metrics (response time, error rate)
   - SSL certificate expiry

3. ✅ **Notification Channels**
   - Slack (non-critical)
   - Email (critical)
   - PagerDuty (P1 incidents)

### Implementation

**File:** `docker-compose.yml` (Prometheus service)

```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=30d'
    ports:
      - "9090:9090"

  # Exporters
  postgres-exporter:
    image: prometheuscommunity/postgres-exporter
    environment:
      DATA_SOURCE_NAME: "postgresql://core:core@db:5432/core?sslmode=disable"

  kafka-exporter:
    image: danielqsj/kafka-exporter
    command:
      - '--kafka.server=kafka:9092'

volumes:
  prometheus-data:
```

**File:** `docker/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

# Alert rules
rule_files:
  - 'alerts/*.yml'

# Scrape configs
scrape_configs:
  # Backend metrics
  - job_name: 'backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']

  # Database metrics
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Kafka metrics
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

**File:** `docker/prometheus/alerts/service-health.yml`

```yaml
groups:
  - name: service_health
    interval: 30s
    rules:
      # Backend Down
      - alert: BackendDown
        expr: up{job="backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Backend service is down"
          description: "Backend has been down for more than 1 minute"

      # Database Down
      - alert: DatabaseDown
        expr: up{job="postgres"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database is down"

      # High CPU
      - alert: HighCPU
        expr: process_cpu_usage{job="backend"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage on backend"
          description: "CPU usage is {{ $value | humanizePercentage }}"

      # SSL Expiry
      - alert: SSLExpiringSoon
        expr: (ssl_certificate_expiry_seconds - time()) / 86400 < 7
        labels:
          severity: warning
        annotations:
          summary: "SSL certificate expiring soon"
          description: "Certificate expires in {{ $value }} days"
```

**File:** `docker/alertmanager/config.yml`

```yaml
global:
  slack_api_url: 'https://hooks.slack.com/services/YOUR_WEBHOOK'

route:
  receiver: 'slack-notifications'
  group_wait: 10s
  group_interval: 5m
  repeat_interval: 3h
  
  routes:
    # Critical alerts → PagerDuty
    - match:
        severity: critical
      receiver: 'pagerduty'
    
    # Warnings → Slack
    - match:
        severity: warning
      receiver: 'slack-notifications'

receivers:
  - name: 'slack-notifications'
    slack_configs:
      - channel: '#alerts'
        text: '{{ .CommonAnnotations.summary }}'

  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_KEY'
```

**Grafana Dashboard:**

```json
{
  "dashboard": {
    "title": "Platform Health",
    "panels": [
      {
        "title": "Service Uptime",
        "targets": [
          {
            "expr": "up{job=\"backend\"}",
            "legendFormat": "Backend"
          }
        ]
      },
      {
        "title": "Response Time (P95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, http_server_requests_seconds_bucket)",
            "legendFormat": "{{ uri }}"
          }
        ]
      }
    ]
  }
}
```

**Effort:** 3 dny  
**LOC:** ~700  
**Priority:** HIGH

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
