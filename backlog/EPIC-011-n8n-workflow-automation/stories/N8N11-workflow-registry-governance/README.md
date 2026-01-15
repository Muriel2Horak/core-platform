---
id: N8N11
epic: EPIC-011-n8n-workflow-automation
title: "Workflow Registry Sync"
priority: P1
status: ready
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "1.5 days"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/workflow/WorkflowService.java
    - backend/src/main/java/cz/muriel/core/workflow/WorkflowController.java
  test_paths: []
  docs_paths:
    - docs/MODULE_REGISTRY.md
    - docs/MODULE_LICENSING.md
---

# N8N11: Workflow Registry Sync

Status: TODO
Priority: High
Estimate: 1.5 days (~500 LOC)
Dependencies: N8N6, N8N7

## Story
As a platform administrator, I want a workflow registry that syncs metadata from n8n so that workflows are discoverable and auditable in Core.

## Acceptance Criteria
- Registry sync pulls workflow metadata from n8n and stores it in Core DB.
- Registry list endpoint returns consistent metadata (id, name, category, active).
- Sync can be triggered manually and scheduled (cron or admin endpoint).
- Service account (Keycloak client credentials) is used for registry sync.
- Internal sync endpoint (`POST /internal/workflow-registry/sync`) accepts `X-Service-Token` from the proxy.
- Proxy triggers sync after successful workflow create/update/activate/delete (debounced).

## Implementation Notes (Java/Spring Boot)
- Implement WorkflowRegistry entity, repository, and service.
- Provide endpoints:
  - GET /api/workflow-registry (list)
  - POST /api/workflow-registry/sync (manual sync)
- Sync job uses n8n REST API and a service account token (n8n-internal).
- Add metrics for sync success/failure and last sync age.
- Add internal endpoint for proxy-triggered sync with service token validation.

## Implementation Tasks

### Task 1: Schema + Entity
**Estimate:** 2 hours  
**Acceptance:**
- [ ] Flyway migration for workflow_registry table (id, name, category, active, tags, updated_at)
- [ ] JPA entity + repository with upsert by workflow_id

### Task 2: n8n Metadata Fetch
**Estimate:** 2 hours  
**Acceptance:**
- [ ] Reuse N8nApiClient or create a registry-specific client
- [ ] Pull workflows with pagination and map to registry fields
- [ ] Normalize tags/category and active flag

### Task 3: Sync Service + Scheduler
**Estimate:** 3 hours  
**Acceptance:**
- [ ] WorkflowRegistryService.sync() writes/updates registry entries
- [ ] Last sync timestamp persisted and exposed
- [ ] Scheduled sync (cron) + manual trigger supported

### Task 4: API Endpoints
**Estimate:** 2 hours  
**Acceptance:**
- [ ] GET /api/workflow-registry returns list with filters (tenant/category/active)
- [ ] POST /api/workflow-registry/sync runs sync and returns summary
- [ ] POST /internal/workflow-registry/sync validates X-Service-Token

### Task 5: Metrics + Audit
**Estimate:** 1 hour  
**Acceptance:**
- [ ] Metrics for sync duration, success/failure, last sync age
- [ ] Audit log entry for manual sync trigger

### Task 6: Tests + Docs
**Estimate:** 3 hours  
**Acceptance:**
- [ ] Unit tests for mapping and sync logic
- [ ] Integration test for internal sync endpoint
- [ ] Docs updated with registry model and sync flow

## Dev Checklist

- [ ] Service account `n8n-internal` created in Keycloak
- [ ] Vault secret for registry client secret available to backend
- [ ] Internal service token configured for proxy-triggered sync
- [ ] n8n REST API reachable from backend network
- [ ] Migration applied and registry list endpoint returns data

## Implementation Mapping
code_paths:
  - backend/src/main/java/com/platform/n8n/registry/WorkflowRegistry.java
  - backend/src/main/java/com/platform/n8n/registry/WorkflowRegistryService.java
  - backend/src/main/java/com/platform/n8n/registry/WorkflowRegistryController.java
  - backend/src/main/resources/db/migration/V__workflow_registry.sql

test_paths:
  - backend/src/test/java/com/platform/n8n/registry/WorkflowRegistryServiceTest.java
  - backend/src/test/java/com/platform/n8n/registry/WorkflowRegistryAccessTest.java

docs_paths:
  - docs/n8n/workflow-registry.md
  - docs/security/workflow-registry.md

## References (isp-migration-tool)
- backend/app/api/workflow_registry.py
- services/n8n-provisioner/server.js (registry enforcement)
- docs/infrastructure/keycloak-oauth2-integration.md
