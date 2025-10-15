#!/usr/bin/env bash
# Demo: SLOW real-time progress (visible updates)

TRACKER="scripts/build/build-progress-tracker.sh"

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  Demo: LIVE Test Progress (Slow - You Can See Updates)          ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "Simulates 50 backend tests with VISIBLE live updates"
echo "(1 test per second so you can see the progress bar filling)"
echo ""
sleep 2

# Initialize pipeline
bash $TRACKER init "BACKEND TESTS - VISIBLE UPDATES" \
    "Unit tests (50 total)"

sleep 1

echo ""
echo "▶️  Starting backend unit tests (50 total)..."
echo "    Watch the progress bar and counter update live!"
echo ""
sleep 2

bash $TRACKER update 1 "IN_PROGRESS" ""

# Run 50 tests SLOWLY so you can see updates
for i in $(seq 1 50); do
    # Update progress - THIS IS VISIBLE
    bash $TRACKER progress 1 "$i" 50
    
    # Slow enough to see (1 test per second)
    sleep 1
done

# Mark as done
bash $TRACKER update 1 "DONE" "50s"
echo ""
echo "✅ All 50 tests completed!"

sleep 2

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  Did you see the progress bar fill up live?                     ║"
echo "║  - Counter: (1/50) → (2/50) → ... → (50/50)                     ║"
echo "║  - Progress bar: [░░░] → [██░] → [████] → [████████]            ║"
echo "║                                                                  ║"
echo "║  In real tests, updates happen every 1-5 seconds as Maven runs  ║"
echo "║  each test class, so you see smooth progress!                   ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
