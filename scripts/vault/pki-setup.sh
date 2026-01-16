#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR=${VAULT_ADDR:-http://127.0.0.1:8200}
VAULT_CONTAINER=${VAULT_CONTAINER:-core-vault}
VAULT_TOKEN_FILE=${VAULT_TOKEN_FILE:-$HOME/.vault-token}

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

enable_pki() {
  local path=$1
  local ttl=$2
  if ! vault_exec secrets list -format=json | jq -e ".\"${path}/\"" >/dev/null 2>&1; then
    vault_exec secrets enable -path="$path" pki >/dev/null
  fi
  vault_exec secrets tune -max-lease-ttl="$ttl" "$path" >/dev/null
}

echo "Setting up Vault PKI..."

# Root CA (10 years)
enable_pki "pki_root" "87600h"
root_json=$(vault_exec write -format=json pki_root/root/generate/internal \
  common_name="Core Platform Root CA" \
  issuer_name="root-2025" \
  ttl="87600h" \
  key_bits=4096 \
  exclude_cn_from_sans=true)

echo "$root_json" | jq -r '.data.certificate' | \
  docker exec -i "$VAULT_CONTAINER" sh -c 'cat > /vault/data/root-ca.crt'

# Intermediate CA (1 year)
enable_pki "pki_int" "8760h"
int_json=$(vault_exec write -format=json pki_int/intermediate/generate/internal \
  common_name="Core Platform Intermediate CA" \
  issuer_name="intermediate-2025" \
  key_bits=4096 \
  exclude_cn_from_sans=true)

csr=$(echo "$int_json" | jq -r '.data.csr')

signed_json=$(vault_exec write -format=json pki_root/root/sign-intermediate \
  issuer_ref="root-2025" \
  csr="$csr" \
  format=pem_bundle \
  ttl="8760h")

signed_cert=$(echo "$signed_json" | jq -r '.data.certificate')
vault_exec write pki_int/intermediate/set-signed certificate="$signed_cert" >/dev/null

# Role for edge TLS
vault_exec write pki_int/roles/edge-tls \
  issuer_ref="intermediate-2025" \
  allowed_domains="core-platform.local" \
  allow_subdomains=true \
  allow_glob_domains=false \
  allow_wildcard_certificates=true \
  max_ttl="24h" \
  key_bits=2048 \
  key_type=rsa \
  server_flag=true \
  client_flag=false \
  enforce_hostnames=true \
  require_cn=false >/dev/null

echo "✅ Vault PKI setup complete"
