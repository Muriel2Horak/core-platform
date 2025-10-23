#!/usr/bin/env bash
#
# 🩺 Monitoring Doctor - Grafana SSO Diagnostic Tool
#
# This script runs 4 critical tests to verify Grafana iFrame + SSO setup:
# 1. JWKS endpoint (BFF public key)
# 2. Auth bridge (_auth/grafana with cookie)
# 3. iFrame endpoint (Grafana monitoring path)
# 4. Org sanity (Grafana API user/orgs)
#
# Usage:
#   ./scripts/monitoring-doctor.sh [--domain DOMAIN] [--user USER] [--password PASSWORD]
#
# Environment variables (or use flags):
#   DOMAIN - domain to test (default: admin.core-platform.local)
#   TEST_USER - username for login test
#   TEST_PASSWORD - password for login test
#

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOMAIN="${DOMAIN:-admin.core-platform.local}"
TEST_USER="${TEST_USER:-test_admin}"
TEST_PASSWORD="${TEST_PASSWORD:-Test.1234}"
CLIENT_ID="web"  # Keycloak client for API access
REALM="admin"

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --domain)
      DOMAIN="$2"
      shift 2
      ;;
    --user)
      TEST_USER="$2"
      shift 2
      ;;
    --password)
      TEST_PASSWORD="$2"
      shift 2
      ;;
    --help)
      echo "Usage: $0 [--domain DOMAIN] [--user USER] [--password PASSWORD]"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

print_header() {
  echo -e "\n${BLUE}════════════════════════════════════════${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}════════════════════════════════════════${NC}\n"
}

print_test() {
  echo -e "${YELLOW}[TEST $1/$2]${NC} $3"
}

print_pass() {
  echo -e "${GREEN}✅ PASS:${NC} $1"
  ((PASSED++))
}

print_fail() {
  echo -e "${RED}❌ FAIL:${NC} $1"
  ((FAILED++))
}

print_warn() {
  echo -e "${YELLOW}⚠️  WARN:${NC} $1"
  ((WARNINGS++))
}

print_info() {
  echo -e "${BLUE}ℹ️  INFO:${NC} $1"
}

print_fix() {
  echo -e "${BLUE}🔧 FIX:${NC} $1"
}

# ==================== TEST 1: JWKS ENDPOINT ====================
test_jwks() {
  print_test 1 4 "JWKS endpoint (BFF public key)"
  
  local url="https://${DOMAIN}/.well-known/jwks.json"
  local response
  
  if ! response=$(curl -k -s "$url" 2>&1); then
    print_fail "Cannot connect to JWKS endpoint"
    print_fix "Check if backend is running: docker ps | grep backend"
    print_fix "Check nginx config: docker exec core-nginx cat /etc/nginx/nginx.conf | grep jwks"
    return 1
  fi
  
  # Check if response contains "keys" array
  if ! echo "$response" | jq -e '.keys' > /dev/null 2>&1; then
    print_fail "JWKS endpoint returned invalid JSON: $response"
    print_fix "Check backend logs: docker logs core-backend | grep -i jwks"
    return 1
  fi
  
  # Extract kid (key ID)
  local kid
  if ! kid=$(echo "$response" | jq -r '.keys[0].kid' 2>&1); then
    print_fail "JWKS response missing 'kid' field"
    print_fix "Check JwksKeyProvider.java implementation"
    return 1
  fi
  
  if [[ "$kid" == "null" || -z "$kid" ]]; then
    print_fail "JWKS kid is null or empty"
    return 1
  fi
  
  print_pass "JWKS endpoint accessible with kid='$kid'"
  print_info "URL: $url"
  return 0
}

