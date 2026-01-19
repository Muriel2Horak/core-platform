# T5: Metrics and Audit

**Parent Story:** N8N13 Workflow Governance and RBAC
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** Backend

---

## Objective
Capture governance metrics and audit logs.

## Tasks
- Add counters for allow/deny/error.
- Add histogram for authorization latency.
- Emit audit logs with workflow_id, action, roles, decision.

## Acceptance Criteria
- [ ] Metrics appear in Prometheus scrape.
- [ ] Audit logs include reason codes and user context.

## Dependencies
- T2 (Governance Decision Service).
