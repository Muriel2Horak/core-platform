# T1: Registry Schema and Entity

**Parent Story:** N8N11 Workflow Registry Sync
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** Backend

---

## Objective
Add database schema and entity for workflow registry entries.

## Tasks
- Create Flyway migration for workflow_registry table.
- Implement JPA entity and repository.
- Add upsert by workflow_id.

## Acceptance Criteria
- [ ] Migration creates required columns and indexes.
- [ ] Entity maps to table with tags/category fields.
- [ ] Repository supports upsert behavior.

## Dependencies
- Database migrations configured.
