---
id: S-P3
epic: EPIC-012-vault-integration
title: "Hardening + Rotation"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-16
estimate: "8 hours"
path_mapping:
  code_paths:
    - scripts/vault/rotate-backend-db-pass.sh
    - scripts/vault/vault-smoke-runtime.sh
    - Makefile
    - docs/VAULT_RUNBOOK.md
    - docs/SECRETS_INVENTORY.md
  test_paths:
    - tests/vault_rotation_tests.sh
  docs_paths:
    - backlog/EPIC-012-vault-integration/README.md
    - backlog/EPIC-012-vault-integration/stories/VLTS-P3-s-p3-hardening-rotation/README.md
---

# S-P3: Hardening + Rotation

**Goal:** Provide rotation tooling, runtime smoke checks, and operational runbooks.

## ✅ Acceptance Criteria

- `make vault-rotate-backend-db-pass` exists and updates Vault + DB.
- Runtime smoke check verifies KV access.
- Runbook + secrets inventory documented.

## 🧪 Testing

```bash
make test-vault-rotation
```
