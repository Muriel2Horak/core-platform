---
id: S-P2
epic: EPIC-012-vault-integration
title: "Secrets Migration (KV v2 + policy + AppRole)"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-16
estimate: "12 hours"
path_mapping:
  code_paths:
    - scripts/vault/bootstrap-vault.sh
    - scripts/vault/list-secrets.sh
    - docker/vault-agent/templates/backend.env.ctmpl
    - docker/vault-agent/templates/keycloak.env.ctmpl
    - docker/vault-agent/templates/grafana-admin-password.ctmpl
    - docker/vault-agent/templates/grafana-db-password.ctmpl
    - docker/vault-agent/templates/grafana-oidc-secret.ctmpl
    - docker/vault-agent/templates/grafana-jwt-secret.ctmpl
    - docker/vault-agent/templates/redis-password.ctmpl
    - docker/vault-agent/templates/minio.env.ctmpl
    - docker/vault-agent/templates/cube.env.ctmpl
    - docker/vault-agent/templates/postgres-password.ctmpl
    - docker/vault-agent/templates/keycloak-db-password.ctmpl
    - docker/vault-agent/templates/pgadmin.env.ctmpl
    - docker/vault-agent/templates/postgres-exporter.env.ctmpl
    - Makefile
  test_paths:
    - tests/vault_secrets_migration_tests.sh
  docs_paths:
    - backlog/EPIC-012-vault-integration/README.md
    - backlog/EPIC-012-vault-integration/stories/VLTS-P2-s-p2-secrets-migration/README.md
---

# S-P2: Secrets Migration (KV v2 + policy + AppRole)

**Goal:** Migrate secrets to Vault KV v2 under `kv/core/*` and update templates/policy.

## ✅ Acceptance Criteria

- Secrets are seeded under `kv/core/*` (no legacy `secret/` path).
- Vault Agent templates read from `kv/data/core/*`.
- Make targets exist for push/list.

## 🧪 Testing

```bash
make test-vault-migration
```
