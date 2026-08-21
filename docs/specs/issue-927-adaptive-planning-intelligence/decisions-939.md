# Decisions — #939 Annotations module: @Cost and enhanced GOAP support

## D1: @Cost method placement

**Choice:** Separate `@Cost("workerName")` method on the `@Case` interface
**Alternatives:**
- `@Worker(costMethod = "methodName")` — keeps cost paired with worker in the annotation, but splits implementation across two methods with an indirect string reference
- `@Cost` on the `@Worker` method itself — conflates worker execution with cost computation
**Rationale:** Clean separation of concerns. Worker methods execute tasks; cost methods evaluate planning-time cost. A dedicated method with its own annotation is consistent with `@Goal`, `@Milestone`, `@Completion` pattern — each concern gets its own annotated method.
**Trade-offs:** Requires string-based cross-reference (`@Cost("workerName")`) validated at build time. Extra method on the interface.
**Sources:** GoapAction.java (7-arg constructor with CostFunction), CostFunction.java, EngineAnnotationsProcessor.java, CaseDefinitionRecorder.java
**Exploration:** quick
**Status:** captured

## D2: @Cost method parameter type

**Choice:** `GoapWorldState` — the planner's native ternary type
**Alternatives:**
- `Map<String, Object>` — simpler for authors but loses ternary semantics (absent = null, not UNKNOWN)
- `JsonNode` — raw working layer, verbose to query
**Rationale:** Authors working with GOAP already use `@SoftDependency` and understand ternary state. `GoapWorldState.get(key)` returns `Condition` (TRUE/FALSE/UNKNOWN), which is exactly what cost functions need to make context-dependent decisions. No adapter needed — the method signature matches `CostFunction` exactly.
**Trade-offs:** Couples annotation authors to `io.casehub.engine.plan.goap.GoapWorldState`. Acceptable since `@Cost` only has meaning in GOAP/ADAPTIVE planning mode.
**Sources:** GoapWorldState.java, CostFunction.java, GE-20260818-534e70
**Exploration:** quick
**Status:** captured

## D3: @SoftDependency ternary world state mapping

**Choice:** No behavioral change needed — document and validate
**Alternatives:**
- Explicit UNKNOWN marking in GoapAction metadata — redundant with existing default-to-UNKNOWN behavior
**Rationale:** `GoapWorldState.openWorld()` already returns UNKNOWN for absent keys. `GoapAction.isApplicable()` treats UNKNOWN as satisfying hard preconditions optimistically. `@SoftDependency` generates `softPreconditions` entries for penalty scoring. The full ternary pipeline is already wired correctly at runtime.
**Trade-offs:** None — this is a documentation/validation-only item.
**Sources:** GoapWorldState.java:64-73 (openWorld), GoapAction.java:67-77 (isApplicable), GE-20260818-534e70
**Exploration:** quick
**Status:** captured

## D4: Action blacklisting identity

**Choice:** Action name (= capability name) is sufficient — no new metadata needed
**Alternatives:**
- Separate stable action ID field — would matter if the same capability could have multiple GOAP actions, but annotations model creates one action per @Worker
**Rationale:** `GoapAction.name()` equals the capability name (from `@Worker.capability()` or method name). `GoapDecompositionStrategy.replan()` already blacklists by name via `PlannerConfig.blacklistedActions`. The annotations path generates action names that match the existing blacklisting mechanism.
**Trade-offs:** None — existing identity is correct.
**Sources:** GoapDecompositionStrategy.java:91-127 (replan), PlannerConfig.java
**Exploration:** quick
**Status:** captured
