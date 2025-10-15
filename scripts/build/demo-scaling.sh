#!/usr/bin/env bash
# Demo: Adding new test stages dynamically

TRACKER="scripts/build/build-progress-tracker.sh"

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  Demo: Dynamic Test Stage Addition                              ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "This demonstrates how easily you can add new test types to the"
echo "build pipeline. The progress tracker automatically adjusts!"
echo ""
sleep 2

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Scenario 1: TODAY - Basic Pipeline (4 steps)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
sleep 1

bash $TRACKER init "TODAY'S PIPELINE" \
    "Cleanup" "Pre-build tests" "Build" "Start"

for i in {1..4}; do
    bash $TRACKER update $i "IN_PROGRESS" ""
    sleep 0.5
    bash $TRACKER update $i "DONE" "$((RANDOM % 30 + 5))s"
    sleep 0.3
done

sleep 2

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Scenario 2: TOMORROW - Added Unit Tests (5 steps)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Developer added: make test-unit"
sleep 1

bash $TRACKER init "TOMORROW'S PIPELINE" \
    "Cleanup" "Pre-build tests" "Build" "Start" \
    "Unit tests"

for i in {1..5}; do
    bash $TRACKER update $i "IN_PROGRESS" ""
    sleep 0.5
    bash $TRACKER update $i "DONE" "$((RANDOM % 30 + 5))s"
    sleep 0.3
done

sleep 2

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Scenario 3: NEXT WEEK - Full Test Suite (10 steps)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Team added: unit, integration, E2E, performance, security"
sleep 1

bash $TRACKER init "NEXT WEEK'S PIPELINE" \
    "Cleanup" \
    "Pre-build tests" \
    "Build images" \
    "Start services" \
    "Unit tests" \
    "Integration tests" \
    "E2E smoke tests" \
    "E2E full scenarios" \
    "Performance tests" \
    "Security scan"

for i in {1..10}; do
    bash $TRACKER update $i "IN_PROGRESS" ""
    sleep 0.4
    bash $TRACKER update $i "DONE" "$((RANDOM % 60 + 10))s"
    sleep 0.2
done

sleep 2

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Scenario 4: FUTURE - With Load Testing (15 steps)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Added: contract tests, visual regression, load tests, chaos engineering"
sleep 1

bash $TRACKER init "FUTURE PIPELINE (UNLIMITED!)" \
    "Cleanup" \
    "Pre-build tests" \
    "Build images" \
    "Start services" \
    "Unit tests" \
    "Integration tests" \
    "Contract tests" \
    "E2E smoke" \
    "E2E full" \
    "Visual regression" \
    "Performance tests" \
    "Load tests" \
    "Security scan" \
    "Chaos engineering" \
    "Final validation"

for i in {1..15}; do
    bash $TRACKER update $i "IN_PROGRESS" ""
    sleep 0.3
    bash $TRACKER update $i "DONE" "$((RANDOM % 90 + 5))s"
    sleep 0.2
done

sleep 2

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  ✅ Demo Complete!                                              ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║  Key Takeaway:                                                   ║"
echo "║  - NO hardcoded limits                                           ║"
echo "║  - Add tests by just adding ONE line to Makefile                 ║"
echo "║  - Progress tracker scales automatically                         ║"
echo "║  - From 4 steps to 100+ steps - no problem!                      ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
