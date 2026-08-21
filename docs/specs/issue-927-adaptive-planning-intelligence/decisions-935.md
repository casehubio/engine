# Decisions — #935 Plan Repair vs Plan Optimization Separation

## D1: Marker sub-interfaces with two config fields

**Choice:** Introduce `RepairStrategy extends PlanRevisionStrategy` and `OptimizationStrategy extends PlanRevisionStrategy` as marker sub-interfaces. Same `revise(RevisionContext) → RevisedPlan` method — zero API duplication. `AdaptationConfig` gains `repair` field and renames `revision` to `optimization`. `DefaultPlanAdaptationEvaluator` routes: LOCAL → `resolve(RepairStrategy.class, config.effectiveRepair(definition))`, COMPOUND → `resolve(OptimizationStrategy.class, config.optimization())`. `EngineStrategyResolver` gets `Instance<RepairStrategy>` and `Instance<OptimizationStrategy>`.
**Alternatives:**
- Flat single SPI with config-only routing — no type-level enforcement, resolver can't validate strategy-scope pairing
- Two fully separate SPIs with independent method signatures — unnecessary duplication when the API shape is identical
- One SPI with scope in context only (single config field) — strategies must handle both scopes internally, less explicit routing
**Rationale:** Marker sub-interfaces give type-level enforcement (a `GoapRepairStrategy` can't be configured as optimization) while preserving the shared `PlanRevisionStrategy` contract. Follows the platform pattern — `EngineStrategyResolver` already resolves by type+id for 15+ SPI types. Each strategy declares its semantic role via its type, not via config.
**Trade-offs:** Two more `Instance<>` params in EngineStrategyResolver constructor (consistent with D6). PlanRevisionStrategy remains as the common base — no backward compat break for existing code that programs to the base type.
**Sources:** `PlanRevisionStrategy.java` (SPI), `AdaptationConfig.java` (current config), `DefaultPlanAdaptationEvaluator.java:259-290` (scope switch that currently ignores scope), D2/D7 from decisions-934.md (scope routing deferred to #935), R1-02 (decision review: marker sub-interface suggestion)
**Exploration:** quick
**Status:** revised — R1-02: adopted marker sub-interfaces for type-level enforcement while preserving shared base SPI

## D2: Auto-detect default repair strategy at config layer

**Choice:** When `AdaptationConfig.repair()` is null, auto-detect via `AdaptationConfig.effectiveRepair(CaseDefinition)`: if definition has GOAP actions → "goap-repair", else → "llm-repair". Pure function at the config layer — evaluator just calls `resolve(RepairStrategy.class, config.effectiveRepair(definition))`. Explicit config always overrides.
**Alternatives:**
- forward-replan fallback — LOCAL and COMPOUND identical by default, users must configure to get differentiated repair. Defeats the purpose of the separation.
- goap-repair always — fails at runtime for non-GOAP cases
- @DefaultBean LlmRepairStrategy (R1-04 suggestion) — follows platform pattern but loses zero-config GOAP benefit
**Rationale:** Cases with GOAP actions get cheap algorithmic repair automatically — the primary value of #935. Non-GOAP cases get LLM repair (cheaper than full optimization). Zero-configuration benefit for both case types. Auto-detect moved to config layer (R1-04) — the evaluator's job is pipeline coordination, not strategy selection.
**Trade-offs:** Auto-detect adds a CaseDefinition parameter to `effectiveRepair()`. Minimal — one `isEmpty()` check on `definition.getGoapActions()`.
**Sources:** `GoapDecompositionStrategy.replan()` (existing GOAP repair logic), `CaseDefinition.getGoapActions()` (action presence check)
**Exploration:** quick — user challenged initial recommendation (forward-replan fallback), auto-detect was the right call
**Status:** revised — R1-04: moved auto-detect from evaluator to AdaptationConfig.effectiveRepair(CaseDefinition)

## D3: Standalone GoapRepairStrategy with own GoapPlanner

**Choice:** `GoapRepairStrategy` has its own `GoapPlanner` instance and builds world state from `RevisionContext`. Does not delegate to `GoapDecompositionStrategy.replan()`.
**Alternatives:**
- Delegate to GoapDecompositionStrategy — reuses existing logic but creates coupling between adaptation and decomposition modules
**Rationale:** Decomposition and adaptation are different concerns with different entry points. GoapDecompositionStrategy.replan() takes `DecompositionContext` (not `RevisionContext`) and requires `TaskNode`/`ReplanContext` types from the decomposition SPI. Delegation would require adapter code that's more complex than standalone implementation.
**Trade-offs:** Minor code duplication of GOAP planner invocation pattern. Acceptable because the invocation is small (build world state, resolve actions, call planner, build DagPlan).
**Sources:** `GoapDecompositionStrategy.java:86-122` (replan method), `GoapPlanner.java` (stateless, thread-safe)
**Exploration:** quick
**Status:** captured

## D4: Drop RefineScope from RevisionContext

**Choice:** Do NOT add `RefineScope` to `RevisionContext`. With marker sub-interfaces (D1 revised), strategies know their role by type — a `RepairStrategy` is always called for repair, an `OptimizationStrategy` for optimization. Scope in context is redundant.
**Alternatives:**
- Add scope to RevisionContext — redundant with type-level routing; enables scope-branching within a single strategy, which contradicts the marker sub-interface design
**Rationale:** R1-07 correctly identified the redundancy. The D4/D5 tension (scope makes LlmRepairStrategy unnecessary; LlmRepairStrategy makes scope unnecessary) is resolved by choosing marker sub-interfaces + separate implementations. Each strategy's type IS its scope declaration.
**Trade-offs:** A custom strategy that wants to serve both roles must implement both interfaces. Acceptable — the platform pattern favors explicit role declaration.
**Sources:** `RevisionContext.java` (current 4-field record, unchanged), R1-07 (redundancy finding), R1-08 (D4/D5 tension)
**Exploration:** quick
**Status:** revised — R1-07: dropped scope from RevisionContext, redundant with marker sub-interfaces

## D5: Three built-in PlanRevisionStrategy implementations

**Choice:** Three built-ins: `GoapRepairStrategy` (id="goap-repair", algorithmic A* repair), `LlmRepairStrategy` (id="llm-repair", repair-focused LLM prompt), `ForwardReplanRevision` (id="forward-replan", full LLM optimization, unchanged).
**Alternatives:**
- Defer LlmRepairStrategy — only goap-repair + forward-replan. Non-GOAP cases get full replan for LOCAL.
**Rationale:** The issue requires both GOAP and LLM repair built-ins. LlmRepairStrategy is small — a ForwardReplanRevision variant with a narrower prompt targeting the failed step. Completes the strategy matrix: algorithmic repair, LLM repair, LLM optimization.
**Trade-offs:** Third strategy implementation to maintain. Minimal — shares prompt infrastructure with ForwardReplanRevision.
**Sources:** Issue #935 acceptance criteria, `ForwardReplanRevision.java` (prompt structure to adapt)
**Exploration:** quick
**Status:** captured

## D6: Fix EngineStrategyResolver explicit registration

**Choice:** Add explicit `Instance<PlanRevisionStrategy>` and `Instance<AdaptationTrigger>` parameters to `EngineStrategyResolver` constructor. Currently both rely on the catch-all `Instance<NamedStrategy>` which is unreliable in Quarkus ARC (GE-20260810-b53fd8).
**Alternatives:**
- Leave on catch-all — risk of build-time pruning missing strategy beans
**Rationale:** Known reliability issue. All other SPI types have explicit Instance<> params. PlanRevisionStrategy and AdaptationTrigger were added before the explicit registration pattern was established.
**Trade-offs:** Two more constructor parameters. Necessary.
**Sources:** `EngineStrategyResolver.java:60-78` (constructor — 15 explicit params, catch-all last), GE-20260810-b53fd8 (garden entry documenting the issue)
**Exploration:** quick
**Status:** captured
