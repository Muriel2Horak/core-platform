# T8: API Client Integration

**Parent Story:** N8N6 Backend n8n Integration (BFF Pattern)
**Status:** TODO
**Priority:** High
**Effort:** 1 hour
**Owner:** Frontend

---

## Objective
Wire frontend API client to the BFF endpoints.

## Tasks
- Add workflow and execution API calls.
- Add activate/deactivate endpoints.
- Inject JWT header and error handling.

## Acceptance Criteria
- [ ] API client exposes workflow list and execution list functions.
- [ ] Admin actions call activate/deactivate endpoints.
- [ ] Unauthorized responses are handled gracefully.

## Dependencies
- T7 (TypeScript Types).
- BFF endpoints available.
