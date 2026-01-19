# TASK-009-02: Migration orchestration scripts

## Goal
Automatizovat validaci a aplikaci migraci napric DB.

## Tasks
- [ ] Implementovat `scripts/db/migrate-all.sh` (validate + migrate).
- [ ] Pridat roll-back/undo postup (navaznost na INF-008).
- [ ] Pridat `make db-migrate-all` target.

## Output
- Orchestrator pro migrace vsech DB.

## Acceptance Criteria for This Subtask
- [ ] `migrate-all` provede validaci i migrace pro core/keycloak/grafana.
- [ ] Failne pri chybe v jakemkoliv kroku.
- [ ] V logu je jasne videt poradi a vysledek DB migraci.
