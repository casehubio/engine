# Decisions — #934 Persist / Refine / Concede Meta-Reasoning

## D1: Meta-reasoner pipeline placement

**Choice:** Layer on top of existing triggers — meta-reasoner sits after trigger as a second evaluation layer
**Alternatives:**
- Replace triggers entirely — single evaluation point, but breaks AdaptationTrigger SPI and discards existing implementations
- Merge into trigger — extend AdaptationSignal to Persist/Refine/Concede, but forces every trigger to become a meta-reasoner
**Rationale:** Preserves EveryStep/OnFailure/ProgressGated triggers as fast pre-filters. Meta-reasoner only runs when trigger says adaptation is warranted — adds cost-benefit evaluation without breaking existing contracts. Pipeline: trigger → (SKIP: stop) → meta-reasoner → (Persist: stop, Refine: revise, Concede: abandon).
**Trade-offs:** Two evaluation layers adds indirection. A trigger returning PROCEED that meta-reasoner overrides to Persist means the trigger work was wasted. Acceptable because triggers are cheap (divergence score lookup or config check) and meta-reasoning is the expensive evaluation.
**Sources:** `DefaultPlanAdaptationEvaluator.java:180-190` (current trigger→revision flow), `AdaptationTrigger.java` (SPI contract), `ProgressGatedTrigger.java` (existing implementation)
**Exploration:** quick
**Status:** captured
**Review note (self):** ReplanHint.ALWAYS from ProgressGatedTrigger must bypass meta-reasoner — if trigger was forced PROCEED by a per-binding hint, the meta-reasoner should not override to Persist. Thread AdaptationSignal.Proceed cause (hint-forced vs evaluated) or check ReplanHint in the meta-reasoner itself.

## D2: Refine scope selects revision strategy

