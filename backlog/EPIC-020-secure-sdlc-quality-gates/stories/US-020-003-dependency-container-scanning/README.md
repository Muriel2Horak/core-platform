---
id: US-020-003
epic: EPIC-020-secure-sdlc-quality-gates
title: "SCA a container scanning"
priority: P0
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths:
    - .github/workflows-disabled/security-scans.yml
    - security/dependency-check-suppressions.xml
    - docker/backend/Dockerfile
    - frontend/package-lock.json
  test_paths:
    - scripts/security/check-vulnerabilities.sh
    - scripts/ci/verify-dependency-convergence.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-003-dependency-container-scanning/README.md
---

# US-020-003: SCA a container scanning

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** TODO
**Estimate:** 3 days

## User Story

**Jako:** DevOps engineer  
**Chci:** automaticke skeny zavislosti a Docker image  
**Aby:** zname CVE neprosly do releasu.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- SCA musi failovat pri Critical/High CVE.
- Container scan musi kontrolovat image pred release.

## Definition of Ready (DoR)

- [ ] Seznam build artefaktu je jasny.
- [ ] Nastroje SCA/Trivy jsou dostupne.
- [ ] Thresholdy pro CVE jsou definovany.
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.
- [ ] Je definovan proces allowlistu.

## Acceptance Criteria

- [ ] OWASP Dependency-Check bezi pro Maven i npm.
- [ ] Trivy skenuje container image a failuje pri Critical/High CVE.
- [ ] Allowlist je verzovany a ma datum expirace.
- [ ] Skeny ukladaji report jako artefakt.

## Definition of Done (DoD)

**Kód:**
- [ ] SCA a container scan jsou integrovane do CI.
- [ ] Fail pravidla jsou konfigurovana.

**Testy:**
- [ ] Testovaci CVE vyvola fail v pipeline.

**Dokumentace:**
- [ ] Popis allowlist procesu je zdokumentovan.

**Deployment:**
- [ ] SCA a container scan bezi v PR i release pipeline.

## Závislosti

- EPIC-020 (CI orchestration)

## Implementační tasky

- [TASK-020-003-01: Nastaveni OWASP Dependency-Check](subtasks/TASK-020-003-01-dependency-scan-setup.md)
- [TASK-020-003-02: Trivy scanning pro image](subtasks/TASK-020-003-02-container-scan-setup.md)
- [TASK-020-003-03: Proces allowlistu CVE](subtasks/TASK-020-003-03-allowlist-process.md)
- [TASK-020-003-04: Integrace reportu do summary](subtasks/TASK-020-003-04-report-integration.md)
