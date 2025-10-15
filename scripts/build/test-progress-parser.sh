#!/usr/bin/env bash
# Real-time test progress parser
# Parsuje Maven Surefire a Vitest output a reportuje progress

STEP_NUM="$1"
COMPONENT="$2"  # backend | frontend

# Find tracker script (works from any directory)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TRACKER="$SCRIPT_DIR/build-progress-tracker.sh"

# Temporary file for counting
TEST_COUNT=0
TEST_TOTAL=0

# Parse Maven Surefire output in real-time
parse_maven_realtime() {
    local line="$1"
    
    # Detect test execution: "[INFO] Running com.example.TestClass"
    if [[ "$line" =~ ^\[INFO\]\ Running\ ([a-zA-Z0-9_.]+) ]]; then
        ((TEST_COUNT++))
        local test_class="${BASH_REMATCH[1]}"
        # Print with color: test number and class name
        echo -e "\033[1;36m→ Test ${TEST_COUNT}/${TEST_TOTAL}:\033[0m \033[0;33m${test_class}\033[0m"
        # Update progress only every 10 tests to reduce redraw frequency
        if [ "$STEP_NUM" -gt 0 ] && (( TEST_COUNT % 10 == 0 || TEST_COUNT == 1 )); then
            bash "$TRACKER" progress "$STEP_NUM" "$TEST_COUNT" "$TEST_TOTAL"
        fi
    fi
    
    # Detect FINAL test summary to get exact total (only from final summary, not individual tests)
    # Final summary looks like: "[INFO] Tests run: 215, Failures: 0, Errors: 0, Skipped: 0"
    if [[ "$line" =~ ^\[INFO\]\ Tests\ run:\ ([0-9]+),\ Failures:\ ([0-9]+) ]]; then
        local total="${BASH_REMATCH[1]}"
        # Only update if this is likely the final summary (not individual test)
        if [ "$total" -gt "$TEST_TOTAL" ]; then
            TEST_TOTAL=$total
            if [ "$STEP_NUM" -gt 0 ]; then
                bash "$TRACKER" progress "$STEP_NUM" "$TEST_COUNT" "$TEST_TOTAL"
            fi
        fi
    fi
}

# Parse Vitest output in real-time
parse_vitest_realtime() {
    local line="$1"
    
    # Vitest shows: "Test Files  9 passed (9)"
    if [[ "$line" =~ Test\ Files\ +([0-9]+)\ passed\ \(([0-9]+)\) ]]; then
        TEST_COUNT=${BASH_REMATCH[1]}
        TEST_TOTAL=${BASH_REMATCH[2]}
        if [ "$STEP_NUM" -gt 0 ]; then
            bash "$TRACKER" progress "$STEP_NUM" "$TEST_COUNT" "$TEST_TOTAL"
        fi
    fi
    
    # Or: "Tests  66 passed (67)"
    if [[ "$line" =~ Tests\ +([0-9]+)\ (passed|failed).*\(([0-9]+)\) ]]; then
        TEST_TOTAL=${BASH_REMATCH[3]}
        if [ "$STEP_NUM" -gt 0 ]; then
            bash "$TRACKER" progress "$STEP_NUM" "$TEST_COUNT" "$TEST_TOTAL"
        fi
    fi
}

# Get estimated test count from previous runs (optional optimization)
get_estimated_count() {
    local cache_file="diagnostics/tests/.test-count-cache"
    if [ -f "$cache_file" ]; then
        local cached=$(grep "^${COMPONENT}=" "$cache_file" 2>/dev/null | cut -d'=' -f2)
        [ -n "$cached" ] && echo "$cached" || echo "0"
    else
        echo "0"
    fi
}

# Save test count for next run
save_test_count() {
    local cache_file="diagnostics/tests/.test-count-cache"
    mkdir -p "$(dirname "$cache_file")"
    
    # Update or append
    if grep -q "^${COMPONENT}=" "$cache_file" 2>/dev/null; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/^${COMPONENT}=.*/${COMPONENT}=${TEST_TOTAL}/" "$cache_file"
        else
            sed -i "s/^${COMPONENT}=.*/${COMPONENT}=${TEST_TOTAL}/" "$cache_file"
        fi
    else
        echo "${COMPONENT}=${TEST_TOTAL}" >> "$cache_file"
    fi
}

# Main: Read stdin line by line and parse
TEST_TOTAL=$(get_estimated_count)

# Only update progress if STEP_NUM > 0 (when called from make)
if [ "$STEP_NUM" -gt 0 ]; then
    bash "$TRACKER" progress "$STEP_NUM" 0 "$TEST_TOTAL"
fi

while IFS= read -r line; do
    # Parse based on component type FIRST (to update counters and print formatted output)
    if [ "$COMPONENT" = "backend" ]; then
        parse_maven_realtime "$line"
    elif [ "$COMPONENT" = "frontend" ]; then
        parse_vitest_realtime "$line"
    fi
    
    # Filter output: only show errors and final results
    # Hide [INFO] Running (parser prints it formatted), hide warnings/DEBUG logs
    if [[ "$line" =~ ^\[ERROR\]|^\[INFO\]\ Tests\ run:|^\[INFO\]\ BUILD\ (SUCCESS|FAILURE)|FAILED ]]; then
        echo "$line"
    fi
done

# Save final count
if [ "$TEST_TOTAL" -gt 0 ]; then
    save_test_count
fi
