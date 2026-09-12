---
name: task-orchestrator
description: Auto-routes user prompts to the appropriate skills and executes the full workflow. Detects intent, selects skills, manages dependencies, and drives the end-to-end development loop.
---

# Task Orchestrator

## Overview

The central orchestration skill that receives user prompts, detects intent, routes to appropriate skills, and executes the mandatory workflow (Steps 0-15). Replaces manual skill selection with intelligent auto-routing based on prompt analysis, codebase context, and CLAUDE.md rules.

## When to Use

- **Always** — This is the entry point for ALL user requests in this project
- User says: "Buatkan fitur X", "Perbaiki bug Y", "Refactor Z", "Tambah test untuk...", etc.
- Any implementation task, bug fix, refactor, or feature request

**When NOT to use:** Pure questions ("Apa itu MVVM?", "Bagaimana cara setup Room?"), research tasks (use `deep-research`), or when user explicitly invokes a specific skill.

## Intent Detection & Skill Routing

### Intent Classification

| User Prompt Pattern | Detected Intent | Primary Skills | Workflow Steps |
|---------------------|-----------------|----------------|----------------|
| "Buatkan aplikasi *", "Buatkan fitur *" | **NEW_FEATURE** | planning, spec, architect, database, ui, implement, test, review, security, perf, ci/cd, build, docs, graph | 0-15 (Full) |
| "Tambah fitur * ke *" | **FEATURE_ADD** | planning, spec, architect, database, ui, implement, test, review, security, perf, build, docs, graph | 0-15 (Full) |
| "Perbaiki bug *", "Fix *", "Error *" | **BUG_FIX** | debugging, spec (minimal), implement, test, review, build, docs, graph | 0,1,6,7,8,10,11,12 |
| "Refactor *", "Bersihkan *", "Simplify *" | **REFACTOR** | code-simplification, planning, implement, test, review, build, docs, graph | 0,1,6,7,8,10,11,12 |
| "Test *", "Coverage *", "TDD *" | **TESTING** | test-driven-development, planning, implement (tests), review, build, docs | 0,1,6,7,8,10,11,12 |
| "Review *", "Audit *", "Quality *" | **CODE_REVIEW** | code-review-and-quality, security-and-hardening, performance-optimization | 8,9,10,11,12 |
| "Optimasi *", "Performa *", "Lambat *" | **PERFORMANCE** | performance-optimization, profiling, implement, test, build, docs | 0,1,6,7,9,10,11,12 |
| "Security *", "Hardening *", "Vulnerability *" | **SECURITY** | security-and-hardening, review, implement, test, build, docs | 0,1,6,7,9,10,11,12 |
| "CI/CD *", "Pipeline *", "Deploy *" | **CI_CD** | ci-cd-pipeline-builder, planning, implement, test, build, docs | 0,1,6,7,8,10,11,12 |
| "Database *", "Schema *", "Migration *" | **DATABASE** | database-designer, planning, spec, implement, test, build, docs | 0-5,7,10,11,12 |
| "UI *", "Compose *", "Design *", "Component *" | **UI_UX** | frontend-ui-engineering, planning, spec, implement, test, review, build, docs | 0-5,7,8,10,11,12 |
| "API *", "Endpoint *", "Contract *" | **API** | api-design-reviewer, api-test-suite-builder, planning, spec, implement, test, build, docs | 0-5,7,8,10,11,12 |

### Skill Dependency Graph