# ==================== TEST 2: AUTH BRIDGE ====================
test_auth_bridge() {
  print_test 2 4 "Auth bridge (/_auth/grafana endpoint)"
  
  print_info "Logging in to get cookies..."
  
  # Step 1: Get access token from Keycloak
  local token_response
  if ! token_response=$(curl -k -s -X POST \
    "https://${DOMAIN}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=${CLIENT_ID}" \
    -d "username=${TEST_USER}" \
    -d "password=${TEST_PASSWORD}" 2>&1); then
    print_fail "Cannot connect to Keycloak"
    print_fix "Check Keycloak: curl -k https://${DOMAIN}/realms/${REALM}"
    return 1
  fi
  
  # Check if token response contains access_token
  if ! echo "$token_response" | jq -e '.access_token' > /dev/null 2>&1; then
    print_fail "Login failed: $(echo "$token_response" | jq -r '.error_description // .error // "Unknown error"')"
    print_fix "Check username/password: $TEST_USER / ****"
    print_fix "Check Keycloak client '${CLIENT_ID}' exists in realm '$REALM'"
    return 1
  fi
  
  local access_token refresh_token
  access_token=$(echo "$token_response" | jq -r '.access_token')
  refresh_token=$(echo "$token_response" | jq -r '.refresh_token')
  
  print_info "✅ Login successful, got tokens"
  
  # Step 2: Call backend /internal/auth/grafana with cookies
  # NOTE: Can't test _auth/grafana directly (it's internal), so test via /core-admin/monitoring/
  local monitoring_response headers
  if ! headers=$(curl -k -s -I \
    "https://${DOMAIN}/core-admin/monitoring/" \
    -H "Cookie: at=${access_token}; rt=${refresh_token}" 2>&1); then
    print_fail "Cannot connect to monitoring endpoint"
    print_fix "Check nginx: docker ps | grep nginx"
    return 1
  fi
  
  # Check HTTP status
  local status
  status=$(echo "$headers" | head -1 | awk '{print $2}')
  
  if [[ "$status" == "200" ]]; then
    print_pass "Auth bridge working - got HTTP 200"
    print_info "Cookies accepted, JWT minted and forwarded to Grafana"
    return 0
  elif [[ "$status" == "302" || "$status" == "303" || "$status" == "307" ]]; then
    # Check if redirect is to /login
    local location
    location=$(echo "$headers" | grep -i "^location:" | awk '{print $2}' | tr -d '\r\n')
    if [[ "$location" == *"/login"* ]]; then
      print_fail "Redirected to login page - JWT auth not working"
      print_fix "Check Grafana [auth.jwt] config: docker exec core-grafana cat /usr/share/grafana/conf/defaults.ini | grep -A20 auth.jwt"
      print_fix "Check Grafana logs: docker logs core-grafana | grep -i jwt"
      return 1
    else
      print_warn "Unexpected redirect to: $location"
      return 1
    fi
  elif [[ "$status" == "401" ]]; then
    print_fail "Auth bridge returned 401 - cookie not passed or JWT decode failed"
    print_fix "Check nginx auth_request config: docker exec core-nginx cat /etc/nginx/nginx.conf | grep -A5 auth_request"
    print_fix "Check backend /internal/auth/grafana logs: docker logs core-backend | grep -i 'grafana auth'"
    return 1
  else
    print_warn "Unexpected status: $status"
    echo "$headers"
    return 1
  fi
}

# ==================== TEST 3: IFRAME ENDPOINT ====================
test_iframe_endpoint() {
  print_test 3 4 "iFrame endpoint (Grafana health check)"
  
  # Get cookies first
  local token_response access_token refresh_token
  token_response=$(curl -k -s -X POST \
    "https://${DOMAIN}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=${CLIENT_ID}" \
    -d "username=${TEST_USER}" \
    -d "password=${TEST_PASSWORD}")
  
  if ! echo "$token_response" | jq -e '.access_token' > /dev/null 2>&1; then
    print_fail "Cannot get auth tokens (prerequisite failed)"
    return 1
  fi
  
  access_token=$(echo "$token_response" | jq -r '.access_token')
  refresh_token=$(echo "$token_response" | jq -r '.refresh_token')
  
  # Test Grafana API health
  local health_response
  if ! health_response=$(curl -k -s \
    "https://${DOMAIN}/core-admin/monitoring/api/health" \
    -H "Cookie: at=${access_token}; rt=${refresh_token}" 2>&1); then
    print_fail "Cannot connect to Grafana API"
    return 1
  fi
  
  # Check if response contains JSON with status
  if echo "$health_response" | jq -e '.database' > /dev/null 2>&1; then
    print_pass "Grafana API accessible via iFrame endpoint"
    print_info "Health status: $(echo "$health_response" | jq -r '.database // "unknown"')"
    return 0
  elif [[ "$health_response" == *"401"* || "$health_response" == *"Unauthorized"* ]]; then
    print_fail "Grafana returned 401 - JWT validation failed"
    print_fix "Check Grafana jwk_set_url: cat docker/grafana/grafana.ini | grep jwk_set_url"
    print_fix "Verify it points to BFF: https://${DOMAIN}/.well-known/jwks.json"
    print_fix "Check Grafana logs for JWT errors: docker logs core-grafana | grep -i 'jwt\|auth'"
    return 1
  elif [[ "$health_response" == *"login"* ]]; then
    print_fail "Redirected to login - auth.jwt not enabled or configured correctly"
    print_fix "Check [auth.jwt] enabled=true in grafana.ini"
    return 1
  else
    print_warn "Unexpected response from Grafana API"
    echo "$health_response" | head -5
    return 1
  fi
}

