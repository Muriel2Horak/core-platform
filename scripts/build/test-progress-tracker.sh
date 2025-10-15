#!/usr/bin/env bash
# Live test progress tracker with table UI

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Progress state
BACKEND_TOTAL=0
BACKEND_RUN=0
BACKEND_PASSED=0
BACKEND_FAILED=0
BACKEND_ERRORS=0
BACKEND_SKIPPED=0
BACKEND_STATUS="⏳"

FRONTEND_TOTAL=0
FRONTEND_RUN=0
FRONTEND_PASSED=0
FRONTEND_FAILED=0
FRONTEND_SKIPPED=0
FRONTEND_STATUS="⏳"

START_TIME=$(date +%s)

# Draw progress bar
draw_bar() {
    local current=$1
    local total=$2
    local width=20
    
    if [ "$total" -eq 0 ]; then
        echo -n "[░░░░░░░░░░░░░░░░░░░░]   0%"
        return
    fi
    
    local percent=$((current * 100 / total))
    local filled=$((current * width / total))
    local empty=$((width - filled))
    
    echo -n "["
    for ((i=0; i<filled; i++)); do echo -n "█"; done
    for ((i=0; i<empty; i++)); do echo -n "░"; done
    echo -n "] $(printf "%3d" $percent)%"
}

# Update display (overwrites previous output)
update_display() {
    local overall_run=$((BACKEND_RUN + FRONTEND_RUN))
    local overall_total=$((BACKEND_TOTAL + FRONTEND_TOTAL))
    local elapsed=$(($(date +%s) - START_TIME))
    local elapsed_fmt=$(printf "%dm %02ds" $((elapsed / 60)) $((elapsed % 60)))
    
    # Clear screen and move to top
    echo -ne "\033[2J\033[H"
    
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}${BLUE}                         📊 TEST PROGRESS                                 ${NC}"
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    # Backend row
    echo -ne "${BOLD}Backend:${NC}  "
    draw_bar $BACKEND_RUN $BACKEND_TOTAL
    echo -ne "  ${BACKEND_STATUS}  "
    echo -ne "${GREEN}✅ $BACKEND_PASSED${NC}  "
    echo -ne "${RED}❌ $BACKEND_FAILED${NC}  "
    echo -ne "${YELLOW}💥 $BACKEND_ERRORS${NC}  "
    echo -e "${CYAN}⏭ $BACKEND_SKIPPED${NC}"
    
    # Frontend row
    echo -ne "${BOLD}Frontend:${NC} "
    draw_bar $FRONTEND_RUN $FRONTEND_TOTAL
    echo -ne "  ${FRONTEND_STATUS}  "
    echo -ne "${GREEN}✅ $FRONTEND_PASSED${NC}  "
    echo -ne "${RED}❌ $FRONTEND_FAILED${NC}  "
    echo -e "${CYAN}⏭ $FRONTEND_SKIPPED${NC}"
    
    echo ""
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    # Overall progress
    echo -ne "${BOLD}Overall:${NC}  "
    draw_bar $overall_run $overall_total
    if [ "$overall_total" -gt 0 ]; then
        echo -ne "  ${BOLD}$overall_run/$overall_total tests${NC}"
    fi
    echo ""
    echo -e "${BOLD}Elapsed:${NC}  $elapsed_fmt"
    
    echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Parse Maven Surefire output
parse_maven_line() {
    local line="$1"
    
    # Extract test counts: "Tests run: 145, Failures: 2, Errors: 0, Skipped: 1"
    if [[ "$line" =~ Tests\ run:\ ([0-9]+),\ Failures:\ ([0-9]+),\ Errors:\ ([0-9]+),\ Skipped:\ ([0-9]+) ]]; then
        BACKEND_RUN=${BASH_REMATCH[1]}
        BACKEND_FAILED=${BASH_REMATCH[2]}
        BACKEND_ERRORS=${BASH_REMATCH[3]}
        BACKEND_SKIPPED=${BASH_REMATCH[4]}
        BACKEND_PASSED=$((BACKEND_RUN - BACKEND_FAILED - BACKEND_ERRORS - BACKEND_SKIPPED))
        
        if [ "$BACKEND_TOTAL" -eq 0 ]; then
            BACKEND_TOTAL=$BACKEND_RUN
        fi
        
        if [ $((BACKEND_FAILED + BACKEND_ERRORS)) -gt 0 ]; then
            BACKEND_STATUS="❌"
        else
            BACKEND_STATUS="✅"
        fi
        
        update_display
    fi
}

# Parse Vitest output
parse_vitest_line() {
    local line="$1"
    
    # "Test Files  9 passed (9)"
    if [[ "$line" =~ Test\ Files\ +([0-9]+)\ passed\ \(([0-9]+)\) ]]; then
        FRONTEND_PASSED=${BASH_REMATCH[1]}
        FRONTEND_TOTAL=${BASH_REMATCH[2]}
        FRONTEND_RUN=$FRONTEND_TOTAL
        FRONTEND_STATUS="✅"
        update_display
    fi
    
    # "Tests  66 passed | 1 skipped (67)"
    if [[ "$line" =~ Tests\ +([0-9]+)\ passed\ \|\ ([0-9]+)\ skipped ]]; then
        FRONTEND_SKIPPED=${BASH_REMATCH[2]}
        update_display
    elif [[ "$line" =~ Tests\ +([0-9]+)\ passed\ \(([0-9]+)\) ]]; then
        FRONTEND_PASSED=${BASH_REMATCH[1]}
        update_display
    fi
    
    # Detect failures
    if [[ "$line" =~ FAIL|failed ]]; then
        FRONTEND_STATUS="❌"
        FRONTEND_FAILED=$((FRONTEND_FAILED + 1))
        update_display
    fi
}

# Main function
track_tests() {
    local component="$1"
    local log_file="$2"
    
    # Initial display
    update_display
    
    # Read log file line by line and update progress
    while IFS= read -r line; do
        if [ "$component" = "backend" ]; then
            parse_maven_line "$line"
        elif [ "$component" = "frontend" ]; then
            parse_vitest_line "$line"
        fi
    done < "$log_file"
}

# Export function for use in other scripts
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    # Script is being executed directly
    track_tests "$@"
fi
