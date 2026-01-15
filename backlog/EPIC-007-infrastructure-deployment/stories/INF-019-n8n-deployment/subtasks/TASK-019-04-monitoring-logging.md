# TASK-019-04: Monitoring + logging integration

## Goal
Zapojit n8n do monitoringu a logovani (Prometheus, Grafana, Loki).

## Tasks
- [ ] Zapnout n8n metrics a vystavit metrics port v compose.
- [ ] Pridat Prometheus scrape config pro n8n.
- [ ] Pridat Grafana dashboard + alert rules pro n8n (failures, latency).
- [ ] Zapojit n8n logy do Loki a oznacit service/tenant labely.
- [ ] Overit signal v Grafane (metrics) a Loki (logs).

## Output
- N8N metrics a logy v observability stacku.
- Alerty pro workflow failures.

## Acceptance Criteria for This Subtask
- [ ] Prometheus sbira n8n metriky.
- [ ] Grafana dashboard zobrazuje zakladni n8n metriky.
- [ ] Loki obsahuje n8n logy s tenant labely.
- [ ] Alert rule se spusti pri simulovanem failure.
