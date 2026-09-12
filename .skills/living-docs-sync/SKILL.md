---
name: living-docs-sync
description: Auto-sync living documentation (Progress.md, PRD.md, QUALITY_CHECK.md, CLAUDE.md, CHANGELOG.md) after every code change. Extracts intent from commits, updated files, and test results to keep docs current.
---

# Living Docs Sync

## Overview

Automatically maintain living documentation that evolves with the codebase. After every implementation, this skill updates:
- **Progress.md** — Feature checklist, status, blockers
- **PRD.md** — Requirements, user stories, acceptance criteria
- **QUALITY_CHECK.md** — Quality gates, thresholds, checklists
- **CLAUDE.md** — Patterns, conventions, tech stack decisions
- **CHANGELOG.md** — Version history, breaking changes, migrations

## When to Use

- After completing any feature implementation (Step 11 in workflow)
- After bug fixes that change behavior
- After refactoring that introduces new patterns
- After architectural decisions (ADR creation)
- After quality gate threshold changes
- Before release (changelog generation)

**When NOT to use:** Pure documentation-only changes, typo fixes in docs.

## The Sync Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  INPUT: Git diff + Test results + Task completion status   │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  EXTRACT: What changed? (features, bugs, patterns, config) │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  UPDATE: Target docs with extracted info                    │
│  • Progress.md → checkboxes, status, blockers              │
│  • PRD.md → new stories, updated acceptance criteria       │
│  • QUALITY_CHECK.md → new thresholds, new checks           │
│  • CLAUDE.md → new patterns, conventions, decisions        │
│  • CHANGELOG.md → version entry (if releasing)             │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  VERIFY: Docs are consistent, no conflicts, valid markdown │
└─────────────────────────────────────────────────────────────┘
```

## Input Sources

| Source | Extracted Info | Target Docs |
|--------|----------------|-------------|
| `git diff HEAD~1` | New files, modified files, deleted files | Progress.md, PRD.md, CHANGELOG.md |
| `git log --oneline -10` | Commit messages (type: feat/fix/refactor) | CHANGELOG.md, Progress.md |
| Test results (`./gradlew test`) | Pass/fail, coverage, flaky tests | QUALITY_CHECK.md |
| Lint results (`./gradlew lint`) | Errors, warnings, new rules | QUALITY_CHECK.md |
| Build results | Success/failure, timing | QUALITY_CHECK.md |
| Task completion (tasks/todo.md) | Done/In Progress/Blocked | Progress.md |
| ADR files (docs/decisions/) | New architectural decisions | CLAUDE.md, PRD.md |

## Document Templates

### Progress.md Format

```markdown
# Progress — [App Name]

## ✅ Done
- [x] Project setup (Gradle, Room, Navigation, Compose)
- [x] Auth flow (Login, Register, Forgot Password)
- [x] Home screen + Project list

## 🚧 In Progress
- [ ] Video Editor — Timeline component (50%)
- [ ] AI Script Generation — Gemini integration (80%)

## 📋 Backlog
- [ ] Export to Gallery
- [ ] Cloud sync
- [ ] Push notifications

## 🚫 Blockers
- Media3 Transformer export fails on API 28 (investigating)

## 📊 Metrics
- Test Coverage: 82%
- Lint Errors: 0
- Build Time: 2m 15s
- Last Updated: 2026-08-10
```

### PRD.md Format

```markdown
# PRD: [Feature Name]

