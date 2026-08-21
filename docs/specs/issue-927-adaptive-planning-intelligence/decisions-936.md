# Decisions — #936 Dynamic Decomposition Depth (ADaPT Pattern)

## D1: Trigger point — on reroute exhaustion

**Choice:** After all reroutes exhaust for a Knowledge failure, decompose instead of faulting.
**Alternatives:**
- On first Knowledge failure — skips rerouting entirely, changes existing behavior
- Via adaptation evaluator — runs on step completion, not exhaustion; wrong lifecycle point
**Rationale:** Gives rerouting a chance first (different agent might succeed). Decomposition is the fallback when the task is too coarse for any single agent. No new event type needed — `WorkerOutcomeResolvedEvent` already carries `FailureCategory`.
**Trade-offs:** Delays decomposition by N reroute attempts. Acceptable — rerouting is cheap relative to decomposition.
**Sources:** WorkerOutcomeResolvedHandler.java:54, WorkerOutcomeResolvedEvent.java:20, GE-20260607-245588
**Exploration:** quick
**Status:** captured

## D2: Structural operation — promoteToCompound() on CasePlanModel

**Choice:** New atomic `promoteToCompound()` method on `CasePlanModel`/`DefaultCasePlanModel`.
**Alternatives:**
- Remove + register — two operations, non-atomic, risk inconsistent state
- Wrapper compound — adds nesting level, increases tree depth unnecessarily
**Rationale:** Follows the pattern of `replaceCompound()` — atomic, updates all index structures. Purpose-built for the promotion operation.
**Trade-offs:** New method on a core interface (pre-release — breaking changes cost nothing).
**Sources:** DefaultCasePlanModel.java:36, GE-20260808-47dc40 (replaceCompound pattern), GE-20260809-fe93ef (structural Primitive executor)
**Exploration:** quick
**Status:** captured

## D3: Module placement — planning module

**Choice:** Handler in `planning/adaptation/` alongside `DefaultPlanAdaptationEvaluator`.
**Alternatives:**
- Runtime module — creates runtime → planning structural coupling
- Split (runtime event, planning handler) — more plumbing
**Rationale:** Planning module already has all needed dependencies: CasePlanModel, DecompositionStrategy, PlanItemStore. `WorkerOutcomeResolvedHandler` is already in planning. Interception happens in the same handler.
**Trade-offs:** None significant.
**Sources:** WorkerOutcomeResolvedHandler.java in planning/handler/
**Exploration:** quick
**Status:** captured

## D4: Depth tracking — compound nesting depth

**Choice:** Walk `getParentOf()` chain to count nesting levels.
**Alternatives:**
- Explicit depth counter on PlanItem — requires persistence schema change
- EventLog-based — requires query at decision time
**Rationale:** Zero storage cost, derived from existing tree structure. Accurate — counts actual nesting, not historical decompositions.
**Trade-offs:** O(depth) tree walk per check. Depth is bounded (max 3) so this is O(1) in practice.
**Sources:** CasePlanModel interface: getParentOf()
**Exploration:** quick
**Status:** captured

## D5: Depth config — CaseDefinition field

**Choice:** `maxDecompositionDepth` on `CaseDefinition` (nullable Integer, default 3).
**Alternatives:**
- Quarkus config property — deployment-wide, less flexible
- AdaptationConfig — decomposition depth is about the HTN tree, not adaptation
**Rationale:** Per-case-definition control. Consistent with other per-case settings (planningStrategy, decompositionStrategy, adaptationConfig).
**Trade-offs:** Another field on CaseDefinition (already large). Acceptable for pre-release.
**Sources:** CaseDefinition.java
**Exploration:** quick
**Status:** captured
