# TASK-001-03: Pre-commit kontrola (lefthook)

## 🎯 Goal
Zajistit, aby pre-commit kontroloval konzistenci generovanych souboru.

## 📋 Tasks
- [ ] Pridat do `lefthook.yml` command pro `make generate-configs` nebo diff check.
- [ ] Nastavit jasnou hlasku pro developery (jak opravit).
- [ ] Zdokumentovat postup v README.

## 📤 Output
- Aktualizovany `lefthook.yml` s kontrolou template consistency.
- Kratky navod pro developery.

## ✅ Acceptance Criteria for This Subtask
- [ ] Commit failne, pokud generated soubory nesedi s templaty.
- [ ] Commit projde, pokud jsou soubory synchronni.
