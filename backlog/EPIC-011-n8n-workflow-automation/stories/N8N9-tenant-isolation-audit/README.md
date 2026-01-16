---
id: N8N9
epic: EPIC-011-n8n-workflow-automation
title: "Tenant Isolation and Audit Trail"
priority: P2
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "1.5 days"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/security/policy/MetamodelPolicyEngine.java
    - backend/src/main/java/cz/muriel/core/monitoring/bff/audit/MonitoringAuditFilter.java
    - docker/promtail
  test_paths:
    - backend/src/test/java/cz/muriel/core/tenant/TenantResolverTest.java
  docs_paths:
    - docs/MULTITENANCY_ARCHITECTURE.md
    - docs/SECURITY_RUNBOOK.md
---


# N8N9: Tenant Isolation and Audit Trail

Status: ✅ DONE
Priority: Medium
Dependencies: N8N8, N8N6

## Story
As a platform administrator, I want enforced tenant isolation and audit evidence for all n8n access so that cross-tenant leakage is prevented and actions are traceable.

## Acceptance Criteria
- Requests with X-Core-Tenant mismatching JWT realm are rejected (403).
- Nginx and backend logs contain tenant, user, and n8n account identifiers.
- Loki queries can filter n8n actions by tenant and user.
- E2E tests confirm tenant A cannot access tenant B workflows or data.
- Workflow role checks use tenant realm roles only (no cross-realm role leakage).
- n8n account mapping is tenant-scoped (tenant A user cannot access tenant B n8n account).

## Implementation Notes (Java/Spring Boot)
- Add a request filter/interceptor to validate tenant headers against JWT realm.
- Emit audit log entries with tenant/user/n8n account on BFF proxy actions.
- Configure Promtail/Loki labels for n8n access logs.

## Implementation Mapping
code_paths:
  - backend/src/main/java/com/platform/security/TenantHeaderGuard.java
  - backend/src/main/java/com/platform/n8n/audit/N8nAuditLogger.java
  - docker/promtail/promtail-config.yml

test_paths:
  - e2e/specs/n8n/n8n-tenant-isolation.spec.ts
  - backend/src/test/java/com/platform/security/TenantHeaderGuardTest.java

docs_paths:
  - docs/security/n8n-tenant-isolation.md

## References (isp-migration-tool)
- docs/security/SECURITY-ARCHITECTURE.md
- docs/security/runbooks/webhook-abuse.md
