# T5: Metrics and Audit

**Parent Story:** N8N11 Workflow Registry Sync
**Status:** TODO
**Priority:** High
**Effort:** 1 hour
**Owner:** Backend

---

## Objective
Add metrics and audit entries for registry sync.

## Tasks
- Emit sync duration and success/failure metrics.
- Track last sync age metric.
- Add audit log entry for manual sync.

## Acceptance Criteria
- [ ] Metrics appear in Prometheus scrape.
- [ ] Manual sync is audited with user context.

## Dependencies
- T3 (Sync Service and Scheduler).
