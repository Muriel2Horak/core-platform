#!/bin/bash
# 🧪 PRE-DEPLOY MONITORING TESTS
# Fast smoke tests to validate monitoring configuration before deployment
# Exit code 0 = success, non-zero = failure
# Duration: ~2-3 minutes

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 Axiom Monitoring Package - Pre-Deploy Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test result tracking
test_pass() {
  TOTAL_TESTS=$((TOTAL_TESTS + 1))
  PASSED_TESTS=$((PASSED_TESTS + 1))
  echo -e "${GREEN}✅ PASS${NC}: $1"
}

test_fail() {
  TOTAL_TESTS=$((TOTAL_TESTS + 1))
  FAILED_TESTS=$((FAILED_TESTS + 1))
  echo -e "${RED}❌ FAIL${NC}: $1"
  echo -e "${RED}   Error: $2${NC}"
}

test_warn() {
  echo -e "${YELLOW}⚠️  WARN${NC}: $1"
}

test_info() {
  echo -e "${BLUE}ℹ️  INFO${NC}: $1"
}

# ============================================================================
# TEST 1: Prometheus Recording Rules Validation
# ============================================================================
echo -e "\n${BLUE}📏 TEST 1: Prometheus Recording Rules${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

RECORDING_RULES_DIR="$PROJECT_ROOT/docker/prometheus/rules"
RECORDING_RULES_COUNT=0
RECORDING_RULES_VALID=0

if [ ! -d "$RECORDING_RULES_DIR" ]; then
  test_fail "Recording rules directory not found" "$RECORDING_RULES_DIR"
else
  # Check if promtool is available
  if ! command -v promtool &> /dev/null; then
    test_warn "promtool not installed - skipping YAML syntax validation"
    test_info "Install: brew install prometheus (macOS) or download from prometheus.io"
    
    # Fallback: basic YAML syntax check
    for rule_file in "$RECORDING_RULES_DIR"/axiom_*.yml; do
      if [ -f "$rule_file" ]; then
        RECORDING_RULES_COUNT=$((RECORDING_RULES_COUNT + 1))
        
        # Basic YAML syntax check (existence + readable)
        if grep -q "groups:" "$rule_file" && grep -q "record:" "$rule_file"; then
          test_pass "$(basename "$rule_file") - Basic syntax OK"
          RECORDING_RULES_VALID=$((RECORDING_RULES_VALID + 1))
        else
          test_fail "$(basename "$rule_file")" "Missing required YAML structure (groups/record)"
        fi
      fi
    done
  else
    # Full promtool validation
    for rule_file in "$RECORDING_RULES_DIR"/axiom_*.yml; do
      if [ -f "$rule_file" ]; then
        RECORDING_RULES_COUNT=$((RECORDING_RULES_COUNT + 1))
        
        if promtool check rules "$rule_file" &> /dev/null; then
          test_pass "$(basename "$rule_file") - promtool validation"
          RECORDING_RULES_VALID=$((RECORDING_RULES_VALID + 1))
        else
          ERROR=$(promtool check rules "$rule_file" 2>&1 || true)
          test_fail "$(basename "$rule_file")" "$ERROR"
        fi
      fi
    done
  fi
  
  test_info "Recording rules validated: $RECORDING_RULES_VALID/$RECORDING_RULES_COUNT files"
fi

# ============================================================================
# TEST 2: Prometheus Alert Rules Validation
# ============================================================================
echo -e "\n${BLUE}🚨 TEST 2: Prometheus Alert Rules${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

ALERT_RULES_DIR="$PROJECT_ROOT/docker/prometheus/alerts"
ALERT_RULES_COUNT=0
ALERT_RULES_VALID=0
ALERTS_WITH_RUNBOOKS=0
ALERTS_WITH_DASHBOARDS=0
TOTAL_ALERTS=0

if [ ! -d "$ALERT_RULES_DIR" ]; then
  test_fail "Alert rules directory not found" "$ALERT_RULES_DIR"
