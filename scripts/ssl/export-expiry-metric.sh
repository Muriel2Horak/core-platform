#!/usr/bin/env bash
set -Eeuo pipefail

CERT_FILE="${CERT_FILE:-/ssl/acme.json}"
DOMAIN_LABEL="${DOMAIN_LABEL:-core-platform}"

if [[ ! -f "$CERT_FILE" ]]; then
  echo "ssl_certificate_expiry_days{domain=\"${DOMAIN_LABEL}\"} 0"
  exit 0
fi

CERT_B64=$(jq -r '.letsencrypt.Certificates[0].certificate' "$CERT_FILE")
if [[ -z "$CERT_B64" || "$CERT_B64" == "null" ]]; then
  echo "ssl_certificate_expiry_days{domain=\"${DOMAIN_LABEL}\"} 0"
  exit 0
fi

EXPIRY_RAW=$(echo "$CERT_B64" | base64 -d | openssl x509 -noout -enddate | cut -d= -f2)

parse_date() {
  local raw="$1"
  if date -d "$raw" +%s >/dev/null 2>&1; then
    date -d "$raw" +%s
    return
  fi
  if date -j -f "%b %e %H:%M:%S %Y %Z" "$raw" +%s >/dev/null 2>&1; then
    date -j -f "%b %e %H:%M:%S %Y %Z" "$raw" +%s
    return
  fi
  echo "0"
}

EXPIRY_TS=$(parse_date "$EXPIRY_RAW")
NOW_TS=$(date +%s)
DAYS_LEFT=$(( (EXPIRY_TS - NOW_TS) / 86400 ))

echo "ssl_certificate_expiry_days{domain=\"${DOMAIN_LABEL}\"} ${DAYS_LEFT}"
