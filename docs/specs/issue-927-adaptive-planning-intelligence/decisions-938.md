## D1: Contingency type on DagNode

**Choice:** `DagPlan<T>` — full sub-plan as nullable field on DagNode record
**Alternatives:**
- Single `T` alternative — simpler but can't model multi-step fallback sequences
- `List<DagPlan<T>>` ordered fallbacks — more flexible but adds complexity to DagDriver
- Contingency map on DagPlan (`Map<String, DagPlan<T>>` keyed by node ID) — avoids nullable field and recursive type, but allows inconsistent state (contingency for non-existent node ID) and separates contingency from the node it protects
**Rationale:** Full sub-plan covers all cases: single-alternative is `DagPlan.singleton()`, multi-step sequences use `DagPlan.sequence()`, complex dependency graphs use `DagPlan.fromNodes()`. Matches the issue spec directly. Nested contingencies work naturally (contingency nodes can themselves have contingencies). Structural validation at DagNode construction time prevents inconsistent state. The nullable field is acceptable given the record has only 5 components.
**Trade-offs:** Every DagNode caller site needs updating (source-breaking record change). Nullable field on a record — callers must handle null. Recursive type structure (DagNode → DagPlan → DagNode) — snapshot serialization must handle recursion. All acceptable for pre-release.
**Sources:** GE-20260818-6546f0 (DagNode edge building), engine#938 issue spec
**Exploration:** quick
**Status:** revised (R1-04: acknowledged plan-level map alternative, maintained choice)

## D2: DagDriver contingency execution model

