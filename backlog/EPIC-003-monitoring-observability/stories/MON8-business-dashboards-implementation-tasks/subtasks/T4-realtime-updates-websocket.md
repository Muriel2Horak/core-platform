# T4: Real-Time Updates (WebSocket)
**Effort:** ~3h | **LOC:** ~200

## Goal
Zajistit live updates pro kriticke metriky.

## Files
- `backend/src/main/java/cz/muriel/core/monitoring/DashboardWebSocketHandler.java`
- `frontend/src/hooks/useRealtimeDashboard.ts`

## Tasks
- [ ] Implementovat WS handler s tenant scoped channel.
- [ ] Pridat client hook s reconnect/backoff.
- [ ] Nastavit update interval a fallback polling.

## Output
- Real-time update stream pro dashboard metriky.

## Acceptance Criteria
- WebSocket connection established
- Metrics update každých 5s
- Auto-reconnect on disconnect
