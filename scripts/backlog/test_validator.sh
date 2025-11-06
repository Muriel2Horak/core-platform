#!/usr/bin/env bash
# test_validator.sh - Validates AC to Test mapping coverage
# Usage: 
#   ./test_validator.sh --story CORE-XXX [--format text|json] [--min-coverage 80]
#   ./test_validator.sh --epic EPIC-XXX [--format text|json]
#
# Checks:
# - Each AC has at least 1 test mapping
# - All test files from mappings exist
# - Reports % test coverage per AC
# - Overall story/epic test coverage

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKLOG_DIR="$PROJECT_ROOT/backlog"

# Default options
FORMAT="text"
MIN_COVERAGE=0
STORY_ID=""
EPIC_ID=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --story)
      STORY_ID="$2"
      shift 2
      ;;
    --epic)
      EPIC_ID="$2"
      shift 2
      ;;
    --format)
      FORMAT="$2"
      shift 2
      ;;
    --min-coverage)
      MIN_COVERAGE="$2"
      shift 2
      ;;
    --help|-h)
      cat << EOF
test_validator.sh - AC to Test Mapping Coverage Validator

USAGE:
  ./test_validator.sh --story CORE-XXX [options]
  ./test_validator.sh --epic EPIC-XXX [options]

OPTIONS:
  --story CORE-XXX       Validate single story
  --epic EPIC-XXX        Validate all stories in epic
  --format text|json     Output format (default: text)
  --min-coverage NN      Fail if coverage < NN% (default: 0)
  --help, -h             Show this help

EXAMPLES:
  # Validate single story with text output
  ./test_validator.sh --story CORE-005

  # Validate epic with JSON output
  ./test_validator.sh --epic EPIC-001-backlog-system --format json

  # Require 80% minimum coverage
  ./test_validator.sh --story CORE-006 --min-coverage 80

EXIT CODES:
  0 - Success (coverage >= min-coverage)
  1 - Validation failed (coverage < min-coverage or errors)
  2 - Invalid arguments

VALIDATION CHECKS:
  ✅ Each AC has test mapping section
  ✅ At least 1 test type per AC (Unit/Integration/E2E)
  ✅ Test files exist on disk
  📊 Coverage percentage per AC
  📊 Overall story coverage

OUTPUT EXAMPLE (text):
  CORE-005: Git Commit Tracker
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  AC1: Track commits per story
    Unit Test: scripts/backlog/test_git_tracker.sh ✅ EXISTS
    Coverage: 33% (1/3 test types) ⚠️
  
  AC2: Generate commit report
    Unit Test: [NOT DEFINED] ❌
    Coverage: 0% (0/3 test types) ❌
  
  Overall Coverage: 16% (1/6 tests) ❌ BELOW TARGET
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

EOF
      exit 0
      ;;
    *)
      echo -e "${RED}❌ Unknown option: $1${NC}" >&2
      echo "Run with --help for usage" >&2
      exit 2
      ;;
  esac
done

# Validate arguments
if [[ -z "$STORY_ID" && -z "$EPIC_ID" ]]; then
  echo -e "${RED}❌ Error: Either --story or --epic is required${NC}" >&2
  echo "Run with --help for usage" >&2
  exit 2
fi

if [[ "$FORMAT" != "text" && "$FORMAT" != "json" ]]; then
  echo -e "${RED}❌ Error: Invalid format '$FORMAT'. Must be 'text' or 'json'${NC}" >&2
  exit 2
fi

# JSON output accumulator
JSON_OUTPUT='{"stories":[],"summary":{}}'

# Function: Find story README path
find_story_path() {
  local story_id="$1"
  
  # Search in backlog directory
  local story_path
  story_path=$(find "$BACKLOG_DIR" -type f -path "*/stories/$story_id/README.md" 2>/dev/null | head -1)
  
  if [[ -z "$story_path" ]]; then
    echo -e "${RED}❌ Error: Story $story_id not found in $BACKLOG_DIR${NC}" >&2
    return 1
  fi
  
  echo "$story_path"
}

# Function: Extract AC sections from story
parse_ac_sections() {
  local story_file="$1"
  
  # Extract AC section numbers (AC1, AC2, AC3, ...)
  grep -E "^### AC[0-9]+:" "$story_file" | sed -E 's/^### (AC[0-9]+):.*/\1/' || true
}

# Function: Extract test mapping table for given AC
extract_test_mapping() {
  local story_file="$1"
  local ac_number="$2"
  
  # Find AC mapping section (e.g., "### AC1: ... → Tests")
  # Extract table rows between that header and next ### or ---
  
  awk -v ac="$ac_number" '
    /^### / { in_section = 0 }
    $0 ~ "^### " ac ": .* → Tests" { in_section = 1; next }
    in_section && /^\|/ && !/Test Type/ { print }
    in_section && /^---/ { exit }
  ' "$story_file"
}