```
                    ┌─────────────────────┐
                    │ 0. BASELINE CHECK   │
                    │ (android-cli)       │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
       ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
       │ 1. PLANNING │  │ 2. SPEC     │  │ 3. ARCH     │
       │ (planning)  │  │ (spec-dev)  │  │ (architect) │
       └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
              │                │                │
              ▼                ▼                ▼
       ┌─────────────────────────────────────────────┐
       │ 4. DATABASE (database-designer)            │
       └────────────────────┬────────────────────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
       │ 5. UI SYSTEM│ │ 6. IMPL     │ │ 7. TEST     │
       │ (frontend)  │ │ (incremental)│ │ (TDD)       │
       └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
              │             │             │
              ▼             ▼             ▼
       ┌─────────────────────────────────────────────┐
       │ 8. REVIEW (code-review)  9. SECURITY (sec) │
       │ 10. PERF (perf-opt)        11. CI/CD (ci)  │
       └────────────────────┬────────────────────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
       │ 12. BUILD   │ │ 13. DOCS    │ │ 14. GRAPH   │
       │ (android)   │ │ (living)    │ │ (graphify)  │
       └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
              │             │             │
              └─────────────┼─────────────┘
                            ▼
                   ┌─────────────────┐
                   │ 15. RELEASE     │
                   │ (release-auto)  │
                   └─────────────────┘
```

## Orchestration Logic

### Main Entry Point

