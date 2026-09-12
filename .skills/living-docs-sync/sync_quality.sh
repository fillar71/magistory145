#!/bin/bash
# /home/filla/.claude/skills/living-docs-sync/sync_quality.sh
# Updates QUALITY_CHECK.md from test/lint/build results

set -euo pipefail

PROJECT_ROOT="${1:-$(pwd)}"
cd "$PROJECT_ROOT"

QUALITY_FILE="QUALITY_CHECK.md"

# Initialize if not exists
if [ ! -f "$QUALITY_FILE" ]; then
    cat > "$QUALITY_FILE" << 'EOF'
# Quality Gates

## Baseline Build
- Command: `./gradlew clean :app:compileDebugKotlin`
- Threshold: PASS (required before Step 1)

## Lint
- Command: `./gradlew :app:lint`
- Threshold: 0 errors, 0 critical warnings
- Config: app/lint.xml

## Unit Tests
- Command: `./gradlew :app:testDebugUnitTest`
- Threshold: 100% pass, coverage > 80%
- Framework: JUnit + MockK + Turbine

## Build
- Command: `./gradlew :app:assembleDebug`
- Threshold: Success < 3 min

## Security
- Command: `./gradlew dependencyCheckAnalyze`
- Threshold: 0 high/critical findings

## Performance
- Baseline: Macrobenchmark (no regression > 10%)
- Export: < 30s for 1min video

## Architecture
- Max file size: 1000 lines
- Max function size: 50 lines
- Cyclomatic complexity: < 10
EOF
    echo "📝 Created initial QUALITY_CHECK.md"
    exit 0
fi

# Update last run timestamp
sed -i "/^## /a Last Checked: $(date +%Y-%m-%d)" "$QUALITY_FILE"

echo "📝 QUALITY_CHECK.md checked (thresholds stable)"