## Objective
[What we're building and why]

## User Stories
- As a [user], I want to [action] so that [benefit]

## Acceptance Criteria
- [ ] [Specific, testable condition]
- [ ] [Specific, testable condition]

## Technical Requirements
- [Requirement 1]
- [Requirement 2]

## Dependencies
- [Dep 1]
- [Dep 2]

## Success Metrics
- [Metric 1: target]
- [Metric 2: target]

## Open Questions
- [Question needing resolution]
```

### QUALITY_CHECK.md Format

```markdown
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
```

### CLAUDE.md Update Patterns

```markdown
# When to update CLAUDE.md:
- New pattern discovered (e.g., "Use X for Y")
- Convention changed (e.g., "Navigation now uses sealed class")
- Tech stack decision (e.g., "Added Coil 2.7 for images")
- Quality gate added/changed
- Project structure changed

# Format: Add to relevant section with date and context
## 📁 PROJECT STRUCTURE TEMPLATE (UPDATED 2026-08-10)
### New: ui/theme/tokens/ for design tokens
```

### CHANGELOG.md Format

```markdown
# Changelog

## [Unreleased]

## [1.2.0] - 2026-08-10
### Added
- Video export to Gallery (MediaStore API)
- AI Script Generation with Gemini 1.5 Flash
- Timeline multi-track editing

### Fixed
- Export crash on API 28 (Media3 Transformer workaround)
- Memory leak in EditorPreview (ExoPlayer release)

### Changed
- Navigation: Type-safe sealed routes (was string-based)
- DI: Manual Factory pattern (removed Hilt)

### Security
- Network Security Config: cleartextTrafficPermitted=false
- Certificate pinning for production API endpoints
```

## Implementation

### Sync Trigger (Post-Implementation)

```bash
# 1. Detect what changed
CHANGED_FILES=$(git diff --name-only HEAD~1)
COMMIT_MSG=$(git log -1 --pretty=%B)

# 2. Determine doc updates needed
# - New feature files → Progress.md + PRD.md + CHANGELOG.md
# - Test files → QUALITY_CHECK.md (coverage)
# - Config files (build.gradle, lint.xml) → QUALITY_CHECK.md
# - Architecture docs (docs/decisions/) → CLAUDE.md + PRD.md
# - Theme/design files → CLAUDE.md (design tokens section)

# 3. Run sync for each target
python3 sync_progress.py --changed "$CHANGED_FILES" --commit "$COMMIT_MSG"
python3 sync_prd.py --changed "$CHANGED_FILES"
python3 sync_quality.py --test-results test-results.xml --lint-results lint-results.xml
python3 sync_claude.py --changed "$CHANGED_FILES" --adrs docs/decisions/
python3 sync_changelog.py --commits 10 --version-next
```

### Sync Scripts (Create in skill folder)

```
living-docs-sync/
├── SKILL.md
├── sync_progress.py      # Updates Progress.md from git diff + tasks
├── sync_prd.py           # Updates PRD.md from feature files
├── sync_quality.py       # Updates QUALITY_CHECK.md from test/lint results
├── sync_claude.py        # Updates CLAUDE.md from patterns + ADRs
├── sync_changelog.py     # Generates CHANGELOG.md from commits
├── templates/
│   ├── progress.md.tmpl
│   ├── prd.md.tmpl
│   ├── quality_check.md.tmpl
│   ├── claude.md.tmpl
│   └── changelog.md.tmpl
└── references/
    └── sync-patterns.md
```

### sync_progress.py Logic

```python
# Input: changed_files, commit_msg, tasks/todo.md
# Output: Updated Progress.md

def update_progress(changed_files, commit_msg, tasks_path):
    # Parse tasks/todo.md for done/in-progress/backlog
    # Categorize changed_files by feature area
    # Update checkboxes based on test results
    # Calculate completion % for in-progress items
    # Add blockers from test failures / build errors
    # Write updated Progress.md
```

### sync_changelog.py Logic

```python
# Input: last N commits, next version (semver)
# Output: CHANGELOG.md entry

def generate_changelog(commits, next_version):
    # Group commits by type (feat, fix, refactor, perf, security, docs, chore)
    # Extract scope from commit message (module/feature)
    # Generate markdown with sections: Added, Fixed, Changed, Security
    # Follow Keep a Changelog format
    # Prepend to CHANGELOG.md
```

## Integration with Workflow

### Step 11: Document (Auto-Update)

```markdown
# In CLAUDE.md workflow:

11️⃣ DOCUMENT → **Skill: living-docs-sync**
    → living-docs-sync runs automatically after build succeeds
    → Updates: Progress.md, PRD.md, QUALITY_CHECK.md, CLAUDE.md
    → If releasing: CHANGELOG.md + version bump + git tag
    → Commits docs changes with message: "docs: sync living documents [skip ci]"
```

### Hook Integration (settings.json)

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit|MultiEdit",
        "hooks": [
          { "type": "command", "command": "bash -c 'if git diff --quiet HEAD; then exit 0; else /home/filla/.claude/skills/living-docs-sync/sync_all.sh; fi'" }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          { "type": "command", "command": "bash -c '/home/filla/.claude/skills/living-docs-sync/sync_all.sh'" }
        ]
      }
    ]
  }
}
```

## Verification Checklist

After sync:

- [ ] Progress.md checkboxes match task completion
- [ ] PRD.md has all new user stories with acceptance criteria
- [ ] QUALITY_CHECK.md thresholds match current CI config
- [ ] CLAUDE.md reflects latest patterns/conventions
- [ ] CHANGELOG.md has entry for unreleased changes
- [ ] All docs have valid markdown (no broken links)
- [ ] No merge conflicts in docs
- [ ] `graphify update .` runs after docs sync

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "Docs are outdated anyway" | Living docs are only useful if current. Auto-sync prevents drift. |
| "I'll update docs manually" | Manual updates are forgotten. Automation ensures consistency. |
| "Changelog is for releases only" | Unreleased section helps track what's coming. |
| "CLAUDE.md is static" | Conventions evolve. CLAUDE.md must track the current truth. |
| "Progress.md is duplicate of GitHub Issues" | Progress.md is for agent context, not project management. |

## Red Flags

- Progress.md shows 100% done but tests failing
- PRD.md missing acceptance criteria for new features
- QUALITY_CHECK.md thresholds don't match CI config
- CLAUDE.md references deleted patterns/files
- CHANGELOG.md missing entries for merged features
- Docs sync runs but produces no changes (stale)
