---
id: N8N7
epic: EPIC-011-n8n-workflow-automation
title: "n8n Provisioning Service"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/service/TenantService.java
    - backend/src/main/java/cz/muriel/core/auth/config/SecurityConfig.java
  test_paths:
    - backend/src/test/java/cz/muriel/core/tenant/TenantFilterIntegrationTest.java
  docs_paths:
    - docs/TENANT_ONBOARDING.md
---


# N8N7: n8n Provisioning Service

Status: ✅ DONE
Priority: High
Dependencies: N8N1, N8N2, N8N8

## Story
As a platform administrator, I want n8n tenant accounts to be auto-created on first access so that each tenant has a dedicated workflow workspace without manual provisioning.

## Acceptance Criteria
- Given a user with WF_READER/WF_EDITOR/WF_ADMIN role in a tenant realm, when they access /{tenant}/n8n, then the tenant n8n account is created if missing.
- Subsequent accesses reuse the existing tenant account and do not create duplicates.
- Account creation is logged with tenant and user context (audit trail).
- Provisioning failures return a clear error and do not expose n8n admin credentials.
- User management is enabled in n8n and verified during health checks.
- n8n owner account is created once if user management is not initialized.
- The proxy maintains an admin session cookie and re-authenticates if expired.
- Workflows marked active are re-activated after import so production webhooks register.

## Implementation Notes (Java/Spring Boot)
- Implement N8nProvisioningService to ensure tenant account exists.
- Implement N8nAdminClient (WebClient) to call n8n REST endpoints.
- Store admin credentials or API key in Vault, injected via environment/secret files.
- Use a stable naming convention: tenant-{realm}@n8n.local.
- Keep session/cookie handling in the BFF (N8N8) and call provisioning only when needed.
- Bootstrap owner account via `/owner/setup` when `settings.userManagement.showSetupOnFirstLoad` is true.
- Establish admin session via `/login` and reuse `n8n-auth` cookie for proxying.
- On startup, scan active workflows and call `/workflows/{id}/activate` to register webhooks.

## Implementation Mapping
code_paths:
  - backend/src/main/java/com/platform/n8n/provisioning/N8nProvisioningService.java
  - backend/src/main/java/com/platform/n8n/client/N8nAdminClient.java
  - backend/src/main/java/com/platform/n8n/model/N8nUser.java
  - backend/src/main/java/com/platform/n8n/model/N8nUserRequest.java

test_paths:
  - backend/src/test/java/com/platform/n8n/provisioning/N8nProvisioningServiceTest.java
  - backend/src/test/java/com/platform/n8n/client/N8nAdminClientTest.java

docs_paths:
  - docs/n8n/provisioning.md

## References (isp-migration-tool)
- services/n8n-provisioner/server.js
- scripts/setup-dev.sh (n8n owner setup)
