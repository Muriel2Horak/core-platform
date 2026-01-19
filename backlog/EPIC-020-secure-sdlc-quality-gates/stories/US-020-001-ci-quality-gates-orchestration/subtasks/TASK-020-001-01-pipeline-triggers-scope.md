# TASK-020-001-01: Definice pipeline triggeru a scope

## 🎯 Goal
Stanovit kdy a co se spousti v PR, nightly a release pipeline.

## 📋 Tasks
- [ ] Vypsat minimalni set gate kontrol pro PR.
- [ ] Definovat nightly scope (DAST, full E2E, full scany).
- [ ] Definovat release scope a blocking podminky.

## 📤 Output
- Matice gate kontrol (PR/nightly/release).
- Dokument se scope a trigger pravidly.

## ✅ Acceptance Criteria for This Subtask
- [ ] PR scope obsahuje unit testy, SAST, SCA, secret scan a IaC lint.
- [ ] Nightly scope obsahuje DAST a full E2E testy.
- [ ] Release scope je definovan jako blocking pro kriticke gate.
- [ ] Matice je verzovana v repozitari.
