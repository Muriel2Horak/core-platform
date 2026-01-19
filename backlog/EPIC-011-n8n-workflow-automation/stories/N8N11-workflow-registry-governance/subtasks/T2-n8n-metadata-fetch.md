# T2: n8n Metadata Fetch

**Parent Story:** N8N11 Workflow Registry Sync
**Status:** TODO
**Priority:** High
**Effort:** 2 hours
**Owner:** Backend

---

## Objective
Fetch workflow metadata from n8n and map to registry fields.

## Tasks
- Reuse or extend N8nApiClient for registry sync.
- Handle pagination and rate limits.
- Map workflow tags/category/active fields.

## Acceptance Criteria
- [ ] All workflows are fetched with pagination.
- [ ] Mapping normalizes tags and categories.
- [ ] Sync handles n8n API errors gracefully.

## Dependencies
- T1 (Registry Schema and Entity).
- n8n API reachable.
