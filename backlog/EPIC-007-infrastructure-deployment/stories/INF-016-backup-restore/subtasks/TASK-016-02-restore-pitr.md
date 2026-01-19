# TASK-016-02: Restore + PITR

## Goal
Zajistit obnovu DB a point-in-time recovery.

## Tasks
- [ ] Implementovat `scripts/backup/pg-restore.sh` pro restore.
- [ ] Nakonfigurovat WAL archiving pro PITR.
- [ ] Pridat dry-run a validaci parametru.

## Output
- Funkcni restore postup vcetne PITR.

## Acceptance Criteria for This Subtask
- [ ] Restore z backupu funguje pro core/keycloak/grafana.
- [ ] PITR umozni obnovu na zvoleny timestamp.
- [ ] Skript validuje parametry a failne pri chybe.