```python
# /home/filla/.claude/skills/task-orchestrator/orchestrator.py

class TaskOrchestrator:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.workflow_state = {}
        self.skills_registry = self._load_skills_registry()
    
    def execute(self, user_prompt: str) -> dict:
        """Main orchestration entry point"""
        
        # 1. Detect intent
        intent = self._detect_intent(user_prompt)
        
        # 2. Determine workflow steps
        steps = self._get_workflow_steps(intent)
        
        # 3. Run baseline check (Step 0) - MANDATORY
        baseline_result = self._run_baseline_check()
        if not baseline_result["success"]:
            return self._request_user_intervention("baseline_failed", baseline_result)
        
        # 4. Execute workflow steps in order
        for step in steps:
            result = self._execute_step(step, user_prompt, intent)
            if not result["success"]:
                return self._handle_step_failure(step, result, intent)
            self.workflow_state[step["id"]] = result
        
        # 5. Final verification
        return self._final_verification()
    
    def _detect_intent(self, prompt: str) -> dict:
        """Classify user intent using keyword matching + context"""
        prompt_lower = prompt.lower()
        
        # Intent patterns (ordered by specificity)
        patterns = {
            "NEW_FEATURE": ["buatkan aplikasi", "buat aplikasi", "buatkan fitur", "buat fitur", "new feature", "create feature"],
            "FEATURE_ADD": ["tambah fitur", "add feature", "implementasikan", "implement"],
            "BUG_FIX": ["perbaiki", "fix", "bug", "error", "crash", "tidak jalan", "gagal"],
            "REFACTOR": ["refactor", "bersihkan", "simplify", "rapikan", "cleanup"],
            "TESTING": ["test", "coverage", "tdd", "unit test", "ui test"],
            "CODE_REVIEW": ["review", "audit", "quality", "kualitas"],
            "PERFORMANCE": ["optimasi", "performa", "performance", "lambat", "slow"],
            "SECURITY": ["security", "hardening", "keamanan", "vulnerability", "vuln"],
            "CI_CD": ["ci", "cd", "pipeline", "deploy", "github actions", "gitlab ci"],
            "DATABASE": ["database", "schema", "migration", "room", "entity", "dao"],
            "UI_UX": ["ui", "compose", "design", "component", "screen", "tampilan"],
            "API": ["api", "endpoint", "contract", "rest", "graphql"],
        }
        
        for intent, keywords in patterns.items():
            if any(kw in prompt_lower for kw in keywords):
                return {"primary": intent, "confidence": 0.9}
        
        # Default: treat as feature request
        return {"primary": "FEATURE_ADD", "confidence": 0.5}
    
    def _get_workflow_steps(self, intent: dict) -> list:
        """Map intent to workflow steps"""
        intent_type = intent["primary"]
        
        # Full workflow steps (all 16 steps)
        all_steps = [
            {"id": "0", "name": "baseline_check", "skill": "android-cli", "mandatory": True},
            {"id": "1", "name": "planning", "skill": "planning-and-task-breakdown", "mandatory": True},
            {"id": "2", "name": "spec", "skill": "spec-driven-development", "mandatory": True},
            {"id": "3", "name": "architecture", "skill": "senior-architect", "mandatory": True},
            {"id": "4", "name": "database", "skill": "database-designer", "mandatory": True},
            {"id": "5", "name": "ui_system", "skill": "frontend-ui-engineering", "mandatory": True},
            {"id": "6", "name": "implement", "skill": "incremental-implementation", "mandatory": True},
            {"id": "7", "name": "test", "skill": "test-driven-development", "mandatory": True},
            {"id": "8", "name": "review", "skill": "code-review-and-quality", "mandatory": True},
            {"id": "9", "name": "security", "skill": "security-and-hardening", "mandatory": True},
            {"id": "10", "name": "performance", "skill": "performance-optimization", "mandatory": True},
            {"id": "11", "name": "ci_cd", "skill": "ci-cd-pipeline-builder", "mandatory": False},
            {"id": "12", "name": "build", "skill": "android-cli", "mandatory": True},
            {"id": "13", "name": "docs", "skill": "living-docs-sync", "mandatory": True},
            {"id": "14", "name": "graph", "skill": "graphify-sync", "mandatory": True},
            {"id": "15", "name": "release", "skill": "release-automation", "mandatory": False},
        ]
        
        # Intent-specific step filtering
        if intent_type in ["BUG_FIX", "REFACTOR", "TESTING"]:
            # Skip spec, arch, database, ui for simpler tasks
            skip = {"2", "3", "4", "5", "11", "15"}
            return [s for s in all_steps if s["id"] not in skip]
        
        if intent_type in ["CODE_REVIEW", "PERFORMANCE", "SECURITY"]:
            # Skip planning, spec, arch, database, ui, implement
            skip = {"1", "2", "3", "4", "5", "6", "11", "15"}
            return [s for s in all_steps if s["id"] not in skip]
        
        if intent_type == "CI_CD":
            skip = {"2", "3", "4", "5", "6", "7", "15"}
            return [s for s in all_steps if s["id"] not in skip]
        
        if intent_type in ["DATABASE", "UI_UX", "API"]:
            # Full workflow but may skip some
            return all_steps
        
        # NEW_FEATURE, FEATURE_ADD: Full workflow
        return all_steps
    
    def _execute_step(self, step: dict, prompt: str, intent: dict) -> dict:
        """Execute a single workflow step via its skill"""
        skill_name = step["skill"]
        step_id = step["id"]
        
        print(f"\n{'='*60}")
        print(f"STEP {step_id}: {step['name'].upper()} ({skill_name})")
        print(f"{'='*60}")
        
        # Invoke skill
        try:
            if skill_name == "android-cli":
                result = self._run_android_cli(step["name"])
            elif skill_name == "planning-and-task-breakdown":
                result = self._run_planning(prompt, intent)
            elif skill_name == "spec-driven-development":
                result = self._run_spec(prompt, intent)
            # ... other skills
            else:
                result = self._invoke_skill(skill_name, prompt, intent)
            
            # Verify step completion
            if self._verify_step(step["name"], result):
                return {"success": True, "step": step_id, "result": result}
            else:
                return {"success": False, "step": step_id, "error": "Verification failed"}
                
        except Exception as e:
            return {"success": False, "step": step_id, "error": str(e)}
    
    def _run_baseline_check(self) -> dict:
        """Step 0: Baseline build check - MANDATORY"""
        print("\n🔍 STEP 0: BASELINE BUILD CHECK")
        print("Running: ./gradlew clean :app:compileDebugKotlin")
        
        result = subprocess.run(
            ["./gradlew", "clean", ":app:compileDebugKotlin"],
            cwd=self.project_root,
            capture_output=True,
            text=True,
            timeout=300
        )
        
        if result.returncode == 0:
            print("✅ Baseline build PASSED")
            return {"success": True, "output": result.stdout}
        else:
            print("❌ Baseline build FAILED")
            print(result.stderr)
            return {"success": False, "error": result.stderr, "output": result.stdout}
    
    def _request_user_intervention(self, reason: str, context: dict) -> dict:
        """Ask user for intervention (only for emergency stops)"""
        if reason == "baseline_failed":
            # This is the ONLY case we ask user per CLAUDE.md
            return {
                "success": False,
                "intervention_required": True,
                "reason": "Baseline build failed. Must fix before continuing.",
                "details": context,
                "action": "Fix build errors then re-run orchestration"
            }
        # Other emergencies: security HIGH/CRITICAL, perf regression >20%, etc.
        return {"success": False, "intervention_required": True, "reason": reason, "details": context}
    
    def _final_verification(self) -> dict:
        """Run final quality gates"""
        print("\n✅ FINAL VERIFICATION")
        
        gates = [
            ("Lint", ["./gradlew", ":app:lint"]),
            ("Unit Tests", ["./gradlew", ":app:testDebugUnitTest"]),
            ("Build", ["./gradlew", ":app:assembleDebug"]),
        ]
        
        results = {}
        for name, cmd in gates:
            result = subprocess.run(cmd, cwd=self.project_root, capture_output=True, text=True, timeout=300)
            results[name] = {"success": result.returncode == 0, "output": result.stdout[-500:]}
            status = "✅" if result.returncode == 0 else "❌"
            print(f"  {status} {name}")
        
        all_passed = all(r["success"] for r in results.values())
        
        return {
            "success": all_passed,
            "gates": results,
            "workflow_state": self.workflow_state
        }
```

