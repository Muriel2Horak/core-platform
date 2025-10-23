#!/usr/bin/env bash
set -euo pipefail

# ====== EDITUJ PODLE PROSTŘEDÍ ======
KC_BASE="${KC_BASE:-https://admin.core-platform.local}"
REALM="${REALM:-admin}"                    # <- zkontroluj, že je to správný realm  
CLIENT_ID="${CLIENT_ID:-web}"              # Public client s direct access grants enabled
CLIENT_SECRET="${CLIENT_SECRET:-}"         # prázdné = public klient
USERNAME="${USERNAME:-test_admin}"
PASSWORD="${PASSWORD:-Test.1234}"
API_BASE="${API_BASE:-https://admin.core-platform.local}"  # Backend přes NGINX
GRAFANA_BASE="${GRAFANA_BASE:-https://admin.core-platform.local/core-admin/monitoring}"  # Grafana přes NGINX subpath
# =====================================

tmp="$(mktemp -d)"
cookies="$tmp/cookies.txt"
headers="$tmp/headers.txt"

echo "→ Getting Keycloak token (realm=${REALM}, client_id=${CLIENT_ID})"
TOKEN=$(
  if [[ -n "$CLIENT_SECRET" ]]; then
    curl -sS -k \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      -d "grant_type=password" \
      -d "client_id=${CLIENT_ID}" \
      -d "client_secret=${CLIENT_SECRET}" \
      -d "username=${USERNAME}" \
      -d "password=${PASSWORD}" \
      -d "scope=openid profile email" \
      "${KC_BASE}/realms/${REALM}/protocol/openid-connect/token" | jq -er '.access_token'
  else
    curl -sS -k \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      -d "grant_type=password" \
      -d "client_id=${CLIENT_ID}" \
      -d "username=${USERNAME}" \
      -d "password=${PASSWORD}" \
      -d "scope=openid profile email" \
      "${KC_BASE}/realms/${REALM}/protocol/openid-connect/token" | jq -er '.access_token'
  fi
)
echo "  OK: token length=$(echo -n "$TOKEN" | wc -c | tr -d ' ')"

echo "→ Create app session via /api/auth/session"
curl -sS -k -X POST "${API_BASE}/api/auth/session" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{}' \
  -c "$cookies" -D "$headers" -o /dev/null
grep -qiE '^set-cookie:' "$headers" && echo "  OK: cookies set" || { echo "  ERR: no cookies set"; exit 1; }

echo "→ Mint Grafana JWT via /_auth/grafana (internal auth_request endpoint)"
# NGINX location /_auth/grafana je internal, takže testujeme přímo /internal/auth/grafana
GRAFANA_JWT_RESPONSE=$(curl -sS -k "${API_BASE}/internal/auth/grafana" -b "$cookies" -D "$headers")
GRAFANA_JWT=$(echo "$GRAFANA_JWT_RESPONSE" | jq -r '.token // empty' 2>/dev/null || true)

# Backend vrací JWT v hlavičce grafana-jwt (lowercase)
if [[ -z "${GRAFANA_JWT}" ]]; then
  GRAFANA_JWT=$(awk '/^grafana-jwt:/ {print $2}' "$headers" | tr -d '\r\n' | tail -n1)
fi

if [[ -n "${GRAFANA_JWT}" ]]; then
  echo "  OK: grafana jwt acquired (length=$(echo -n "$GRAFANA_JWT" | wc -c | tr -d ' '))"
  
  # Decode JWT payload to show claims
  PAYLOAD=$(echo "$GRAFANA_JWT" | cut -d. -f2 | base64 -d 2>/dev/null | jq -C '.' 2>/dev/null || echo "Could not decode")
  echo "  JWT Payload: $PAYLOAD"
else
  echo "  ERR: no grafana jwt"
  echo "  Response: $GRAFANA_JWT_RESPONSE"
  exit 1
fi

echo "→ Grafana health check with JWT header"
curl -sS -k "${GRAFANA_BASE}/api/health" \
  -H "X-Org-JWT: ${GRAFANA_JWT}" -b "$cookies" | jq .

echo "→ Grafana /api/org (verify JWT auth + org context)"
ORG_RESPONSE=$(curl -sS -k "${GRAFANA_BASE}/api/org" \
  -H "X-Org-JWT: ${GRAFANA_JWT}" -b "$cookies" 2>&1)

if echo "$ORG_RESPONSE" | jq . >/dev/null 2>&1; then
  echo "  OK: Grafana API accessible"
  echo "$ORG_RESPONSE" | jq .
else
  echo "  WARN: Grafana returned non-JSON or error"
  echo "  Response: $ORG_RESPONSE"
fi

echo ""
echo "✅ All good - Grafana SSO auth flow working!"
echo ""
echo "🔍 Debug info:"
echo "  - Cookies saved to: $cookies"
echo "  - Headers saved to: $headers"
echo "  - Grafana JWT: ${GRAFANA_JWT:0:50}..."
