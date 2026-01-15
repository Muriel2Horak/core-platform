# TASK-001-02: Validace env promennych pro templates

## 🎯 Goal
Overit, ze vsechny ${VAR} pouzite v templatech maji hodnotu v .env.

## 📋 Tasks
- [ ] Zparsovat promennne z template souboru (realm, nginx, compose).
- [ ] Porovnat s .env a vypsat chybejici hodnoty.
- [ ] Definovat allowlist pro optional promennne.
- [ ] Aktualizovat dokumentaci validace v README.

## 📤 Output
- Rozsirena validace v `scripts/env-validate.sh`.
- Prehled chybejicich promennych v error vystupu.

## ✅ Acceptance Criteria for This Subtask
- [ ] Chybejici promenna vraci jasny error se seznamem.
- [ ] Validni .env projde bez false positives.
