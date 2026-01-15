# TASK-025-03: CrowdSec integrace a bouncer

## 🎯 Goal
Nasadit CrowdSec a napojit ho na Nginx pro dynamicke bany.

## 📋 Tasks
- [ ] Pridat CrowdSec service do docker compose.
- [ ] Nakonfigurovat parsovani Nginx logu.
- [ ] Pridat nginx bouncer a integraci rozhodnuti.
- [ ] Nastavit manualni allowlist a ban TTL.

## 📤 Output
- CrowdSec service + nginx bouncer config.
- Evidencni logy o banu a allowlistu.

## ✅ Acceptance Criteria for This Subtask
- [ ] CrowdSec detekuje brute-force z logu.
- [ ] Ban se projevi na edge proxy.