# Function: Parse test path from table row
parse_test_path() {
  local row="$1"
  
  # Table format: | Test Type | Test Path | Status | Coverage | Last Run | Test ID |
  # Extract 2nd column (Test Path)
  echo "$row" | awk -F '|' '{gsub(/^[ \t]+|[ \t]+$/, "", $3); print $3}'
}

# Function: Check if file exists (handle backticks)
test_file_exists() {
  local path="$1"
  
  # Remove backticks if present
  path="${path//\`/}"
  
  # Handle [NOT DEFINED] or empty paths
  if [[ "$path" == *"NOT DEFINED"* || -z "$path" || "$path" == "-" ]]; then
    return 1
  fi
  
  # Check if file exists relative to project root
  if [[ -f "$PROJECT_ROOT/$path" ]]; then
    return 0
  else
    return 1
  fi
}

# Function: Validate single story
validate_story() {
  local story_id="$1"
  
  # Find story file
  local story_file
  story_file=$(find_story_path "$story_id") || return 1
  
  local story_dir
  story_dir=$(dirname "$story_file")
  
  # Extract story title
  local story_title
  story_title=$(grep -E "^# $story_id:" "$story_file" | sed "s/^# $story_id: //" || echo "Unknown Title")
  
  if [[ "$FORMAT" == "text" ]]; then
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}📊 Test Coverage Report: $story_id${NC}"
    echo -e "${BLUE}Title: $story_title${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
  fi
  
  # Parse AC sections
  local ac_sections
  ac_sections=$(parse_ac_sections "$story_file")
  
  if [[ -z "$ac_sections" ]]; then
    if [[ "$FORMAT" == "text" ]]; then
      echo -e "${YELLOW}⚠️  No AC sections found in story${NC}"
    fi
    return 0
  fi
  
  local total_tests=0
  local passing_tests=0
  local ac_count=0
  local ac_with_tests=0
  
  # Iterate over AC sections
  while IFS= read -r ac_num; do
    ((ac_count++))
    
    # Extract AC title
    local ac_title
    ac_title=$(grep -E "^### $ac_num:" "$story_file" | sed "s/^### $ac_num: //" || echo "Unknown")
    
    if [[ "$FORMAT" == "text" ]]; then
      echo -e "${BLUE}$ac_num: $ac_title${NC}"
    fi
    
    # Extract test mapping table
    local test_rows
    test_rows=$(extract_test_mapping "$story_file" "$ac_num")
    
    if [[ -z "$test_rows" ]]; then
      if [[ "$FORMAT" == "text" ]]; then
        echo -e "  ${RED}❌ No test mapping defined${NC}"
        echo ""
      fi
      continue
    fi
    
    local ac_tests=0
    local ac_passing=0
    local has_any_test=0
    
    # Parse each test row
    while IFS= read -r row; do
      if [[ -z "$row" ]]; then continue; fi
      
      ((ac_tests++))
      ((total_tests++))
      
      # Extract test type (first column)
      local test_type
      test_type=$(echo "$row" | awk -F '|' '{gsub(/^[ \t]+|[ \t]+$/, "", $2); print $2}')
      
      # Extract test path (second column)
      local test_path
      test_path=$(parse_test_path "$row")
      
      # Check if file exists
      if test_file_exists "$test_path"; then
        ((ac_passing++))
        ((passing_tests++))
        has_any_test=1
        
        if [[ "$FORMAT" == "text" ]]; then
          echo -e "  ${GREEN}✅ $test_type: $test_path${NC}"
        fi
      else
        if [[ "$FORMAT" == "text" ]]; then
          echo -e "  ${RED}❌ $test_type: $test_path (NOT FOUND)${NC}"
        fi
      fi
    done <<< "$test_rows"
    
    # AC coverage percentage
    local ac_coverage=0
    if [[ $ac_tests -gt 0 ]]; then
      ac_coverage=$((100 * ac_passing / ac_tests))
    fi
    
    if [[ $has_any_test -eq 1 ]]; then
      ((ac_with_tests++))
    fi
    
    if [[ "$FORMAT" == "text" ]]; then
      local coverage_icon="❌"
      local coverage_color="$RED"
      
      if [[ $ac_coverage -ge 100 ]]; then
        coverage_icon="✅"
        coverage_color="$GREEN"
      elif [[ $ac_coverage -ge 50 ]]; then
        coverage_icon="⚠️ "
        coverage_color="$YELLOW"
      fi
      
      echo -e "  ${coverage_color}Coverage: $ac_coverage% ($ac_passing/$ac_tests test types) $coverage_icon${NC}"
      echo ""
    fi
    
  done <<< "$ac_sections"
  
  # Overall coverage
  local overall_coverage=0
  if [[ $total_tests -gt 0 ]]; then
    overall_coverage=$((100 * passing_tests / total_tests))
  fi
  
  local ac_coverage_pct=0
  if [[ $ac_count -gt 0 ]]; then
    ac_coverage_pct=$((100 * ac_with_tests / ac_count))
  fi
  
  # Determine status
  local status="❌ TESTS REQUIRED"
  local status_color="$RED"
  
  if [[ $overall_coverage -ge 100 ]]; then
    status="✅ COMPLETE"
    status_color="$GREEN"
  elif [[ $overall_coverage -ge $MIN_COVERAGE ]]; then
    status="⚠️  PARTIAL"
    status_color="$YELLOW"
  fi
  
  if [[ "$FORMAT" == "text" ]]; then
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}📈 Summary${NC}"
    echo -e "  Total AC: $ac_count"
    echo -e "  AC with tests: $ac_with_tests ($ac_coverage_pct%)"
    echo -e "  Total test slots: $total_tests"
    echo -e "  Tests defined: $passing_tests"
    echo -e "  ${status_color}Overall Coverage: $overall_coverage% $status${NC}"
    
    if [[ $overall_coverage -lt $MIN_COVERAGE ]]; then
      echo -e "  ${RED}⚠️  Below minimum required coverage: $MIN_COVERAGE%${NC}"
    fi
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
  else
    # JSON output
    local story_json
    story_json=$(cat <<EOF
{
  "id": "$story_id",
  "title": "$story_title",
  "ac_count": $ac_count,
  "ac_with_tests": $ac_with_tests,
  "ac_coverage_pct": $ac_coverage_pct,
  "total_tests": $total_tests,
  "passing_tests": $passing_tests,
  "overall_coverage": $overall_coverage,
  "status": "$(if [[ $overall_coverage -ge 100 ]]; then echo "complete"; elif [[ $overall_coverage -ge $MIN_COVERAGE ]]; then echo "partial"; else echo "incomplete"; fi)",
  "meets_minimum": $(if [[ $overall_coverage -ge $MIN_COVERAGE ]]; then echo "true"; else echo "false"; fi)
}
EOF
)
    JSON_OUTPUT=$(echo "$JSON_OUTPUT" | jq ".stories += [$story_json]")
  fi
  
  # Return exit code based on coverage
  if [[ $overall_coverage -lt $MIN_COVERAGE ]]; then
    return 1
  else
    return 0
  fi
}

