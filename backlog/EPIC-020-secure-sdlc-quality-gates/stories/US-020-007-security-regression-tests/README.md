---
id: US-020-007
epic: EPIC-020-secure-sdlc-quality-gates
title: "Security regression testy (tenant isolation + RBAC)"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-18
estimate: "4 days"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/tenant/TenantFilter.java
    - backend/src/main/java/cz/muriel/core/tenant/TenantResolver.java
    - backend/src/main/java/cz/muriel/core/auth/config/SecurityConfig.java
    - scripts/ci/security-regression.sh
    - .github/workflows/quality-gates-nightly.yml
  test_paths:
    - backend/src/test/java/cz/muriel/core/tenant/TenantFilterIntegrationTest.java
    - backend/src/test/java/cz/muriel/core/tenant/TenantResolverTest.java
    - tests/multitenancy_smoke.sh
    - tests/rbac_smoke.sh
    - tests/test_tenant_api.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-007-security-regression-tests/README.md
    - docs/SECURITY_REGRESSION_TESTS.md
---

# US-020-007: Security regression testy (tenant isolation + RBAC)

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** ✅ **DONE**
**Estimate:** 4 days

## User Story

**Jako:** QA engineer  
**Chci:** automatizovane bezpecnostni regresni testy pro tenant izolaci a RBAC  
**Aby:** zadna zmena neporusila bezpecnostni boundary.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- Tenant = realm = subdomena je zavazny model.
- RBAC musi blokovat admin endpointy pro bezne role.

## Definition of Ready (DoR)

- [ ] Testovaci data pro vice tenantu jsou dostupna.
- [ ] E2E framework (EPIC-002) je pripraven.
- [ ] Scope testu je odsouhlasen.
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.
- [ ] Je definovan smoke vs full test set.

## Acceptance Criteria

- [x] Cross-tenant pristup k datum je blokovan (testy musi failnout).
- [x] Admin-only endpointy jsou nedostupne pro neadmin role.
- [x] Smoke subset bezi na PR, full set nightly.
- [x] Test reporty jsou soucasti CI summary.

## Definition of Done (DoD)

**Kód:**
- [x] E2E a API testy jsou implementovane.
- [x] Testy jsou tagovane pro smoke/full.

**Testy:**
- [x] Testy pokryvaji tenant isolation a RBAC scenare.

**Dokumentace:**
- [x] Test matrix a scenare jsou popsane.

**Deployment:**
- [x] Smoke testy bezi na PR, full testy nightly.

## Závislosti

- EPIC-002 (E2E framework)
- EPIC-010 (RBAC/tenancy model)

## Implementační tasky

- [TASK-020-007-01: Test matrix a scenare](subtasks/TASK-020-007-01-test-matrix-scenarios.md)
- [TASK-020-007-02: E2E testy pro tenant isolation](subtasks/TASK-020-007-02-e2e-tenant-isolation.md)
- [TASK-020-007-03: API testy pro RBAC](subtasks/TASK-020-007-03-rbac-api-tests.md)
- [TASK-020-007-04: Integrace do CI a dokumentace](subtasks/TASK-020-007-04-ci-integration-docs.md)
