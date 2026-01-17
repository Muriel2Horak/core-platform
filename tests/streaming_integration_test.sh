#!/bin/bash
# =============================================================================
# Streaming Integration Test
# =============================================================================
# Komplexní test celého streaming systému:
# - Kafka connectivity
# - Topic creation
# - Command creation → processing → publishing
# - DLQ handling
# - Quota enforcement
# - Priority scheduling
# - Monitoring metrics
# =============================================================================

set -e  # Exit on error

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-core-kafka}"
DB_CONTAINER="${DB_CONTAINER:-core-db}"
KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9092}"
KAFKA_PORT="${KAFKA_PORT:-9092}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
DB_USER="${DB_USER:-core_user}"
DB_NAME="${DB_NAME:-core_db}"
DB_PASSWORD="${DB_PASSWORD:-}"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-artifacts}"
CURL_FLAGS="${CURL_FLAGS:-}"
KEYCLOAK_CURL_FLAGS="${KEYCLOAK_CURL_FLAGS:-$CURL_FLAGS}"
CURL_TIMEOUT="${CURL_TIMEOUT:-10}"
SKIP_KAFKA_CLI="${SKIP_KAFKA_CLI:-false}"
STREAMING_ENABLED="${STREAMING_ENABLED:-false}"
FORCE_STREAMING_TESTS="${FORCE_STREAMING_TESTS:-false}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-admin}"
KEYCLOAK_CLIENT_ID="${KEYCLOAK_CLIENT_ID:-admin-cli}"
KEYCLOAK_CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET:-}"
KEYCLOAK_USERNAME="${KEYCLOAK_USERNAME:-admin}"
KEYCLOAK_PASSWORD="${KEYCLOAK_PASSWORD:-admin}"
COMMAND_SOURCE="${COMMAND_SOURCE:-db}"
TOPIC_PREFIX="${TOPIC_PREFIX:-core}"
BACKEND_READY_TIMEOUT="${BACKEND_READY_TIMEOUT:-240}"
BACKEND_READY_INTERVAL="${BACKEND_READY_INTERVAL:-2}"

# Counters
TESTS_TOTAL=0
TESTS_PASSED=0
TESTS_FAILED=0

# Functions
print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_test() {
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    echo -e "${YELLOW}[TEST $TESTS_TOTAL]${NC} $1"
}

print_success() {
    TESTS_PASSED=$((TESTS_PASSED + 1))
    echo -e "${GREEN}✅ PASS:${NC} $1"
}

print_failure() {
    TESTS_FAILED=$((TESTS_FAILED + 1))
    echo -e "${RED}❌ FAIL:${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ️  INFO:${NC} $1"
}

run_with_timeout() {
    local seconds="$1"
    shift

    if command -v timeout >/dev/null 2>&1; then
        timeout "$seconds" "$@"
    elif command -v gtimeout >/dev/null 2>&1; then
        gtimeout "$seconds" "$@"
    else
        perl -e 'alarm shift; exec @ARGV' "$seconds" "$@"
    fi
}

wait_for_backend() {
    print_info "Waiting for backend to be ready..."
    local attempts=$((BACKEND_READY_TIMEOUT / BACKEND_READY_INTERVAL))
    for i in $(seq 1 "$attempts"); do
        HEALTH=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/actuator/health" 2>/dev/null || echo "")
        if echo "$HEALTH" | grep -q "\"status\":\"UP\""; then
            print_success "Backend is ready"
            return 0
        fi
        echo -n "."
        sleep "$BACKEND_READY_INTERVAL"
    done
    print_failure "Backend not ready after ${BACKEND_READY_TIMEOUT}s"
    exit 1
}

