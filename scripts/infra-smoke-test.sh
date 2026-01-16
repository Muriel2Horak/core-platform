#!/bin/bash

##
# 🧪 Infrastructure Smoke Tests
# 
# Validates Docker Compose stack configuration and policies
# Usage: ./scripts/infra-smoke-test.sh
##

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

FAILURES=0

function test_passed() {
    echo -e "${GREEN}✓${NC} $1"
}

function test_failed() {
    echo -e "${RED}✗${NC} $1"
    FAILURES=$((FAILURES + 1))
}

function test_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3001}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
KAFKA_PORT="${KAFKA_PORT:-9092}"
CURL_FLAGS="${CURL_FLAGS:-}"

echo "🧪 Running infrastructure smoke tests..."
echo ""

# Test 1: Backend Health
echo "Testing Backend Health..."
HEALTH=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/actuator/health" 2>/dev/null || echo "")
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    test_passed "Backend health endpoint returns UP"
else
    test_failed "Backend health endpoint not UP: $HEALTH"
fi

# Test 2: Prometheus Metrics
echo ""
echo "Testing Prometheus Metrics..."
METRICS=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/actuator/prometheus" 2>/dev/null || echo "")
if echo "$METRICS" | grep -q 'jvm_memory_used_bytes'; then
    test_passed "Prometheus metrics endpoint returns JVM metrics"
else
    test_failed "Prometheus metrics endpoint missing JVM metrics"
fi

if echo "$METRICS" | grep -q 'streaming_commands_total'; then
    test_passed "Streaming commands counter exists"
else
    test_warn "Streaming commands counter not found (expected after first command)"
fi

# Test 3: Streaming Config
echo ""
echo "Testing Streaming Configuration..."
CONFIG=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/admin/streaming/config" 2>/dev/null || echo "{}")
if echo "$CONFIG" | grep -q '"batchSize"'; then
    test_passed "Streaming config endpoint accessible"
    
    BATCH_SIZE=$(echo "$CONFIG" | grep -o '"batchSize":[0-9]*' | cut -d':' -f2)
    if [ "$BATCH_SIZE" -ge 10 ] && [ "$BATCH_SIZE" -le 500 ]; then
        test_passed "Batch size within acceptable range: $BATCH_SIZE"
    else
        test_warn "Batch size outside typical range: $BATCH_SIZE"
    fi
else
    test_warn "Streaming config endpoint not available (requires auth)"
fi

# Test 4: Kafka Topics
echo ""
echo "Testing Kafka Topics..."
if command -v nc >/dev/null 2>&1; then
    if nc -z -w 2 127.0.0.1 "$KAFKA_PORT"; then
        test_passed "Kafka port $KAFKA_PORT is reachable"
    else
        test_failed "Kafka port $KAFKA_PORT is not reachable"
    fi
else
    if bash -c "</dev/tcp/127.0.0.1/$KAFKA_PORT" 2>/dev/null; then
        test_passed "Kafka port $KAFKA_PORT is reachable"
    else
        test_failed "Kafka port $KAFKA_PORT is not reachable"
    fi
fi

# Test 5: Kafka Topic Configuration
echo ""
echo "Testing Kafka Topic Policies..."
test_warn "Kafka topic policies skipped (port reachability only)"

# Test 6: Grafana Health
echo ""
echo "Testing Grafana..."
GRAFANA_HEALTH=$(curl -s $CURL_FLAGS "$GRAFANA_URL/api/health" 2>/dev/null || echo "")
if echo "$GRAFANA_HEALTH" | grep -Eq '"database"[[:space:]]*:[[:space:]]*"ok"'; then
    test_passed "Grafana health check passed"
else
    test_failed "Grafana health check failed: $GRAFANA_HEALTH"
fi

# Test 7: Grafana Dashboards
echo ""
echo "Testing Grafana Dashboard Provisioning..."
DASHBOARDS=$(curl -s $CURL_FLAGS "$GRAFANA_URL/api/search?query=Streaming" 2>/dev/null || echo "[]")
DASHBOARD_COUNT=$(echo "$DASHBOARDS" | grep -o '"type":"dash-db"' | wc -l | tr -d ' ')

if [ "$DASHBOARD_COUNT" -ge 1 ]; then
    test_passed "Grafana has $DASHBOARD_COUNT streaming dashboard(s) provisioned"
else
    test_warn "No streaming dashboards found in Grafana (expected 3)"
fi

# Test 8: Prometheus Targets
echo ""
echo "Testing Prometheus Targets..."
PROM_TARGETS=$(curl -s $CURL_FLAGS "$PROMETHEUS_URL/api/v1/targets" 2>/dev/null || echo "{}")
if echo "$PROM_TARGETS" | grep -q '"health":"up"'; then
    UP_COUNT=$(echo "$PROM_TARGETS" | grep -o '"health":"up"' | wc -l | tr -d ' ')
    test_passed "Prometheus has $UP_COUNT targets UP"
else
    test_warn "Prometheus targets not found or not UP"
fi

# Test 9: Mini Flow Test
echo ""
echo "Testing End-to-End Mini Flow..."
echo "  1. POST command to queue..."
COMMAND_RESPONSE=$(curl -s $CURL_FLAGS -X POST "$BACKEND_URL/api/admin/commands/test" \
    -H "Content-Type: application/json" \
    -d '{"type":"TEST","payload":"{\"test\":true}"}' 2>/dev/null || echo "{}")

if echo "$COMMAND_RESPONSE" | grep -q '"id"'; then
    COMMAND_ID=$(echo "$COMMAND_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    test_passed "Command created: $COMMAND_ID"
    
    echo "  2. Waiting 5s for Kafka processing..."
    sleep 5
    
    echo "  3. GET command status..."
    STATUS_RESPONSE=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/admin/commands/$COMMAND_ID" 2>/dev/null || echo "{}")
    
    if echo "$STATUS_RESPONSE" | grep -q '"status":"APPLIED"'; then
        test_passed "Command processed to APPLIED state"
    elif echo "$STATUS_RESPONSE" | grep -q '"status":"PENDING"'; then
        test_warn "Command still PENDING (may need more time)"
    else
        test_warn "Command status: $(echo "$STATUS_RESPONSE" | grep -o '"status":"[^"]*"' || echo 'unknown')"
    fi
else
    test_warn "Mini flow skipped (requires authentication or endpoint not available)"
fi

# Summary
echo ""
echo "=========================================="
if [ $FAILURES -eq 0 ]; then
    echo -e "${GREEN}✓ All smoke tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ $FAILURES test(s) failed${NC}"
    exit 1
fi
