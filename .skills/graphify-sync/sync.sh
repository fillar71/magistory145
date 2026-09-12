#!/bin/bash
# /home/filla/.claude/skills/graphify-sync/sync.sh
# Wrapper for graphify incremental update

set -euo pipefail

PROJECT_ROOT="${1:-.}"
FORCE="${2:-false}"
DEEP="${3:-false}"
PATH_FILTER="${4:-}"
HTML="${5:-false}"
MCP="${6:-false}"
DRY_RUN="${7:-false}"

cd "$PROJECT_ROOT"

# Check if graph exists
if [ ! -f "graphify-out/graph.json" ] || [ "$FORCE" = "true" ]; then
    echo "🔄 No existing graph or --force specified. Running full build..."
    if [ "$DRY_RUN" = "true" ]; then
        echo "[DRY RUN] Would run: graphify ."
        exit 0
    fi
    graphify .
    exit $?
fi

# Run incremental update
echo "🔄 Running incremental graphify update..."

UPDATE_ARGS="--update"
[ "$DEEP" = "true" ] && UPDATE_ARGS="$UPDATE_ARGS --mode deep"
[ -n "$PATH_FILTER" ] && UPDATE_ARGS="$UPDATE_ARGS $PATH_FILTER"
[ "$HTML" = "true" ] && UPDATE_ARGS="$UPDATE_ARGS --html"
[ "$MCP" = "true" ] && UPDATE_ARGS="$UPDATE_ARGS --mcp"

if [ "$DRY_RUN" = "true" ]; then
    echo "[DRY RUN] Would run: graphify $UPDATE_ARGS"
    exit 0
fi

# Execute update
graphify $UPDATE_ARGS

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
