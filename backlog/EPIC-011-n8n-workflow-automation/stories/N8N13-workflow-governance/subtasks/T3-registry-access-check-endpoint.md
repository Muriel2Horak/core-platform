# T3: Registry Access Check Endpoint

**Parent Story:** N8N13 Workflow Governance and RBAC
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** Backend

---

## Objective
Expose access check endpoint for the n8n proxy.

## Tasks
- Implement POST /api/workflow-registry/access/check.
- Require WF_PROXY_SERVICE service account.
- Return allow/deny decision with reason and category.

## Acceptance Criteria
- [ ] Endpoint validates service account role.
- [ ] Response includes allowed and reason fields.
- [ ] Errors are logged with trace ID.

## Dependencies
- T2 (Governance Decision Service).
- N8N11 registry data available.