## Auto-Routing Rules (Claude Decision Matrix)

### When User Says X → Orchestrator Does Y

| User Input | Orchestrator Action |
|------------|---------------------|
| "Buatkan aplikasi Toko Online" | Full workflow (0-15), creates PRD, plans sprints, implements slice by slice |
| "Tambah fitur export PDF" | Full workflow for that feature slice |
| "Perbaiki crash di EditorScreen" | Debug → minimal spec → implement fix → test → verify |
| "Refactor ProjectRepository" | Simplify → implement → test → review → build |
| "Test coverage di ViewModel" | TDD cycle for missing tests |
| "Review kode ini" | Code review + security + perf analysis |
| "Optiminasi export video" | Profile → identify bottlenecks → optimize → benchmark |
| "Setup CI/CD" | Detect stack → generate pipeline → validate → deploy config |
| "Migrasi Room ke v2" | Database designer → migration → test → verify |

## Integration with CLAUDE.md Workflow

### Updated Workflow (Steps 0-15)

```markdown
# MANDATORY WORKFLOW (UPDATED)

0️⃣ BASELINE BUILD CHECK → **Skill: android-cli** (compileDebugKotlin)
    → GAGAL? → STOP & AskUserQuestion (perbaiki dulu)
    → BERHASIL? → Lanjut ke Step 1

1️⃣ PLANNING → **Skill: planning-and-task-breakdown** (+ task-orchestrator)
    → Vertical slicing, dependency graph, checkpoints, tasks/plan.md + tasks/todo.md

2️⃣ SPEC → **Skill: spec-driven-development**
    → SPEC.md dengan success criteria + boundaries + API contracts

3️⃣ ARCHITECTURE → **Skill: senior-architect**
    → project_architect.py assessment + ADR generation

4️⃣ DATABASE → **Skill: database-designer**
    → schema_analyzer.py + index_optimizer.py + migration_generator.py

5️⃣ UI SYSTEM → **Skill: frontend-ui-engineering**
    → Design tokens + Component library + A11y checklist

6️⃣ IMPLEMENT → **Skill: incremental-implementation**
    → Slice by slice (vertical), simplicity checks, commit per slice

7️⃣ TEST → **Skill: test-driven-development**
    → RED-GREEN-REFACTOR + Prove-It pattern + Fake repos + Turbine

8️⃣ REVIEW → **Skill: code-review-and-quality**
    → 5-axis review + structural remedies + dead code hygiene

9️⃣ SECURITY → **Skill: security-and-hardening**
    → Threat model (STRIDE) + OWASP patterns + Dependency audit

🔟 PERFORMANCE → **Skill: performance-optimization**
    → Measure-first (Lighthouse/RUM) + Core Web Vitals + bottlenecks

1️⃣1️⃣ CI/CD → **Skill: ci-cd-pipeline-builder**
    → stack_detector.py → pipeline_generator.py → validate

1️⃣2️⃣ BUILD → **Skill: android-cli**
    → assembleDebug + lint + test + device testing

1️⃣3️⃣ DOCS → **Skill: living-docs-sync**
    → Auto-update: Progress.md, PRD.md, QUALITY_CHECK.md, CLAUDE.md, CHANGELOG.md

1️⃣4️⃣ GRAPH → **Skill: graphify-sync**
    → graphify --update (incremental knowledge graph sync)

1️⃣5️⃣ RELEASE → **Skill: release-automation** (when releasing)
    → Semver bump + Changelog gen + Git tag + Release notes + Bundle signing
```

