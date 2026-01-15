# T3: Sync Service and Scheduler

**Parent Story:** N8N11 Workflow Registry Sync
**Status:** TODO
**Priority:** High
**Effort:** 3 hours
**Owner:** Backend

---

## Objective
Implement registry sync service and scheduled sync job.

## Tasks
- Implement WorkflowRegistryService.sync().
- Persist last sync timestamp and status.
- Add scheduled sync (cron) and manual trigger.

## Acceptance Criteria
- [ ] Sync writes or updates registry entries.
- [ ] Last sync timestamp is stored and exposed.
- [ ] Scheduled job runs at configured interval.

## Dependencies
- T1 (Registry Schema and Entity).
- T2 (n8n Metadata Fetch).
