#!/bin/bash
# /home/filla/.claude/skills/living-docs-sync/sync_claude.sh
# Updates CLAUDE.md from new patterns and ADRs

set -euo pipefail

PROJECT_ROOT="${1:-$(pwd)}"
cd "$PROJECT_ROOT"

CLAUDE_FILE="CLAUDE.md"
ADR_DIR="docs/decisions"

# Check for new ADRs
if [ -d "$ADR_DIR" ]; then
    NEW_ADRS=$(ls -t "$ADR_DIR"/*.md 2>/dev/null | head -5)
    if [ -n "$NEW_ADRS" ]; then
        echo "📝 New ADRs found, consider updating CLAUDE.md architecture section:"
        echo "$NEW_ADRS"
    fi
fi

# Check for new patterns in codebase
NEW_PATTERNS=$(git diff --name-only HEAD~1 2>/dev/null | xargs grep -l "NEW PATTERN\|NEW CONVENTION" 2>/dev/null | head -5)

if [ -n "$NEW_PATTERNS" ]; then
    echo "📝 New pattern files detected:"
    echo "$NEW_PATTERNS"
fi

echo "📝 CLAUDE.md checked (manual updates for new patterns/conventions)"