**Choice:** LOCAL resolves to a repair-focused PlanRevisionStrategy; COMPOUND resolves to ForwardReplanRevision (full LLM re-plan). DeeperDecompositionHandler stays separate on the failure-exhaustion path.
**Alternatives:**
- Scope unifies all recovery — routes both adaptation and deeper decomposition through meta-reasoner, requires restructuring handler flow
- No scope — always COMPOUND, defer LOCAL to #935
**Rationale:** Clean separation of concerns. DeeperDecompositionHandler (#936) runs on EXHAUSTED+Knowledge in WorkerOutcomeResolvedHandler. Meta-reasoner runs on the adaptation path (PlanItemCompletionHandler/WorkerRetryExhaustionHandler). Different triggers, different handler chains. Unifying would require major handler restructuring for marginal benefit.
**Trade-offs:** Two separate decision points for recovery (meta-reasoner for adaptation, DeeperDecompositionHandler for failure exhaustion). Future unification tracked in #935.
**Review note (self):** LOCAL scope has no dedicated repair strategy in v1 — only ForwardReplanRevision exists. LOCAL falls back to COMPOUND until #935 adds RepairStrategy. The scope field is structural preparation.
**Sources:** `WorkerOutcomeResolvedHandler.java:80-100` (deeper decomposition check), `DeeperDecompositionHandler.java` (failure-path decomposition), `PlanItemCompletionHandler.java:139` (adaptation call site)
**Exploration:** quick
**Status:** captured

## D3: Cumulative cost tracked via adaptation generation and enriched EventLog

**Choice:** Adaptation count from CasePlanModel.getAdaptationGeneration() (zero-query, in-memory). Token cost tracking added to PLAN_ADAPTED EventLog entries by extending DefaultPlanAdaptationEvaluator.writeEventLog() to capture token usage from the revision strategy's response.
**Alternatives:**
- In-memory ConcurrentHashMap tracker — fast but doesn't survive restarts, adds state management
- CasePlanModel field — zero query overhead but couples cost tracking to plan model
- EventLog-only (original D3) — token usage does not exist in PLAN_ADAPTED metadata today; would require EventLog query for data that's cheaper to track in-memory
**Rationale:** adaptationGeneration on CasePlanModel already tracks adaptation count per compound with zero queries — this is the primary cost ceiling signal. Token usage is NOT currently written to PLAN_ADAPTED events (writeEventLog records goalName, compoundId, triggerStrategy, revisionStrategy, stepCounts, generation, rationale — no token data). #934 extends writeEventLog to capture token cost from PlanRevisionStrategy, making token-based cost ceiling possible via EventLog query. Count-based ceiling (adaptationGeneration vs max-adaptations) provides value immediately; token-based ceiling is additive.
**Trade-offs:** Token cost tracking requires modifying PlanRevisionStrategy contract or RevisedPlan to include cost metadata. adaptationGeneration-based count is the primary signal and has zero overhead.
**Sources:** `DefaultPlanAdaptationEvaluator.java:writeEventLog()` (current metadata — no token fields), `CasePlanModel.getAdaptationGeneration()` (existing count), `DefaultCasePlanModel.java:384` (AtomicInteger generation tracking)
**Exploration:** quick
**Status:** revised — fixed factual error: token usage is not in EventLog today. Primary signal is adaptationGeneration (zero-query). Token cost tracking is new work.

## D4: Concede faults compound directly

**Choice:** Mark pending PlanItems CANCELLED, fault the compound directly via a new compound-level fault transition (CasePlanModel.faultCompound()). For parent compounds, the existing evaluateCompletion semantics handle propagation — FAULTED counts as terminal, so MOfN/FirstWins parents may still complete while ALL parents propagate the terminal state upward. Audit via PLAN_CONCEDED event.
**Alternatives:**
- Directly fault case — heavy-handed, doesn't respect compound composition (multi-compound cases would fail entirely)
- Signal context and let goals decide — flexible but indirect, depends on goal configuration
- Rely on CompoundCompletionEvaluator to detect un-completable state — CCE only transitions to COMPLETED (tryDefinitionTransition to COMPLETED), has no fault detection logic
**Rationale:** Concede is a deliberate cost-based decision — the meta-reasoner knows it's abandoning. It directly faults the compound rather than relying on CompoundCompletionEvaluator to infer the state. CCE's evaluateCompletion correctly treats FAULTED and CANCELLED as terminal statuses (DefaultCasePlanModel.evaluateCompletion counts all terminal children), so parent compound evaluation continues to work. CasePlanModel.faultCompound() needs to be added as part of #934 — this does not exist today. PLAN_CONCEDED EventLog captures the deliberate abandonment intent for audit/observability.
**Trade-offs:** Requires new CasePlanModel.faultCompound() capability. Under ALL semantics, a parent compound with a FAULTED child currently transitions to COMPLETED (all children terminal) rather than propagating the fault — this is a pre-existing gap that Concede makes explicit. #934 adds fault-aware logic to CompoundCompletionEvaluator: if completion criteria are met but any required child is FAULTED/CANCELLED (not COMPLETED), the parent should fault rather than complete.
**Review note (self):** Only PENDING PlanItems are CANCELLED. RUNNING items complete naturally — their result is discarded (compound already faulted). This matches existing replaceCompound() behavior where RUNNING PlanItems are preserved.
**Sources:** `CompoundCompletionEvaluator.java:evaluate()` (transitions to COMPLETED only), `DefaultCasePlanModel.evaluateCompletion()` (counts FAULTED/CANCELLED as terminal), `WorkerOutcomeResolvedHandler.java:100-120` (existing fault → CCE pattern)
**Exploration:** quick
**Status:** revised — clarified direct fault mechanism, acknowledged CasePlanModel.faultCompound() and fault-aware completion propagation as new work.

## D5: Default meta-reasoner uses cost-ceiling heuristic with failure-category scope selection

**Choice:** Classical cost-ceiling heuristic evaluating: (1) adaptation count vs configurable max-adaptations (from CasePlanModel.getAdaptationGeneration()), (2) failure category from AdaptationContext for scope selection, (3) remaining steps ratio for Concede threshold. Routing table — first match wins, cost ceiling checked before all routes:
- Cost ceiling exceeded (adaptation count ≥ max-adaptations) → Concede
- Remaining steps ratio below threshold → Concede
- FailureCategory absent (success path — trigger fired on divergence after successful completion) → Refine(COMPOUND)
- Transient → Persist (let retry/reroute handle)
- Knowledge → Refine(LOCAL)
- Repeated Knowledge on same compound → Refine(COMPOUND)
- Infeasible → Concede
**Alternatives:**
- Always-refine passthrough — simplest default but no value without custom implementation
- Rich multi-signal cost-BENEFIT evaluation (MPDF/SOFAI-LM style) — requires estimating expected improvement from adaptation, which is inherently uncertain without LLM reasoning. Better suited as an LLM-backed strategy, not the classical default.
- Divergence-based scope selection (original D5) — divergence magnitude doesn't correlate with repair scope. High divergence from one catastrophic step may need LOCAL repair; low divergence from gradual drift across many steps may need COMPOUND replan.
**Rationale:** Purely classical, no ChatModelProvider dependency. Divergence evaluation is the trigger's responsibility (ProgressGatedTrigger already computes and gates on divergence) — the meta-reasoner uses DIFFERENT signals to avoid duplication. Failure category (from #930 FailureCategory sealed type) is the architecturally correct scope selector per the TART taxonomy (research §2.3): the TYPE of failure determines the scope of response, not the magnitude of divergence. The success path (PlanItemCompletionHandler calls evaluateAdaptation with COMPLETED) has no FailureCategory — the trigger fired because divergence exceeded the threshold after a successful step that produced unexpected output. In this case, Refine(COMPOUND) is the correct default: the plan has drifted from expectations, and without a specific failed step to target, full replanning is the safe choice. Cost ceiling (adaptation count, remaining steps ratio) is checked first on ALL paths — Concede overrides any routing decision. Cost-ceiling is intentional for the default — a classical heuristic cannot reliably estimate adaptation benefit. An LLM-backed meta-reasoner can implement full cost-benefit analysis as a named strategy resolved via EngineStrategyResolver.
**Trade-offs:** Cost-ceiling cannot distinguish scenarios where adaptation cost is high but benefit is higher (e.g., step 9 of 10 failed, one adaptation would complete the case). Acceptable limitation for the classical default. Requires AdaptationContext to carry optional failure category (null on success path, populated on failure path).
**Sources:** `CasePlanModel.getAdaptationGeneration()` (zero-query count), `FailureCategory.java` (sealed type from #930), research §2.3 (TART taxonomy for failure-category-based routing), `PlanItemCompletionHandler.java:139` (success path — COMPLETED status, no FailureCategory), `ProgressGatedTrigger.java` (divergence — trigger's job, not meta-reasoner's)
**Exploration:** quick
**Status:** revised — R1: scope selection changed from divergence-based to failure-category-based. R2: added success-path routing (null FailureCategory → Refine(COMPOUND)). Cost ceiling checked first on all paths.

## D6: New AdaptationMetaReasoner SPI with AdaptationDecision return type

**Choice:** Introduce AdaptationMetaReasoner as a new SPI in engine-api (extends NamedStrategy). Returns AdaptationDecision sealed type: Persist(reason) | Refine(scope, cause) | Concede(reason). AdaptationSignal remains {Proceed, Skip} — triggers keep their existing vocabulary. The two SPIs have different return types reflecting different responsibilities: triggers gate (binary), meta-reasoners decide (ternary with scope).
**Alternatives:**
- Enrich AdaptationSignal from {Proceed, Skip} to {Persist, Refine(scope), Concede(reason)} — forces every AdaptationTrigger implementation to make the full meta-reasoning decision. EveryStepTrigger has no cost/failure data and would always return Refine(COMPOUND), defeating cost-gating. OnFailureTrigger lacks failure category to decide scope. Breaks existing SPI contract.
- No new SPI — hardcode meta-reasoning in DefaultPlanAdaptationEvaluator — not extensible, cannot swap heuristic for LLM-backed implementation
**Rationale:** Clean separation: AdaptationTrigger evaluates WHETHER to consider adaptation (binary gate). AdaptationMetaReasoner evaluates WHAT adaptation to perform (ternary decision with scope). Different responsibilities, different SPIs, different return types. The meta-reasoner has strictly more input signals (adaptation count, failure category, remaining plan value) than triggers need. Strategy resolution via EngineStrategyResolver allows swapping the default cost-ceiling heuristic for an LLM-backed reasoner per case definition.
**Trade-offs:** Adds a second SPI to engine-api. Acceptable because the responsibilities are genuinely different and the SPI boundary prevents conflation.
**Sources:** `AdaptationSignal.java` (current sealed interface — Proceed/Skip), `AdaptationTrigger.java` (current SPI contract), research §2.3 Issue 7 (Persist/Refine/Concede trichotomy)
**Exploration:** surfaced by review (R1-06)
**Status:** captured

## D7: LOCAL scope falls back to ForwardReplanRevision until Issue 8

**Choice:** Refine(LOCAL) resolves to ForwardReplanRevision (same as COMPOUND) until Issue 8 delivers dedicated repair strategies (GoapRepairStrategy, LlmRepairStrategy). The LOCAL scope exists in the decision vocabulary from #934; the execution differentiation arrives with Issue 8.
**Alternatives:**
- Defer LOCAL entirely — remove it from Refine scope options until repair strategies exist. Reduces vocabulary to Refine(COMPOUND) only.
- Map LOCAL to Persist — semantically wrong. LOCAL means "repair this step"; Persist means "don't adapt at all."
**Rationale:** The research explicitly places repair strategies (Issue 8) as dependent on meta-reasoning (#934/Issue 7). #934 establishes the decision framework; Issue 8 provides differentiated execution. Defining LOCAL now ensures the meta-reasoner's decision vocabulary is complete — callers (tests, configs, LLM-backed reasoners) can express LOCAL intent. The fallback to ForwardReplanRevision is a temporary degradation, not a semantic error: full replanning is a superset of local repair (produces a valid plan, just more expensive than necessary).
**Trade-offs:** Until Issue 8, LOCAL and COMPOUND produce identical behavior. The scope distinction has no runtime effect but establishes the contract for future differentiation.
**Sources:** Research §2.3 Issue 8 (Plan Repair vs Plan Optimization — depends on Issue 7), `ForwardReplanRevision.java` (sole PlanRevisionStrategy implementation)
**Exploration:** surfaced by review (R1-07)
**Status:** captured

## D8: Meta-reasoner configuration via AdaptationConfig

**Choice:** AdaptationConfig gains a new optional field: metaReasoner (String, strategy name, default "cost-ceiling"). Max-adaptations configured via config property casehub.engine.adaptation.max-adaptations (default: 5). The meta-reasoner is resolved via EngineStrategyResolver, consistent with trigger and revision resolution.
**Alternatives:**
- Separate MetaReasoningConfig record — adds configuration complexity for one strategy name
- Hardcoded default with no configuration — prevents customization per case definition
- AdaptationConfig becomes a builder/sealed hierarchy — over-engineering for one additional field
**Rationale:** AdaptationConfig already configures the adaptation pipeline: trigger (strategy name), revision (strategy name), threshold. Adding metaReasoner (strategy name) is consistent — the pipeline is trigger → metaReasoner → revision, and each stage is a named strategy. Resolution via EngineStrategyResolver provides the same extensibility pattern: users implement AdaptationMetaReasoner and register it as a named strategy. The metaReasoner field is optional with default "cost-ceiling" so existing case definitions work without modification.
**Trade-offs:** AdaptationConfig grows from 3 to 4 fields. Minor — the record is the natural home for pipeline configuration.
**Sources:** `AdaptationConfig.java` (current: trigger, revision, threshold), `EngineStrategyResolver.java` (strategy resolution pattern)
**Exploration:** surfaced by review (R1-08)
**Status:** captured
