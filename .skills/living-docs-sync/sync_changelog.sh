#!/bin/bash
# /home/filla/.claude/skills/living-docs-sync/sync_changelog.sh
# Generates CHANGELOG.md from git commits

set -euo pipefail

PROJECT_ROOT="${1:-$(pwd)}"
cd "$PROJECT_ROOT"

CHANGELOG_FILE="CHANGELOG.md"

# Initialize if not exists
if [ ! -f "$CHANGELOG_FILE" ]; then
    cat > "$CHANGELOG_FILE" << 'EOF'
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

EOF
    echo "📝 Created initial CHANGELOG.md"
    exit 0
fi

# Get recent commits (last 20)
COMMITS=$(git log --oneline -20 --pretty=format:"%s" 2>/dev/null)

if [ -z "$COMMITS" ]; then
    echo "📝 No commits to process"
    exit 0
fi

# Check if we already have an Unreleased section with content
if grep -q "## \[Unreleased\]" "$CHANGELOG_FILE"; then
    # Check if there's content after Unreleased
    if ! grep -A 20 "## \[Unreleased\]" "$CHANGELOG_FILE" | grep -q "^- "; then
        echo "📝 Unreleased section exists but empty, will populate"
    else
        echo "📝 Unreleased section already has content"
        exit 0
    fi
fi

# Parse commits by type
FEAT=$(echo "$COMMITS" | grep -i "^feat" | sed 's/^feat(\([^)]*\))?: /- /' | sed 's/^feat: /- /')
FIX=$(echo "$COMMITS" | grep -i "^fix" | sed 's/^fix(\([^)]*\))?: /- /' | sed 's/^fix: /- /')
REFACTOR=$(echo "$COMMITS" | grep -i "^refactor" | sed 's/^refactor(\([^)]*\))?: /- /' | sed 's/^refactor: /- /')
PERF=$(echo "$COMMITS" | grep -i "^perf" | sed 's/^perf(\([^)]*\))?: /- /' | sed 's/^perf: /- /')
SECURITY=$(echo "$COMMITS" | grep -i "^security" | sed 's/^security(\([^)]*\))?: /- /' | sed 's/^security: /- /')
DOCS=$(echo "$COMMITS" | grep -i "^docs" | sed 's/^docs(\([^)]*\))?: /- /' | sed 's/^docs: /- /')
CHORE=$(echo "$COMMITS" | grep -i "^chore\|^build\|^ci" | sed 's/^chore(\([^)]*\))?: /- /' | sed 's/^chore: /- /' | sed 's/^build: /- /' | sed 's/^ci: /- /')

# Build new Unreleased section
NEW_CONTENT="## [Unreleased]\n"

[ -n "$FEAT" ] && NEW_CONTENT="${NEW_CONTENT}\n### Added\n$FEAT\n"
[ -n "$FIX" ] && NEW_CONTENT="${NEW_CONTENT}\n### Fixed\n$FIX\n"
[ -n "$REFACTOR" ] && NEW_CONTENT="${NEW_CONTENT}\n### Changed\n$REFACTOR\n"
[ -n "$PERF" ] && NEW_CONTENT="${NEW_CONTENT}\n### Improved\n$PERF\n"
[ -n "$SECURITY" ] && NEW_CONTENT="${NEW_CONTENT}\n### Security\n$SECURITY\n"
[ -n "$DOCS" ] && NEW_CONTENT="${NEW_CONTENT}\n### Documentation\n$DOCS\n"
[ -n "$CHORE" ] && NEW_CONTENT="${NEW_CONTENT}\n### Maintenance\n$CHORE\n"

# Replace Unreleased section
# Use a temp file for safety
TEMP_FILE=$(mktemp)
awk -v new_content="$NEW_CONTENT" '
    /^## \[Unreleased\]/ {
        print new_content
        in_unreleased=1
        next
    }
    in_unreleased && /^## \[/ {
        in_unreleased=0
    }
    !in_unreleased {
        print
    }
' "$CHANGELOG_FILE" > "$TEMP_FILE"

mv "$TEMP_FILE" "$CHANGELOG_FILE"

echo "📝 Updated CHANGELOG.md with recent commits"
