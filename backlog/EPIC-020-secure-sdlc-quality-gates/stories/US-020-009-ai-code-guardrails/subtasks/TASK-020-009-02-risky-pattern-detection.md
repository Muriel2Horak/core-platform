# TASK-020-009-02: Risky pattern detection step

## 🎯 Goal
Pridat kontrolu rizikovych patternu do PR pipeline.

## 📋 Tasks
- [ ] Vybrat seznam rizikovych patternu (auth, secrets, crypto).
- [ ] Implementovat script/step, ktery kontroluje diff.
- [ ] Pridat output s varovanim a odkazem na checklist.

## 📤 Output
- CI step nebo script pro risky pattern detection.
- Seznam patternu verziovany v repu.

## ✅ Acceptance Criteria for This Subtask
- [ ] Risky pattern detection bezi na PR.
- [ ] Nalez generuje warning a odkaz na review.
- [ ] Pattern list je udrzovany v repozitari.
