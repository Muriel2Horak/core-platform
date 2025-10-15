#!/usr/bin/env bash
# Build progress tracker - displays persistent status panel during make

# State file for progress tracking (shared across processes)
STATE_FILE="${BUILD_PROGRESS_STATE:-/tmp/make-progress-shared}"
# No trap - we want state to persist across invocations

# Colors
BOLD='\033[1m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

# Unicode characters
CHECK="✅"
CROSS="❌"
HOUR="⏳"
PAUSE="⏸️"
SKIP="⏭️"

# Initialize state - DYNAMIC step allocation
init_progress() {
    local pipeline_name="$1"
    shift
    local step_names=("$@")  # All remaining args are step names
    local total_steps=${#step_names[@]}
    
    # Start state file
    cat > "$STATE_FILE" <<EOF
PIPELINE_NAME="$pipeline_name"
TOTAL_STEPS=$total_steps
CURRENT_STEP=0
START_TIME=$(date +%s)
EOF
    
    # Dynamically add steps with sub-progress support
    for i in "${!step_names[@]}"; do
        local step_num=$((i + 1))
        cat >> "$STATE_FILE" <<EOF
STEP_${step_num}_STATUS=PENDING
STEP_${step_num}_NAME="${step_names[$i]}"
STEP_${step_num}_TIME=""
STEP_${step_num}_CURRENT=0
STEP_${step_num}_TOTAL=0
EOF
    done
}

# Update step status
update_step() {
    local step_num="$1"
    local status="$2"  # PENDING, IN_PROGRESS, DONE, FAILED, SKIPPED
    local duration="$3"
    
    # Update state file (macOS compatible)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^STEP_${step_num}_STATUS=.*/STEP_${step_num}_STATUS=$status/" "$STATE_FILE"
        if [ -n "$duration" ]; then
            sed -i '' "s/^STEP_${step_num}_TIME=.*/STEP_${step_num}_TIME=\"$duration\"/" "$STATE_FILE"
        fi
        sed -i '' "s/^CURRENT_STEP=.*/CURRENT_STEP=$step_num/" "$STATE_FILE"
    else
        sed -i "s/^STEP_${step_num}_STATUS=.*/STEP_${step_num}_STATUS=$status/" "$STATE_FILE"
        if [ -n "$duration" ]; then
            sed -i "s/^STEP_${step_num}_TIME=.*/STEP_${step_num}_TIME=\"$duration\"/" "$STATE_FILE"
        fi
        sed -i "s/^CURRENT_STEP=.*/CURRENT_STEP=$step_num/" "$STATE_FILE"
    fi
    
    # Redraw display
    draw_panel
}

# Update step sub-progress (e.g., 145/215 tests)
update_step_progress() {
    local step_num="$1"
    local current="$2"
    local total="$3"
    
    # Update state file
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^STEP_${step_num}_CURRENT=.*/STEP_${step_num}_CURRENT=$current/" "$STATE_FILE"
        sed -i '' "s/^STEP_${step_num}_TOTAL=.*/STEP_${step_num}_TOTAL=$total/" "$STATE_FILE"
    else
        sed -i "s/^STEP_${step_num}_CURRENT=.*/STEP_${step_num}_CURRENT=$current/" "$STATE_FILE"
        sed -i "s/^STEP_${step_num}_TOTAL=.*/STEP_${step_num}_TOTAL=$total/" "$STATE_FILE"
    fi
    
    # Redraw display
    draw_panel
}

# Draw progress bar
draw_bar() {
    local percent="$1"
    local width=12
    local filled=$((percent * width / 100))
    local empty=$((width - filled))
    
    echo -n "["
    for ((i=0; i<filled; i++)); do echo -n "█"; done
    for ((i=0; i<empty; i++)); do echo -n "░"; done
    echo -n "]"
}

# Get status icon and color
get_status_display() {
    local status="$1"
    
    case "$status" in
        PENDING)
            echo -e "${GRAY}${PAUSE}${NC}"
            ;;
        IN_PROGRESS)
            echo -e "${YELLOW}${HOUR}${NC}"
            ;;
        DONE)
            echo -e "${GREEN}${CHECK}${NC}"
            ;;
        FAILED)
            echo -e "${RED}${CROSS}${NC}"
            ;;
        SKIPPED)
            echo -e "${CYAN}${SKIP}${NC}"
            ;;
        CANCELLED)
            echo -e "${GRAY}⏸️${NC}"
            ;;
        *)
            echo -e "${GRAY}?${NC}"
            ;;
    esac
}

