# TASK-012-02: Alertmanager + rules

## Goal
Pridat alerting rules a Alertmanager konfiguraci.

## Tasks
- [ ] Pridat alerting rules (availability, CPU, error rate, SSL expiry).
- [ ] Nastavit Alertmanager service a routes.
- [ ] Otestovat firing alertu na testovaci metriku.

## Output
- Aktivni alerting pravidla a Alertmanager.

## Acceptance Criteria for This Subtask
- [ ] Alertmanager prijima alerty z Promethea.
- [ ] Kriticke alerty jsou oznacene severity=critical.
- [ ] Test alert se objevi v Alertmanager UI.
