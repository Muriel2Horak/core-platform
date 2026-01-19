# TASK-001-04: CI template-check workflow

## 🎯 Goal
Pridat CI kontrolu, ktera hlida konzistenci generovanych souboru.

## 📋 Tasks
- [ ] Vytvorit `.github/workflows/template-check.yml`.
- [ ] V CI vygenerovat configy z templates a porovnat s gitem.
- [ ] Pri nesouladu failnout job a vypsat jasny error.

## 📤 Output
- CI workflow pro template-check.
- Log s vysvetlenim chyby a opravnym postupem.

## ✅ Acceptance Criteria for This Subtask
- [ ] PR failne, pokud generated soubory nejsou commitnute.
- [ ] PR projde, pokud jsou soubory synchronni.
