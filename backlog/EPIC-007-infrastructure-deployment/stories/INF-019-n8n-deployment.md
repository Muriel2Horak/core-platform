# INF-019: N8N Workflow Engine Deployment

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 3 dny, ~800 LOC  
**Owner:** Platform + Workflow Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```yaml
# docker-compose.yml NEMÁ N8N service!
# Pouze zmínky v:
# - INF-003: n8n_client_secret
# - DB migrations: V3__n8n_integration.sql

# N8N běží KDE? JAK?
# - Local developer install?
# - Separate deployment?
# - Žádná integrace s platformou
```

**Issues:**
- Workflow engine není součástí platformy
- Není multi-tenant podpora
- Žádná integrace s Keycloak SSO
- Chybí monitoring (Prometheus + Grafana)

### Goal

**N8N jako first-class service:**

```yaml
services:
  n8n:
    image: n8nio/n8n:latest
    container_name: core-n8n
    environment:
      - N8N_BASIC_AUTH_ACTIVE=false
      - N8N_AUTH_BACKEND=keycloak
      - WEBHOOK_URL=https://workflows.${DOMAIN}
      - DB_TYPE=postgresdb
      - DB_POSTGRESDB_HOST=db
      - DB_POSTGRESDB_DATABASE=n8n
    labels:
      - traefik.http.routers.n8n.rule=Host(`workflows.${DOMAIN}`)
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **N8N Service Deployment**
   - Docker Compose service
   - PostgreSQL database (separate `n8n` DB)
   - Keycloak SSO integration
   - Subdomain routing: `workflows.core-platform.local`

2. ✅ **Multi-Tenant Support**
   - Tenant isolation via N8N workspaces
   - Separate credentials per tenant
   - Row-level security v DB

3. ✅ **Monitoring Integration**
   - Prometheus metrics export
   - Grafana dashboard
   - Loki log aggregation
   - Alerting (workflow failures)

### Implementation

**File:** `docker/docker-compose.yml` (N8N service)

```yaml
services:
  n8n:
    image: n8nio/n8n:latest
    container_name: core-n8n
    depends_on:
      db:
        condition: service_healthy
      keycloak:
        condition: service_started
    environment:
      # Database
      - DB_TYPE=postgresdb
      - DB_POSTGRESDB_HOST=db
      - DB_POSTGRESDB_PORT=5432
      - DB_POSTGRESDB_DATABASE=n8n
      - DB_POSTGRESDB_USER=${N8N_DB_USERNAME}
      - DB_POSTGRESDB_PASSWORD=${N8N_DB_PASSWORD}
      
      # Authentication (Keycloak SSO)
      - N8N_BASIC_AUTH_ACTIVE=false
      - N8N_AUTH_BACKEND=oauth2
      - OAUTH2_AUTHORIZATION_URL=https://${DOMAIN}/realms/admin/protocol/openid-connect/auth
      - OAUTH2_TOKEN_URL=https://keycloak:8443/realms/admin/protocol/openid-connect/token
      - OAUTH2_CLIENT_ID=n8n-client
      - OAUTH2_CLIENT_SECRET_FILE=/run/secrets/n8n_client_secret
      
      # Webhooks
      - WEBHOOK_URL=https://workflows.${DOMAIN}
      - N8N_PROTOCOL=https
      - N8N_HOST=workflows.${DOMAIN}
      
      # Multi-tenancy
      - N8N_MULTI_TENANT_ENABLED=true
      - N8N_TENANT_MODE=subdomain
      
      # Monitoring
      - N8N_METRICS=true
      - N8N_METRICS_PORT=9090
      
      # Execution
      - EXECUTIONS_MODE=queue
      - EXECUTIONS_PROCESS=main
      - QUEUE_BULL_REDIS_HOST=redis
      - QUEUE_BULL_REDIS_PORT=6379
    
    volumes:
      - n8n-data:/home/node/.n8n
    
    secrets:
      - n8n_client_secret
    
    networks:
      - core-net
    
    labels:
      # Traefik routing
      - traefik.enable=true
      - traefik.http.routers.n8n.rule=Host(`workflows.${DOMAIN}`)
      - traefik.http.routers.n8n.tls=true
      - traefik.http.routers.n8n.tls.certresolver=letsencrypt
      - traefik.http.services.n8n.loadbalancer.server.port=5678
      
      # Prometheus metrics
      - prometheus.job=n8n
      - prometheus.port=9090

