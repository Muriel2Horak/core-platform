#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

DOMAIN="${DOMAIN:-core-platform.local}"
VALIDITY_DAYS="${VALIDITY_DAYS:-90}"

CERT_DIR="docker/ssl"
KEY_FILE="$CERT_DIR/key.pem"
CERT_FILE="$CERT_DIR/cert.pem"
CSR_FILE="$CERT_DIR/server.csr"

mkdir -p "$CERT_DIR"

openssl genrsa -out "$KEY_FILE" 4096

openssl req -new \
  -key "$KEY_FILE" \
  -out "$CSR_FILE" \
  -subj "/CN=*.${DOMAIN}/O=Core Platform/C=CZ"

openssl x509 -req \
  -in "$CSR_FILE" \
  -signkey "$KEY_FILE" \
  -out "$CERT_FILE" \
  -days "$VALIDITY_DAYS" \
  -extensions v3_req \
  -extfile <(cat <<EOF
[v3_req]
subjectAltName = DNS:*.${DOMAIN},DNS:${DOMAIN}
EOF
)

rm -f "$CSR_FILE"

openssl x509 -in "$CERT_FILE" -noout -subject -dates
echo "✅ SSL certificate generated ($VALIDITY_DAYS days)"
