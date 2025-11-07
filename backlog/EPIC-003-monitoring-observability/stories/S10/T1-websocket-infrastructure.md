# T1: WebSocket Infrastructure
**Effort:** ~4h | **LOC:** ~300

## Objective
WebSocket server pro real-time metrics broadcast

## Files
- `backend/src/main/java/cz/muriel/core/monitoring/LiveMetricsWebSocketHandler.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/LiveMetricsDTO.java`
- `backend/src/main/java/cz/muriel/core/config/WebSocketConfig.java`

## Acceptance Criteria
- WebSocket endpoint `/ws/live-metrics` funguje
- Broadcast každých 2s
- Auto-reconnect on disconnect
- Session management

## Implementation
See [S10 Story](../S10.md#t1-websocket-infrastructure)
