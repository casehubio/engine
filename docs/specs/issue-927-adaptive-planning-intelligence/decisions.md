# Decisions — #929 GOAP as DecompositionStrategy

## D1: Scope

**Choice:** Full scope — all 8 items in one issue
**Alternatives:**
- Core + optimizations only — defer ternary and dynamic cost
- Minimal core — just wiring, ceiling, heuristic
**Rationale:** Pre-release platform. Full scope delivers a complete classical planning alternative in one pass. Ternary and dynamic cost are architecturally intertwined with the planner enhancements.
**Trade-offs:** Larger issue, but all items are internally cohesive.
**Sources:** research/2026-08-18-adaptive-planning-intelligence.md §2, §6
**Exploration:** quick
**Status:** captured

## D2: Cost range constraint

**Choice:** Remove [0,1] constraint on GoapAction.cost. New constraint: cost >= 0.0, no upper bound.
**Alternatives:**
- Keep [0,1] normalized — dynamic costs must normalize. Constrains CBR-learned costs.
**Rationale:** Dynamic cost functions produce context-dependent values (e.g., estimated duration in seconds). Forcing normalization loses information and complicates the CBR integration in #937.
**Trade-offs:** Existing actions with [0,1] costs continue to work. Benefit range stays [0,1] (it's a multiplier, not an absolute value).
**Sources:** GoapAction.java:38, research doc §Issue 2 (dynamic cost), §Issue 10 (learned costs)
**Exploration:** quick
**Status:** captured

## D3: Ternary world state UNKNOWN semantics

**Choice:** Optimistic — UNKNOWN satisfies preconditions. Plan optimistically, fail at runtime if the assumption was wrong.
**Alternatives:**
- Pessimistic — UNKNOWN blocks preconditions. Safer but produces fewer plans.
- Branching — generate plans for both values, evaluate at runtime if plans differ. Most correct but most complex.
**Rationale:** Matches Embabel's lazy-evaluation pattern. In an orchestration engine, runtime failure triggers adaptation (#934, #936) — the system can recover. Pessimistic planning under partial observability often produces no plan at all.
**Trade-offs:** May produce plans that fail at runtime due to unsatisfied UNKNOWN conditions. The adaptation pipeline (#930, #931, #934) handles this.
**Sources:** research doc §6 (Embabel ternary), GoapWorldState.java
**Exploration:** quick
**Status:** captured

## D4: Backward pruning and forward simulation location

**Choice:** In GoapPlanner itself, enriching the core planner for all consumers.
**Alternatives:**
- In GoapDecompositionStrategy only — pre/post-processing. Other consumers don't benefit.
**Rationale:** GoapPlanningStrategy (dispatch-time) and AdaptivePlanningStrategy also benefit from pruning and simulation. Placing it in the planner means all consumers get better plans without code duplication.
**Trade-offs:** Changes to engine-api (GoapPlanner) ripple to all consumers. Pre-release, so no backward compat concern.
**Sources:** GoapPlanner.java, GoapPlanningStrategy.java, AdaptivePlanningStrategy.java
**Exploration:** quick
**Status:** captured