**Choice:** Nested DagDriver — self-contained execution within `executeNode()`
**Alternatives:**
- Inline node expansion — inject contingency nodes into outer driver's state map. All nodes visible in one DagResult but significantly more complex (mutable runtime node registry, CountDownLatch breakage, modified propagateFailures/computeReadySet)
- Pre-flattened ANY_OF edges — flatten at construction time. No runtime changes but loses conditional activation semantics entirely
**Rationale:** Preserves DagPlan immutability and DagDriver simplicity. Contingency execution is opaque to the outer driver — dependents see Completed or Failed. New `DagEventListener.onContingencyActivated(nodeId, task, DagResult<R> contingencyResult)` callback provides full observability — receives the nested `DagResult` so listeners can capture the complete contingency execution trace for audit, CBR learning, and post-mortem analysis. Nested contingencies work recursively. The nested driver inherits the outer's listeners (passed at construction) so `onContingencyActivated` fires on the same listener set.
**Cancellation:** Outer driver passes its `cancelled` flag reference to the nested driver (or the nested driver checks the outer's flag after execution). If the outer is cancelled while a contingency is mid-execution, the nested driver's result is discarded and the original node is marked Cancelled.
**Thread pool:** Nested driver uses virtual threads (the default). No thread pool sharing concern.
**Trade-offs:** Contingency internal node events fire on the nested driver, not the outer. Contingency nodes don't appear in the outer DagResult — they appear in the `DagResult` passed to `onContingencyActivated`.
**Sources:** DagDriver.java (engine-common), NodeState.java, DagEventListener.java
**Exploration:** deep-analysis
**Status:** revised (R1-06: onContingencyActivated receives DagResult; R1-07: cancellation propagation)

## D3: Result extraction from contingency sub-plan

**Choice:** Last completed exit node result (topological order), with single-exit validation
**Alternatives:**
- Merged results map — changes result type contract for dependents
- Contingency-specific extractor function — maximum flexibility but adds complexity to DagNode record
**Rationale:** Simple, deterministic, matches the mental model of "the contingency replaces the original node." Contingency DagPlans are validated at DagNode construction: if `contingency != null && contingency.exitNodeIds().size() > 1`, throw `IllegalArgumentException`. This enforces the single-exit assumption structurally rather than relying on callers to produce correct plans.
**Trade-offs:** Contingency plans with parallel exit nodes are rejected at construction time. Decomposition strategies must produce single-exit contingencies — use `DagPlan.sequence()` to chain to a single exit point when needed.
**Sources:** DagPlan.exitNodeIds(), DagPlan.topologicalSort()
**Exploration:** quick
**Status:** revised (R1-05: added single-exit validation)

## D4: GOAP contingency generation threshold

**Choice:** Configurable per CaseDefinition via `AdaptationConfig.contingencyThreshold`, default 0.15
**Alternatives:**
- Fixed 0.15 — simpler but can't be tuned per domain
- Always generate when alternative exists — wastes storage
- Place on CbrConfig — wrong home; contingency generation is a planning concern, not a CBR concern
**Rationale:** 0.15 is a heuristic default (not a research-validated constant) — the crossover where pre-computed contingencies are likely cheaper than reactive replanning depends on domain-specific factors (replanning latency, action failure distribution, remaining plan value). Configurable so domains can tune: expensive replanning → lower threshold; cheap fallbacks → higher. `AdaptationConfig` is the correct home alongside `threshold` (progress-gated), `trigger`, `repair`, `optimization`, and `metaReasoner`.
**Pipeline:** `ExperienceAnalyser.workerSuccessRates()` computes per-capability failure rates from CBR history. `GoapDecompositionStrategy` queries this via `GoalDecompositionContext.experiences()`, computes `failureRate = 1 - successRate` per action, and generates contingency when `failureRate >= contingencyThreshold`. Minimum sample count from `CbrConfig.minCostSamples` gates activation (no contingency generation below sample threshold).
**Trade-offs:** Additional configuration surface. Default 0.15 works as a starting point.
**Sources:** research/2026-08-18-adaptive-planning-intelligence.md §2.3, ExperienceAnalyser.java, AdaptationConfig.java
**Exploration:** quick
**Status:** revised (R1-01: moved to AdaptationConfig, clarified pipeline, reframed as heuristic)

## D5: YAML contingency syntax and pipeline flow

**Choice:** Inline capability list on binding — `contingency: [alt-capability-1, alt-capability-2]`
**Pipeline:** `DefaultGoalDecomposer` attaches YAML contingencies AFTER decomposition, not during. The decomposition strategy produces the primary DagPlan without contingency awareness. The decomposer then iterates the returned DagNodes, matches each to its binding via `definition.findBindingsByCapability()`, reads `Binding.contingency()`, and wraps the DagNode with a contingency sub-plan built from the capability list (resolved to GoalSteps via the same binding lookup). For GOAP/LLM-generated contingencies, the strategy produces them internally using CBR data (via `GoalDecompositionContext.experiences()`) — no binding access needed.
**Alternatives:**
- Nested plan block with step IDs and dependencies — maximum control but verbose for the common case
- Both modes (short + long form) — parser complexity for marginal benefit
**Rationale:** The common case is "try these alternative capabilities in order." The list maps naturally to `DagPlan.sequence()` of `GoalStep`s. The post-decomposition attachment pattern is consistent with how binding resolution already works in `DefaultGoalDecomposer`.
**Trade-offs:** Cannot declare parallel fallback branches or complex dependency graphs in YAML. Those cases use programmatic GOAP/LLM decomposition strategy generation instead. YAML contingencies and strategy-generated contingencies could overlap — strategy-generated takes precedence (it has CBR context; YAML is a manual override).
**Sources:** CaseDefinitionYamlMapper.java, Binding.java, DefaultGoalDecomposer.java
**Exploration:** quick
**Status:** revised (R1-02: specified post-decomposition attachment pipeline)

## D6: DagDriver execution scope

**Choice:** Contingencies are a DagDriver-level mechanism only. Engine PlanItem dispatch uses the existing adaptation system (meta-reasoning, repair, optimization) for failure handling.
**Alternatives:**
- Add contingency support at PlanItem level — redundant with the adaptation system
- Bypass adaptation when contingencies exist — loses the graduated response (repair → optimize → concede)
**Rationale:** Clean separation of concerns. DagDriver handles pre-computed contingencies (fast, in-process, blocks patterns). The engine's adaptation system handles reactive replanning (distributed, compound-level). When decomposition produces DagPlan with contingencies and it's materialized as PlanItems, contingencies are stored as metadata on the compound for audit/inspection but not executed by PlanItem dispatch.
**Trade-offs:** Engine-level compounds don't get contingency activation — they rely on the adaptation system. This is intentional: the adaptation system IS the engine-level contingency mechanism.
**Sources:** DefaultPlanAdaptationEvaluator.java, PlanningStrategyLoopControl
**Exploration:** deep-analysis
**Status:** captured

## D7: Contingency activation trigger

**Choice:** Exception from `taskExecutor.apply()` — the sole activation signal
**Alternatives:**
- Differentiate by failure category (Transient/Knowledge/Infeasible) — DagDriver is in engine-common and has no access to the engine's failure classification system
- Timeout-based activation — DagDriver has no per-node timeout mechanism; the 10-minute global latch is a safety net, not a deadline
- Expectation violation (node succeeds but produces wrong results) — would require effect validation in DagDriver, coupling it to GOAP semantics it currently knows nothing about
**Rationale:** DagDriver is a simple concurrent execution driver. It executes functions and catches exceptions. The distinction between transient, knowledge, and infeasible failures is an engine-level concern handled by the adaptation system after contingency exhaustion. The trigger is binary: exception → try contingency → if contingency fails → mark Failed → propagation and adaptation handle the rest. No interaction with retry/reroute (engine-level concerns not visible to DagDriver).
**Trade-offs:** All exceptions trigger contingency, including transient ones where retry might be cheaper. This is acceptable because (1) DagDriver doesn't know about retries, and (2) a contingency that succeeds is always better than a retry that might also fail.
**Sources:** DagDriver.executeNode(), engine's FailureClassifier
**Exploration:** deep-analysis
**Status:** captured
