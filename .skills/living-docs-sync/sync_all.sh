#!/bin/bash
# /home/filla/.claude/skills/living-docs-sync/sync_all.sh
# Master sync script - runs all living docs sync operations

set -euo pipefail

SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${1:-$(pwd)}"

cd "$PROJECT_ROOT"

echo "🔄 Running living-docs-sync..."

# Run individual sync scripts
"$SKILL_DIR/sync_progress.sh" "$PROJECT_ROOT"
"$SKILL_DIR/sync_prd.sh" "$PROJECT_ROOT"
"$SKILL_DIR/sync_quality.sh" "$PROJECT_ROOT"
"$SKILL_DIR/sync_claude.sh" "$PROJECT_ROOT"
"$SKILL_DIR/sync_changelog.sh" "$PROJECT_ROOT"

echo "✅ Living docs sync complete"
