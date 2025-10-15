#!/usr/bin/env bash
# Auto-split terminal for progress monitoring
# Creates split pane with fixed status panel on top and scrolling output below

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TRACKER="$SCRIPT_DIR/build-progress-tracker.sh"
WATCH_SCRIPT="$SCRIPT_DIR/watch-progress.sh"

# Command to run (all arguments)
CMD="$@"

# Check if we should use split mode
USE_SPLIT="${PROGRESS_SPLIT:-auto}"

if [ "$USE_SPLIT" = "auto" ]; then
    # Auto-detect: use split if tmux is available and not already in a split
    if command -v tmux &> /dev/null && [ -z "${TMUX:-}" ]; then
        USE_SPLIT="yes"
    elif [ -n "${TMUX:-}" ]; then
        # Already in tmux, check if we can split
        USE_SPLIT="yes"
    else
        USE_SPLIT="no"
    fi
fi

# Function to run in tmux split
run_with_tmux_split() {
    if [ -z "${TMUX:-}" ]; then
        # Not in tmux - start new session with split
        echo "🚀 Starting build in tmux split-pane mode..."
        echo "   Top pane: Live progress dashboard"
        echo "   Bottom pane: Build output"
        echo ""
        sleep 1
        
        # Create new tmux session with vertical split
        tmux new-session -d -s "build-$$" "cd '$PWD' && $CMD; echo ''; echo 'Press Enter to close...'; read"
        
        # Split window: top 30% for status, bottom 70% for build
        tmux split-window -t "build-$$" -v -p 70 "cd '$PWD' && bash '$WATCH_SCRIPT'"
        
        # Focus on build pane (bottom)
        tmux select-pane -t "build-$$:0.1"
        
        # Attach to session
        tmux attach-session -t "build-$$"
        
        # Cleanup after session ends
        tmux kill-session -t "build-$$" 2>/dev/null || true
    else
        # Already in tmux - split current window
        echo "🚀 Creating split-pane for progress monitoring..."
        
        # Get current session and window
        SESSION=$(tmux display-message -p '#S')
        WINDOW=$(tmux display-message -p '#I')
        
        # Split window: top 30% for status, bottom 70% for build
        tmux split-window -t "$SESSION:$WINDOW" -v -b -p 30 "cd '$PWD' && bash '$WATCH_SCRIPT'; read -p 'Press Enter to close...'"
        
        # Run build in current pane (bottom)
        eval "$CMD"
        
        # After build, ask user before closing status pane
        echo ""
        echo "✅ Build complete! Status panel is still visible in top pane."
        echo "   Press Ctrl+B then X to close the status pane when done."
    fi
}

# Main logic
if [ "$USE_SPLIT" = "yes" ]; then
    if ! command -v tmux &> /dev/null; then
        echo "⚠️  tmux not found. Install with: brew install tmux"
        echo "   Falling back to normal mode..."
        echo ""
        sleep 1
        eval "$CMD"
    else
        run_with_tmux_split
    fi
else
    # Normal mode - no split
    eval "$CMD"
fi
