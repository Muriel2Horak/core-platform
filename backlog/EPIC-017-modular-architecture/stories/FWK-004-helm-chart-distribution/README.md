---
id: FWK-004
epic: EPIC-017-modular-architecture
title: "Helm Chart Distribution"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-017-modular-architecture/stories/FWK-004-helm-chart-distribution/README.md
    - backlog/EPIC-017-modular-architecture/README.md
---

# FWK-004: Helm Chart Distribution

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🟡 MEDIUM  
**Dependencies:** -  
**Category:** CORE as Framework

---

## 📖 User Story

**As a vendor**,  
I want Helm charts for easy CORE deployment,  
So that I can run CORE in Kubernetes with my custom modules.

---

## 🎯 Acceptance Criteria

- ⏳ Helm chart published to Helm repository
- ⏳ Chart: `core-platform/runtime`
- ⏳ Configurable via `values.yaml` (DB URL, Redis, Kafka, modules)
- ⏳ Volume mounts for custom modules
- ⏳ Horizontal Pod Autoscaling (HPA)

---

## 💻 Implementation

### Helm Chart Structure

```
charts/core-platform/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   └── pvc.yaml
```

### values.yaml

```yaml
# Core Platform Configuration
replicaCount: 2

image:
  repository: core-platform/backend
  tag: "1.0.0"

database:
  host: postgres
  port: 5432
  name: core
  username: core
  password: changeme

modules:
  enabled:
    - helpdesk
    - project-management
  
  volumeMounts:
    - name: custom-modules
      mountPath: /app/modules
      
persistence:
  enabled: true
  size: 10Gi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

### deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "core-platform.fullname" . }}
spec:
  replicas: {{ .Values.replicaCount }}
  template:
    spec:
      containers:
      - name: backend
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        env:
        - name: DATABASE_URL
          value: "jdbc:postgresql://{{ .Values.database.host }}:{{ .Values.database.port }}/{{ .Values.database.name }}"
        volumeMounts:
        {{- range .Values.modules.volumeMounts }}
        - name: {{ .name }}
          mountPath: {{ .mountPath }}
        {{- end }}
```

---

## 📦 Publishing

```bash
# Package chart
helm package charts/core-platform

# Publish to Helm repo
helm push core-platform-1.0.0.tgz oci://registry.muriel.cz/charts
```

---

**Last Updated:** 9. listopadu 2025
