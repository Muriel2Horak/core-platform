# TASK-020-005-04: Nginx config lint

## 🎯 Goal
Overit syntaxi Nginx konfiguraci.

## 📋 Tasks
- [ ] Pridat lint krok (nginx -t nebo custom script).
- [ ] Napojit na CI pipeline.
- [ ] Zaznamenat report.

## 📤 Output
- CI job pro Nginx lint.
- Zaznam o validaci konfigurace.

## ✅ Acceptance Criteria for This Subtask
- [ ] Nginx lint failuje pri chybne konfiguraci.
- [ ] Job bezi na PR.
- [ ] Vysledek je soucasti CI summary.
