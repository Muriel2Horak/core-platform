# TASK-022-03: Auth + tenant context + caching

## Goal
Zajistit auth, tenant context a caching pro BFF.

## Tasks
- [ ] Propagovat JWT a tenant headers do backendu.
- [ ] Implementovat tenant-aware cache (Redis).
- [ ] Pridat circuit breaker a timeouty pro backend calls.

## Output
- Bezpecny a rychly BFF s tenant izolaci.

## Acceptance Criteria for This Subtask
- [ ] Tenant context se propisuje do backend callu.
- [ ] Cache je oddelena per tenant.
- [ ] P95 response time <200ms pro hot paths.
