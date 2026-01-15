# TASK-003-04: Migration guide + CI guard

## 🎯 Goal
Popsat migraci a pridat CI kontrolu proti commitovani secretu.

## 📋 Tasks
- [ ] Vytvorit runbook `docs/runbooks/secrets-migration.md`.
- [ ] Popsat postup dev -> staging -> prod + rollback.
- [ ] Pridat CI check, ktery failne pri nalezu realnych secret souboru.

## 📤 Output
- Migration runbook.
- CI guard na secret files.

## ✅ Acceptance Criteria for This Subtask
- [ ] Runbook obsahuje kroky a rollback.
- [ ] CI failne pri pokusu commitnout realny secret.
