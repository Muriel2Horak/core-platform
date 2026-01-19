# T2: Governance Decision Service

**Parent Story:** N8N13 Workflow Governance and RBAC
**Status:** TODO
**Priority:** High
**Effort:** 3 hours
**Owner:** Backend

---

## Objective
Implement decision logic for workflow access by role and action.

## Tasks
- Implement WorkflowGovernanceService rules for view/edit/run/admin.
- Add short TTL cache for decisions.
- Return decision reason codes.

## Acceptance Criteria
- [ ] Decisions align with WF_* role mapping.
- [ ] Cache reduces repeated checks.
- [ ] Decision reason codes are stable.

## Dependencies
- T1 (Role Mapping and Headers).
