#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
VAULT_CONTAINER=${VAULT_CONTAINER:-core-vault}
VAULT_TOKEN_FILE=${VAULT_TOKEN_FILE:-$HOME/.vault-token}
OIDC_ISSUER=${OIDC_ISSUER:-https://token.actions.githubusercontent.com}
OIDC_AUDIENCE=${OIDC_AUDIENCE:-vault}
GITHUB_ORG=${GITHUB_ORG:-}
GITHUB_REPO=${GITHUB_REPO:-}
OIDC_ROLE=${OIDC_ROLE:-github-actions}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}

require_cmd docker
require_cmd jq

if [[ -z "${VAULT_TOKEN:-}" ]] && [[ -f "$VAULT_TOKEN_FILE" ]]; then
  VAULT_TOKEN="$(cat "$VAULT_TOKEN_FILE")"
fi

if [[ -z "${VAULT_TOKEN:-}" ]]; then
  echo "VAULT_TOKEN is required (set VAULT_TOKEN or VAULT_TOKEN_FILE)" >&2
  exit 1
fi

if [[ -z "$GITHUB_ORG" || -z "$GITHUB_REPO" ]]; then
  echo "GITHUB_ORG and GITHUB_REPO are required" >&2
  exit 1
fi

vault_exec() {
  docker exec -e VAULT_ADDR="$VAULT_ADDR" -e VAULT_TOKEN="$VAULT_TOKEN" "$VAULT_CONTAINER" vault "$@"
}

# Enable JWT auth for GitHub Actions OIDC
vault_exec auth enable jwt >/dev/null 2>&1 || true

vault_exec write auth/jwt/config \
  oidc_discovery_url="$OIDC_ISSUER" \
  bound_issuer="$OIDC_ISSUER" >/dev/null

vault_exec write auth/jwt/role/$OIDC_ROLE \
  role_type="jwt" \
  bound_audiences="$OIDC_AUDIENCE" \
  user_claim="sub" \
  bound_subject="repo:${GITHUB_ORG}/${GITHUB_REPO}:*" \
  policies="core-platform-agent" \
  ttl="15m" >/dev/null

echo "✅ Vault OIDC role configured for ${GITHUB_ORG}/${GITHUB_REPO}"
