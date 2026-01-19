---
id: S-P4
epic: EPIC-012-vault-integration
title: "CI/CD + DR"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-16
estimate: "10 hours"
path_mapping:
  code_paths:
    - scripts/vault/oidc-setup.sh
    - scripts/vault/snapshot-save.sh
    - scripts/vault/snapshot-restore.sh
    - docker/prometheus/prometheus.yml
    - docker/prometheus/alerts/axiom_vault.yml
    - Makefile
    - docs/VAULT_CI_OIDC.md
    - docs/VAULT_RUNBOOK.md
  test_paths:
    - tests/vault_oidc_tests.sh
    - tests/vault_snapshot_tests.sh
    - tests/vault_monitoring_tests.sh
  docs_paths:
    - backlog/EPIC-012-vault-integration/README.md
    - backlog/EPIC-012-vault-integration/stories/VLTS-P4-s-p4-ci-dr/README.md
---

# S-P4: CI/CD + DR

**Goal:** Enable CI OIDC auth, add snapshot/restore tooling, and configure monitoring alerts.

## ✅ Acceptance Criteria

- GitHub Actions OIDC setup script exists.
- Vault snapshot save/restore scripts exist.
- Prometheus scrapes Vault metrics and alert rules are defined.

## 🧪 Testing

```bash
make test-vault-oidc
make test-vault-snapshot
make test-vault-monitoring
```
