# TASK-015-02: Staging deploy + smoke tests

## Goal
Automatizovat deploy na staging a navazne smoke testy.

## Tasks
- [ ] Pridat deploy job pro staging environment.
- [ ] Spustit smoke testy po deployi.
- [ ] Pri failu spustit rollback.

## Output
- Staging deploy s automatickou validaci.

## Acceptance Criteria for This Subtask
- [ ] Smoke testy jsou povinne po kazdem staging deployi.
- [ ] Selhani testu automaticky spousti rollback.
- [ ] Logy deploye jsou dostupne v CI.
