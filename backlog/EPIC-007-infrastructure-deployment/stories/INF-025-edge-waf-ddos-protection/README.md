---
id: INF-025
epic: EPIC-007-infrastructure-deployment
title: "Edge WAF + DDoS protection (Nginx + ModSecurity + CrowdSec)"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "4 days"
path_mapping:
  code_paths:
    - docker/nginx/nginx-ssl.conf.template
    - docker/nginx/docker-entrypoint.sh
    - docker/docker-compose.yml
    - scripts/docker/up-nginx.sh
  test_paths:
    - scripts/infra-smoke-test.sh
    - scripts/env-validate.sh
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-025-edge-waf-ddos-protection/README.md
---

# INF-025: Edge WAF + DDoS protection (Nginx + ModSecurity + CrowdSec)

**Epic:** EPIC-007 Infrastructure & Deployment  
**Priority:** P0  
**Status:** ✅ **DONE**
**Estimate:** 4 days

## User Story

**Jako:** DevOps/Platform engineer  
**Chci:** mit lokalni edge ochranu (WAF + rate limiting + dynamicke blokovani)  
**Aby:** byly mitigovany L7 utoky bez zavislosti na cloud WAF.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md  
**Další zdroj:** backlog/EPIC-007-infrastructure-deployment/README.md

**Obsah z dokumentace:**
- Nginx je edge proxy a musi mit rate limiting a audit headers.
- Lokální WAF a dynamicke blokovani jsou soucasti security baseline.
- Volumetricky DDoS je resen eskalaci na ISP/upstream.

## Definition of Ready (DoR)

- [ ] Nginx reverse proxy je standardni entrypoint pro vsechny sluzby.
- [ ] WAF stack je potvrzeny (ModSecurity + OWASP CRS).
- [ ] CrowdSec nasazeni je odsouhlasene.
- [ ] Log format z Nginx obsahuje potrebna pole (IP, path, status).
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.

## Acceptance Criteria

- [ ] Nginx bezi s ModSecurity + OWASP CRS pro /api, /auth, /n8n.
- [ ] WAF mod je prepinatelny (detection vs blocking) a zdokumentovany.
- [ ] Rate limiting a connection limiting jsou definovane pro kriticke endpointy.
- [ ] CrowdSec sbira Nginx logy a aplikuje bany pres nginx bouncer.
- [ ] WAF/CrowdSec udalosti jsou logovane a dohledatelne v Loki.
- [ ] Runbook popisuje postup pro false positives a eskalaci volumetrickeho DDoS.

## Definition of Done (DoD)

**Kód:**
- [ ] Docker sluzby a config pro ModSecurity + CRS + CrowdSec existuji.
- [ ] Nginx config obsahuje WAF include a rate limit pravidla.

**Testy:**
- [ ] SQLi/XSS test request je zablokovan nebo flagged.
- [ ] Rate limit vyvola 429 pri prekroceni limitu.
- [ ] CrowdSec ban se aplikuje po brute-force simulaci.

**Dokumentace:**
- [ ] Konfigurace, limity a allowlist jsou popsane.
- [ ] Runbook pro DDoS/WAF incident je hotovy.

**Deployment:**
- [ ] Edge ochrana je aktivni pro prod-like compose.

## Závislosti

- EPIC-000 (Security baseline)
- EPIC-007 (Nginx infrastructure)
- EPIC-003 (Observability, Loki)

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [TASK-025-01: ModSecurity + OWASP CRS integrace](subtasks/TASK-025-01-modsecurity-crs.md) | 10h | none |
| 2 | [TASK-025-02: Rate limiting + connection limiting](subtasks/TASK-025-02-rate-limit-conn-limit.md) | 6h | TASK-025-01 |
| 3 | [TASK-025-03: CrowdSec integrace a bouncer](subtasks/TASK-025-03-crowdsec-bouncer.md) | 8h | TASK-025-01 |
| 4 | [TASK-025-04: Testy, observabilita, runbook](subtasks/TASK-025-04-tests-observability-runbook.md) | 8h | TASK-025-01, TASK-025-02, TASK-025-03 |
