# Quality Gates Runbook

## Scope

Quality gates enforce EPIC-020 checks for pull requests, nightly builds, and releases. Each gate records a JSON result and the aggregator blocks the workflow if any required gate fails.

## Gate Matrix

- PR: `unit-tests`, `sast`, `sca`, `secret-scan`, `iac-lint`
- Nightly: PR gates + `dast`, `security-regression`
- Release: PR gates + `container-scan`

Source of truth: `scripts/ci/gate-matrix.json`.

## How Gates Work

1. Each gate job writes a JSON result to `gate-results/<gate>.json`.
2. The aggregator downloads all gate results and runs `scripts/ci/aggregate-gates.sh`.
3. The aggregator writes `gate-summary/summary.json` and `gate-summary/summary.md`.
4. If any required gate is missing or fails, the aggregator exits non-zero.

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
