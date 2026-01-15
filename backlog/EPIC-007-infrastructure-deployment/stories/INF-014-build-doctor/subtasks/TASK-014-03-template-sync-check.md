# TASK-014-03: Template sync check

## 🎯 Goal
Zavest kontrolu, ze generovane configy jsou synchronni s templaty.

## 📋 Tasks
- [ ] Implementovat `check-templates.sh` (diff nebo generate + diff).
- [ ] Vypisovat jasnou chybu a navod na opravu.
- [ ] Zajistit kompatibilitu s INF-001 (generate-configs).

## 📤 Output
- Check, ktery failne pri rozjezdu template a generated souboru.

## ✅ Acceptance Criteria for This Subtask
- [ ] Check failne, pokud se lisi generated soubory.
- [ ] Check projde, pokud jsou soubory v sync.