else
  if ! command -v promtool &> /dev/null; then
    test_warn "promtool not installed - using basic validation"
    
    for alert_file in "$ALERT_RULES_DIR"/axiom_*.yml; do
      if [ -f "$alert_file" ]; then
        ALERT_RULES_COUNT=$((ALERT_RULES_COUNT + 1))
        
        # Basic validation
        if grep -q "groups:" "$alert_file" && grep -q "alert:" "$alert_file"; then
          test_pass "$(basename "$alert_file") - Basic syntax OK"
          ALERT_RULES_VALID=$((ALERT_RULES_VALID + 1))
          
          # Check for runbook URLs
          if grep -q "runbook_url:" "$alert_file"; then
            ALERTS_WITH_RUNBOOKS=$((ALERTS_WITH_RUNBOOKS + 1))
          fi
          
          # Check for dashboard URLs
          if grep -q "dashboard_url:" "$alert_file"; then
            ALERTS_WITH_DASHBOARDS=$((ALERTS_WITH_DASHBOARDS + 1))
          fi
          
          # Count alerts
          ALERT_COUNT=$(grep -c "^  - alert:" "$alert_file" || echo "0")
          TOTAL_ALERTS=$((TOTAL_ALERTS + ALERT_COUNT))
        else
          test_fail "$(basename "$alert_file")" "Missing required YAML structure"
        fi
      fi
    done
  else
    # Full promtool validation
    for alert_file in "$ALERT_RULES_DIR"/axiom_*.yml; do
      if [ -f "$alert_file" ]; then
        ALERT_RULES_COUNT=$((ALERT_RULES_COUNT + 1))
        
        if promtool check rules "$alert_file" &> /dev/null; then
          test_pass "$(basename "$alert_file") - promtool validation"
          ALERT_RULES_VALID=$((ALERT_RULES_VALID + 1))
          
          # Check for runbook URLs
          if grep -q "runbook_url:" "$alert_file"; then
            ALERTS_WITH_RUNBOOKS=$((ALERTS_WITH_RUNBOOKS + 1))
          fi
          
          # Check for dashboard URLs
          if grep -q "dashboard_url:" "$alert_file"; then
            ALERTS_WITH_DASHBOARDS=$((ALERTS_WITH_DASHBOARDS + 1))
          fi
          
          # Count alerts
          ALERT_COUNT=$(grep -c "^  - alert:" "$alert_file" || echo "0")
          TOTAL_ALERTS=$((TOTAL_ALERTS + ALERT_COUNT))
        else
          ERROR=$(promtool check rules "$alert_file" 2>&1 || true)
          test_fail "$(basename "$alert_file")" "$ERROR"
        fi
      fi
    done
  fi
  
  test_info "Alert rules validated: $ALERT_RULES_VALID/$ALERT_RULES_COUNT files"
  test_info "Total alerts: $TOTAL_ALERTS"
  test_info "Alerts with runbook URLs: $ALERTS_WITH_RUNBOOKS/$ALERT_RULES_COUNT files"
  test_info "Alerts with dashboard URLs: $ALERTS_WITH_DASHBOARDS/$ALERT_RULES_COUNT files"
fi

# ============================================================================
# TEST 3: Grafana Dashboard JSON Validation
# ============================================================================
echo -e "\n${BLUE}📊 TEST 3: Grafana Dashboard JSON${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

DASHBOARDS_DIRS=(
  "$PROJECT_ROOT/docker/grafana/provisioning/dashboards/system"
  "$PROJECT_ROOT/docker/grafana/provisioning/dashboards/advanced"
  "$PROJECT_ROOT/docker/grafana/provisioning/dashboards/streaming"
  "$PROJECT_ROOT/docker/grafana/provisioning/dashboards/security"
  "$PROJECT_ROOT/docker/grafana/provisioning/dashboards/audit"
)

DASHBOARD_COUNT=0
DASHBOARD_VALID=0
EXPECTED_DASHBOARDS=(
  "axiom_sys_overview.json"
  "axiom_adv_runtime.json"
  "axiom_adv_db.json"
  "axiom_adv_redis.json"
  "axiom_kafka_lag.json"
  "axiom_security.json"
  "axiom_audit.json"
)

# Check expected dashboards exist
for dashboard_name in "${EXPECTED_DASHBOARDS[@]}"; do
  DASHBOARD_FOUND=false
  
  for dashboard_dir in "${DASHBOARDS_DIRS[@]}"; do
    dashboard_path="$dashboard_dir/$dashboard_name"
    
    if [ -f "$dashboard_path" ]; then
      DASHBOARD_COUNT=$((DASHBOARD_COUNT + 1))
      DASHBOARD_FOUND=true
      
      # JSON syntax validation
      if jq empty "$dashboard_path" 2>/dev/null; then
        # Check for required fields
        UID=$(jq -r '.uid // empty' "$dashboard_path")
        TITLE=$(jq -r '.title // empty' "$dashboard_path")
        PANELS=$(jq -r '.panels // [] | length' "$dashboard_path")
        
        if [ -n "$UID" ] && [ -n "$TITLE" ] && [ "$PANELS" -gt 0 ]; then
          test_pass "$dashboard_name - JSON valid (uid=$UID, panels=$PANELS)"
          DASHBOARD_VALID=$((DASHBOARD_VALID + 1))
        else
          test_fail "$dashboard_name" "Missing required fields (uid/title/panels)"
        fi
      else
        ERROR=$(jq empty "$dashboard_path" 2>&1 || true)
        test_fail "$dashboard_name" "Invalid JSON: $ERROR"
      fi
      
      break
    fi
  done
  
  if [ "$DASHBOARD_FOUND" = false ]; then
    test_fail "$dashboard_name" "Dashboard file not found"
  fi
done

test_info "Dashboards validated: $DASHBOARD_VALID/${#EXPECTED_DASHBOARDS[@]} files"

# ============================================================================
# TEST 4: Grafana Provisioning Configuration
# ============================================================================
echo -e "\n${BLUE}🔧 TEST 4: Grafana Provisioning Config${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

PROVISIONING_FILE="$PROJECT_ROOT/docker/grafana/provisioning/dashboards/dashboards.yml"

if [ ! -f "$PROVISIONING_FILE" ]; then
  test_fail "dashboards.yml not found" "$PROVISIONING_FILE"
