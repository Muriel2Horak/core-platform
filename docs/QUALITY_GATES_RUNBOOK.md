# Quality Gates Runbook

## Scope

Quality gates enforce EPIC-020 checks for pull requests, nightly builds, and releases. Each gate records a JSON result and the aggregator blocks the workflow if any required gate fails.

## Gate Matrix

- PR: `unit-tests`, `sast`, `sca`, `secret-scan`, `iac-lint`
- Nightly: PR gates + `dast`, `security-regression`
- Release: PR gates + `container-scan`
- AI guardrails: `ai-guardrails` runs on PR/nightly/release

Source of truth: `scripts/ci/gate-matrix.json`.

## How Gates Work

1. Each gate job writes a JSON result to `gate-results/<gate>.json`.
2. The aggregator downloads all gate results and runs `scripts/ci/aggregate-gates.sh`.
3. The aggregator writes `gate-summary/summary.json` and `gate-summary/summary.md`.
4. Compliance evidence is written to `compliance-evidence/compliance-summary.json`.
5. If any required gate is missing or fails, the aggregator exits non-zero.

## Required Secrets (CI)

- `SONAR_HOST_URL`, `SONAR_TOKEN`
- `SONAR_BACKEND_PROJECT_KEY` (optional, default `core-platform-backend`)
- `SONAR_FRONTEND_PROJECT_KEY` (optional, default `core-platform-frontend`)
- `DAST_TARGET_URL` (nightly DAST target)
- Security regression secrets: `KC_BASE`, `KC_REALM`, `KC_CLIENT_ID`, `TEST_USER1`, `TEST_PASSWORD1`, `TEST_USER2`, `TEST_PASSWORD2`, `TENANT1_KEY`, `TENANT2_KEY`, `BE_BASE`, `BE_API_PATH`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `LOKI_BASE`, `SERVICE_LABEL`, `TIMEOUT`, `RETRY_COUNT`
- Optional Trivy auth: `TRIVY_USERNAME`, `TRIVY_PASSWORD`, `TRIVY_DB_REPOSITORY`

## Gate Flags

- `RUN_DAST=1` enables OWASP ZAP baseline in nightly.
- `RUN_SECURITY_REGRESSION=1` enables multitenancy/RBAC regression in nightly.

## Local Execution (Developer)

Use the same scripts locally to validate the aggregator:

```bash
mkdir -p gate-results
scripts/ci/write-gate-result.sh --name unit-tests --status pass
scripts/ci/write-gate-result.sh --name sast --status pass
scripts/ci/write-gate-result.sh --name sca --status pass
scripts/ci/write-gate-result.sh --name secret-scan --status pass
scripts/ci/write-gate-result.sh --name iac-lint --status pass
scripts/ci/aggregate-gates.sh --event pr --results gate-results --output gate-summary
```

## Failure Handling

- PR: Fix the failing gate and re-run the workflow.
- Nightly: Triage findings the next working day; block release until resolved.
- Release: Do not tag or deploy until the gate summary is green.

## Exceptions

Exceptions require security approval and must be documented in the PR. Add the rationale to the PR description and link to the approved ticket.
