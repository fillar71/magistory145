---
name: graphify-sync
description: Incremental knowledge graph sync using graphify --update. Runs after code changes to keep the knowledge graph current with the codebase. Lightweight wrapper around graphify's built-in incremental update.
---

# Graphify Sync

## Overview

Lightweight wrapper around `graphify --update` to keep the knowledge graph synchronized with the codebase after every change. Runs incrementally — only re-extracts new/changed files, preserving cached semantic extraction for unchanged files.

## When to Use

- After any code implementation (Step 12 in workflow)
- After refactoring that changes structure
- After adding new modules/features
- After documentation updates (ADRs, specs)
- Before querying the graph (ensures freshness)

**When NOT to use:** 
- First-time graph build (use `/graphify .` directly)
- Major restructuring (may need full rebuild)
- When `graphify-out/` doesn't exist

## The Sync Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  TRIGGER: Post-implementation / Pre-query / Scheduled      │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  CHECK: Does graphify-out/graph.json exist?                │
│  → NO: Run full `/graphify .` (first build)                │
│  → YES: Continue                                           │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  RUN: graphify --update                                    │
│  • Detects changed/new/deleted files                       │
│  • Re-extracts only delta                                  │
│  • Re-clusters if structure changed                        │
│  • Updates graph.json, GRAPH_REPORT.md, analysis           │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  VERIFY: Graph health check (dangling edges, size)         │
│  → WARNING if graph shrank unexpectedly                    │
│  → WARNING if health issues detected                       │
└─────────────────────────┬───────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  REPORT: Summary of changes                                │
│  • Nodes added/removed                                      │
│  • Communities changed                                      │
│  • God nodes updated                                        │
│  • Cost (tokens) for this update                           │
└─────────────────────────────────────────────────────────────┘
```

## Command Interface

```bash
# Basic incremental update (most common)
graphify-sync

# Force full rebuild (if graph corrupted or major restructure)
graphify-sync --force

# Update specific path only
graphify-sync --path app/src/main/java/com/example/feature

# Update with deep mode (richer semantic extraction)
graphify-sync --deep

# Update and generate HTML viz
graphify-sync --html

# Update and start MCP server for agent access
graphify-sync --mcp

# Dry run - show what would be updated without doing it
graphify-sync --dry-run
```

## Implementation

### Main Sync Script

```bash
#!/bin/bash
# /home/filla/.claude/skills/graphify-sync/sync.sh

set -euo pipefail

SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${1:-.}"
FORCE="${2:-false}"
DEEP="${3:-false}"
PATH_FILTER="${4:-}"
HTML="${5:-false}"
MCP="${5:-false}"
DRY_RUN="${6:-false}"

cd "$PROJECT_ROOT"

# Check if graph exists
if [ ! -f "graphify-out/graph.json" ] || [ "$FORCE" = "true" ]; then
    echo "No existing graph or --force specified. Running full build..."
    if [ "$DRY_RUN" = "true" ]; then
        echo "[DRY RUN] Would run: /graphify ."
        exit 0
    fi
    /graphify .
    exit $?
fi

# Run incremental update
echo "Running incremental graphify update..."

UPDATE_ARGS="--update"
[ "$DEEP" = "true" ] && UPDATE_ARGS="$UPDATE_ARGS --mode deep"
[ -n "$PATH_FILTER" ] && UPDATE_ARGS="$UPDATE_ARGS $PATH_FILTER"
[ "$HTML" = "true" ] && UPDATE_ARGS="$UPDATE_ARGS --html"
[ "$MCP" = "true" ] && UPDATE_ARGS="$UPDATE_ARGS --mcp"

if [ "$DRY_RUN" = "true" ]; then
    echo "[DRY RUN] Would run: /graphify $UPDATE_ARGS"
    exit 0
fi

# Execute update
/graphify $UPDATE_ARGS

# Capture exit code
EXIT_CODE=$?

