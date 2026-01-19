# T4: Proxy Enforcement

**Parent Story:** N8N13 Workflow Governance and RBAC
**Status:** TODO
**Priority:** High
**Effort:** 3 hours
**Owner:** Backend

---

## Objective
Enforce governance decisions in the proxy layer.

## Tasks
- Enforce checks before workflow write operations.
- Optional list filtering for view-only access.
- Fallback allow with audit log when registry unavailable.

## Acceptance Criteria
- [ ] Write operations are blocked when decision is deny.
- [ ] List filtering hides workflows without view access.
- [ ] Registry outages degrade to allow with audit note.

## Dependencies
- T3 (Registry Access Check Endpoint).
