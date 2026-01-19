# T4: DTOs and Mapping

**Parent Story:** N8N6 Backend n8n Integration (BFF Pattern)
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** Backend

---

## Objective
Provide sanitized DTOs for workflow and execution responses.

## Tasks
- Define workflow summary and detail DTOs.
- Define execution DTOs with duration and status.
- Implement mapping helpers in the controller.

## Acceptance Criteria
- [ ] DTOs exclude credentials and sensitive node params.
- [ ] Execution DTOs include status, duration, and timestamps.
- [ ] Mapping helpers are unit tested.

## Dependencies
- T2 (n8n Domain Models).
