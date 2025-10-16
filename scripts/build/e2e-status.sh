#!/usr/bin/env bash
# Quick E2E test status checker

set -euo pipefail

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║  E2E TEST STATUS                                              ║"
echo "╠═══════════════════════════════════════════════════════════════╣"

# Check if E2E is running
if ps aux | grep -q "[p]laywright test"; then
    echo "║  Status: 🔄 RUNNING                                           ║"
    
    # Count completed tests
    PRE_COUNT=$(find e2e/test-results -type d -name "*-pre" 2>/dev/null | wc -l | tr -d ' ')
    POST_COUNT=$(find e2e/test-results -type d -name "*-post" 2>/dev/null | wc -l | tr -d ' ')
    
    echo "║  Pre-deploy:  $PRE_COUNT tests completed                            ║"
    echo "║  Post-deploy: $POST_COUNT tests completed                            ║"
    
    # Show recent activity
    LATEST=$(ls -t e2e/test-results/ 2>/dev/null | head -1 || echo "none")
    if [ "$LATEST" != "none" ]; then
        LATEST_TIME=$(stat -f "%Sm" -t "%H:%M:%S" "e2e/test-results/$LATEST" 2>/dev/null || echo "unknown")
        echo "║  Latest:      $LATEST_TIME                                       ║"
    fi
    
    echo "╠═══════════════════════════════════════════════════════════════╣"
    echo "║  💡 E2E tests don't show real-time progress                   ║"
    echo "║  ⏱️  Each test can take 15-60 seconds                          ║"
    echo "║  📊 Watch tmux session for live output:                       ║"
    echo "║     tmux attach -t build-*                                    ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
else
    echo "║  Status: ⏸️  NOT RUNNING                                       ║"
    
    if [ -d "e2e/test-results" ]; then
        PRE_COUNT=$(find e2e/test-results -type d -name "*-pre" 2>/dev/null | wc -l | tr -d ' ')
        POST_COUNT=$(find e2e/test-results -type d -name "*-post" 2>/dev/null | wc -l | tr -d ' ')
        
        echo "║  Last run results:                                            ║"
        echo "║  Pre-deploy:  $PRE_COUNT tests                                      ║"
        echo "║  Post-deploy: $POST_COUNT tests                                      ║"
    fi
    
    echo "╚═══════════════════════════════════════════════════════════════╝"
fi

# Show last few test results
if [ -f "e2e/test-results/.last-run.json" ]; then
    echo ""
    echo "Recent test results:"
    cat e2e/test-results/.last-run.json 2>/dev/null | grep -E "expected|actual|status" | head -10 || true
fi
