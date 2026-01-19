# TASK-020-004-03: Vault-only policy check

## 🎯 Goal
Zabránit commitum souboru s plaintext secrety.

## 📋 Tasks
- [ ] Definovat seznam zakazanych souboru (napr. .env).
- [ ] Vytvorit CI check pro policy.
- [ ] Dokumentovat vyjimky.

## 📤 Output
- Policy check v CI.
- Seznam zakazanych souboru a vyjimek.

## ✅ Acceptance Criteria for This Subtask
- [ ] CI failne pri nalezu zakazaneho souboru.
- [ ] Vyjimky jsou verzovane a schvalene.
- [ ] Policy je popsana v dokumentaci.
