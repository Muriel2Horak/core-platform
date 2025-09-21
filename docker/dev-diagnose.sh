#!/usr/bin/env bash
set -euo pipefail

# 🔍 Development Diagnose Script
# Ověřuje Keycloak issuer, token získání a API endpointy

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DOCKER_DIR="${REPO_ROOT}/docker"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }

DOMAIN=${DOMAIN:-core-platform.local}
KEYCLOAK_REALM="core-platform"
KEYCLOAK_CLIENT_ID="web"
TEST_USERNAME="test"
TEST_PASSWORD="Test.1234"

echo -e "${CYAN}"
echo "🔍 =================================="
echo "   Core Platform - Diagnose Script"
echo "   Domain: $DOMAIN"
echo "==================================="
echo -e "${NC}"

# a) Echo proměnných
print_info "Konfigurace:"
echo "  DOMAIN: $DOMAIN"
echo "  Expected issuer: https://$DOMAIN/realms/$KEYCLOAK_REALM"
echo "  Keycloak client: $KEYCLOAK_CLIENT_ID"
echo "  Test user: $TEST_USERNAME"
echo ""

cd "$DOCKER_DIR"

# b) Spuštění Keycloaku
print_info "Spouštím Keycloak..."
docker compose up -d --build keycloak

# c) Čekání na health Keycloaku
print_info "Čekám na Keycloak health check..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -sf "http://localhost:8081/realms/$KEYCLOAK_REALM/.well-known/openid-configuration" >/dev/null 2>&1; then
        print_success "Keycloak je dostupný"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_error "Keycloak neodpovídá po $max_attempts pokusech"
        exit 1
    fi
    
    echo -n "."
    sleep 2
    ((attempt++))
done

echo ""

# d) Ověření issueru z well-known
print_info "Kontroluji issuer z well-known endpoint..."
if command -v jq >/dev/null 2>&1; then
    ACTUAL_ISSUER=$(curl -sf "http://localhost:8081/realms/$KEYCLOAK_REALM/.well-known/openid-configuration" | jq -r '.issuer')
    EXPECTED_ISSUER="https://$DOMAIN/realms/$KEYCLOAK_REALM"
    
    echo "  Očekávaný issuer: $EXPECTED_ISSUER"
    echo "  Skutečný issuer:  $ACTUAL_ISSUER"
    
    if [ "$ACTUAL_ISSUER" = "$EXPECTED_ISSUER" ]; then
        print_success "Issuer je správně nastaven"
    else
        print_error "Issuer NESOUHLASÍ!"
        echo ""
        print_info "Well-known response:"
        curl -sf "http://localhost:8081/realms/$KEYCLOAK_REALM/.well-known/openid-configuration" | jq .
        exit 1
    fi
else
    print_warning "jq není nainstalováno, přeskakuji JSON parsing"
    curl -sf "http://localhost:8081/realms/$KEYCLOAK_REALM/.well-known/openid-configuration"
fi

echo ""

# e) Spuštění backend a nginx
print_info "Spouštím backend a nginx..."
docker compose up -d --build backend nginx

print_info "Čekám na backend a nginx..."
sleep 15

# f) Získání tokenu z Keycloaku
print_info "Získávám token pro test uživatele..."

TOKEN_RESPONSE=$(curl -sf -X POST \
  "https://$DOMAIN/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$KEYCLOAK_CLIENT_ID" \
  -d "username=$TEST_USERNAME" \
  -d "password=$TEST_PASSWORD" \
  -k 2>/dev/null || echo "")

if [ -z "$TOKEN_RESPONSE" ]; then
    print_error "Nepodařilo se získat token od Keycloaku"
    print_info "Zkouším diagnostiku Keycloak endpointu..."
    curl -k -i "https://$DOMAIN/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" || true
    exit 1
fi

if command -v jq >/dev/null 2>&1; then
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')
    if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
        print_error "Token response neobsahuje access_token"
        echo "Response: $TOKEN_RESPONSE"
        exit 1
    fi
    
    # Ověření audience v tokenu
    TOKEN_PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null || echo "{}")
    if echo "$TOKEN_PAYLOAD" | jq -e '.aud' >/dev/null 2>&1; then
        AUDIENCE=$(echo "$TOKEN_PAYLOAD" | jq -r '.aud')
        echo "  Token audience: $AUDIENCE"
        if echo "$AUDIENCE" | grep -q "api"; then
            print_success "Token obsahuje požadovanou audience 'api'"
        else
            print_warning "Token neobsahuje audience 'api'"
        fi
    fi
    
    print_success "Token úspěšně získán"