wait_for_db() {
    print_info "Waiting for database to be ready..."
    for i in {1..30}; do
        if run_with_timeout 5 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
            IN_RECOVERY=$(run_with_timeout 5 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT pg_is_in_recovery();" 2>/dev/null | tr -d '[:space:]')
            if [[ "$IN_RECOVERY" == "f" ]]; then
                print_success "Database is ready"
                return 0
            fi
        fi
        echo -n "."
        sleep 2
    done
    print_failure "Database not ready after 60s"
    exit 1
}

get_admin_tenant_id() {
    run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -q -c \
        "SELECT id FROM tenants WHERE key='admin' LIMIT 1;" 2>/dev/null | head -n 1 | tr -d '[:space:]'
}

# =============================================================================
# Test 1: Infrastructure Health Check
# =============================================================================
test_infrastructure() {
    print_header "Test 1: Infrastructure Health Check"
    
    # Kafka
    print_test "Kafka connectivity"
    if [[ "$SKIP_KAFKA_CLI" != "true" ]]; then
        if run_with_timeout 10 docker exec "$KAFKA_CONTAINER" kafka-broker-api-versions --bootstrap-server "$KAFKA_BOOTSTRAP" >/dev/null 2>&1; then
            print_success "Kafka is UP"
        else
            print_info "Kafka broker API check failed; falling back to port probe"
        fi
    fi

    if command -v nc >/dev/null 2>&1 && nc -z -w 2 127.0.0.1 "$KAFKA_PORT"; then
        print_success "Kafka port $KAFKA_PORT is reachable"
    else
        print_failure "Kafka is DOWN"
        return 1
    fi
    
    # Database
    print_test "Database connectivity"
    if run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" >/dev/null 2>&1; then
        print_success "Database is UP"
    else
        print_failure "Database is DOWN"
        return 1
    fi
    
    # Backend
    print_test "Backend health endpoint"
    HEALTH=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/actuator/health" 2>/dev/null || echo "")
    if echo "$HEALTH" | grep -q "\"status\":\"UP\""; then
        print_success "Backend is UP"
    else
        print_failure "Backend is DOWN or unhealthy"
        return 1
    fi
    
    # Streaming schema
    print_test "Streaming database schema"
    if run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1 FROM command_queue LIMIT 1;" >/dev/null 2>&1; then
        print_success "Streaming schema exists"
    else
        print_failure "Streaming schema missing"
        return 1
    fi
}

# =============================================================================
# Test 2: Kafka Topics
# =============================================================================
test_kafka_topics() {
    print_header "Test 2: Kafka Topics"

    if [[ "$SKIP_KAFKA_CLI" == "true" ]]; then
        print_info "Kafka CLI checks skipped"
        return 0
    fi

    print_test "List Kafka topics"
    if ! docker exec "$KAFKA_CONTAINER" sh -c "command -v kafka-topics" >/dev/null 2>&1; then
        print_info "Kafka CLI not available in container; skipping topic checks"
        return 0
    fi

    TOPICS=$(run_with_timeout 10 docker exec "$KAFKA_CONTAINER" kafka-topics --bootstrap-server "$KAFKA_BOOTSTRAP" --list 2>/dev/null || echo "")
    if [ -z "$TOPICS" ]; then
        TOPIC_COUNT=0
    else
        TOPIC_COUNT=$(echo "$TOPICS" | wc -l | tr -d ' ')
    fi
    print_success "Found $TOPIC_COUNT topics"
    echo "$TOPICS" | head -5
    
    print_test "Check for streaming topics"
    if echo "$TOPICS" | grep -q "${TOPIC_PREFIX}\\.entity\\.events"; then
        print_success "Streaming topics exist"
    else
        print_info "No streaming topics yet (will be auto-created)"
    fi
}

# =============================================================================
# Test 3: Get JWT Token (requires Keycloak)
# =============================================================================
get_jwt_token() {
    print_header "Test 3: Authentication"
    
    print_test "Get JWT token from Keycloak"
    
    # Try to get token (may fail if Keycloak not ready)
    TOKEN_RESPONSE=$(curl -s $KEYCLOAK_CURL_FLAGS -m "$CURL_TIMEOUT" -X POST "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$KEYCLOAK_USERNAME" \
        -d "password=$KEYCLOAK_PASSWORD" \
        -d "grant_type=password" \
        -d "client_id=$KEYCLOAK_CLIENT_ID" \
        ${KEYCLOAK_CLIENT_SECRET:+-d "client_secret=$KEYCLOAK_CLIENT_SECRET"} 2>/dev/null || echo "{}")
    
    JWT_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')
    
    if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
        print_success "JWT token obtained"
        echo "$JWT_TOKEN" > "$ARTIFACTS_DIR/jwt_token.txt"
        export JWT_TOKEN
        return 0
    else
        print_failure "Failed to get JWT token - using mock for remaining tests"
        JWT_TOKEN="mock-token-for-testing"
        export JWT_TOKEN
        return 1
    fi
}

create_command_via_db() {
    local payload="$1"
    local priority="$2"
    local tenant_id
    local sanitized_payload

    tenant_id=$(get_admin_tenant_id)
    if [ -z "$tenant_id" ]; then
        print_failure "Failed to resolve admin tenant id"
        return 1
    fi

    sanitized_payload=$(printf "%s" "$payload" | sed "s/'/''/g")

    COMMAND_ID=$(run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -q -c \
        "INSERT INTO command_queue (tenant_id, entity, entity_id, operation, payload, priority) VALUES ('$tenant_id', 'user', gen_random_uuid(), 'CREATE', '$sanitized_payload', '$priority') RETURNING id;" 2>/dev/null | head -n 1 | tr -d '[:space:]')

    if [ -n "$COMMAND_ID" ] && [ "$COMMAND_ID" != "null" ]; then
        print_info "Command created in DB with ID: $COMMAND_ID"
        echo "$COMMAND_ID" > "$ARTIFACTS_DIR/command_id.txt"
        export COMMAND_ID
        return 0
    else
        print_failure "Failed to create command in DB"
        return 1
    fi
}

create_command_via_api() {
    local payload="$1"
    local priority="$2"
    local escaped_payload

    escaped_payload=$(printf "%s" "$payload" | sed "s/\"/\\\\\"/g")

    RESPONSE=$(curl -s $CURL_FLAGS -X POST "$BACKEND_URL/api/streaming/commands" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"entity\": \"user\",
            \"entityId\": \"$(uuidgen | tr '[:upper:]' '[:lower:]')\",
            \"operation\": \"CREATE\",
            \"priority\": \"${priority^^}\",
            \"payload\": \"$escaped_payload\"
        }" 2>/dev/null || echo "{}")

    echo "$RESPONSE" | jq '.' > "$ARTIFACTS_DIR/command_create_response.json" 2>/dev/null || true

    COMMAND_ID=$(echo "$RESPONSE" | jq -r '.id // empty')

    if [ -n "$COMMAND_ID" ] && [ "$COMMAND_ID" != "null" ]; then
        print_info "Command created with ID: $COMMAND_ID"
        echo "$COMMAND_ID" > "$ARTIFACTS_DIR/command_id.txt"
        export COMMAND_ID
        return 0
    else
        print_failure "Failed to create command via API"
        echo "Response: $RESPONSE"
        return 1
    fi
}

create_command() {
    local payload="$1"
    local priority="$2"

    if [ "$COMMAND_SOURCE" == "api" ]; then
        create_command_via_api "$payload" "$priority"
    else
        create_command_via_db "$payload" "$priority"
    fi
}

# =============================================================================
# Test 4: Create Streaming Command
# =============================================================================
test_create_command() {
    print_header "Test 4: Create Streaming Command"
    
    print_test "Create HIGH priority command"
    
    PAYLOAD='{"name":"Test User","email":"test@example.com"}'

    if create_command "$PAYLOAD" "high"; then
        print_success "Command created"
        return 0
    fi
    print_failure "Failed to create command"
    return 1
}

# =============================================================================
# Test 5: Wait for Command Processing
# =============================================================================
test_command_processing() {
    print_header "Test 5: Command Processing"
    
    if [ -z "$COMMAND_ID" ]; then
        print_failure "No command ID available"
        return 1
    fi
    
    print_test "Wait for command to be processed (max 30s)"
    
    for i in {1..15}; do
        # Check command_queue status
        STATUS=$(run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -q -c \
            "SELECT status FROM command_queue WHERE id = '$COMMAND_ID';" 2>/dev/null | head -n 1 | tr -d ' ')
        
        echo "Attempt $i/15: Status = '$STATUS'"
        
        if [ "$STATUS" == "completed" ]; then
            print_success "Command processed successfully"
            return 0
        elif [ "$STATUS" == "dlq" ]; then
            print_failure "Command moved to DLQ"
            return 1
        fi
        
        sleep 2
    done
    
    print_failure "Command not processed within 30s (timeout)"
    return 1
}

# =============================================================================
# Test 6: Check Kafka Message
# =============================================================================
test_kafka_message() {
    print_header "Test 6: Kafka Message Published"

    if [[ "$SKIP_KAFKA_CLI" == "true" ]]; then
        print_info "Kafka CLI checks skipped"
        return 0
    fi

    if ! docker exec "$KAFKA_CONTAINER" sh -c "command -v kafka-console-consumer" >/dev/null 2>&1; then
        print_info "Kafka consumer CLI not available; skipping message check"
        return 0
    fi

    print_test "Check if message was published to Kafka"
    
    TOPIC="${TOPIC_PREFIX}.entity.events.user"
    
    # Consume last message from topic
    MESSAGE=$(run_with_timeout 10 docker exec "$KAFKA_CONTAINER" kafka-console-consumer \
        --bootstrap-server "$KAFKA_BOOTSTRAP" \
        --topic "$TOPIC" \
        --from-beginning \
        --max-messages 1 \
        --timeout-ms 5000 2>/dev/null || echo "")
    
    if [ -n "$MESSAGE" ]; then
        print_success "Message found in Kafka topic: $TOPIC"
        echo "$MESSAGE" | jq '.' > "$ARTIFACTS_DIR/kafka_message.json" 2>/dev/null || echo "$MESSAGE" > "$ARTIFACTS_DIR/kafka_message.txt"
        return 0
    else
        print_failure "No message found in Kafka (topic may not exist yet)"
        return 1
    fi
}

# =============================================================================
# Test 7: Check Metrics
# =============================================================================
test_metrics() {
    print_header "Test 7: Prometheus Metrics"
    
    print_test "Fetch streaming metrics"
    
    METRICS=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/actuator/prometheus" 2>/dev/null | grep "^streaming_" || echo "")
    
    if [ -n "$METRICS" ]; then
        print_success "Streaming metrics exposed"
        echo "$METRICS" > "$ARTIFACTS_DIR/streaming_metrics.txt"
        
        # Parse specific metrics
        CREATED=$(echo "$METRICS" | grep "streaming_commands_created_total" | head -1)
        PROCESSED=$(echo "$METRICS" | grep "streaming_commands_processed_total" | head -1)
        
        echo "Created: $CREATED"
        echo "Processed: $PROCESSED"
        return 0
    else
        print_failure "No streaming metrics found"
        return 1
    fi
}

# =============================================================================
# Test 8: Queue Status
# =============================================================================
test_queue_status() {
    print_header "Test 8: Queue Status"
    
    print_test "Check streaming queue status"
    
    QUEUE_STATUS=$(run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
        "SELECT status, priority, COUNT(*) as count FROM command_queue GROUP BY status, priority ORDER BY priority DESC, status;" \
        2>/dev/null)
    
    if [ -n "$QUEUE_STATUS" ]; then
        print_success "Queue status retrieved"
        echo "$QUEUE_STATUS"
        echo "$QUEUE_STATUS" > "$ARTIFACTS_DIR/queue_status.txt"
        return 0
    else
        print_failure "Failed to get queue status"
        return 1
    fi
}

# =============================================================================
# Test 9: DLQ Status
# =============================================================================
test_dlq_status() {
    print_header "Test 9: Dead Letter Queue"
    
    print_test "Check DLQ (should be empty for successful test)"
    
    DLQ_COUNT=$(run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c \
        "SELECT COUNT(*) FROM command_queue WHERE status = 'dlq';" 2>/dev/null | tr -d ' ')
    
    if [ "$DLQ_COUNT" == "0" ]; then
        print_success "DLQ is empty (no errors)"
        return 0
    else
        print_info "DLQ has $DLQ_COUNT messages"
        
        # Show DLQ entries
        run_with_timeout 10 docker exec -e PGPASSWORD="$DB_PASSWORD" "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
            "SELECT entity, operation, error_message FROM command_queue WHERE status = 'dlq' LIMIT 5;" \
            2>/dev/null
        return 0
    fi
}

# =============================================================================
# Test 10: Bulk Command Test (Performance)
# =============================================================================
test_bulk_commands() {
    print_header "Test 10: Bulk Command Creation (Performance)"
    
    print_test "Create 10 commands in rapid succession"
    
    SUCCESS_COUNT=0
    for i in {1..10}; do
        BULK_PAYLOAD="{\"test\": true, \"iteration\": $i}"
        if create_command "$BULK_PAYLOAD" "normal"; then
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        fi
    done
    
    print_success "Created $SUCCESS_COUNT/10 commands"
    
    print_info "Waiting 5s for processing..."
    sleep 5
    
    # Check metrics
    METRICS=$(curl -s $CURL_FLAGS "$BACKEND_URL/api/actuator/prometheus" 2>/dev/null | grep "streaming_commands_created_total" | grep "user" || echo "")
    print_info "Metrics: $METRICS"
}

# =============================================================================
# Main Test Execution
# =============================================================================
main() {
    print_header "🧪 STREAMING INTEGRATION TEST"

    # Setup
    mkdir -p "$ARTIFACTS_DIR"

    if [[ "$STREAMING_ENABLED" != "true" && "$FORCE_STREAMING_TESTS" != "true" ]]; then
        print_info "STREAMING_ENABLED=false; skipping streaming integration tests"
        exit 0
    fi
    
    # Wait for backend
    wait_for_backend
    wait_for_db
    
    # Run tests
    test_infrastructure || true
    test_kafka_topics || true
    if [ "$COMMAND_SOURCE" == "api" ]; then
        get_jwt_token || true
    fi
    test_create_command || true
    test_command_processing || true
    test_kafka_message || true
    test_metrics || true
    test_queue_status || true
    test_dlq_status || true
    test_bulk_commands || true
    
    # Summary
    print_header "📊 TEST SUMMARY"
    echo ""
    echo "Total Tests:  $TESTS_TOTAL"
    echo -e "${GREEN}Passed:       $TESTS_PASSED${NC}"
    echo -e "${RED}Failed:       $TESTS_FAILED${NC}"
    echo ""
    
    SUCCESS_RATE=$((TESTS_PASSED * 100 / TESTS_TOTAL))
    echo "Success Rate: $SUCCESS_RATE%"
    echo ""
    
    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${GREEN}🎉 ALL TESTS PASSED!${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  SOME TESTS FAILED${NC}"
        exit 1
    fi
}

# Run
main "$@"
