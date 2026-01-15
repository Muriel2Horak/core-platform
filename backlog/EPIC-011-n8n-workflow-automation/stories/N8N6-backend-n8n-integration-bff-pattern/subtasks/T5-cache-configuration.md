# T5: Cache Configuration

**Parent Story:** N8N6 Backend n8n Integration (BFF Pattern)
**Status:** TODO
**Priority:** High
**Effort:** 0.5 hour
**Owner:** Backend

---

## Objective
Configure workflow list caching for the BFF API.

## Tasks
- Add Caffeine cache config for n8n-workflows.
- Set TTL to 5 minutes and size to 100 entries.
- Evict cache on activate/deactivate.

## Acceptance Criteria
- [ ] Cache config is present in application.yml.
- [ ] Cache eviction runs on workflow state changes.

## Dependencies
- T3 (BFF Proxy Controller).