# Draw the complete status panel
draw_panel() {
    # Load current state
    source "$STATE_FILE"
    
    # Calculate overall progress
    local completed=0
    for i in $(seq 1 "$TOTAL_STEPS"); do
        eval "status=\$STEP_${i}_STATUS"
        if [ "$status" = "DONE" ]; then
            ((completed++)) || true
        fi
    done
    
    local overall_percent=$((completed * 100 / TOTAL_STEPS))
    local elapsed=$(($(date +%s) - START_TIME))
    local elapsed_fmt=$(printf "%dm %02ds" $((elapsed / 60)) $((elapsed % 60)))
    
    # Clear screen and redraw panel from top
    # Simple approach: let it scroll naturally with output
    clear
    
    echo -e "${BOLD}╔══════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}║  🏗️  $(printf '%-66s' "$PIPELINE_NAME")║${NC}"
    echo -e "${BOLD}╠══════════════════════════════════════════════════════════════════════════╣${NC}"
    
    # Draw each step DYNAMICALLY
    for i in $(seq 1 "$TOTAL_STEPS"); do
        eval "status=\$STEP_${i}_STATUS"
        eval "name=\$STEP_${i}_NAME"
        eval "time=\$STEP_${i}_TIME"
        eval "current=\$STEP_${i}_CURRENT"
        eval "total=\$STEP_${i}_TOTAL"
        
        local icon=$(get_status_display "$status")
        local bar=""
        local status_text=""
        
        case "$status" in
            PENDING|CANCELLED)
                bar=$(draw_bar 0)
                status_text="PENDING"
                [ "$status" = "CANCELLED" ] && status_text="CANCELLED"
                ;;
            IN_PROGRESS)
                # Calculate progress percentage if we have total
                if [ "$total" -gt 0 ]; then
                    local percent=$((current * 100 / total))
                    bar=$(draw_bar "$percent")
                    # Show test count prominently: "145/215 tests (67%)"
                    status_text="${CYAN}${current}/${total} tests${NC} ${GRAY}(${percent}%)${NC}"
                else
                    bar=$(draw_bar 50)
                    status_text="${YELLOW}IN PROGRESS${NC}"
                fi
                ;;
            DONE)
                bar=$(draw_bar 100)
                status_text="${GREEN}DONE${NC}"
                [ -n "$time" ] && status_text="${status_text} ${GRAY}(${time})${NC}"
                # Show final count if available
                if [ "$total" -gt 0 ]; then
                    status_text="${status_text} ${GRAY}[$total tests]${NC}"
                fi
                ;;
            FAILED)
                bar=$(draw_bar 100)
                status_text="${RED}FAILED${NC}"
                [ -n "$time" ] && status_text="${status_text} ${GRAY}(${time})${NC}"
                if [ "$current" -gt 0 ] && [ "$total" -gt 0 ]; then
                    status_text="${status_text} ${GRAY}($current/$total)${NC}"
                fi
                ;;
            SKIPPED)
                bar=$(draw_bar 100)
                status_text="${CYAN}SKIPPED${NC}"
                ;;
        esac
        
        local step_label=$(printf "%d/%d" "$i" "$TOTAL_STEPS")
        local name_padded=$(printf "%-25s" "$name")
        
        echo -e "║  $icon ${BOLD}$step_label${NC}  ${name_padded} $bar $(printf '%-20s' "$status_text") ║"
    done
    
    echo -e "${BOLD}╠══════════════════════════════════════════════════════════════════════════╣${NC}"
    
    # Overall progress
    local overall_bar=$(draw_bar "$overall_percent")
    local overall_text=$(printf "%d/%d (%d%%)" "$completed" "$TOTAL_STEPS" "$overall_percent")
    local elapsed_text="Elapsed: $elapsed_fmt"
    
    echo -e "║  ${BOLD}Overall:${NC} $overall_bar ${CYAN}$overall_text${NC}  │  ${BLUE}$elapsed_text${NC}$(printf '%29s' '')║"
    echo -e "${BOLD}╚══════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""  # Add blank line after panel
}

