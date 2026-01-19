#!/usr/bin/env bash
set -euo pipefail

if [[ "${RUN_SECURITY_REGRESSION:-0}" != "1" ]]; then
  echo "Security regression tests skipped (RUN_SECURITY_REGRESSION=1 to enable)."
  exit 0
fi

mode="${SECURITY_REGRESSION_MODE:-full}"
status=0

run_test() {
  local label="$1"
  local path="$2"
  if [[ -x "$path" ]]; then
    "$path" || status=1
  else
    echo "Missing ${label} (${path})"
    status=1
  fi
}

case "$mode" in
  smoke)
    run_test "multitenancy smoke" "tests/multitenancy_smoke.sh"
    run_test "rbac smoke" "tests/rbac_smoke.sh"
    ;;
  full)
    run_test "multitenancy smoke" "tests/multitenancy_smoke.sh"
    run_test "rbac smoke" "tests/rbac_smoke.sh"
    run_test "tenant api" "tests/test_tenant_api.sh"
    ;;
  *)
    echo "Unknown SECURITY_REGRESSION_MODE: $mode (expected smoke|full)"
    exit 1
    ;;
esac

exit "$status"
