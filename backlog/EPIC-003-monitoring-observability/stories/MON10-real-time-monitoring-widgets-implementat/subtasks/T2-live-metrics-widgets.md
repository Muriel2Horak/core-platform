# T2: Live Metrics Widgets
**Effort:** ~4h | **LOC:** ~400

## Goal
Implementovat React widgety pro live metriky.

## Files
- `frontend/src/components/monitoring/LiveMetricWidget.tsx`
- `frontend/src/components/monitoring/SystemHealthWidget.tsx`
- `frontend/src/hooks/useLiveMetrics.ts`

## Tasks
- [ ] Pridat widgety pro users, RPS, error rate, latency.
- [ ] Dopsat gauges pro DB connections a Kafka lag.
- [ ] Pridat sparklines a last-updated timestamp.
- [ ] Napojit na WS stream s fallback pollingem.

## Output
- Sada live widgetu s real-time update.

## Acceptance Criteria
- Active Users widget
- Requests/sec widget
- Error Rate widget
- Response Time widget
- DB Connections gauge
- Kafka Lag gauge
- Real-time updates (2s)
- Sparklines zobrazeny
