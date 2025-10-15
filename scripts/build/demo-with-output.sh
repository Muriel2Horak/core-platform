#!/usr/bin/env bash
# Demo: Panel at top + test output below

TRACKER="scripts/build/build-progress-tracker.sh"

# Initialize
bash $TRACKER init "BACKEND TESTS - WITH OUTPUT" \
    "Unit tests (25 total)"

sleep 1

bash $TRACKER update 1 "IN_PROGRESS" ""

# Simulate test output
TESTS=(
    "TenantServiceTest"
    "RoleServiceTest"  
    "UserServiceTest"
    "AuthServiceTest"
    "PermissionServiceTest"
    "AuditServiceTest"
    "NotificationServiceTest"
    "ConfigServiceTest"
    "CacheServiceTest"
    "MetricsServiceTest"
    "HealthCheckTest"
    "ValidationTest"
    "MapperTest"
    "RepositoryTest"
    "ControllerTest"
    "FilterTest"
    "InterceptorTest"
    "SchedulerTest"
    "EventListenerTest"
    "ExceptionHandlerTest"
    "SecurityConfigTest"
    "WebConfigTest"
    "DataSourceConfigTest"
    "KafkaConfigTest"
    "RedisConfigTest"
)

echo ""
echo "▶️  Running backend unit tests:"
echo ""

for i in "${!TESTS[@]}"; do
    test_num=$((i + 1))
    test_name="${TESTS[$i]}"
    
    # Update progress
    bash $TRACKER progress 1 "$test_num" "${#TESTS[@]}"
    
    # Simulate test output
    echo "[INFO] Running com.example.${test_name}"
    sleep 0.5
    
    # Random number of sub-tests
    num_tests=$((RANDOM % 15 + 3))
    time=$((RANDOM % 3000 + 500))
    time_sec=$(echo "scale=3; $time/1000" | bc)
    
    echo "[INFO] Tests run: $num_tests, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ${time_sec} s"
    echo ""
    
    sleep 0.3
done

bash $TRACKER update 1 "DONE" "18s"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ All tests completed!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
