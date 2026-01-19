# TASK-020-006-03: Auth a scope konfigurace

## 🎯 Goal
Zajistit autenticaci a omezeni scope pro DAST.

## 📋 Tasks
- [x] Nastavit autentizaci (pokud je potreba).
- [x] Definovat allowed/denied URL.
- [ ] Otestovat, ze scan nezasahuje mimo scope.

## 📤 Output
- Auth postup pro ZAP.
- Dokumentovany allowed/denied scope.

## ✅ Acceptance Criteria for This Subtask
- [ ] ZAP skenuje pouze povoleny scope.
- [ ] Auth je funkcni pro chranene oblasti.
- [ ] Scan nezasahuje admin/system endpointy mimo scope.
