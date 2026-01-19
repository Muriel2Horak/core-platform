# T10: E2E Tests

**Parent Story:** N8N6 Backend n8n Integration (BFF Pattern)
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** QA

---

## Objective
Validate dashboard behavior and role gating in E2E.

## Tasks
- Add Playwright tests for workflow dashboard.
- Verify role-based UI (WF_READER vs WF_EDITOR/WF_ADMIN).
- Verify activate/deactivate flow.

## Acceptance Criteria
- [ ] E2E tests pass for reader and editor/admin roles.
- [ ] Admin actions trigger correct API calls.

## Dependencies
- T6 (Frontend Dashboard Component).
- T8 (API Client Integration).
