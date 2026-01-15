---
id: N8N8
epic: EPIC-011-n8n-workflow-automation
title: "Multi-Tenant SSO and Routing"
priority: P1
status: ready
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths:
    - docker/nginx/nginx-ssl.conf.template
    - docker/nginx/nginx.dev.conf
    - docker/docker-compose.yml
    - docker/keycloak/realm-tenant-template.json
  test_paths: []
  docs_paths:
    - docs/DYNAMIC_JWT_MULTITENANCY.md
    - docs/MULTITENANCY_ARCHITECTURE.md
---

# N8N8: Multi-Tenant SSO and Routing

Status: TODO
Priority: High
Dependencies: N8N1, N8N2

## Story
As a platform administrator, I want multi-tenant routing to n8n via Nginx and oauth2-proxy so that tenant users access n8n only through Keycloak SSO with correct tenant context.

## Acceptance Criteria
- Given a tenant user, when they access https://{tenant}.${DOMAIN}/n8n, then Keycloak login is enforced via oauth2-proxy.
- Given a user without WF_* roles, when they access /n8n, then access is denied (403).
- Nginx injects X-Core-Tenant, X-Core-User, and X-Core-N8N-Account headers into the BFF.
- OAuth2-proxy maps realm_access.roles into X-Auth-Request-Roles and Nginx forwards it to the BFF.
- Role decisions are tenant-scoped (realm roles from the tenant token only).
- Webhook endpoints /n8n/webhook/* and /n8n/webhook-test/* bypass auth (public integrations only).
- OAuth2-proxy cookies use secure, samesite, and expiration settings.

## Implementation Notes
- Nginx routes /{tenant}/n8n/* and /n8n/* to oauth2-proxy.
- oauth2-proxy validates Keycloak issuer and restricts roles via allowed-group.
- Secrets (oauth2-proxy env, client secret, cookie secret) are sourced from Vault.
- Backend BFF validates tenant realm against JWT and proxies to n8n (see N8N6).

## Implementation Mapping
code_paths:
  - docker/nginx/nginx-ssl.conf.template
  - docker/environments/dev/compose.yml
  - docker/vault-agent/config.hcl
  - docker/vault-agent/templates/oauth2-proxy.env.ctmpl
  - docker/vault-agent/templates/oauth2-proxy-flower.env.ctmpl

test_paths:
  - e2e/specs/n8n/n8n-sso.spec.ts
  - e2e/specs/security/oauth2-proxy.spec.ts

docs_paths:
  - docs/infrastructure/oauth2-proxy-setup.md
  - docs/infrastructure/keycloak-oauth2-integration.md

## References (isp-migration-tool)
- docs/infrastructure/oauth2-proxy-setup.md
- docs/infrastructure/keycloak-oauth2-integration.md
- docs/security/auth-flows.md
