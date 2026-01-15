# T4: Registry API Endpoints

**Parent Story:** N8N11 Workflow Registry Sync
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** Backend

---

## Objective
Expose registry list and sync endpoints (public and internal).

## Tasks
- Implement GET /api/workflow-registry with filters.
- Implement POST /api/workflow-registry/sync.
- Implement POST /internal/workflow-registry/sync with service token.

## Acceptance Criteria
- [ ] List endpoint returns registry entries with filters.
- [ ] Manual sync returns summary result.
- [ ] Internal sync validates X-Service-Token.

## Dependencies
- T3 (Sync Service and Scheduler).
