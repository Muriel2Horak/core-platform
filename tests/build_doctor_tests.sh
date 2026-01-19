#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

ENV_BACKUP=""
ENV_PRESENT=false
REALM_BACKUP=""

cleanup() {
  if [[ "$ENV_PRESENT" == "true" ]]; then
    if [[ -n "$ENV_BACKUP" && -f "$ENV_BACKUP" ]]; then
      mv "$ENV_BACKUP" .env
    fi
  else
    rm -f .env
  fi
  if [[ -n "$REALM_BACKUP" && -f "$REALM_BACKUP" ]]; then
    mv "$REALM_BACKUP" docker/keycloak/realm-admin.json
  fi
}

trap cleanup EXIT

echo "🧪 Build Doctor tests"

if [[ -f .env ]]; then
  ENV_PRESENT=true
  ENV_BACKUP="$(mktemp)"
  cp .env "$ENV_BACKUP"
fi

cp .env.template .env

echo "▶️  check-env passes with valid .env"
bash scripts/build-doctor/check-env.sh

echo "▶️  check-env fails when .env missing"
mv .env .env.test.tmp
if bash scripts/build-doctor/check-env.sh >/dev/null 2>&1; then
  echo "❌ Expected check-env to fail when .env is missing"
  exit 1
fi
mv .env.test.tmp .env

echo "▶️  check-templates passes when generated files in sync"
bash scripts/build-doctor/check-templates.sh

echo "▶️  check-templates fails when realm-admin.json is modified"
REALM_BACKUP="$(mktemp)"
cp docker/keycloak/realm-admin.json "$REALM_BACKUP"
echo "" >> docker/keycloak/realm-admin.json
if bash scripts/build-doctor/check-templates.sh >/dev/null 2>&1; then
  echo "❌ Expected check-templates to fail with modified realm-admin.json"
  exit 1
fi

echo "✅ Build Doctor tests passed"
