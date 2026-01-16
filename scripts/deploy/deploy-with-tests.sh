#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

ENVIRONMENT="${ENVIRONMENT:-${ENV:-development}}"
DEPLOY_CMD="${DEPLOY_CMD:-make up}"
ROLLBACK_CMD="${ROLLBACK_CMD:-}"
SMOKE_TEST_CMD="${SMOKE_TEST_CMD:-bash scripts/deploy/smoke-tests.sh}"

WEBHOOK_URL="${SMOKE_TEST_WEBHOOK_URL:-${SLACK_WEBHOOK_URL:-}}"

notify() {
  local message="$1"
  if [[ -n "$WEBHOOK_URL" ]]; then
    curl -sS -X POST "$WEBHOOK_URL" \
      -H "Content-Type: application/json" \
      -d "{\"text\":\"$message\"}" >/dev/null || true
  fi
}

echo "🚀 Deploying environment: $ENVIRONMENT"
echo "▶️  Running: $DEPLOY_CMD"
eval "$DEPLOY_CMD"

echo "⏳ Waiting for services to stabilize..."
sleep 15

echo "🧪 Running smoke tests..."
if eval "$SMOKE_TEST_CMD"; then
  echo "✅ Deployment validated"
  notify "✅ Deployment to ${ENVIRONMENT} succeeded"
  exit 0
fi

echo "❌ Smoke tests failed"
notify "❌ Deployment to ${ENVIRONMENT} failed. Smoke tests did not pass."

if [[ -n "$ROLLBACK_CMD" ]]; then
  echo "↩️  Running rollback: $ROLLBACK_CMD"
  eval "$ROLLBACK_CMD" || true
else
  echo "⚠️  No rollback command configured (set ROLLBACK_CMD)"
fi

exit 1