# Post-update health check
if [ $EXIT_CODE -eq 0 ] && [ -f "graphify-out/graph.json" ]; then
    NODE_COUNT=$(jq '.nodes | length' graphify-out/graph.json 2>/dev/null || echo "unknown")
    EDGE_COUNT=$(jq '.edges | length' graphify-out/graph.json 2>/dev/null || echo "unknown")
    COMMUNITIES=$(jq '.communities | length' graphify-out/graph.json 2>/dev/null || echo "unknown")
    
    echo "✅ Graph sync complete"
    echo "   Nodes: $NODE_COUNT"
    echo "   Edges: $EDGE_COUNT"
    echo "   Communities: $COMMUNITIES"
    
    # Check for warnings in GRAPH_REPORT.md
    if grep -q "GRAPH HEALTH WARNING" graphify-out/GRAPH_REPORT.md 2>/dev/null; then
        echo "⚠️  Graph health warnings detected - check GRAPH_REPORT.md"
    fi
    
    # Check for shrink warning
    if grep -q "refused to shrink" graphify-out/GRAPH_REPORT.md 2>/dev/null; then
        echo "⚠️  Graph shrink detected - may need --force rebuild"
    fi
fi

exit $EXIT_CODE
```

### Python Wrapper (Optional, for richer output)

```python
# /home/filla/.claude/skills/graphify-sync/sync.py

import subprocess
import json
import sys
from pathlib import Path
from datetime import datetime

class GraphifySync:
    def __init__(self, project_root: str = "."):
        self.project_root = Path(project_root).resolve()
        self.graphify_out = self.project_root / "graphify-out"
        
    def needs_full_build(self) -> bool:
        """Check if graph exists and is valid"""
        graph_json = self.graphify_out / "graph.json"
        if not graph_json.exists():
            return True
        try:
            data = json.loads(graph_json.read_text())
            return data.get("nodes", []) == []
        except:
            return True
    
    def run_update(self, force: bool = False, deep: bool = False, 
                   path_filter: str = "", html: bool = False, 
                   mcp: bool = False, dry_run: bool = False) -> dict:
        """Run graphify update and return structured result"""
        
        if self.needs_full_build() or force:
            cmd = ["/graphify", "."]
            if deep: cmd.append("--mode deep")
            if path_filter: cmd.append(path_filter)
            if html: cmd.append("--html")
            if mcp: cmd.append("--mcp")
            action = "full build"
        else:
            cmd = ["/graphify", "--update"]
            if deep: cmd.append("--mode deep")
            if path_filter: cmd.append(path_filter)
            if html: cmd.append("--html")
            if mcp: cmd.append("--mcp")
            action = "incremental update"
        
        if dry_run:
            return {
                "action": action,
                "command": " ".join(cmd),
                "dry_run": True,
                "timestamp": datetime.utcnow().isoformat()
            }
        
        print(f"Running {action}...")
        result = subprocess.run(cmd, cwd=self.project_root, capture_output=True, text=True)
        
        # Parse output for summary
        summary = self._parse_output(result.stdout, result.stderr)
        summary.update({
            "action": action,
            "command": " ".join(cmd),
            "exit_code": result.returncode,
            "timestamp": datetime.utcnow().isoformat(),
            "dry_run": False
        })
        
        return summary
    
    def _parse_output(self, stdout: str, stderr: str) -> dict:
        """Extract metrics from graphify output"""
        summary = {
            "nodes": "unknown",
            "edges": "unknown",
            "communities": "unknown",
            "warnings": [],
            "cost": {"input_tokens": 0, "output_tokens": 0}
        }
        
        # Parse stdout for metrics
        for line in stdout.split('\n'):
            if "Graph:" in line and "nodes" in line:
                # Graph: 1234 nodes, 5678 edges, 12 communities
                parts = line.split()
                for i, p in enumerate(parts):
                    if p.endswith("nodes,"):
                        summary["nodes"] = p.replace("nodes,", "")
                    elif p.endswith("edges,"):
                        summary["edges"] = p.replace("edges,", "")
                    elif p.endswith("communities"):
                        summary["communities"] = p.replace("communities", "")
            elif "ERROR:" in line:
                summary["warnings"].append(line.strip())
            elif "WARNING:" in line or "GRAPH HEALTH WARNING" in line:
                summary["warnings"].append(line.strip())
            elif "input tokens" in line and "output tokens" in line:
                # This run: 1,234 input tokens, 567 output tokens
                import re
                match = re.search(r'(\d+,?\d*) input tokens, (\d+,?\d*) output tokens', line)
                if match:
                    summary["cost"]["input_tokens"] = int(match.group(1).replace(",", ""))
                    summary["cost"]["output_tokens"] = int(match.group(2).replace(",", ""))
        
        return summary

