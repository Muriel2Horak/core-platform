---
id: US-020-004
epic: EPIC-020-secure-sdlc-quality-gates
title: "Secret scanning a Vault-only policy"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths:
    - .github/workflows-disabled/security-scan.yml
    - docker/vault/config.hcl
    - docker/vault/start-vault.sh
    - docker/vault/with-secrets.sh
  test_paths:
    - scripts/env-validate.sh
    - scripts/devcontainer/test-env-check.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-004-secret-scanning-vault-only/README.md
---

# US-020-004: Secret scanning a Vault-only policy

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** ✅ **DONE**
**Estimate:** 3 days

## User Story

**Jako:** Security engineer  
**Chci:** automaticky detekovat plaintext secrety a vynucovat Vault-only policy  
**Aby:** se zadny secret nedostal do gitu.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- Zadne secrety v gitu, .env a klice jsou zakazane.
- Secret scan musi blokovat merge.

## Definition of Ready (DoR)

- [ ] Je zvoleny nastroj secret scanningu (Gitleaks).
- [ ] Definice zakazanych patternu je jasna.
- [ ] Proces vyjimek je odsouhlasen.
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.
- [ ] Napojeni na Vault policy je definovano.

## Acceptance Criteria

- [ ] Secret scan bezi na kazdy PR a blokuje merge pri nalezu.
- [ ] Pre-commit hook je dostupny a dokumentovany.
- [ ] Vault-only policy kontroluje zakazane soubory/patterny.
- [ ] Full repo scan vytvari report a remediation seznam.

## Definition of Done (DoD)

**Kód:**
- [ ] Secret scan workflow je nasazen.
- [ ] Policy check je soucasti CI.

**Testy:**
- [ ] Testovaci secret vyvola fail v CI.
- [ ] Pre-commit hook zachyti secret lokalne.

**Dokumentace:**
- [ ] Popis Vault-only policy a vyjimek je zdokumentovan.

**Deployment:**
- [ ] Secret scan je aktivni v PR pipeline.

## Závislosti

- EPIC-012 (Vault)
- EPIC-020 (CI orchestration)

## Implementační tasky

- [TASK-020-004-01: Nastaveni Gitleaks scan](subtasks/TASK-020-004-01-gitleaks-scan-setup.md)
- [TASK-020-004-02: Pre-commit hook pro secret scan](subtasks/TASK-020-004-02-precommit-hook.md)
- [TASK-020-004-03: Vault-only policy check](subtasks/TASK-020-004-03-vault-only-policy-check.md)
- [TASK-020-004-04: Full repo audit a remediation seznam](subtasks/TASK-020-004-04-full-repo-audit.md)
