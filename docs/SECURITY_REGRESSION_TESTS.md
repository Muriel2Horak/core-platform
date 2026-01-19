# Security Regression Tests (Tenant Isolation + RBAC)

## Scope

These tests validate the EPIC-000 security baseline for tenant isolation and RBAC. The CI pipeline runs smoke checks on PRs and a fuller regression set nightly.

## Test Matrix

| Scenario | Expected | Smoke | Full | Test |
| --- | --- | --- | --- | --- |
| Cross-tenant data access | Blocked (403/404) | ✅ | ✅ | `tests/multitenancy_smoke.sh` |
| Admin endpoint access (admin role) | Allowed (2xx) | ✅ | ✅ | `tests/rbac_smoke.sh` |
| Admin endpoint access (non-admin role) | Blocked (401/403) | ✅ | ✅ | `tests/rbac_smoke.sh` |
| Tenant CRUD via admin API | Allowed for admin | ❌ | ✅ | `tests/test_tenant_api.sh` |

## Smoke vs Full

- **Smoke (PR):** `tests/multitenancy_smoke.sh`, `tests/rbac_smoke.sh`
- **Full (nightly):** Smoke set + `tests/test_tenant_api.sh`

## Local Run

Prepare `tests/.env` with credentials and endpoints used by the scripts. Minimal keys:

```bash
KC_BASE=https://admin.core-platform.local
KC_REALM=admin
KC_CLIENT_ID=web
KC_CLIENT_SECRET=
TEST_USER1=test
TEST_PASSWORD1=Test.1234
TEST_USER2=test_admin
TEST_PASSWORD2=Test.1234
BE_BASE=https://admin.core-platform.local
BE_API_PATH=/api
```

Run smoke or full:

```bash
RUN_SECURITY_REGRESSION=1 SECURITY_REGRESSION_MODE=smoke scripts/ci/security-regression.sh
RUN_SECURITY_REGRESSION=1 SECURITY_REGRESSION_MODE=full scripts/ci/security-regression.sh
```

## Notes

- The scripts assume Keycloak and backend are reachable at the configured URLs.
- Smoke tests are safe for PRs; full tests perform admin tenant CRUD.
