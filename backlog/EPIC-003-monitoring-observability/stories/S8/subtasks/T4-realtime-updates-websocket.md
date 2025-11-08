# T4: Real-Time Updates (WebSocket)
**Effort:** ~3h | **LOC:** ~200

## Objective
Live updates pro kritické metriky

## Files
- `backend/src/main/java/cz/muriel/core/monitoring/DashboardWebSocketHandler.java`
- `frontend/src/hooks/useRealtimeDashboard.ts`

## Acceptance Criteria
- WebSocket connection established
- Metrics update každých 5s
- Auto-reconnect on disconnect

## Implementation
See [S8 Story](../S8.md#t4-real-time-updates-websocket)