# Show error summary below panel (panel stays visible)
show_error() {
    local step_num="$1"
    local error_log="$2"
    
    # Get step name from state
    source "$STATE_FILE"
    eval "step_name=\$STEP_${step_num}_NAME"
    
    echo ""
    echo -e "${RED}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}${BOLD}❌ STEP $step_num FAILED: $step_name${NC}"
    echo -e "${RED}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    if [ -f "$error_log" ]; then
        # Parse test failures if it's a test log
        if grep -q "Tests run:" "$error_log" 2>/dev/null; then
            # Backend test summary
            local summary=$(grep -E "Tests run:" "$error_log" | tail -1)
            echo -e "${BOLD}Backend Tests:${NC} $summary"
            echo ""
            
            # Failed test names
            if grep -q "\[ERROR\].*-- Time elapsed:" "$error_log"; then
                echo -e "${BOLD}Failed Tests:${NC}"
                grep -E "\[ERROR\].*-- Time elapsed:" "$error_log" | sed 's/\[ERROR\]/  •/' | head -5
                echo ""
            fi
            
            # Key error messages
            echo -e "${BOLD}Error Details:${NC}"
            grep -A 3 "java.lang.*Exception\|AssertionError" "$error_log" 2>/dev/null | head -20 | sed 's/^/  /'
            echo ""
            
            # Suggestion
            if grep -q "Testcontainers\|Container.*failed" "$error_log"; then
                echo -e "${YELLOW}💡 Suggestion:${NC} This appears to be a Testcontainers issue (common on macOS)."
                echo -e "   ${CYAN}Fix:${NC} SKIP_TEST_CLASSES=\"FailedTestName\" make rebuild"
            fi
        elif grep -q "Test Files" "$error_log" 2>/dev/null; then
            # Frontend test summary
            grep -E "Test Files|Tests.*passed" "$error_log" | tail -2
            echo ""
            
            echo -e "${BOLD}Failed Tests:${NC}"
            grep -E "FAIL|✖" "$error_log" | head -10 | sed 's/^/  /'
        else
            # Generic error output
            echo -e "${BOLD}Error Output:${NC}"
            tail -30 "$error_log" | sed 's/^/  /'
        fi
        
        echo ""
        echo -e "${GRAY}📁 Full log: $error_log${NC}"
    else
        echo -e "${YELLOW}No error log available${NC}"
    fi
    
    echo ""
    echo -e "${BOLD}🤖 GitHub Copilot:${NC} Error details are visible above - analyze and suggest fixes."
    echo ""
}

# Main command dispatcher
case "${1:-}" in
    init)
        # Usage: init "Pipeline Name" "Step1" "Step2" "Step3" ...
        shift
        init_progress "$@"
        draw_panel
        ;;
    update)
        # Usage: update <step_num> <status> [duration]
        update_step "$2" "$3" "$4"
        ;;
    progress)
        # Usage: progress <step_num> <current> <total>
        update_step_progress "$2" "$3" "$4"
        ;;
    error)
        # Usage: error <step_num> <error_log_path>
        show_error "$2" "$3"
        ;;
    draw)
        draw_panel
        ;;
    *)
        echo "Usage: $0 {init|update|progress|error|draw} [args...]"
        echo ""
        echo "Examples:"
        echo "  $0 init \"MAKE CLEAN\" \"Cleanup\" \"Tests\" \"Build\" \"Deploy\""
        echo "  $0 update 1 IN_PROGRESS"
        echo "  $0 progress 2 145 215    # 145 tests done out of 215"
        echo "  $0 update 1 DONE \"5s\""
        echo "  $0 update 2 FAILED \"10s\""
        echo "  $0 error 2 \"path/to/error.log\""
        exit 1
        ;;
esac
