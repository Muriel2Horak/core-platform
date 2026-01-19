# TASK-025-04: Testy, observabilita, runbook

## 🎯 Goal
Overit ochranu a pripravit runbook pro incidenty.

## 📋 Tasks
- [ ] Pridat test scenare (SQLi/XSS, brute-force, rate limit).
- [ ] Zajistit logy z WAF/CrowdSec v Loki.
- [ ] Popsat runbook pro false positives a DDoS eskalaci na ISP.

## 📤 Output
- Test scenare a logy v Loki.
- Runbook pro edge ochranu.

## ✅ Acceptance Criteria for This Subtask
- [ ] Testy potvrzuji detekci a blokovani.
- [ ] Runbook obsahuje postup pro volumetricky DDoS.
