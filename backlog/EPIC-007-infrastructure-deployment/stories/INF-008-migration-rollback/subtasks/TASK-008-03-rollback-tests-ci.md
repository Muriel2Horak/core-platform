# TASK-008-03: Rollback tests + CI gate

## Goal
Overit, ze V->U->V cyklus funguje a zablokovat merge pri chybe.

## Tasks
- [ ] Pridat testy, ktere aplikujou V migraci, rollback U a znovu apply.
- [ ] Overit integritu dat po rollbacku.
- [ ] Napojit test na CI pipeline.

## Output
- Automaticke testy pro rollback stabilitu.

## Acceptance Criteria for This Subtask
- [ ] CI failne, pokud rollback neprojde.
- [ ] Testy potvrdi, ze data nejsou poskozena.
- [ ] Log obsahuje informace o verzi a kroku rollbacku.
