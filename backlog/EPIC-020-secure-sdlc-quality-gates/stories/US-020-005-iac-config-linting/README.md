---
id: US-020-005
epic: EPIC-020-secure-sdlc-quality-gates
title: "IaC a config linting"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths:
    - docker/nginx/nginx-ssl.conf.template
    - docker/docker-compose.yml
    - docker/prometheus/prometheus.yml
  test_paths:
    - scripts/env-validate.sh
    - scripts/validate-alerts.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-005-iac-config-linting/README.md
---

# US-020-005: IaC a config linting

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** ✅ **DONE**
**Estimate:** 3 days

## User Story

**Jako:** DevOps engineer  
**Chci:** lintovat Docker/K8s/Nginx/Actions konfiguraci  
**Aby:** se do main nedostaly chybne nebo nebezpecne konfigurace.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- IaC a konfigurace musi prochazet lintem.
- Chyby v konfiguraci blokuji merge.

## Definition of Ready (DoR)

- [ ] Seznam konfig souboru je znamy.
- [ ] Lint nastroje jsou dostupne.
- [ ] Pravidla lintu jsou odsouhlasena.
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.
- [ ] Proces vyjimek je definovan.

## Acceptance Criteria

- [ ] Hadolint bezi pro vsechny Dockerfile a failuje pri erroru.
- [ ] actionlint kontroluje GitHub Actions a failuje pri chybe.
- [ ] K8s lint (kube-linter/kube-score) generuje report.
- [ ] Nginx config lint overuje syntaxi a failuje pri chybe.

## Definition of Done (DoD)

**Kód:**
- [ ] Lint joby jsou zapojene do CI pipeline.
- [ ] Pravidla lintu jsou verzovana.

**Testy:**
- [ ] Testovaci config chyba vyvola fail.

**Dokumentace:**
- [ ] Lint pravidla a vyjimky jsou popsane.

**Deployment:**
- [ ] Lint joby jsou aktivni v PR pipeline.

## Závislosti

- EPIC-020 (CI orchestration)
- EPIC-007 (Infra)

## Implementační tasky

- [TASK-020-005-01: Hadolint pro Dockerfiles](subtasks/TASK-020-005-01-hadolint-dockerfiles.md)
- [TASK-020-005-02: actionlint pro GitHub Actions](subtasks/TASK-020-005-02-actionlint-workflows.md)
- [TASK-020-005-03: K8s linting (kube-linter/kube-score)](subtasks/TASK-020-005-03-k8s-linting.md)
- [TASK-020-005-04: Nginx config lint](subtasks/TASK-020-005-04-nginx-config-lint.md)
