#!/usr/bin/env bash
# ====================================================================
# ENV VALIDATION & DOCTOR - Environment Health Check
# ====================================================================
# Purpose: Validate .env configuration and service connectivity
# Usage: 
#   bash scripts/env-validate.sh          # Quick validation
#   bash scripts/env-validate.sh --full   # Full doctor check
# ====================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0
CHECKS=0

# Print functions
info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
success() { echo -e "${GREEN}✅ $1${NC}"; }
warning() { echo -e "${YELLOW}⚠️  $1${NC}"; WARNINGS=$((WARNINGS + 1)); }
error() { echo -e "${RED}❌ $1${NC}"; ERRORS=$((ERRORS + 1)); }

check() {
  CHECKS=$((CHECKS + 1))
  echo -e "${BLUE}[${CHECKS}]${NC} $1"
}

# ====================================================================
# 1. ENV FILE VALIDATION
# ====================================================================
validate_env_file() {
  info "📋 Checking .env file..."
  
  if [[ ! -f .env ]]; then
    error ".env file NOT FOUND!"
    echo "   Run: cp .env.template .env"
    return 1
  fi
  
  success ".env file exists"
  
  # Check critical variables
  local REQUIRED_VARS=(
    "DATABASE_URL"
    "KEYCLOAK_BASE_URL"
    "OIDC_ISSUER_URI"
    "DOMAIN"
    "POSTGRES_PASSWORD"
    "KEYCLOAK_ADMIN_PASSWORD"
  )
  
  source .env 2>/dev/null || { error "Failed to source .env"; return 1; }
  
  for var in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!var:-}" ]]; then
      error "Required variable $var is NOT SET in .env"
    else
      success "$var is set"
    fi
  done
  
  # Check for common mistakes
  if echo "$DATABASE_URL" | grep -q "core_platform"; then
    error "DATABASE_URL contains 'core_platform' (should be 'core')"
    echo "   Fix: sed -i '' 's|core_platform|core|g' .env"
  fi
  
  if [[ -z "${REDIS_PASSWORD:-}" ]]; then
    warning "REDIS_PASSWORD is empty (Redis runs without auth)"
  fi
}

