#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="${BUILD_DOCTOR_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
cd "$PROJECT_ROOT"

REALM_DIR="$PROJECT_ROOT/docker/keycloak"
REALM_TEMPLATE="$REALM_DIR/realm-admin.template.json"
REALM_CURRENT="$REALM_DIR/realm-admin.json"

if [[ ! -f "$REALM_TEMPLATE" ]]; then
  echo "❌ Missing Keycloak realm template: $REALM_TEMPLATE"
  exit 1
fi

if [[ ! -f "$REALM_CURRENT" ]]; then
  echo "❌ Missing generated realm file: $REALM_CURRENT"
  echo "💡 Run: bash docker/keycloak/generate-realm.sh"
  exit 1
fi

TEMP_REALM="$(mktemp)"
cleanup() { rm -f "$TEMP_REALM"; }
trap cleanup EXIT

REALM_TEMPLATE="$REALM_TEMPLATE" REALM_OUTPUT="$TEMP_REALM" \
  bash docker/keycloak/generate-realm.sh >/dev/null 2>&1

if diff -u "$REALM_CURRENT" "$TEMP_REALM" >/dev/null; then
  exit 0
fi

echo "❌ Generated realm config is out of sync with template"
echo "💡 Run: bash docker/keycloak/generate-realm.sh"
exit 1
