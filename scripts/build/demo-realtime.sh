#!/usr/bin/env bash
# Demo: Real-time test progress (simulates Maven output)

TRACKER="scripts/build/build-progress-tracker.sh"

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  Demo: Real-Time Test Progress                                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "Simulates running 215 backend tests with live progress updates"
echo ""
sleep 2

# Initialize pipeline
bash $TRACKER init "BACKEND TESTS - LIVE PROGRESS" \
    "Unit tests" "Integration tests"

sleep 1

# Simulate 215 tests running
echo "Starting backend unit tests (215 total)..."
bash $TRACKER update 1 "IN_PROGRESS" ""

for i in $(seq 1 215); do
    # Update progress
    bash $TRACKER progress 1 "$i" 215
    
    # Simulate test execution time (fast demo)
    sleep 0.05
    
    # Every 20 tests, show what's happening
    if [ $((i % 20)) -eq 0 ]; then
        echo "[INFO] Running Test $i/215..."
    fi
done

# Mark as done
bash $TRACKER update 1 "DONE" "45s"
echo ""
echo "✅ All 215 tests completed!"
sleep 2

# Now simulate 500 tests (tomorrow's scenario)
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Tomorrow: You added more tests - now 500 total"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
sleep 2

bash $TRACKER init "BACKEND TESTS - EXPANDED SUITE" \
    "Unit tests (500)" "Integration tests (150)"

sleep 1

echo "Starting backend unit tests (500 total)..."
bash $TRACKER update 1 "IN_PROGRESS" ""

for i in $(seq 1 500); do
    bash $TRACKER progress 1 "$i" 500
    sleep 0.02
    
    if [ $((i % 50)) -eq 0 ]; then
        echo "[INFO] Running Test $i/500..."
    fi
done

bash $TRACKER update 1 "DONE" "1m 20s"
echo ""
echo "✅ All 500 tests completed!"
sleep 1

# Integration tests
echo ""
echo "Starting integration tests (150 total)..."
bash $TRACKER update 2 "IN_PROGRESS" ""

for i in $(seq 1 150); do
    bash $TRACKER progress 2 "$i" 150
    sleep 0.03
    
    if [ $((i % 30)) -eq 0 ]; then
        echo "[INFO] Running Integration Test $i/150..."
    fi
done

bash $TRACKER update 2 "DONE" "50s"
echo ""
echo "✅ All integration tests completed!"

sleep 2

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  ✅ Demo Complete!                                              ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║  Key Features:                                                   ║"
echo "║  - Real-time progress bar updates (145/215, 67%)                 ║"
echo "║  - Works with ANY number of tests (215 → 500 → unlimited)        ║"
echo "║  - Parser detects test count automatically from Maven/Vitest     ║"
echo "║  - Caches test count for better initial estimates               ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
