# TASK-020-009-02: Risky pattern detection step

## 🎯 Goal
Pridat kontrolu rizikovych patternu do PR pipeline.

## 📋 Tasks
- [x] Vybrat seznam rizikovych patternu (auth, secrets, crypto).
- [x] Implementovat script/step, ktery kontroluje diff.
- [x] Pridat output s varovanim a odkazem na checklist.

## 📤 Output
- CI step nebo script pro risky pattern detection.
- Seznam patternu verziovany v repu.

## ✅ Acceptance Criteria for This Subtask
- [x] Risky pattern detection bezi na PR.
- [x] Nalez generuje warning a odkaz na review.
- [x] Pattern list je udrzovany v repozitari.
