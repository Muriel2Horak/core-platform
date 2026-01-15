# TASK-014-01: Build Doctor core checks

## 🎯 Goal
Zavest jadro Build Doctoru a zakladni pre-flight kontroly.

## 📋 Tasks
- [ ] Vytvorit `scripts/build-doctor/` strukturu a master `pre-build-checks.sh`.
- [ ] Implementovat `check-env.sh`, `check-docker.sh`, `check-ports.sh`, `check-disk.sh`.
- [ ] Napojit `check-env.sh` na existujici `scripts/env-validate.sh`.
- [ ] Vysledky zobrazit v jednotnem summary vystupu.

## 📤 Output
- Build Doctor skripty pro core kontroly.
- Fail-fast chovani s jasnym outputem.

## ✅ Acceptance Criteria for This Subtask
- [ ] Pre-flight check failne pri chybejici .env.
- [ ] Pre-flight check failne pri nebezicim Dockeru.
- [ ] Vsechny kontroly se provedou do 10s.
