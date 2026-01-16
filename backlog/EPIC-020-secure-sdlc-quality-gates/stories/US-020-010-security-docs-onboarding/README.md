---
id: US-020-010
epic: EPIC-020-secure-sdlc-quality-gates
title: "Security pipeline documentation and onboarding"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths:
    - docs/SECURITY_RUNBOOK.md
    - docs/AUTOMATED_TESTING.md
    - docs/POST_DEPLOYMENT_VERIFICATION.md
    - docs/TROUBLESHOOTING.md
  test_paths:
    - scripts/monitoring-doctor.sh
    - scripts/validate-dashboard-structure.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-010-security-docs-onboarding/README.md
---


# US-020-010: Security pipeline documentation and onboarding

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** ✅ **DONE**
**Estimate:** 2 days

## User Story

**Jako:** Team lead nebo security owner  
**Chci:** mit jednotnou dokumentaci pro security pipeline a onboarding  
**Aby:** novy clovek rychle pochopil quality gates a procesy.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md
**Další zdroj:** backlog/EPIC-000-security-platform-hardening/README.md

**Obsah z dokumentace:**
- Security pipeline musi byt zdokumentovana (PR/nightly/release).
- Onboarding checklist a runbooky musi byt aktualni.
- FAQ a Loki/Grafana navody musi byt k dispozici.

## Definition of Ready (DoR)

- [ ] Seznam toolu a workflow je finalni.
- [ ] Existuje vlastnik dokumentace (owner).
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.

## Acceptance Criteria

- [ ] SECURITY_PIPELINE.md popisuje vsechny tools a workflows.
- [ ] Onboarding checklist je zdokumentovan a aktualni.
- [ ] Runbooky pro incident response jsou dostupne.
- [ ] Grafana dashboard pro security metrics je popsany.
- [ ] Loki audit queries jsou zdokumentovane.
- [ ] FAQ odpovida na casto kladene dotazy.

## Definition of Done (DoD)

**Kód:**
- [ ] Dokumentacni soubory jsou v repozitari.
- [ ] Odkazy v README jsou aktualni.

**Testy:**
- [ ] Dokumentace je validovana v backlog validatoru.

**Dokumentace:**
- [ ] Onboarding checklist a FAQ jsou soucasti docs.
- [ ] Runbooky a troubleshooting sekce jsou doplnene.

**Deployment:**
- [ ] Dokumentace je publikovana pro tym.

## Závislosti

- EPIC-020 (Quality gates scope)
- EPIC-003 (Observability, Loki/Grafana)

## Implementační tasky

- [TASK-020-010-01: Security pipeline guide](subtasks/TASK-020-010-01-security-pipeline-guide.md)
- [TASK-020-010-02: Onboarding checklist + FAQ](subtasks/TASK-020-010-02-onboarding-faq.md)
- [TASK-020-010-03: Runbooky a troubleshooting](subtasks/TASK-020-010-03-runbooks-troubleshooting.md)
- [TASK-020-010-04: Loki/Grafana queries a dashboard docs](subtasks/TASK-020-010-04-loki-grafana-docs.md)
