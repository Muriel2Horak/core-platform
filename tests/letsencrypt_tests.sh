#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

CERT_SOURCE="docker/ssl/cert.pem"
if [[ ! -f "$CERT_SOURCE" ]]; then
  echo "❌ Missing cert for test: $CERT_SOURCE"
  exit 1
fi

TMP_ACME="$(mktemp)"
cleanup() { rm -f "$TMP_ACME"; }
trap cleanup EXIT

CERT_B64=$(base64 < "$CERT_SOURCE" | tr -d '\n')
cat > "$TMP_ACME" <<EOF
{"letsencrypt":{"Certificates":[{"certificate":"$CERT_B64"}]}}
EOF

echo "🧪 ACME expiry check dry-run"
ACME_FILE="$TMP_ACME" bash scripts/ssl/check-expiry.sh --dry-run

echo "🧪 Metric export"
CERT_FILE="$TMP_ACME" DOMAIN_LABEL="test-domain" bash scripts/ssl/export-expiry-metric.sh | grep -q "ssl_certificate_expiry_days"

echo "✅ Let's Encrypt checks passed"
