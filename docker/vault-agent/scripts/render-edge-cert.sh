#!/usr/bin/env sh
set -e

BUNDLE_PATH=${1:-/vault/secrets/edge.bundle}
CERT_PATH=${2:-/vault/secrets/cert.pem}
KEY_PATH=${3:-/vault/secrets/key.pem}
CA_PATH=${4:-/vault/secrets/ca.pem}

if [ ! -f "$BUNDLE_PATH" ]; then
  echo "Bundle not found: $BUNDLE_PATH" >&2
  exit 1
fi

awk 'BEGIN{p=0} /BEGIN PRIVATE KEY/{p=1} {if(p) print} /END PRIVATE KEY/{exit}' \
  "$BUNDLE_PATH" > "$KEY_PATH"

awk 'BEGIN{p=0} /BEGIN CERTIFICATE/{p=1} {if(p) print} /END CERTIFICATE/{if(p) exit}' \
  "$BUNDLE_PATH" > "$CERT_PATH"

awk 'BEGIN{p=0;count=0} /BEGIN CERTIFICATE/{count++; if(count>=2) p=1} {if(p) print}' \
  "$BUNDLE_PATH" > "$CA_PATH" || true

chmod 640 "$CERT_PATH" "$KEY_PATH" "$CA_PATH" 2>/dev/null || true
