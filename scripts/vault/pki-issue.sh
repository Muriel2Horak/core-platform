#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
VAULT_CONTAINER=${VAULT_CONTAINER:-core-vault}
VAULT_TOKEN_FILE=${VAULT_TOKEN_FILE:-$HOME/.vault-token}
VAULT_ROLE=${VAULT_ROLE:-edge-tls}
VAULT_COMMON_NAME=${VAULT_COMMON_NAME:-admin.core-platform.local}
VAULT_ALT_NAMES=${VAULT_ALT_NAMES:-*.core-platform.local,core-platform.local}
VAULT_TTL=${VAULT_TTL:-24h}

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

vault_exec() {
  docker exec -e VAULT_ADDR="$VAULT_ADDR" -e VAULT_TOKEN="$VAULT_TOKEN" "$VAULT_CONTAINER" vault "$@"
}

issue_json=$(vault_exec write -format=json "pki_int/issue/${VAULT_ROLE}" \
  common_name="$VAULT_COMMON_NAME" \
  alt_names="$VAULT_ALT_NAMES" \
  ttl="$VAULT_TTL")

cert=$(echo "$issue_json" | jq -r '.data.certificate')
key=$(echo "$issue_json" | jq -r '.data.private_key')
ca=$(echo "$issue_json" | jq -r '.data.issuing_ca')

write_secret() {
  local name=$1
  local content=$2
  printf "%s\n" "$content" | \
    docker run --rm -i -v vault_secrets:/vault/secrets alpine sh -c "cat > /vault/secrets/${name}"
}

write_secret "cert.pem" "$cert"
write_secret "key.pem" "$key"
write_secret "ca.pem" "$ca"

docker run --rm -v vault_secrets:/vault/secrets alpine sh -c "chmod 640 /vault/secrets/cert.pem /vault/secrets/key.pem /vault/secrets/ca.pem"

if docker ps --format '{{.Names}}' | grep -q '^core-nginx$'; then
  docker exec core-nginx nginx -s reload
fi

echo "✅ Vault PKI certificate issued and stored in vault_secrets"
