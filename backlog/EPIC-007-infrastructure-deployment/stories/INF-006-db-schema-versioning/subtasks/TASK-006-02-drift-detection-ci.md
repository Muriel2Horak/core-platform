# TASK-006-02: Drift detection + CI check

## Goal
Detekovat rozdily mezi schematem v Gitu a bezicim DB.

## Tasks
- [ ] Implementovat `scripts/db/schema-diff.sh` pro core/keycloak/grafana.
- [ ] Pridat CI workflow pro kontrolu driftu pri PR.
- [ ] Vratit jasny report s diffem a navodem na opravu.

## Output
- Automaticka detekce schema driftu.

## Acceptance Criteria for This Subtask
- [ ] CI failne pri detekci driftu.
- [ ] Diff obsahuje zmenene tabulky/columns/indexy.
- [ ] README/CI log obsahuje navod `make db-schema-update`.
