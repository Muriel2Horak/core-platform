# T1: WebSocket Infrastructure
**Effort:** ~4h | **LOC:** ~300

## Goal
Zprovoznit WebSocket server pro real-time metrics broadcast.

## Files
- `backend/src/main/java/cz/muriel/core/monitoring/LiveMetricsWebSocketHandler.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/LiveMetricsDTO.java`
- `backend/src/main/java/cz/muriel/core/config/WebSocketConfig.java`

## Tasks
- [ ] Implementovat WS endpoint `/ws/live-metrics` s JWT auth.
- [ ] Zavest tenant scope v session a payloadu.
- [ ] Pridat broadcast scheduler (2s) a backpressure limit.
- [ ] Zajistit reconnect/backoff v klientu.

## Output
- Stabilni WS infrastruktura pro live metriky.

## Acceptance Criteria
- WebSocket endpoint `/ws/live-metrics` funguje
- Broadcast každých 2s
- Auto-reconnect on disconnect
- Session management
