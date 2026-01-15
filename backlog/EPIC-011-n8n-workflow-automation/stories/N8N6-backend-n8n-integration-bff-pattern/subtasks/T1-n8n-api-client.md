# T1: n8n API Client

**Parent Story:** N8N6 Backend n8n Integration (BFF Pattern)
**Status:** TODO
**Priority:** High
**Effort:** 3 hours
**Owner:** Backend

---

## Objective
Implement a typed n8n REST API client for workflows and executions.

## Tasks
- Configure WebClient with base URL, API key, and timeouts.
- Implement list/detail/executions/activate/deactivate/delete calls.
- Add error handling with structured logging.

## Acceptance Criteria
- [ ] WebClient uses configured base URL and API key.
- [ ] Calls return typed models or empty list on failure.
- [ ] Timeouts and logging are in place.
- [ ] Unit tests use MockWebServer.

## Dependencies
- n8n service reachable on internal network.
