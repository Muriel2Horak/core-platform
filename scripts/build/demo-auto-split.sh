#!/usr/bin/env bash
# Demo auto-split with simulated build

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create a simple test command
TEST_CMD="bash $SCRIPT_DIR/demo-fixed-panel.sh"

# Run with auto-split
bash "$SCRIPT_DIR/auto-split.sh" "$TEST_CMD"
