#!/usr/bin/env bash

# ========================================
# Story Generator Script
# ========================================
# Creates new User Story from template with automatic:
# - Story ID assignment (finds next available CORE-XXX)
# - Directory creation
# - Template copy & placeholder replacement
# - Git branch creation
#
# Usage:
#   bash scripts/backlog/new_story.sh
#   make backlog-new STORY="Feature Name"
# ========================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root (script is in scripts/backlog/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKLOG_DIR="$PROJECT_ROOT/backlog"
TEMPLATE_FILE="$BACKLOG_DIR/templates/story.md"

# ========================================
# Helper Functions
# ========================================

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Story Generator (CORE-003)${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# ========================================
# Find Next Available Story ID
# ========================================
find_next_story_id() {
    # Find all CORE-XXX directories
    local max_id=0
    
    for story_dir in "$BACKLOG_DIR"/EPIC-*/stories/CORE-*; do
        if [ -d "$story_dir" ]; then
            # Extract number from CORE-XXX
            local story_name=$(basename "$story_dir")
            local id_num=$(echo "$story_name" | grep -oE 'CORE-[0-9]+' | grep -oE '[0-9]+')
            
            if [ -n "$id_num" ] && [ "$id_num" -gt "$max_id" ]; then
                max_id=$id_num
            fi
        fi
    done
    
    # Next ID
    local next_id=$((max_id + 1))
    printf "CORE-%03d" "$next_id"
}

# ========================================
# Sanitize Title for Directory Name
# ========================================
sanitize_title() {
    local title="$1"
    # Convert to lowercase, replace spaces with hyphens, remove special chars
    echo "$title" | tr '[:upper:]' '[:lower:]' | tr ' ' '-' | sed 's/[^a-z0-9-]//g' | sed 's/--*/-/g'
}

# ========================================
# Interactive Prompts
# ========================================
prompt_user_input() {
    # Story Title
    if [ -z "$STORY_TITLE" ]; then
        read -p "Story Title (required): " STORY_TITLE
        if [ -z "$STORY_TITLE" ]; then
            print_error "Story title is required!"
            exit 1
        fi
    fi
    
    # Epic ID
    if [ -z "$EPIC_ID" ]; then
        read -p "Epic ID (default: EPIC-001-backlog-system): " EPIC_ID
        EPIC_ID=${EPIC_ID:-EPIC-001-backlog-system}
    fi
    
    # Priority
    if [ -z "$PRIORITY" ]; then
        read -p "Priority - P1/P2/P3 (default: P1): " PRIORITY
        PRIORITY=${PRIORITY:-P1}
    fi
    
    # Estimate
    if [ -z "$ESTIMATE" ]; then
        read -p "Estimate (default: 1 day): " ESTIMATE
        ESTIMATE=${ESTIMATE:-1 day}
    fi
    
    # Assignee
    if [ -z "$ASSIGNEE" ]; then
        read -p "Assignee (default: empty): " ASSIGNEE
        ASSIGNEE=${ASSIGNEE:-""}
    fi
}

# ========================================
# Validate Epic Exists
# ========================================
validate_epic() {
    local epic_dir="$BACKLOG_DIR/$EPIC_ID"
    if [ ! -d "$epic_dir" ]; then
        print_warning "Epic directory not found: $epic_dir"
        read -p "Create new epic directory? (y/n): " create_epic
        if [ "$create_epic" = "y" ]; then
            mkdir -p "$epic_dir/stories"
            print_success "Created epic directory: $epic_dir"
        else
            print_error "Cannot create story without epic directory!"
            exit 1
        fi
    fi
}

# ========================================
# Create Story Directory & Copy Template
# ========================================
create_story() {
    local story_id="$1"
    local title_slug=$(sanitize_title "$STORY_TITLE")
    local story_dir_name="${story_id}-${title_slug}"
    local story_dir="$BACKLOG_DIR/$EPIC_ID/stories/$story_dir_name"
    local story_file="$story_dir/README.md"
    
    # Create directory
    if [ -d "$story_dir" ]; then
        print_error "Story directory already exists: $story_dir"
        exit 1
    fi
    
    mkdir -p "$story_dir"
    print_success "Created directory: $story_dir"
    
    # Copy template
    if [ ! -f "$TEMPLATE_FILE" ]; then
        print_error "Template file not found: $TEMPLATE_FILE"
        exit 1
    fi
    
    cp "$TEMPLATE_FILE" "$story_file"
    print_success "Copied template to: $story_file"
    
    # Replace placeholders
    local today=$(date +%Y-%m-%d)
    
    # YAML frontmatter placeholders
    sed -i '' "s/id: CORE-XXX/id: $story_id/g" "$story_file"
    sed -i '' "s/epic: EPIC-XXX-epic-name/epic: $EPIC_ID/g" "$story_file"
    sed -i '' "s/title: \"Short Story Title\"/title: \"$STORY_TITLE\"/g" "$story_file"
    sed -i '' "s/priority: P1/priority: $PRIORITY/g" "$story_file"
    sed -i '' "s/assignee: \"\"/assignee: \"$ASSIGNEE\"/g" "$story_file"
    sed -i '' "s/created: YYYY-MM-DD/created: $today/g" "$story_file"
    sed -i '' "s/updated: YYYY-MM-DD/updated: $today/g" "$story_file"
    sed -i '' "s/estimate: \"X days\"/estimate: \"$ESTIMATE\"/g" "$story_file"
    
    # Markdown content placeholders
    sed -i '' "s/# CORE-XXX:/# $story_id:/g" "$story_file"
    sed -i '' "s/\[Story Title\]/$STORY_TITLE/g" "$story_file"
    sed -i '' "s/EPIC-XXX: Epic Name/$EPIC_ID/g" "$story_file"
    sed -i '' "s/PX/$PRIORITY/g" "$story_file"
    sed -i '' "s/\[Status\]/ready/g" "$story_file"
    sed -i '' "s/X days/$ESTIMATE/g" "$story_file"
    
    print_success "Replaced placeholders (story ID, title, dates, priority)"
    
    echo "$story_dir"
}

# ========================================
# Create Git Branch
# ========================================
create_git_branch() {
    local story_id="$1"
    local title_slug=$(sanitize_title "$STORY_TITLE")
    local branch_name="feature/${story_id}-${title_slug}"
    
    cd "$PROJECT_ROOT"
    
    # Check if branch exists
    if git rev-parse --verify "$branch_name" >/dev/null 2>&1; then
        print_warning "Git branch already exists: $branch_name"
        read -p "Checkout existing branch? (y/n): " checkout
        if [ "$checkout" = "y" ]; then
            git checkout "$branch_name"
            print_success "Checked out existing branch: $branch_name"
        fi
    else
        git checkout -b "$branch_name"
        print_success "Created and checked out Git branch: $branch_name"
    fi
}

# ========================================
# Main Execution
# ========================================
main() {
    print_header
    
    # Parse command-line arguments (for Make integration)
    while [[ $# -gt 0 ]]; do
        case $1 in
            --title)
                STORY_TITLE="$2"
                shift 2
                ;;
            --epic)
                EPIC_ID="$2"
                shift 2
                ;;
            --priority)
                PRIORITY="$2"
                shift 2
                ;;
            --estimate)
                ESTIMATE="$2"
                shift 2
                ;;
            --assignee)
                ASSIGNEE="$2"
                shift 2
                ;;
            --help)
                echo "Usage: $0 [options]"
                echo ""
                echo "Options:"
                echo "  --title TITLE       Story title (required if not interactive)"
                echo "  --epic EPIC_ID      Epic ID (default: EPIC-001-backlog-system)"
                echo "  --priority P1|P2|P3 Priority (default: P1)"
                echo "  --estimate ESTIMATE Estimate (default: 1 day)"
                echo "  --assignee NAME     Assignee name"
                echo "  --help              Show this help"
                echo ""
                echo "Interactive mode (no args): Prompts for all values"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done
    
    # Interactive prompts for missing values
    prompt_user_input
    
    # Validate epic exists
    validate_epic
    
    # Find next story ID
    STORY_ID=$(find_next_story_id)
    print_info "Assigned Story ID: $STORY_ID"
    
    # Create story
    STORY_DIR=$(create_story "$STORY_ID")
    
    # Create Git branch
    create_git_branch "$STORY_ID"
    
    # Summary
    echo ""
    print_success "Story created successfully!"
    echo ""
    echo -e "${BLUE}📋 Summary:${NC}"
    echo -e "   ID:       $STORY_ID"
    echo -e "   Title:    $STORY_TITLE"
    echo -e "   Epic:     $EPIC_ID"
    echo -e "   Priority: $PRIORITY"
    echo -e "   Estimate: $ESTIMATE"
    echo -e "   File:     $STORY_DIR/README.md"
    echo ""
    print_info "Next steps:"
    echo "   1. Edit story: vim $STORY_DIR/README.md"
    echo "   2. Fill in AC, DoD, Path Mapping"
    echo "   3. Commit: git add . && git commit -m 'feat($STORY_ID): Add story'"
    echo ""
}

# Run main
main "$@"
