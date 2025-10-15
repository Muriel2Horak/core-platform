#!/usr/bin/env bash
# Demo progress tracker - shows how it will look

TRACKER="scripts/build/build-progress-tracker.sh"

echo "=== Demo 1: Standard 6-step pipeline ==="
echo ""

# Initialize with 6 steps
bash $TRACKER init "MAKE CLEAN - STANDARD" "Cleanup" "Pre-build tests" "Build images" "Start services" "E2E pre-deploy" "E2E post-deploy"
sleep 2

# Step 1: Cleanup
bash $TRACKER update 1 "IN_PROGRESS" ""
sleep 2
bash $TRACKER update 1 "DONE" "8s"
sleep 1

# Step 2: Pre-build tests
bash $TRACKER update 2 "IN_PROGRESS" ""
sleep 2
bash $TRACKER update 2 "DONE" "42s"
sleep 1

# Step 3: Build
bash $TRACKER update 3 "IN_PROGRESS" ""
sleep 2
bash $TRACKER update 3 "DONE" "1m 25s"
sleep 1

# Step 4: Start services
bash $TRACKER update 4 "IN_PROGRESS" ""
sleep 2
bash $TRACKER update 4 "DONE" "15s"
sleep 1

# Step 5: E2E Pre
bash $TRACKER update 5 "IN_PROGRESS" ""
sleep 2
bash $TRACKER update 5 "DONE" "1m 05s"
sleep 1

# Step 6: E2E Post
bash $TRACKER update 6 "IN_PROGRESS" ""
sleep 2
bash $TRACKER update 6 "DONE" "2m 10s"

sleep 2

echo ""
echo "=== Demo 2: Extended pipeline with 10 steps (added UNIT, INT, PERF tests) ==="
echo ""
sleep 2

# Initialize with 10 steps - DYNAMICALLY!
bash $TRACKER init "MAKE CLEAN - EXTENDED" \
    "Cleanup" \
    "Pre-build tests" \
    "Build images" \
    "Start services" \
    "Unit tests" \
    "Integration tests" \
    "E2E pre-deploy" \
    "E2E post-deploy" \
    "Performance tests" \
    "Security scan"

sleep 2

# Run through quickly
for i in {1..10}; do
    bash $TRACKER update $i "IN_PROGRESS" ""
    sleep 0.5
    bash $TRACKER update $i "DONE" "$((RANDOM % 60 + 5))s"
    sleep 0.3
done

sleep 2

echo ""
echo "=== Demo 3: Failure scenario ==="
echo ""
sleep 2

bash $TRACKER init "MAKE CLEAN - FAILURE DEMO" "Cleanup" "Pre-build tests" "Build" "Deploy"

bash $TRACKER update 1 "IN_PROGRESS" ""
sleep 1
bash $TRACKER update 1 "DONE" "8s"
sleep 0.5

bash $TRACKER update 2 "IN_PROGRESS" ""
sleep 2
bash $TRACKER update 2 "FAILED" "42s"

# Show error below panel
bash $TRACKER error 2 "diagnostics/tests/backend-20251015-212547.log"

echo ""
echo "✅ Demo complete! System supports ANY number of steps dynamically."