# Function: Validate epic (all stories)
validate_epic() {
  local epic_id="$1"
  
  # Find epic directory
  local epic_dir="$BACKLOG_DIR/$epic_id"
  
  if [[ ! -d "$epic_dir" ]]; then
    echo -e "${RED}❌ Error: Epic $epic_id not found in $BACKLOG_DIR${NC}" >&2
    return 1
  fi
  
  # Find all stories in epic
  local story_dirs
  story_dirs=$(find "$epic_dir/stories" -maxdepth 1 -mindepth 1 -type d 2>/dev/null || true)
  
  if [[ -z "$story_dirs" ]]; then
    echo -e "${YELLOW}⚠️  No stories found in epic $epic_id${NC}"
    return 0
  fi
  
  if [[ "$FORMAT" == "text" ]]; then
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}📊 Epic Test Coverage Report: $epic_id${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  fi
  
  local all_passed=0
  local story_count=0
  
  # Validate each story
  while IFS= read -r story_dir; do
    local story_id
    story_id=$(basename "$story_dir")
    
    ((story_count++))
    
    if validate_story "$story_id"; then
      ((all_passed++))
    fi
  done <<< "$story_dirs"
  
  # Epic summary
  local epic_coverage=0
  if [[ $story_count -gt 0 ]]; then
    epic_coverage=$((100 * all_passed / story_count))
  fi
  
  if [[ "$FORMAT" == "text" ]]; then
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}📈 Epic Summary${NC}"
    echo -e "  Total stories: $story_count"
    echo -e "  Stories meeting coverage: $all_passed"
    
    if [[ $all_passed -eq $story_count ]]; then
      echo -e "  ${GREEN}✅ Epic test coverage: 100%${NC}"
    else
      echo -e "  ${YELLOW}⚠️  Epic test coverage: $epic_coverage%${NC}"
    fi
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
  else
    # JSON summary
    JSON_OUTPUT=$(echo "$JSON_OUTPUT" | jq ".summary = {
      \"epic_id\": \"$epic_id\",
      \"total_stories\": $story_count,
      \"stories_meeting_coverage\": $all_passed,
      \"epic_coverage_pct\": $epic_coverage
    }")
  fi
  
  if [[ $all_passed -eq $story_count ]]; then
    return 0
  else
    return 1
  fi
}

# Main execution
EXIT_CODE=0

if [[ -n "$STORY_ID" ]]; then
  # Validate single story
  validate_story "$STORY_ID" || EXIT_CODE=1
elif [[ -n "$EPIC_ID" ]]; then
  # Validate epic
  validate_epic "$EPIC_ID" || EXIT_CODE=1
fi

# Output JSON if requested
if [[ "$FORMAT" == "json" ]]; then
  echo "$JSON_OUTPUT" | jq '.'
fi

exit $EXIT_CODE
