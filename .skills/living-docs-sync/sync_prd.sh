#!/bin/bash
# /home/filla/.claude/skills/living-docs-sync/sync_prd.sh
# Updates PRD.md from feature implementations

set -euo pipefail

PROJECT_ROOT="${1:-$(pwd)}"
cd "$PROJECT_ROOT"

PRD_FILE="PRD.md"

# Initialize if not exists
if [ ! -f "$PRD_FILE" ]; then
    cat > "$PRD_FILE" << 'EOF'
# PRD: Magistory — AI-Powered Video Story Editor

## Objective
Build a production-ready Android video editor with AI-assisted script generation, multi-track timeline, and high-quality export.

## User Stories
- As a content creator, I want to create video stories from templates so I can produce content quickly
- As a user, I want AI to generate scripts from my ideas so I don't start from blank page
- As a creator, I want multi-track timeline editing so I can layer audio, video, and effects
- As a user, I want to export high-quality videos to share on social platforms

## Acceptance Criteria
- [ ] App launches without crashes (minSdk 28+)
- [ ] User can create new project from template
- [ ] AI generates script from prompt (Gemini integration)
- [ ] Timeline supports multi-track (video, audio, text, effects)
- [ ] Export produces 1080p/4K video < 30s for 1min content
- [ ] All quality gates pass (lint, tests, security, performance)

## Technical Requirements
- Kotlin 2.0 + Compose + Room + Media3
- MVVM + Repository + Manual DI
- Type-safe navigation (sealed routes)
- String UUID primary keys
- Secrets via secrets-gradle-plugin

## Success Metrics
- Crash-free rate: > 99.5%
- Export success rate: > 95%
- User retention D7: > 40%

## Open Questions
- Cloud sync strategy (Firebase vs custom)
- Monetization model
EOF
    echo "📝 Created initial PRD.md"
    exit 0
fi

# Detect new feature files added
NEW_FEATURES=$(git diff --name-only --diff-filter=A HEAD~1 2>/dev/null | grep -E "(screen|viewmodel|repository|entity)" | head -10)

if [ -n "$NEW_FEATURES" ]; then
    echo "📝 New feature files detected:"
    echo "$NEW_FEATURES"
    # Could enhance to auto-extract feature names and update PRD
fi

echo "📝 PRD.md checked (manual updates recommended for new features)"
