# TASK-003-01: Secrets struktura + .gitignore

## 🎯 Goal
Zavest jednotnou strukturu pro Docker secrets a zajistit, ze secrety nejsou v gitu.

## 📋 Tasks
- [ ] Vytvorit `secrets/` slozku s `.gitkeep`.
- [ ] Vytvorit `secrets/.template/` s placeholder soubory (bez realnych hodnot).
- [ ] Aktualizovat `.gitignore` tak, aby ignoroval `secrets/*.txt`.
- [ ] Popsat seznam podporovanych secret souboru.

## 📤 Output
- Struktura `secrets/` + `.template/` pripraveno.
- `.gitignore` blokuje realne secrety.

## ✅ Acceptance Criteria for This Subtask
- [ ] `secrets/` je verzovane jen s `.gitkeep` a `.template/`.
- [ ] Zadny realny secret soubor nelze omylem commitnout.
