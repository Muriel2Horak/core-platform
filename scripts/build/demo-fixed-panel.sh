#!/usr/bin/env bash
# Demo: Fixed panel at top with scrolling output below

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TRACKER="$SCRIPT_DIR/build-progress-tracker.sh"

# Initialize with 4 steps
"$TRACKER" init "DEMO - FIXED PANEL" "Cleanup" "Pre-build tests" "Build images" "Start services"

# Step 1: Quick cleanup
"$TRACKER" update 1 IN_PROGRESS
sleep 1
"$TRACKER" update 1 DONE "1s"

# Step 2: Tests with simulated output
"$TRACKER" update 2 IN_PROGRESS

echo ""
echo "🧪 Running tests..."
echo ""

for i in {1..30}; do
    # Update progress counter
    "$TRACKER" progress 2 "$i" 30
    
    # Simulate test output scrolling below
    echo "→ Test $i/30: com.example.test.TestClass${i}"
    echo "[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.123 s"
    
    sleep 0.3
done

"$TRACKER" update 2 DONE "9s"

# Step 3: Build
"$TRACKER" update 3 IN_PROGRESS
echo ""
echo "🔨 Building Docker images..."
for i in {1..10}; do
    echo "Step $i/8: Building layer sha256:abc123..."
    sleep 0.5
done
"$TRACKER" update 3 DONE "5s"

# Step 4: Start
"$TRACKER" update 4 IN_PROGRESS
echo ""
echo "🚀 Starting services..."
for service in backend frontend postgres kafka redis; do
    echo "Starting $service..."
    sleep 0.5
done
"$TRACKER" update 4 DONE "3s"

# Cleanup panel
"$TRACKER" cleanup

echo ""
echo "✅ Demo complete!"
