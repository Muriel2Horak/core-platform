#!/usr/bin/env bash
# Compact status line - one line status at top, scrolling output below

STATE_FILE="${BUILD_PROGRESS_STATE:-/tmp/make-progress-shared}"

# Colors
BOLD='\033[1m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

draw_compact_status() {
    if [ ! -f "$STATE_FILE" ]; then
        return
    fi
    
    source "$STATE_FILE"
    
    # Count completed steps
    local completed=0
    for i in $(seq 1 "$TOTAL_STEPS"); do
        eval "status=\$STEP_${i}_STATUS"
        if [ "$status" = "DONE" ]; then
            ((completed++)) || true
        fi
    done
    
    # Find current step
    local current_step_name=""
    local current_step_progress=""
    for i in $(seq 1 "$TOTAL_STEPS"); do
        eval "status=\$STEP_${i}_STATUS"
        if [ "$status" = "IN_PROGRESS" ]; then
            eval "current_step_name=\$STEP_${i}_NAME"
            eval "current=\$STEP_${i}_CURRENT"
            eval "total=\$STEP_${i}_TOTAL"
            if [ "$total" -gt 0 ]; then
                current_step_progress=" [$current/$total]"
            fi
            break
        fi
    done
    
    # Calculate overall progress
    local overall_percent=$((completed * 100 / TOTAL_STEPS))
    local elapsed=$(($(date +%s) - START_TIME))
    local elapsed_fmt=$(printf "%dm%02ds" $((elapsed / 60)) $((elapsed % 60)))
    
    # Draw compact status line at top
    tput sc 2>/dev/null || true  # Save cursor
    tput cup 0 0 2>/dev/null || true  # Jump to top
    
    # Clear line and draw status
    printf "\r\033[K"  # Clear line
    echo -ne "${BOLD}▶ $PIPELINE_NAME${NC} │ ${CYAN}$completed/$TOTAL_STEPS${NC} (${overall_percent}%%) │ ${YELLOW}$current_step_name$current_step_progress${NC} │ ${GRAY}${elapsed_fmt}${NC}"
    
    tput rc 2>/dev/null || true  # Restore cursor
}

# Main
draw_compact_status
