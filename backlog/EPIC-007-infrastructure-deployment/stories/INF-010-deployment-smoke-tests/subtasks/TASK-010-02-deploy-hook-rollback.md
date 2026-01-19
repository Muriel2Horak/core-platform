# TASK-010-02: Deploy hook + rollback trigger

## Goal
Napojit smoke test do deploy pipeline a spustit rollback pri selhani.

## Tasks
- [ ] Zabalit deploy + smoke test do `deploy-with-tests.sh`.
- [ ] Pri failu zavolat rollback script.
- [ ] Pridat support pro ENV a BASE_URL.

## Output
- Automaticky rollback pri selhani smoke testu.

## Acceptance Criteria for This Subtask
- [ ] Deploy se zastavi pri selhani testu.
- [ ] Rollback se spusti automaticky.
- [ ] Log obsahuje jasny duvod rollbacku.
