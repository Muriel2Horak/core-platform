# TASK-016-03: Backup verification + alerts

## Goal
Overit zalohy a alertovat pri selhani.

## Tasks
- [ ] Pridat pravidelny restore test do separata DB.
- [ ] Pridat integritu check (pg_restore --list).
- [ ] Posilat alerty pri failu backup/restore.

## Output
- Overene zalohy s alertingem.

## Acceptance Criteria for This Subtask
- [ ] Verifikace probehne minimalne 1x tydne.
- [ ] Pri failu se spusti alert.
- [ ] Existuje log s vysledkem verifikace.
