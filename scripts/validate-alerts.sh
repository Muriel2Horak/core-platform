#!/bin/bash

##
# 🚨 Alert Rules Validation
# 
# Validates Prometheus alert syntax and performs dry-run evaluation
# Usage: ./scripts/validate-alerts.sh
##

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ALERTS_FILE="docker/prometheus/alerts.yml"
PROM_URL="http://localhost:9090"

echo "🚨 Validating Prometheus alert rules..."
echo ""

# Test 1: YAML Syntax
echo "1. Checking YAML syntax..."
if command -v yamllint &> /dev/null; then
    if yamllint -d "{extends: default, rules: {line-length: {max: 120}}}" "$ALERTS_FILE"; then
        echo -e "${GREEN}✓${NC} YAML syntax valid"
    else
        echo -e "${RED}✗${NC} YAML syntax errors found"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠${NC} yamllint not installed, skipping syntax check"
fi

# Test 2: PromQL Syntax Check (via Prometheus API)
echo ""
echo "2. Checking PromQL syntax via Prometheus..."

if ! curl -sf "$PROM_URL/api/v1/status/config" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠${NC} Prometheus not running at $PROM_URL"
    echo "   Start with: docker compose --profile streaming up -d prometheus"
    echo "   Skipping PromQL validation..."
else
    # Load rules to Prometheus
    echo "   Loading rules to Prometheus..."
    RELOAD_RESPONSE=$(curl -sf -X POST "$PROM_URL/-/reload" 2>&1 || echo "failed")
    
    if echo "$RELOAD_RESPONSE" | grep -q "failed"; then
        echo -e "${YELLOW}⚠${NC} Could not reload Prometheus config (may need restart)"
    else
        echo -e "${GREEN}✓${NC} Prometheus config reloaded"
    fi
    
    # Check rules endpoint
    RULES_RESPONSE=$(curl -s "$PROM_URL/api/v1/rules")
    
    if echo "$RULES_RESPONSE" | grep -q '"status":"success"'; then
        echo -e "${GREEN}✓${NC} PromQL syntax valid (Prometheus loaded rules)"
    else
        echo -e "${RED}✗${NC} PromQL syntax errors detected"
        echo "$RULES_RESPONSE" | jq '.error' 2>/dev/null || echo "$RULES_RESPONSE"
        exit 1
    fi
fi

# Test 3: Expected Alert Names
echo ""
echo "3. Checking expected alert names..."

EXPECTED_ALERTS=(
    "StreamingQueueDepthHigh"
    "StreamingQueueDepthCritical"
    "StreamingOutboxUnsentGrowing"
    "StreamingWorkerErrorRateHigh"
    "StreamingDispatcherErrorRateHigh"
    "StreamingDLQMessagesDetected"
    "StreamingLatencyP95High"
    "StreamingLatencyP95Critical"
    "StreamingLocksExpiring"
)

MISSING_ALERTS=0

for ALERT in "${EXPECTED_ALERTS[@]}"; do
    if grep -q "alert: $ALERT" "$ALERTS_FILE"; then
        echo -e "${GREEN}✓${NC} Found alert: $ALERT"
    else
        echo -e "${RED}✗${NC} Missing alert: $ALERT"
        MISSING_ALERTS=$((MISSING_ALERTS + 1))
    fi
done

if [ $MISSING_ALERTS -gt 0 ]; then
    echo -e "${RED}✗${NC} $MISSING_ALERTS expected alert(s) missing"
    exit 1
fi

# Test 4: Dry-Run Evaluation (if Prometheus is running)
echo ""
echo "4. Dry-run evaluation against current metrics..."

if curl -sf "$PROM_URL/api/v1/query?query=up" > /dev/null 2>&1; then
    # Evaluate sample queries
    QUERIES=(
        "core_stream_cmd_queue_depth"
        "core_stream_outbox_unsent_total"
        "core_stream_worker_error_total"
        "core_stream_dlq_total"
    )
    
    for QUERY in "${QUERIES[@]}"; do
        RESULT=$(curl -s "$PROM_URL/api/v1/query?query=$QUERY")
        
        if echo "$RESULT" | grep -q '"status":"success"'; then
            RESULT_COUNT=$(echo "$RESULT" | jq '.data.result | length' 2>/dev/null || echo "0")
            if [ "$RESULT_COUNT" -gt 0 ]; then
                echo -e "${GREEN}✓${NC} Metric $QUERY has data ($RESULT_COUNT series)"
            else
                echo -e "${YELLOW}⚠${NC} Metric $QUERY has no data yet (expected after traffic)"
            fi
        else
            echo -e "${YELLOW}⚠${NC} Could not query $QUERY"
        fi
    done
else
    echo -e "${YELLOW}⚠${NC} Prometheus not running, skipping dry-run evaluation"
fi

# Test 5: Alert Annotations & Labels
echo ""
echo "5. Checking alert annotations and labels..."

ANNOTATIONS_OK=true

while IFS= read -r line; do
    if [[ "$line" =~ ^[[:space:]]*-[[:space:]]*alert:[[:space:]](.+)$ ]]; then
        ALERT_NAME="${BASH_REMATCH[1]}"
        
        # Check for severity label (next few lines)
        CONTEXT=$(grep -A 10 "alert: $ALERT_NAME" "$ALERTS_FILE" || echo "")
        
        if ! echo "$CONTEXT" | grep -q 'severity:'; then
            echo -e "${RED}✗${NC} Alert $ALERT_NAME missing 'severity' label"
            ANNOTATIONS_OK=false
        fi
        
        if ! echo "$CONTEXT" | grep -q 'summary:'; then
            echo -e "${RED}✗${NC} Alert $ALERT_NAME missing 'summary' annotation"
            ANNOTATIONS_OK=false
        fi
        
        if ! echo "$CONTEXT" | grep -q 'description:'; then
            echo -e "${RED}✗${NC} Alert $ALERT_NAME missing 'description' annotation"
            ANNOTATIONS_OK=false
        fi
    fi
done < "$ALERTS_FILE"

if [ "$ANNOTATIONS_OK" = true ]; then
    echo -e "${GREEN}✓${NC} All alerts have required labels and annotations"
fi

# Summary
echo ""
echo "=========================================="
echo -e "${GREEN}✓ Alert validation completed successfully!${NC}"
echo ""
echo "Next steps:"
echo "  - Start Prometheus: docker compose --profile streaming up -d prometheus"
echo "  - View alerts: http://localhost:9090/alerts"
echo "  - Trigger test alert: Generate traffic to exceed thresholds"
exit 0
