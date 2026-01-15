# TASK-014-02: Makefile integration + skip flag

## 🎯 Goal
Spustit Build Doctor automaticky pred build/up/deploy.

## 📋 Tasks
- [ ] Upravit Makefile tak, aby volal Build Doctor pred `build`, `up`, `clean`, `deploy`.
- [ ] Pridat `SKIP_DOCTOR=true` bypass (pro CI).
- [ ] Pridat warning do logu pri preskoceni.

## 📤 Output
- Makefile targety s automatickym checkem.
- Možnost skipnout Build Doctor v CI.

## ✅ Acceptance Criteria for This Subtask
- [ ] Bez `SKIP_DOCTOR` se build neprovede pri failu.
- [ ] Se `SKIP_DOCTOR=true` se build provede a vypise varovani.
