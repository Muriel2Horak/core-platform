# T9: Integration Tests

**Parent Story:** N8N6 Backend n8n Integration (BFF Pattern)
**Status:** TODO
**Priority:** High
**Effort:** 3 hours
**Owner:** Backend

---

## Objective
Add integration tests for BFF endpoints and role enforcement.

## Tasks
- Mock n8n API responses (WireMock).
- Test workflow list and executions endpoints.
- Test role-based access for WF_READER/WF_EDITOR/WF_ADMIN.
- Verify cache hit/miss and audit logging.

## Acceptance Criteria
- [ ] Tests cover success and error cases.
- [ ] Role-based access is enforced.
- [ ] Cache and audit behavior verified.

## Dependencies
- T3 (BFF Proxy Controller).
