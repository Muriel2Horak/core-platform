# TASK-020-001-02: Workflow implementace a job struktura

## 🎯 Goal
Vytvorit a zapojit workflow soubory pro PR, nightly a release.

## 📋 Tasks
- [ ] Vytvorit workflow soubory pro PR/nightly/release.
- [ ] Rozdelit joby na opakovatelne kroky.
- [ ] Nastavit artefakty a summary vystup.

## 📤 Output
- Workflow soubory v .github/workflows/.
- Reprodukovatelne joby se standardnim vystupem.

## ✅ Acceptance Criteria for This Subtask
- [ ] Workflow pro PR bezi pri otevreni a update PR.
- [ ] Nightly workflow bezi na schedule.
- [ ] Release workflow lze spustit manualne a blokuje release pri failu.
