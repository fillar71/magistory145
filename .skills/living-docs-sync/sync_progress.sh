#!/bin/bash
# /home/filla/.claude/skills/living-docs-sync/sync_progress.sh
# Updates Progress.md from git changes and task status

set -euo pipefail

PROJECT_ROOT="${1:-$(pwd)}"
cd "$PROJECT_ROOT"

PROGRESS_FILE="Progress.md"

# Initialize if not exists
if [ ! -f "$PROGRESS_FILE" ]; then
    cat > "$PROGRESS_FILE" << 'EOF'
# Progress — Magistory

## ✅ Done
- [x] Project setup (Gradle, Room, Navigation, Compose)

## 🚧 In Progress
- [ ] Initial features

## 📋 Backlog
- [ ] Feature backlog

## 🚫 Blockers
- None

## 📊 Metrics
- Test Coverage: 0%
- Lint Errors: 0
- Build Time: N/A
- Last Updated: $(date +%Y-%m-%d)
EOF
    echo "📝 Created initial Progress.md"
    exit 0
fi

# Get recent changes
CHANGED_FILES=$(git diff --name-only HEAD~1 2>/dev/null || git ls-files --others --exclude-standard 2>/dev/null | head -20)
COMMIT_MSG=$(git log -1 --pretty=%B 2>/dev/null || echo "No commits")

# Update timestamp
sed -i "s/- Last Updated: .*/- Last Updated: $(date +%Y-%m-%d)/" "$PROGRESS_FILE"

# Extract test coverage if available
if [ -f "app/build/reports/jacoco/testDebugUnitTestCoverageReport/html/index.html" ]; then
    COVERAGE=$(grep -oP 'Total.*?(\d+)%' "app/build/reports/jacoco/testDebugUnitTestCoverageReport/html/index.html" 2>/dev/null | head -1 | grep -oP '\d+%' || echo "N/A")
    sed -i "s/- Test Coverage: .*/- Test Coverage: $COVERAGE/" "$PROGRESS_FILE"
fi

# Extract lint errors
LINT_ERRORS=$(./gradlew :app:lint --quiet 2>&1 | grep -c "Error:" || echo "0")
sed -i "s/- Lint Errors: .*/- Lint Errors: $LINT_ERRORS/" "$PROGRESS_FILE"

# Extract build time from last build
BUILD_TIME=$(grep -oP 'BUILD SUCCESSFUL in \K[\ds]+' "$PROJECT_ROOT/.gradle/build-time.log" 2>/dev/null || echo "N/A")
sed -i "s/- Build Time: .*/- Build Time: $BUILD_TIME/" "$PROGRESS_FILE"

echo "📝 Updated Progress.md"
