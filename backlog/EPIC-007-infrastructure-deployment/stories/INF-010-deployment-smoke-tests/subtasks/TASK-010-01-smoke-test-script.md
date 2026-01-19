# TASK-010-01: Smoke test script

## Goal
Vytvorit skript, ktery overi kriticke sluzby po deployi.

## Tasks
- [ ] Implementovat `scripts/deploy/smoke-tests.sh` s 5-7 kontrolami.
- [ ] Pridat timeouty, fail-fast a citelny output.
- [ ] Overit testy pro backend, frontend, auth, DB, message bus.

## Output
- Opakovatelny smoke test po deployi.

## Acceptance Criteria for This Subtask
- [ ] Skript vraci non-zero pri prvnim failu.
- [ ] Vsechny kriticke sluzby jsou pokryte testem.
- [ ] Testy dobehnou do 5 minut.
