# T3: BFF Proxy Controller

**Parent Story:** N8N6 Backend n8n Integration (BFF Pattern)
**Status:** TODO
**Priority:** High
**Effort:** 4 hours
**Owner:** Backend

---

## Objective
Expose BFF endpoints with role enforcement, audit logging, and governance hooks.

## Tasks
- Implement BFF endpoints for workflows and executions.
- Add role checks with WF_READER/WF_EDITOR/WF_ADMIN.
- Add cache eviction for activate/deactivate.
- Log admin actions to AuditService.
- Reuse service session cookie and refresh on expiry.
- Enforce governance checks before write operations.
- Trigger registry sync after successful writes (debounced).
- Expose /metrics for governance allow/deny and latency.

## Acceptance Criteria
- [ ] GET /api/n8n/workflows is guarded by WF_READER/WF_EDITOR/WF_ADMIN.
- [ ] Activate/deactivate require WF_EDITOR or WF_ADMIN.
- [ ] Delete execution requires WF_ADMIN.
- [ ] Audit logs are emitted for admin actions.
- [ ] Governance checks run before write operations.
- [ ] Registry sync is triggered after workflow writes.
- [ ] Proxy session is reused and refreshed when expired.
- [ ] /metrics exposes governance counters and latency.

## Dependencies
- T1 (n8n API Client).
- N8N11 (registry sync endpoint).
- N8N13 (governance rules and access check).