else
    print_warning "jq není nainstalováno, přeskakuji token parsing"
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4 || echo "")
    if [ -z "$ACCESS_TOKEN" ]; then
        print_error "Nepodařilo se extrahovat access_token"
        exit 1
    fi
    print_success "Token úspěšně získán (bez audience verifikace)"
fi

echo ""

# g) curl testy s tokenem
print_info "Testuji API endpointy s tokenem..."

# Test GET /api/roles
print_info "Test 1: GET /api/roles"
ROLES_RESPONSE=$(curl -k -s -i -w "\nHTTP_CODE:%{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "https://$DOMAIN/api/roles" 2>/dev/null || echo "HTTP_CODE:000")

ROLES_HTTP_CODE=$(echo "$ROLES_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
echo "  HTTP kód: $ROLES_HTTP_CODE"

if [ "$ROLES_HTTP_CODE" = "200" ]; then
    print_success "GET /api/roles vrátilo 200 OK"
elif [ "$ROLES_HTTP_CODE" = "403" ]; then
    print_warning "GET /api/roles vrátilo 403 Forbidden (test user nemá role)"
elif [ "$ROLES_HTTP_CODE" = "401" ]; then
    print_error "GET /api/roles vrátilo 401 Unauthorized (problém s tokenem)"
    echo "Response headers:"
    echo "$ROLES_RESPONSE" | head -20
else
    print_error "GET /api/roles vrátilo neočekávaný kód: $ROLES_HTTP_CODE"
    echo "Response:"
    echo "$ROLES_RESPONSE"
fi

echo ""

# Test POST /api/frontend-logs
print_info "Test 2: POST /api/frontend-logs"
LOGS_RESPONSE=$(curl -k -s -i -w "\nHTTP_CODE:%{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{"timestamp":"2024-01-01T12:00:00Z","level":"INFO","operation":"DIAGNOSE_TEST","message":"Test log from diagnose script","details":{"category":"test"}}' \
  "https://$DOMAIN/api/frontend-logs" 2>/dev/null || echo "HTTP_CODE:000")

LOGS_HTTP_CODE=$(echo "$LOGS_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
echo "  HTTP kód: $LOGS_HTTP_CODE"

if [ "$LOGS_HTTP_CODE" = "200" ] || [ "$LOGS_HTTP_CODE" = "201" ]; then
    print_success "POST /api/frontend-logs vrátilo $LOGS_HTTP_CODE (úspěch)"
elif [ "$LOGS_HTTP_CODE" = "401" ]; then
    print_error "POST /api/frontend-logs vrátilo 401 Unauthorized"
    echo "Response headers:"
    echo "$LOGS_RESPONSE" | head -20
else
    print_error "POST /api/frontend-logs vrátilo neočekávaný kód: $LOGS_HTTP_CODE"
    echo "Response:"
    echo "$LOGS_RESPONSE"
fi

echo ""

# h) Shrnutí
print_info "Shrnutí výsledků:"

TOTAL_TESTS=3
PASSED_TESTS=0

# Issuer test
if [ "${ACTUAL_ISSUER:-}" = "https://$DOMAIN/realms/$KEYCLOAK_REALM" ]; then
    echo "  ✅ Issuer test: PASS"
    ((PASSED_TESTS++))
else
    echo "  ❌ Issuer test: FAIL"
fi

# Roles endpoint test
if [ "$ROLES_HTTP_CODE" = "200" ] || [ "$ROLES_HTTP_CODE" = "403" ]; then
    echo "  ✅ GET /api/roles: PASS ($ROLES_HTTP_CODE)"
    ((PASSED_TESTS++))
else
    echo "  ❌ GET /api/roles: FAIL ($ROLES_HTTP_CODE)"
fi

# Logs endpoint test
if [ "$LOGS_HTTP_CODE" = "200" ] || [ "$LOGS_HTTP_CODE" = "201" ]; then
    echo "  ✅ POST /api/frontend-logs: PASS ($LOGS_HTTP_CODE)"
    ((PASSED_TESTS++))
else
    echo "  ❌ POST /api/frontend-logs: FAIL ($LOGS_HTTP_CODE)"
fi

echo ""
echo "Výsledek: $PASSED_TESTS/$TOTAL_TESTS testů prošlo"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    print_success "🎉 Všechny testy prošly!"
    exit 0
else
    print_error "🚨 Některé testy selhaly!"
    exit 1
fi