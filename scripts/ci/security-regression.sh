#!/usr/bin/env bash
set -euo pipefail

if [[ "${RUN_SECURITY_REGRESSION:-0}" != "1" ]]; then
  echo "Security regression tests skipped (RUN_SECURITY_REGRESSION=1 to enable)."
  exit 0
fi

status=0

if [[ -x tests/multitenancy_smoke.sh ]]; then
  tests/multitenancy_smoke.sh || status=1
else
  echo "Missing tests/multitenancy_smoke.sh"
  status=1
fi

if [[ -x tests/test_tenant_api.sh ]]; then
  tests/test_tenant_api.sh || status=1
else
  echo "Missing tests/test_tenant_api.sh"
  status=1
fi

exit "$status"
