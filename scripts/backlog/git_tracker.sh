#!/bin/bash
# ============================================================================
# Git Commit Tracker for Backlog Stories (CORE-005)
# ============================================================================
# 
# Tracks Git commits mapped to backlog stories (CORE-XXX references)
# Generates reports showing commit counts per story
#
# Usage:
#   bash scripts/backlog/git_tracker.sh --epic EPIC-001
#   bash scripts/backlog/git_tracker.sh --story CORE-003
#   bash scripts/backlog/git_tracker.sh --epic EPIC-001 --format json
#
# Author: GitHub Copilot
# Story: CORE-005
# Epic: EPIC-001-backlog-system
# ============================================================================

set -euo pipefail

# ============================================================================
# CONFIGURATION
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKLOG_DIR="$PROJECT_ROOT/backlog"

# Default values
EPIC=""
STORY=""
FORMAT="text"
SHOW_ZERO=false

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

# Print usage information
usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Track Git commits for backlog stories.

OPTIONS:
    --epic EPIC-XXX         Filter by epic (e.g., EPIC-001)
    --story CORE-XXX        Filter by specific story (e.g., CORE-003)
    --format FORMAT         Output format: text (default) or json
    --show-zero             Include stories with 0 commits
    --help                  Show this help message

EXAMPLES:
    # Show all commits for EPIC-001
    $(basename "$0") --epic EPIC-001

    # Show commits for specific story
    $(basename "$0") --story CORE-003

    # JSON output for automation
    $(basename "$0") --epic EPIC-001 --format json

    # Show all stories including those without commits
    $(basename "$0") --epic EPIC-001 --show-zero

EOF
    exit 0
}

# Print error and exit
error() {
    echo "❌ Error: $1" >&2
    exit 1
}

# Print info message
info() {
    echo "ℹ️  $1"
}

# Print success message
success() {
    echo "✅ $1"
}

# ============================================================================
# VALIDATION FUNCTIONS
# ============================================================================

# Validate epic exists in backlog directory
validate_epic() {
    local epic="$1"
    
    if [[ ! -d "$BACKLOG_DIR/$epic" ]]; then
        error "Epic '$epic' not found in $BACKLOG_DIR/"
    fi
}

# Validate story exists
validate_story() {
    local story="$1"
    
    # Find story in any epic directory
    local found=false
    for epic_dir in "$BACKLOG_DIR"/EPIC-*/stories/"$story"-*/; do
        if [[ -d "$epic_dir" ]]; then
            found=true
            break
        fi
    done
    
    if [[ "$found" == "false" ]]; then
        error "Story '$story' not found in backlog"
    fi
}

# Validate format option
validate_format() {
    local format="$1"
    
    if [[ "$format" != "text" && "$format" != "json" ]]; then
        error "Invalid format '$format'. Use 'text' or 'json'"
    fi
}

# ============================================================================
# ARGUMENT PARSING
# ============================================================================

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --epic)
                EPIC="$2"
                shift 2
                ;;
            --story)
                STORY="$2"
                shift 2
                ;;
            --format)
                FORMAT="$2"
                shift 2
                ;;
            --show-zero)
                SHOW_ZERO=true
                shift
                ;;
            --help|-h)
                usage
                ;;
            *)
                error "Unknown option: $1. Use --help for usage."
                ;;
        esac
    done
    
    # Validate required arguments
    if [[ -z "$EPIC" && -z "$STORY" ]]; then
        error "Either --epic or --story must be specified. Use --help for usage."
    fi
    
    # Validate format
    validate_format "$FORMAT"
    
    # Validate epic if specified
    if [[ -n "$EPIC" ]]; then
        validate_epic "$EPIC"
    fi
    
    # Validate story if specified
    if [[ -n "$STORY" ]]; then
        validate_story "$STORY"
    fi
}

# ============================================================================
# GIT LOG PARSING FUNCTIONS
# ============================================================================

# Parse git log for CORE-XXX references
# Supports patterns: feat(CORE-XXX), fix(CORE-XXX), chore(CORE-XXX), docs(CORE-XXX)
# Also handles multi-story commits: feat(CORE-001,CORE-003)
parse_commits() {
    local pattern="${1:-CORE-[0-9]+}"
    
    # Git log with oneline format, search for CORE-XXX pattern
    # Format: <SHA> <commit message>
    git -C "$PROJECT_ROOT" log --all --oneline \
        | grep -E "(feat|fix|chore|docs|test|refactor)\(.*$pattern" || true
}

# Extract story IDs from commit message
# Handles: feat(CORE-001), docs(CORE-001,CORE-003), etc.
extract_story_ids() {
    local commit_message="$1"
    
    # Extract all CORE-XXX patterns from the commit message
    echo "$commit_message" | grep -oE "CORE-[0-9]+" | sort -u
}

# Get all commits for a specific story
get_commits_for_story() {
    local story_id="$1"
    
    # Parse all commits and filter by story ID
    parse_commits | while IFS= read -r line; do
        local sha="${line%% *}"
        local message="${line#* }"
        
        # Check if this commit references the story
        if echo "$message" | grep -qE "\b$story_id\b"; then
            echo "$line"
        fi
    done
}

