#!/usr/bin/env bash
# Progress wrapper - creates split pane with fixed status panel

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TRACKER="$SCRIPT_DIR/build-progress-tracker.sh"
STATE_FILE="${BUILD_PROGRESS_STATE:-/tmp/make-progress-shared}"

# Check if we should use split-pane mode
USE_SPLIT_PANE="${USE_SPLIT_PANE:-auto}"

# Auto-detect: use split pane if in tmux or screen
if [ "$USE_SPLIT_PANE" = "auto" ]; then
    if [ -n "$TMUX" ] || [ -n "$STY" ]; then
        USE_SPLIT_PANE="yes"
    else
        USE_SPLIT_PANE="no"
    fi
fi

# Function to start status panel watcher in tmux pane
start_status_watcher() {
    local session_name="$1"
    local pane_id="$2"
    
    # Send commands to top pane to watch status
    tmux send-keys -t "$session_name:$pane_id" "watch -n 1 -t '$TRACKER draw'" C-m
}

# Function to run build in split pane mode
run_with_split_pane() {
    local cmd="$@"
    
    if [ -z "$TMUX" ]; then
        echo "❌ Split pane mode requires tmux. Install: brew install tmux"
        echo "   Or set USE_SPLIT_PANE=no to disable"
        exit 1
    fi
    
    # Get current tmux session and window
    local session=$(tmux display-message -p '#S')
    local window=$(tmux display-message -p '#I')
    
    # Split window: top 30% for status, bottom 70% for output
    tmux split-window -v -p 70 -t "$session:$window"
    
    # Get pane IDs
    local top_pane=$(tmux list-panes -t "$session:$window" -F '#{pane_index}' | head -1)
    local bottom_pane=$(tmux list-panes -t "$session:$window" -F '#{pane_index}' | tail -1)
    
    # Start status watcher in top pane
    start_status_watcher "$session" "$top_pane" &
    
    # Run build in bottom pane (current pane)
    eval "$cmd"
    
    # After build completes, kill status watcher
    tmux send-keys -t "$session:$top_pane" C-c
    tmux send-keys -t "$session:$top_pane" "exit" C-m
}

# Main logic
if [ "$USE_SPLIT_PANE" = "yes" ]; then
    run_with_split_pane "$@"
else
    # Regular mode - panel scrolls with output
    eval "$@"
fi
