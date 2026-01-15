# TASK-003-03: Generate/rotate/validate skripty

## 🎯 Goal
Mit skripty pro generovani, rotaci a validaci Docker secrets.

## 📋 Tasks
- [ ] Vytvorit `scripts/secrets/generate-all.sh` (prvni init).
- [ ] Vytvorit `scripts/secrets/rotate-all.sh` a `rotate-single.sh`.
- [ ] Vytvorit `scripts/secrets/validate-secrets.sh`.
- [ ] Zajistit restart relevantnich sluzeb po rotaci.

## 📤 Output
- Skripty pro generate/rotate/validate.
- Jasny output s vysledkem (pass/fail).

## ✅ Acceptance Criteria for This Subtask
- [ ] Generate vytvori vsechny secret soubory.
- [ ] Rotate zmeni hodnotu a restartuje sluzby.
- [ ] Validate vraci chybu pri chybejicich souborech.