# ====================================================================
# 2. EFFECTIVE VALUES DISPLAY
# ====================================================================
show_effective_values() {
  info "🔍 Effective configuration values:"
  
  source .env 2>/dev/null || return 1
  
  local db_pass_len=0
  [[ -n "${DATABASE_PASSWORD:-}" ]] && db_pass_len=${#DATABASE_PASSWORD}
  
  local kc_pass_len=0
  [[ -n "${KEYCLOAK_ADMIN_PASSWORD:-}" ]] && kc_pass_len=${#KEYCLOAK_ADMIN_PASSWORD}
  
  local kc_secret_len=0
  [[ -n "${KEYCLOAK_ADMIN_CLIENT_SECRET:-}" ]] && kc_secret_len=${#KEYCLOAK_ADMIN_CLIENT_SECRET}
  
  local redis_pass_len=0
  [[ -n "${REDIS_PASSWORD:-}" ]] && redis_pass_len=${#REDIS_PASSWORD}
  
  echo ""
  echo "  📊 DATABASE"
  echo "    URL:      ${DATABASE_URL:-NOT SET}"
  echo "    Username: ${DATABASE_USERNAME:-core}"
  echo "    Password: ${DATABASE_PASSWORD:-EMPTY} ($db_pass_len chars)"
  
  echo ""
  echo "  🔐 KEYCLOAK"
  echo "    Base URL:     ${KEYCLOAK_BASE_URL:-NOT SET}"
  echo "    Issuer:       ${OIDC_ISSUER_URI:-NOT SET}"
  echo "    Admin Pass:   ${KEYCLOAK_ADMIN_PASSWORD:-EMPTY} ($kc_pass_len chars)"
  echo "    Client Secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:-EMPTY} ($kc_secret_len chars)"
  
  echo ""
  echo "  🌐 DOMAIN"
  echo "    Domain:       ${DOMAIN:-NOT SET}"
  echo "    Frontend:     https://${DOMAIN:-NOT-SET}/"
  echo "    Admin:        https://admin.${DOMAIN:-NOT-SET}/"
  
  echo ""
  echo "  📦 REDIS"
  echo "    Host:         ${REDIS_HOST:-core-redis}"
  echo "    Port:         ${REDIS_PORT:-6379}"
  echo "    Password:     ${REDIS_PASSWORD:-EMPTY} ($redis_pass_len chars)"
  
  echo ""
}

# ====================================================================
# 3. SERVICE CONNECTIVITY CHECKS
# ====================================================================
check_service_connectivity() {
  info "🔌 Checking service connectivity..."
  
  # PostgreSQL
  check "PostgreSQL connectivity"
  if docker exec core-db pg_isready -U postgres >/dev/null 2>&1; then
    success "PostgreSQL is ready"
  else
    error "PostgreSQL is NOT reachable (container: core-db)"
    echo "   Run: docker ps | grep core-db"
  fi
  
  # Redis
  check "Redis connectivity"
  if docker exec core-redis redis-cli ping >/dev/null 2>&1; then
    success "Redis is ready"
  else
    error "Redis is NOT reachable (container: core-redis)"
  fi
  
  # Keycloak
  check "Keycloak connectivity"
  if docker exec core-keycloak curl -sf http://localhost:8080/health >/dev/null 2>&1; then
    success "Keycloak is ready"
  else
    warning "Keycloak is NOT reachable or still starting"
  fi
  
  # Backend
  check "Backend connectivity"
  if docker exec core-backend curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    success "Backend is ready"
    
    # Get actual datasource URL from backend
    local BACKEND_DS=$(docker exec core-backend curl -s http://localhost:8080/actuator/env 2>/dev/null | \
      jq -r '.propertySources[] | select(.name | contains("applicationConfig")) | .properties["spring.datasource.url"].value' 2>/dev/null || echo "UNKNOWN")
    
    if [[ "$BACKEND_DS" != "UNKNOWN" ]]; then
      echo "   Effective datasource: $BACKEND_DS"
      
      # Check if it's using env var
      source .env 2>/dev/null
      if [[ "$BACKEND_DS" == "${DATABASE_URL}" ]]; then
        success "Backend uses DATABASE_URL from .env ✅"
      else
        warning "Backend datasource DIFFERS from .env DATABASE_URL"
        echo "   Expected: ${DATABASE_URL}"
        echo "   Actual:   $BACKEND_DS"
      fi
    fi
  else
    error "Backend is NOT reachable (container: core-backend)"
    echo "   Check logs: make logs-backend"
  fi
}

# ====================================================================
# 4. DATABASE CONNECTIVITY DEEP CHECK
# ====================================================================
check_database_deep() {
  info "🗄️  Deep database check..."
  
  source .env 2>/dev/null || return 1
  
  # Check if databases exist
  check "Database 'core' existence"
  if docker exec core-db psql -U postgres -lqt | cut -d \| -f 1 | grep -qw core; then
    success "Database 'core' exists"
  else
    error "Database 'core' does NOT exist!"
    echo "   Run: docker exec core-db psql -U postgres -c 'CREATE DATABASE core;'"
  fi
  
  check "Database 'keycloak' existence"
  if docker exec core-db psql -U postgres -lqt | cut -d \| -f 1 | grep -qw keycloak; then
    success "Database 'keycloak' exists"
  else
    error "Database 'keycloak' does NOT exist!"
  fi
  
  # Check user permissions
  check "User 'core' permissions"
  if docker exec core-db psql -U postgres -c "SELECT 1 FROM pg_user WHERE usename='core'" | grep -q 1; then
    success "User 'core' exists"
  else
    warning "User 'core' does NOT exist"
  fi
}

# ====================================================================
# 5. KEYCLOAK REALM CHECK
# ====================================================================
check_keycloak_realm() {
  info "👤 Checking Keycloak realm configuration..."
  
  source .env 2>/dev/null || return 1
  
  check "Admin realm accessibility"
  local REALM_URL="${KEYCLOAK_BASE_URL}/realms/admin"
  
  if curl -sSf "$REALM_URL" >/dev/null 2>&1; then
    success "Admin realm is accessible: $REALM_URL"
    
    # Check issuer
    local ISSUER=$(curl -sSf "$REALM_URL" 2>/dev/null | jq -r '.issuer' 2>/dev/null || echo "UNKNOWN")
    echo "   Issuer: $ISSUER"
    
    if [[ "$ISSUER" == "${OIDC_ISSUER_URI}" ]]; then
      success "Issuer matches OIDC_ISSUER_URI from .env ✅"
    else
      warning "Issuer DIFFERS from .env OIDC_ISSUER_URI"
      echo "   Expected: ${OIDC_ISSUER_URI}"
      echo "   Actual:   $ISSUER"
    fi
  else
    error "Admin realm NOT accessible: $REALM_URL"
    echo "   Check: make logs-keycloak"
  fi
}

# ====================================================================
# MAIN EXECUTION
# ====================================================================
main() {
  echo ""
  echo "╔═══════════════════════════════════════════════════════════════╗"
  echo "║         ENVIRONMENT VALIDATION & DOCTOR                       ║"
  echo "╚═══════════════════════════════════════════════════════════════╝"
  echo ""
  
  FULL_CHECK="${1:-}"
  
  # Always run basic checks
  validate_env_file
  echo ""
  show_effective_values
  
  # Full check includes connectivity
  if [[ "$FULL_CHECK" == "--full" ]]; then
    echo ""
    check_service_connectivity
    echo ""
    check_database_deep
    echo ""
    check_keycloak_realm
  fi
  
  # Summary
  echo ""
  echo "╔═══════════════════════════════════════════════════════════════╗"
  echo "║                      SUMMARY                                  ║"
  echo "╚═══════════════════════════════════════════════════════════════╝"
  echo ""
  echo "  Total checks: ${CHECKS}"
  
  if [[ $ERRORS -gt 0 ]]; then
    error "Errors: $ERRORS"
  else
    success "No errors found"
  fi
  
  if [[ $WARNINGS -gt 0 ]]; then
    warning "Warnings: $WARNINGS"
  else
    success "No warnings"
  fi
  
  echo ""
  
  if [[ $ERRORS -eq 0 && $WARNINGS -eq 0 ]]; then
    success "✨ Environment is HEALTHY! Ready to build."
    return 0
  elif [[ $ERRORS -eq 0 ]]; then
    warning "⚠️  Environment has warnings but should work"
    return 0
  else
    error "❌ Environment has CRITICAL errors. Fix them before building."
    return 1
  fi
}

main "$@"