# Get all unique story IDs from git log
get_all_story_ids() {
    local epic_pattern="${1:-}"
    
    # Parse all commits and extract unique story IDs
    parse_commits | while IFS= read -r line; do
        extract_story_ids "$line"
    done | sort -u
}

# ============================================================================
# COUNTING & REPORTING FUNCTIONS
# ============================================================================

# Count commits per story ID
count_commits_per_story() {
    local story_id="$1"
    
    # Get all commits for this story and count them
    get_commits_for_story "$story_id" | wc -l | xargs
}

# Get commit SHAs for a story
get_commit_shas() {
    local story_id="$1"
    
    # Get commits and extract just the SHA (first field)
    get_commits_for_story "$story_id" | awk '{print $1}'
}

# Determine story status emoji
get_story_status() {
    local commit_count="$1"
    
    if [[ "$commit_count" -eq 0 ]]; then
        echo "📋"  # No commits
    elif [[ "$commit_count" -ge 1 ]]; then
        echo "✅"  # Has commits
    fi
}

# Generate text report
generate_text_report() {
    local epic_filter="$1"
    local story_filter="$2"
    
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    if [[ -n "$story_filter" ]]; then
        echo "📊 Git Activity Report: $story_filter"
    elif [[ -n "$epic_filter" ]]; then
        echo "📊 Git Activity Report: $epic_filter"
    else
        echo "📊 Git Activity Report: All Stories"
    fi
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # Get story IDs (filtered or all)
    local story_ids
    if [[ -n "$story_filter" ]]; then
        story_ids="$story_filter"
    else
        story_ids=$(get_all_story_ids)
    fi
    
    if [[ -z "$story_ids" ]]; then
        echo "No stories found with Git commits."
        echo ""
        return
    fi
    
    local total_commits=0
    local total_stories=0
    
    # Process each story
    while IFS= read -r story_id; do
        local count
        count=$(count_commits_per_story "$story_id")
        
        # Skip zero-commit stories unless --show-zero
        if [[ "$count" -eq 0 && "$SHOW_ZERO" == "false" ]]; then
            continue
        fi
        
        total_stories=$((total_stories + 1))
        total_commits=$((total_commits + count))
        
        local status
        status=$(get_story_status "$count")
        
        # Format: ✅ CORE-001: 2 commits
        printf "%s %-12s %2d commit(s)" "$status" "$story_id:" "$count"
        
        # Show commit SHAs
        if [[ "$count" -gt 0 ]]; then
            local shas
            shas=$(get_commit_shas "$story_id" | tr '\n' ', ' | sed 's/,$//')
            printf " (%s)" "$shas"
        fi
        
        echo ""
    done <<< "$story_ids"
    
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📈 Summary: $total_commits commits across $total_stories stories"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Generate JSON report
generate_json_report() {
    local epic_filter="$1"
    local story_filter="$2"
    
    # Get story IDs (filtered or all)
    local story_ids
    if [[ -n "$story_filter" ]]; then
        story_ids="$story_filter"
    else
        story_ids=$(get_all_story_ids)
    fi
    
    if [[ -z "$story_ids" ]]; then
        echo '{"epic":"'"${epic_filter:-all}"'","total_commits":0,"stories":[]}'
        return
    fi
    
    local json_stories="["
    local first=true
    local total_commits=0
    
    # Process each story
    while IFS= read -r story_id; do
        local count
        count=$(count_commits_per_story "$story_id")
        
        # Skip zero-commit stories unless --show-zero
        if [[ "$count" -eq 0 && "$SHOW_ZERO" == "false" ]]; then
            continue
        fi
        
        total_commits=$((total_commits + count))
        
        # Add comma separator
        if [[ "$first" == "false" ]]; then
            json_stories+=","
        fi
        first=false
        
        # Get commit SHAs as JSON array
        local shas_json="["
        local sha_first=true
        while IFS= read -r sha; do
            if [[ -n "$sha" ]]; then
                if [[ "$sha_first" == "false" ]]; then
                    shas_json+=","
                fi
                sha_first=false
                shas_json+="\"$sha\""
            fi
        done <<< "$(get_commit_shas "$story_id")"
        shas_json+="]"
        
        # Build story JSON object
        json_stories+="{\"id\":\"$story_id\",\"commits\":$count,\"shas\":$shas_json}"
    done <<< "$story_ids"
    
    json_stories+="]"
    
    # Build final JSON
    local filter_name="${story_filter:-${epic_filter:-all}}"
    echo "{\"epic\":\"$filter_name\",\"total_commits\":$total_commits,\"stories\":$json_stories}"
}

# ============================================================================
# MAIN FUNCTION
# ============================================================================

main() {
    # Parse arguments
    parse_args "$@"
    
    # Generate report based on format
    if [[ "$FORMAT" == "text" ]]; then
        generate_text_report "${EPIC:-}" "${STORY:-}"
    elif [[ "$FORMAT" == "json" ]]; then
        generate_json_report "${EPIC:-}" "${STORY:-}"
    fi
}

# Entry point
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