volumes:
  n8n-data:

secrets:
  n8n_client_secret:
    file: ./secrets/n8n_client_secret.txt
```

**File:** `docker/postgres/init-n8n-db.sql`

```sql
-- Create N8N database
CREATE DATABASE n8n;

-- Create separate user for N8N
CREATE USER n8n_app WITH PASSWORD '${N8N_DB_PASSWORD}';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE n8n TO n8n_app;

-- Connect to n8n DB
\c n8n;

-- Multi-tenancy schema
CREATE TABLE tenants (
    id SERIAL PRIMARY KEY,
    subdomain VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Row-level security
ALTER TABLE workflow_entity ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON workflow_entity
    USING (tenant_id = current_setting('app.tenant_id')::int);
```

**File:** `backend/src/main/java/cz/muriel/core/n8n/N8NWebhookController.java`

```java
@RestController
@RequestMapping("/api/workflows")
public class N8NWebhookController {
    
    @Value("${n8n.base-url}")
    private String n8nBaseUrl;
    
    @Value("${n8n.api-key}")
    private String n8nApiKey;
    
    private final WebClient n8nClient;
    
    @PostMapping("/trigger/{workflowId}")
    public Mono<WorkflowExecutionResponse> triggerWorkflow(
            @PathVariable String workflowId,
            @RequestBody WorkflowTriggerRequest request,
            @AuthenticationPrincipal JwtAuthenticationToken auth) {
        
        // Extract tenant from JWT
        String tenant = auth.getToken().getClaim("tenant");
        
        return n8nClient.post()
            .uri("/webhook/{workflowId}", workflowId)
            .header("X-Tenant-ID", tenant)
            .header("X-N8N-API-KEY", n8nApiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(WorkflowExecutionResponse.class);
    }
}
```

**File:** `docker/prometheus/n8n-alerts.yml`

```yaml
groups:
  - name: n8n_workflows
    interval: 30s
    rules:
      # Workflow failures
      - alert: N8NWorkflowFailing
        expr: rate(n8n_workflow_executions_failed_total[5m]) > 0.1
        labels:
          severity: warning
        annotations:
          summary: "N8N workflow failures detected"
          description: "{{ $value }} workflows failed in last 5 min"
      
      # Webhook latency
      - alert: N8NWebhookSlow
        expr: histogram_quantile(0.95, n8n_webhook_duration_seconds_bucket) > 2
        labels:
          severity: warning
        annotations:
          summary: "N8N webhook response time high"
```

**File:** `docker/grafana/dashboards/n8n-dashboard.json`

```json
{
  "dashboard": {
    "title": "N8N Workflows",
    "panels": [
      {
        "title": "Workflow Executions",
        "targets": [{
          "expr": "rate(n8n_workflow_executions_total[5m])"
        }]
      },
      {
        "title": "Execution Time (P95)",
        "targets": [{
          "expr": "histogram_quantile(0.95, n8n_execution_duration_seconds_bucket)"
        }]
      },
      {
        "title": "Active Workflows by Tenant",
        "targets": [{
          "expr": "count(n8n_workflow_active) by (tenant)"
        }]
      }
    ]
  }
}
```

**Effort:** 3 dny  
**LOC:** ~800  
**Priority:** 🔥 CRITICAL (required for workflow features)

---

## 🔗 DEPENDENCIES

**Blocks:**
- WF13-WF19 (Workflow Management features)
- N8N1-N8N6 (N8N Integration features)

**Requires:**
- INF-003: Docker Secrets (n8n_client_secret)
- INF-007: DB Separate Users (n8n_app user)
- INF-012: Monitoring (Prometheus + Grafana)

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
