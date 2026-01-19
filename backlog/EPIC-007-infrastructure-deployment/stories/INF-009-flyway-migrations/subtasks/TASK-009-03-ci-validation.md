# TASK-009-03: CI validation + pre-deploy checks

## Goal
Vynutit validaci migraci v CI a pred deployem.

## Tasks
- [ ] Pridat CI job pro `flyway:validate` pro vsechny DB.
- [ ] Napojit validaci na deploy pipeline jako gate.
- [ ] Pridat report s chybami migraci.

## Output
- CI gate pro migrace.

## Acceptance Criteria for This Subtask
- [ ] PR neprojde, pokud flyway validate failne.
- [ ] Deploy se zastavi pri validacni chybe.
- [ ] Report obsahuje informaci o DB a souboru s chybou.
