# TASK-020-001-03: Quality gate aggregator a pravidla failu

## 🎯 Goal
Zajistit jednoznacne PASS/FAIL vyhodnoceni napric joby.

## 📋 Tasks
- [ ] Definovat agregacni krok (script/job).
- [ ] Implementovat vyhodnoceni podle konfigu (thresholdy).
- [ ] Zobrazit summary do CI vystupu.

## 📤 Output
- Agregacni job pro gate vysledek.
- Konfigurace thresholdu (napr. JSON/YAML).

## ✅ Acceptance Criteria for This Subtask
- [ ] Agregacni job failne pipeline pri jedinem failu kriticke kontroly.
- [ ] Thresholdy jsou nastavene z konfigu, ne natvrdo.
- [ ] CI summary obsahuje stav kazde gate.
