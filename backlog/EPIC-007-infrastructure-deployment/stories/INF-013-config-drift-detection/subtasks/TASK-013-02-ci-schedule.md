# TASK-013-02: CI schedule + PR gate

## Goal
Napojit drift detection na CI a pravidelny schedule.

## Tasks
- [ ] Pridat CI workflow s nightly cronem.
- [ ] Pridat PR gate na zmeny v templatech.
- [ ] Posilat notifikace pri driftu.

## Output
- CI kontrola driftu bezi automaticky.

## Acceptance Criteria for This Subtask
- [ ] Nightly job se spusti a publikuje vysledek.
- [ ] PR s drift detekci neprojde.
- [ ] Notifikace obsahuje link na log a diff.
