---
id: N8N13
epic: EPIC-011-n8n-workflow-automation
title: "Workflow Governance and RBAC"
priority: P1
status: ready
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/security/policy/MetamodelPolicyEngine.java
    - backend/src/main/java/cz/muriel/core/auth/config/SecurityConfig.java
    - docker/nginx/nginx-ssl.conf.template
  test_paths: []
  docs_paths:
    - backlog/WORKFLOW_UNIFIED_ARCHITECTURE.md
    - backlog/WORKFLOW_N8N_AUDIT_2025.md
---

# N8N13: Workflow Governance and RBAC

Status: TODO
Priority: High
Estimate: 2 days (~500 LOC)
Dependencies: N8N11, N8N2, N8N3

## Story
As a platform administrator, I want workflow access governed by Keycloak roles so that n8n usage aligns with RBAC and audit policies across tenants.

## Acceptance Criteria
- Keycloak realm roles defined: WF_ADMIN, WF_EDITOR, WF_READER, WF_PROXY_SERVICE (admin + tenant realms).
- OAuth2 proxy maps roles claim (`roles` or `realm_access.roles`) into headers (e.g. `X-Auth-Request-Roles`), passed through Nginx to BFF.
- Role-to-action mapping is enforced:
  - view: WF_READER or higher
  - edit/run: WF_EDITOR or WF_ADMIN
  - admin/governance: WF_ADMIN
- Registry access checks evaluate roles + workflow category and return allow/deny with reason.
- n8n UI access respects role checks (view/edit/run) via proxy/BFF enforcement.
- Audit log includes workflow_id, category, action, subject email, roles, and decision outcome.
- Decisions are cached (short TTL) and include reason codes (e.g. registry_disabled, registry_unavailable).
- If registry is unavailable, proxy falls back to allow and logs the degraded decision.
- Optional list filtering hides workflows the user cannot view (governance-gated list).
- Governance metrics expose allow/deny counts and authorization latency.

## Implementation Notes (Java/Spring Boot)
- Define role mapping strategy (Keycloak realm roles -> workflow permissions).
- Accept roles from `X-Auth-Request-Roles` (preferred) or `X-Auth-Request-Groups`, fallback to JWT claim `realm_access.roles`.
- Enforce access checks in proxy/BFF before calling n8n REST endpoints.
- Require service account role (WF_PROXY_SERVICE) for registry access checks called by proxy.
- Add caching for governance decisions (short TTL) to reduce latency.
- Provide a registry access check endpoint for the proxy (e.g. POST /api/workflow-registry/access/check).
- Emit Prometheus metrics for allow/deny and authorization latency.

## Implementation Tasks

### Task 1: Role Mapping + Headers
**Estimate:** 2 hours  
**Acceptance:**
- [ ] Parse roles from X-Auth-Request-Roles or X-Auth-Request-Groups
- [ ] Fallback to JWT realm_access.roles when headers missing
- [ ] Normalize roles to WF_READER/WF_EDITOR/WF_ADMIN

### Task 2: Governance Decision Service
**Estimate:** 3 hours  
**Acceptance:**
- [ ] Implement WorkflowGovernanceService decision rules (view/edit/run/admin)
- [ ] Short TTL cache for decisions (configurable)
- [ ] Return reason codes (allowed/denied + reason)

### Task 3: Registry Access Check Endpoint
**Estimate:** 2 hours  
**Acceptance:**
- [ ] POST /api/workflow-registry/access/check accepts workflow_id/action/user/roles
- [ ] Requires WF_PROXY_SERVICE service account
- [ ] Returns allow/deny with reason and category

### Task 4: Proxy Enforcement
**Estimate:** 3 hours  
**Acceptance:**
- [ ] Enforce checks before write operations (/rest/workflows create/edit/activate/delete)
- [ ] Optional list filtering for /rest/workflows (view-only)
- [ ] Fallback allow with audit log when registry unavailable

### Task 5: Metrics + Audit
**Estimate:** 2 hours  
**Acceptance:**
- [ ] Prometheus counters for allow/deny/error
- [ ] Histogram for authorization latency
- [ ] Audit log includes workflow_id, action, roles, decision

### Task 6: Tests + Docs
**Estimate:** 3 hours  
**Acceptance:**
- [ ] Unit tests for decision rules and caching
- [ ] Integration test for access check endpoint
- [ ] E2E test for role-gated workflow edit
- [ ] Docs for role mapping + governance flow

## Dev Checklist

- [ ] WF_* roles created in admin and tenant realms
- [ ] oauth2-proxy forwards role headers to BFF
- [ ] Registry sync (N8N11) running and populated
- [ ] Proxy can call access check endpoint with service account
- [ ] Governance metrics visible in Prometheus

## Implementation Mapping
code_paths:
  - backend/src/main/java/com/platform/n8n/governance/WorkflowGovernanceService.java
  - backend/src/main/java/com/platform/n8n/governance/WorkflowAccessDecision.java
  - backend/src/main/java/com/platform/n8n/proxy/N8nProxyController.java
  - docker/nginx/nginx-ssl.conf.template

test_paths:
  - backend/src/test/java/com/platform/n8n/governance/WorkflowGovernanceServiceTest.java
  - e2e/specs/n8n/n8n-governance.spec.ts

docs_paths:
  - docs/security/workflow-governance.md
  - docs/n8n/role-mapping.md

## References (isp-migration-tool)
- docs/workflow-governance-setup.md
- docs/infrastructure/keycloak-oauth2-integration.md
- backend/app/models/workflow_registry.py
