# TASK-013-01: Drift detection script

## Goal
Detekovat rozdily mezi template a realnou konfiguraci.

## Tasks
- [ ] Implementovat `scripts/config/detect-drift.sh` pro .env, compose, realm.
- [ ] Normalizovat dynamicka pole (IDs, timestamps).
- [ ] Vystupovat diff a summary.

## Output
- Script, ktery identifikuje drift.

## Acceptance Criteria for This Subtask
- [ ] Script vraci non-zero pri driftu.
- [ ] Diff je citelny a ukazuje konkretni rozdily.
- [ ] Normalizace odstrani nestabilni pole.
