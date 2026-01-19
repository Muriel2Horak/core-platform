# TASK-012-01: Prometheus + exporters

## Goal
Doplnit Prometheus a exportery pro backend/DB/Kafka.

## Tasks
- [ ] Pridat Prometheus service do compose.
- [ ] Pridat postgres-exporter a kafka-exporter.
- [ ] Nakonfigurovat scrape targets a retention.

## Output
- Metriky z core sluzeb dostupne v Prometheu.

## Acceptance Criteria for This Subtask
- [ ] Prometheus sbira metriky z backendu, DB a Kafka.
- [ ] Retention je nastavena na 30d.
- [ ] Scrape targety jsou visible jako UP.
