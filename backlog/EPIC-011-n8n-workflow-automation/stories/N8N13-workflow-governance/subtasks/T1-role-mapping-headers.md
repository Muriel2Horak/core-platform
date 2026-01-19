# T1: Role Mapping and Headers

**Parent Story:** N8N13 Workflow Governance and RBAC
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** Backend

---

## Objective
Parse roles from auth headers and normalize to WF_* roles.

## Tasks
- Read X-Auth-Request-Roles or X-Auth-Request-Groups.
- Fallback to JWT realm_access.roles.
- Normalize to WF_READER/WF_EDITOR/WF_ADMIN.

## Acceptance Criteria
- [ ] Header parsing supports comma-separated roles.
- [ ] Fallback to JWT works when headers missing.
- [ ] Normalization map is unit tested.

## Dependencies
- OAuth2-proxy forwards role headers.
