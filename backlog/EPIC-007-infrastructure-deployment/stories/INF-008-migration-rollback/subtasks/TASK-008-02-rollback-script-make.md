# TASK-008-02: Rollback script + Make target

## Goal
Pridat jednotny rollback command pro databazove migrace.

## Tasks
- [ ] Implementovat `scripts/db/rollback.sh` s validaci version parametru.
- [ ] Zahrnout pred-rollback backup a update Flyway history.
- [ ] Pridat `make db-rollback VERSION=...` target.

## Output
- Bezpecny rollback proces s backupem.

## Acceptance Criteria for This Subtask
- [ ] Rollback se spusti jen s validnim VERSION parametrem.
- [ ] Pred rollbackem se ulozi backup.
- [ ] Flyway history odpovida cilove verzi.
