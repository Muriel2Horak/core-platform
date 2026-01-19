# TASK-005-03: Monitoring + expiry alerts

## Goal
Pridat monitoring expirace certifikatu a alerty.

## Tasks
- [ ] Exportovat metriku `ssl_certificate_expiry_days` (script/exporter).
- [ ] Pridat Prometheus rule a alert pro expiraci <30 dni.
- [ ] Pridat Grafana panel pro stav certifikatu.

## Output
- Viditelnost expirace certifikatu v monitoringu.

## Acceptance Criteria for This Subtask
- [ ] Prometheus sbira metriku expirace.
- [ ] Alert se spusti pri expiraci pod threshold.
- [ ] Grafana dashboard zobrazuje stav certifikatu.