### Skill Invocation in Orchestrator

```python
# Each step invokes its skill with context
async def invoke_skill(self, skill_name: str, context: dict) -> dict:
    """Invoke a skill with the current context"""
    
    # Build skill-specific prompt from context
    prompt = self._build_skill_prompt(skill_name, context)
    
    # Use Agent tool to run skill
    result = await agent(
        prompt=prompt,
        opts={
            "label": skill_name,
            "phase": context.get("current_step"),
            "schema": self._get_skill_schema(skill_name)
        }
    )
    
    return result
```

## Verification Checklist

After orchestration:

- [ ] Baseline build passed (Step 0)
- [ ] Tasks created in tasks/todo.md (Step 1)
- [ ] SPEC.md exists with acceptance criteria (Step 2)
- [ ] Architecture decisions documented (Step 3)
- [ ] Database schema updated with migrations (Step 4)
- [ ] Design tokens + components created (Step 5)
- [ ] Implementation complete with vertical slices (Step 6)
- [ ] Tests passing (>80% coverage) (Step 7)
- [ ] Code review passed (0 critical issues) (Step 8)
- [ ] Security scan clean (Step 9)
- [ ] Performance baseline met (Step 10)
- [ ] CI/CD pipeline generated/updated (Step 11)
- [ ] Build successful (assembleDebug) (Step 12)
- [ ] Living docs synced (Progress, PRD, Quality, CLAUDE, Changelog) (Step 13)
- [ ] Knowledge graph updated (graphify --update) (Step 14)
- [ ] Release artifacts ready (if releasing) (Step 15)

## Emergency Stop Conditions (Orchestrator Enforces)

The orchestrator **automatically stops and asks user** ONLY for:

1. **Baseline build fails** (Step 0) — "Fix build first"
2. **Security finding HIGH/CRITICAL** (Step 9) — "Review security issue"
3. **Performance regression > 20%** (Step 10) — "Investigate perf regression"
4. **Build fails > 2 attempts** (Step 12) — "Build unstable"
5. **Test flaky detected** (Step 7) — "Fix flaky test"
6. **Breaking change to public API** — "Confirm API change"
7. **Data migration risk** — "Confirm migration strategy"

**NOT for:** Multiple valid approaches, "which design?", "continue?", "confirm?"

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "I'll pick skills manually" | Manual routing misses steps, creates inconsistency. Auto-routing ensures completeness. |
| "Orchestrator is too rigid" | Workflow is mandatory per CLAUDE.md. Orchestrator enforces what's already required. |
| "Some steps aren't needed" | Steps are conditional (see intent mapping). Orchestrator skips appropriately. |
| "I want to skip to implementation" | Skipping planning/spec/arch leads to rework. Baseline check prevents building on broken foundation. |

## Red Flags

- Orchestrator runs but produces no tasks/plan.md
- Steps execute out of order (database before arch)
- Skills invoked without context from previous steps
- Verification gates skipped
- Emergency stops not triggering when they should
- User intervention requested for non-emergency reasons