def main():
    import argparse
    parser = argparse.ArgumentParser(description="Graphify incremental sync")
    parser.add_argument("--force", action="store_true", help="Force full rebuild")
    parser.add_argument("--deep", action="store_true", help="Deep extraction mode")
    parser.add_argument("--path", help="Specific path to update")
    parser.add_argument("--html", action="store_true", help="Generate HTML viz")
    parser.add_argument("--mcp", action="store_true", help="Start MCP server")
    parser.add_argument("--dry-run", action="store_true", help="Show what would run")
    parser.add_argument("--project-root", default=".", help="Project root directory")
    parser.add_argument("--json", action="store_true", help="Output JSON result")
    
    args = parser.parse_args()
    
    sync = GraphifySync(args.project_root)
    result = sync.run_update(
        force=args.force,
        deep=args.deep,
        path_filter=args.path or "",
        html=args.html,
        mcp=args.mcp,
        dry_run=args.dry_run
    )
    
    if args.json:
        print(json.dumps(result, indent=2))
    else:
        if result.get("dry_run"):
            print(f"[DRY RUN] {result['action']}: {result['command']}")
        elif result["exit_code"] == 0:
            print(f"✅ {result['action']} complete")
            print(f"   Nodes: {result['nodes']}")
            print(f"   Edges: {result['edges']}")
            print(f"   Communities: {result['communities']}")
            if result["cost"]["input_tokens"] > 0:
                print(f"   Cost: {result['cost']['input_tokens']:,} in / {result['cost']['output_tokens']:,} out tokens")
            for w in result["warnings"]:
                print(f"   ⚠️  {w}")
        else:
            print(f"❌ {result['action']} failed (exit {result['exit_code']})")
            sys.exit(result["exit_code"])

if __name__ == "__main__":
    main()
```

## Integration with Workflow

### Step 12: Knowledge Graph Sync

```markdown
# In CLAUDE.md workflow:

12️⃣ KNOWLEDGE GRAPH → **Skill: graphify-sync**
    → Runs `graphify-sync` (or `graphify --update`) automatically
    → Updates: graphify-out/graph.json, GRAPH_REPORT.md, analysis
    → Runs AFTER living-docs-sync (docs are part of corpus)
    → If graph health warnings: surface to user
    → If graph shrink detected: suggest --force rebuild
```

### Hook Integration (settings.json)

```json
{
  "hooks": {
    "Stop": [
      {
        "hooks": [
          { "type": "command", "command": "bash -c 'cd /home/filla/AndroidStudioProjects/magistory122 && /home/filla/.claude/skills/graphify-sync/sync.sh . false false \"\" false false false'" }
        ]
      }
    ]
  }
}
```

### Scheduled Sync (Cron)

```bash
# Every 30 minutes during active development
*/30 * * * * cd /home/filla/AndroidStudioProjects/magistory122 && /home/filla/.claude/skills/graphify-sync/sync.sh . false false "" false false false >> graphify-sync.log 2>&1
```

## Verification Checklist

After sync:

- [ ] `graphify-out/graph.json` exists and has nodes
- [ ] `GRAPH_REPORT.md` updated with current timestamp
- [ ] No "Graph is empty" error
- [ ] No "refused to shrink" error (or handled with --force)
- [ ] Health check passes (no dangling/missing/collapsed edges)
- [ ] God nodes reflect current architecture
- [ ] Communities match current module structure
- [ ] Cost tracker updated (cost.json)

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "Graph is static after first build" | Code evolves. Graph must evolve to stay useful for queries. |
| "--update is slow" | Incremental update is ~10x faster than full rebuild (only processes delta). |
| "I'll rebuild before querying" | Forgetting to rebuild = stale answers. Auto-sync prevents this. |
| "Only code changes matter" | Docs, ADRs, specs also change architecture understanding. Include them. |

## Red Flags

- `graphify-out/graph.json` missing or empty
- Sync runs but node count doesn't change for days (stale)
- "refused to shrink" error recurring (corrupted manifest)
- Health warnings accumulating
- God nodes showing deleted modules
- Communities not matching current package structure
- Cost.json shows 0 tokens for multiple runs (extraction skipped)

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Graph empty after update | Run `graphify-sync --force` |
| "refused to shrink" | Delete `graphify-out/` and run full build |
| Health warnings persist | Check `GRAPH_REPORT.md` for details, fix extraction |
| Sync takes too long | Use `--path` to limit scope, or exclude test/build dirs |
| MCP server won't start | Ensure `graph.json` exists and is valid JSON |