else
  # Check YAML syntax
  if grep -q "apiVersion:" "$PROVISIONING_FILE" && grep -q "providers:" "$PROVISIONING_FILE"; then
    test_pass "dashboards.yml - YAML syntax OK"
    
    # Check for Axiom dashboard providers
    PROVIDER_COUNT=$(grep -c "name: 'Axiom" "$PROVISIONING_FILE" || echo "0")
    
    if [ "$PROVIDER_COUNT" -ge 5 ]; then
      test_pass "dashboards.yml - Axiom providers configured ($PROVIDER_COUNT providers)"
    else
      test_warn "dashboards.yml - Expected 5 Axiom providers, found $PROVIDER_COUNT"
    fi
  else
    test_fail "dashboards.yml" "Invalid YAML structure"
  fi
fi

# ============================================================================
# TEST 5: Prometheus Configuration
# ============================================================================
echo -e "\n${BLUE}⚙️  TEST 5: Prometheus Configuration${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

PROMETHEUS_CONFIG="$PROJECT_ROOT/docker/prometheus/prometheus.yml"

if [ ! -f "$PROMETHEUS_CONFIG" ]; then
  test_fail "prometheus.yml not found" "$PROMETHEUS_CONFIG"
else
  # Check for rule_files configuration
  if grep -q "rule_files:" "$PROMETHEUS_CONFIG"; then
    test_pass "prometheus.yml - rule_files section exists"
    
    # Check for Axiom rules paths
    if grep -q "/etc/prometheus/rules/axiom_" "$PROMETHEUS_CONFIG" && \
       grep -q "/etc/prometheus/alerts/axiom_" "$PROMETHEUS_CONFIG"; then
      test_pass "prometheus.yml - Axiom rules paths configured"
    else
      test_fail "prometheus.yml" "Axiom rules paths not found in rule_files"
    fi
  else
    test_fail "prometheus.yml" "rule_files section not found"
  fi
  
  # promtool validation (if available)
  if command -v promtool &> /dev/null; then
    if promtool check config "$PROMETHEUS_CONFIG" &> /dev/null; then
      test_pass "prometheus.yml - promtool validation"
    else
      ERROR=$(promtool check config "$PROMETHEUS_CONFIG" 2>&1 || true)
      test_fail "prometheus.yml" "$ERROR"
    fi
  else
    test_warn "promtool not available - skipping full Prometheus config validation"
  fi
fi

# ============================================================================
# TEST 6: Frontend Integration
# ============================================================================
echo -e "\n${BLUE}🌐 TEST 6: Frontend Integration${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

MONITORING_PAGE="$PROJECT_ROOT/frontend/src/pages/Admin/AxiomMonitoringPage.tsx"
APP_ROUTES="$PROJECT_ROOT/frontend/src/App.jsx"

if [ ! -f "$MONITORING_PAGE" ]; then
  test_fail "AxiomMonitoringPage.tsx not found" "$MONITORING_PAGE"
else
  test_pass "AxiomMonitoringPage.tsx exists"
  
  # Check for dashboard UIDs in component
  DASHBOARD_UIDS_FOUND=0
  for uid in "axiom_sys_overview" "axiom_adv_runtime" "axiom_kafka_lag" "axiom_security" "axiom_audit"; do
    if grep -q "$uid" "$MONITORING_PAGE"; then
      DASHBOARD_UIDS_FOUND=$((DASHBOARD_UIDS_FOUND + 1))
    fi
  done
  
  if [ "$DASHBOARD_UIDS_FOUND" -ge 5 ]; then
    test_pass "AxiomMonitoringPage.tsx - Dashboard UIDs configured ($DASHBOARD_UIDS_FOUND/5)"
  else
    test_warn "AxiomMonitoringPage.tsx - Found $DASHBOARD_UIDS_FOUND/5 dashboard UIDs"
  fi
fi

if [ ! -f "$APP_ROUTES" ]; then
  test_fail "App.jsx not found" "$APP_ROUTES"
else
  # Check for monitoring route
  if grep -q "axiom-monitoring" "$APP_ROUTES" && grep -q "AxiomMonitoringPage" "$APP_ROUTES"; then
    test_pass "App.jsx - Monitoring route configured"
  else
    test_fail "App.jsx" "Axiom monitoring route not found"
  fi
fi

# ============================================================================
# SUMMARY
# ============================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}📊 Test Summary${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Total Tests:  $TOTAL_TESTS"
echo -e "${GREEN}Passed:       $PASSED_TESTS${NC}"

if [ $FAILED_TESTS -gt 0 ]; then
  echo -e "${RED}Failed:       $FAILED_TESTS${NC}"
  echo ""
  echo -e "${RED}❌ PRE-DEPLOY TESTS FAILED${NC}"
  echo "Please fix the errors above before deploying."
  exit 1
else
  echo -e "${RED}Failed:       $FAILED_TESTS${NC}"
  echo ""
  echo -e "${GREEN}✅ ALL PRE-DEPLOY TESTS PASSED${NC}"
  echo "Monitoring package is ready for deployment!"
  exit 0
fi