# ==================== TEST 4: ORG SANITY ====================
test_org_sanity() {
  print_test 4 4 "Org sanity (datasources & membership)"
  
  # Get cookies
  local token_response access_token refresh_token
  token_response=$(curl -k -s -X POST \
    "https://${DOMAIN}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=${CLIENT_ID}" \
    -d "username=${TEST_USER}" \
    -d "password=${TEST_PASSWORD}")
  
  if ! echo "$token_response" | jq -e '.access_token' > /dev/null 2>&1; then
    print_fail "Cannot get auth tokens"
    return 1
  fi
  
  access_token=$(echo "$token_response" | jq -r '.access_token')
  refresh_token=$(echo "$token_response" | jq -r '.refresh_token')
  
  # Get user orgs
  local orgs_response
  if ! orgs_response=$(curl -k -s \
    "https://${DOMAIN}/core-admin/monitoring/api/user/orgs" \
    -H "Cookie: at=${access_token}; rt=${refresh_token}" 2>&1); then
    print_fail "Cannot get user orgs from Grafana"
    return 1
  fi
  
  # Check if response is JSON array
  if ! echo "$orgs_response" | jq -e '. | length' > /dev/null 2>&1; then
    print_fail "Invalid response from /api/user/orgs"
    echo "$orgs_response" | head -5
    return 1
  fi
  
  local org_count active_org
  org_count=$(echo "$orgs_response" | jq '. | length')
  
  if [[ "$org_count" -eq 0 ]]; then
    print_fail "User is not member of any organization"
    print_fix "Check backend provisioning: docker logs core-backend | grep -i 'ensureOrgMembership'"
    return 1
  fi
  
  # Find active org (orgId == 2 for admin tenant)
  active_org=$(echo "$orgs_response" | jq -r '.[] | select(.orgId == 2) | .name // empty')
  
  if [[ -z "$active_org" ]]; then
    print_warn "User not in org 2 (admin tenant)"
    print_info "Available orgs: $(echo "$orgs_response" | jq -r '.[].name' | tr '\n' ', ')"
    ((WARNINGS++))
  else
    print_pass "User is member of org 2: '$active_org'"
  fi
  
  # Check datasources in org 2
  local ds_response
  if ! ds_response=$(curl -k -s \
    "https://${DOMAIN}/core-admin/monitoring/api/datasources" \
    -H "Cookie: at=${access_token}; rt=${refresh_token}" \
    -H "X-Grafana-Org-Id: 2" 2>&1); then
    print_warn "Cannot check datasources"
    return 0 # Not critical
  fi
  
  if ! echo "$ds_response" | jq -e '. | length' > /dev/null 2>&1; then
    print_warn "Cannot parse datasources response"
    return 0
  fi
  
  local ds_count
  ds_count=$(echo "$ds_response" | jq '. | length')
  
  if [[ "$ds_count" -eq 0 ]]; then
    print_warn "No datasources found in org 2"
    print_fix "Check provisioning: docker logs core-grafana | grep -i datasource"
  else
    print_pass "Found $ds_count datasource(s) in org 2"
    print_info "Datasources: $(echo "$ds_response" | jq -r '.[].name' | tr '\n' ', ')"
  fi
  
  return 0
}

# ==================== MAIN ====================
main() {
  print_header "🩺 Monitoring Doctor - Grafana SSO Diagnostics"
  
  echo "Testing domain: $DOMAIN"
  echo "Test user: $TEST_USER"
  echo ""
  
  # Run tests (don't exit on failure, collect results)
  test_jwks || true
  test_auth_bridge || true
  test_iframe_endpoint || true
  test_org_sanity || true
  
  # Summary
  print_header "📊 Summary"
  
  local total=$((PASSED + FAILED))
  
  if [[ $FAILED -eq 0 ]]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED${NC} ($PASSED/$total)"
    
    if [[ $WARNINGS -gt 0 ]]; then
      echo -e "${YELLOW}⚠️  $WARNINGS warning(s)${NC} - review above for details"
    fi
    
    echo -e "\n${GREEN}🎉 Grafana SSO is working correctly!${NC}"
    exit 0
  else
    echo -e "${RED}❌ TESTS FAILED${NC} ($FAILED/$total failed, $PASSED passed)"
    
    if [[ $WARNINGS -gt 0 ]]; then
      echo -e "${YELLOW}⚠️  $WARNINGS warning(s)${NC}"
    fi
    
    echo -e "\n${RED}🔧 Review failed tests and apply suggested fixes above${NC}"
    exit 1
  fi
}

main "$@"
