# Decisions — #928 Plan Monitoring and Expectation Tracking

## D1: Effect metadata source

**Choice:** GOAP actions for GOAP bindings; promoted Binding.producedKeys for non-GOAP bindings
**Alternatives:**
- GOAP actions only — graceful degradation for non-GOAP bindings, but no validation coverage for YAML case definitions without full GOAP structure
- Binding.producedKeys as proxy — reuses existing field but producedKeys is an audit hint, not a behavioral contract
- GOAP actions + new Binding.declaredEffects — adds a third effect representation creating three-way precedence conflicts with producedKeys and GoapAction.effects
**Rationale:** GOAP actions are the primary source for GOAP bindings (annotations compile into them via GoapActionInferrer). For non-GOAP bindings, promote Binding.producedKeys from audit hint to behavioral contract — it already has Set<String> type, YAML parsing support (BindingProducedKeysTest), annotation support (@Bind(producedKeys = ...)), and builder support. Two sources, one per binding type, no precedence conflict.
**Trade-offs:** producedKeys is Set<String> (presence-only), while GoapAction.effects is Map<String, Boolean> (directional). For non-GOAP bindings, validation is limited to presence checking. This is acceptable — non-GOAP bindings don't have GOAP's typed effect model.
**Sources:** GoapAction.java:23 (effects field), CaseDefinition.getGoapActions(), Binding.producedKeys, BindingProducedKeysTest
**Exploration:** quick
**Status:** revised — removed declaredEffects; unified to GOAP effects + promoted producedKeys per R1-02

## D2: Divergence measurement model

**Choice:** Value-aware validation using Condition semantics
**Alternatives:**
- Presence-based validation — simpler but silently misses FALSE effects ({key: false}) and wrong-direction booleans
- Value-equality (Map<String, Object>) — requires richer effect declarations, heavier API surface than needed
- Presence + unexpected key detection — catches unexpected writes but adds false positives for legitimate side effects
**Rationale:** GoapAction.effects is Map<String, Boolean> — effects can be {key: false} (expect removal/absence). GoapAction.applyTo() applies the actual boolean value via GoapWorldState.with(key, value). The validator must use the same semantic model: check Condition.fromBoolean(expectedValue) against the actual world state condition for each effect key. This is barely more complex than presence checking (one boolean comparison per key) and eliminates silent false negatives for removal effects. For non-GOAP bindings using producedKeys (Set<String>), presence-based validation remains correct since producedKeys implies presence.
**Trade-offs:** GOAP bindings get value-aware validation; non-GOAP bindings get presence-only. This asymmetry reflects the type difference between Map<String, Boolean> and Set<String>.
**Sources:** GoapAction.java (effects Map<String, Boolean>, applyTo()), GoapWorldState.java (Condition enum, with(key, boolean), openWorld()), Condition.java (TRUE/FALSE/UNKNOWN, fromBoolean())
**Exploration:** quick
**Status:** revised — changed from presence-based to value-aware per R1-05, R1-06

## D3: State storage and recovery

**Choice:** On-demand computation from EventLog — no in-memory state
**Alternatives:**
- In-memory DivergenceTracker + EventLog recovery — hot-path reads but requires novel recovery pattern (no codebase precedent), order-dependent replay, thread-safety management, and startup cost proportional to active cases
- Persistence SPI — clean abstraction but adds module, Flyway migration, and test infrastructure for state derivable from EventLog
- In-memory only — simplest but loses state on restart
**Rationale:** Compute divergence on demand when the AdaptationTrigger evaluates. Query recent EventLog entries for the compound, compute the score from violation metadata recorded per completion. This eliminates recovery entirely (no state to reconstruct), eliminates thread-safety concerns (no mutable in-memory state), and requires zero startup cost. Adaptation evaluation already involves reading the CasePlanModel, building step lists, resolving trigger strategy, and potentially invoking an LLM — an EventLog query scanning a bounded set of completion events within a compound is not the bottleneck. If benchmarking proves otherwise, a lazy-computed cache with TTL is a middle ground that still requires no recovery.
**Trade-offs:** Per-query cost instead of amortized in-memory reads. Bounded per compound (only completions within that compound's bindings).
**Sources:** EventLogRepository (query SPI), WorkflowExecutionCompletedHandler.buildMetadata() (already writes producedKeys from diff)
**Exploration:** quick
**Status:** revised — changed from in-memory+recovery to on-demand computation per R1-08, R1-09, R1-10

## D4: Divergence score model

**Choice:** Windowed average over last N completions, computed on demand
**Alternatives:**
- EWMA (exponentially weighted moving average) — smooth decay but requires stateful tracking, a decay factor parameter, order-dependent replay for recovery, and contributes ancient violations forever (infinite tail)
- Count-based — simpler but doesn't normalize (10-effect action with 1 miss looks same as 1-effect action with 1 miss)
- Weighted — effects carry importance weights, most flexible but adds configuration surface
**Rationale:** Per-completion ratio (missingEffects / totalDeclaredEffects) normalizes across actions of different complexity. Windowed average over the last N completions replaces EWMA: query `count(violations) / count(completions)` in the last N EventLog entries for a compound. Recent events dominate by construction (only the last N are in the window). Clear forgetting boundary — once violations age out, they're gone. No decay parameter to tune. No state to maintain. With on-demand computation (D3), this is a pure EventLog query.
**Trade-offs:** Cliff at window boundary where old events drop off. For a threshold check (score > threshold), this cliff is arguably better than EWMA's infinite tail — a recovered plan should not carry elevated scores from ancient violations. Window size N is the sole configuration parameter.
**Sources:** research/2026-08-18-adaptive-planning-intelligence.md §2.3 (divergence-triggered adaptation concept)
**Exploration:** quick
**Status:** revised — changed from EWMA to windowed average per R1-12; removed loose SIPS citation per R1-13

## D5: Trigger architecture

**Choice:** Event + consumer trigger separation with defined threshold semantics
**Alternatives:**
- AdaptationTrigger directly — simpler integration but couples measurement to adaptation decision
- Pure measurement only — cleanest separation but #928 has no runtime effect on its own
**Rationale:** #928 publishes an ExpectationViolationEvent when per-completion divergence exceeds a per-completion threshold. This is the detection signal — an audit/observability event. #931's ProgressGatedTrigger queries the on-demand divergence score against a cumulative threshold. These serve independent purposes: per-completion events enable audit dashboards and alerting; cumulative scores drive adaptation decisions. A low per-completion threshold captures more audit events without affecting adaptation; a high cumulative threshold ensures adaptation fires only after sustained divergence.
**Module boundary:** With on-demand computation (D3), no DivergenceTracker component exists. The divergence score is computed inline by the trigger implementation (planning module) via EventLogRepository queries (api module SPI). Per-completion validation runs in a separate handler (common or runtime module) consuming an EventBus event. No cross-module stateful component needed.
**Trade-offs:** The per-completion event is fire-and-forget — consumers are optional. Without #931, the event is logged but doesn't trigger adaptation. This is intentional: the measurement infrastructure is valuable for audit even without the trigger.
**Sources:** AdaptationTrigger.java (SPI in api), EveryStepTrigger/OnFailureTrigger (existing impls in planning), EventBusAddresses (event pattern in common)
**Exploration:** quick
**Status:** revised — clarified threshold interaction per R1-15; specified module boundary per R1-16

## D6: Validation call site

**Choice:** Event-driven validation in a separate handler
**Alternatives:**
- WorkflowExecutionCompletedHandler success path — has diff and binding context but adds to a God object (879 lines, 22 injected dependencies) that already orchestrates output application, event logging, six recorder calls, CDI events, and EventBus publications
- DefaultPlanAdaptationEvaluator — runs later, would need diff threaded through
- CONTEXT_CHANGED event consumer — decoupled but CaseContextChangedEvent lacks binding identity (carries only CaseInstance, CaseContext snapshot, changedLayer)
**Rationale:** Extend buildMetadata() to record expected effects alongside the existing producedKeys extraction. Publish a new WorkerCompletionSuccessEvent on the EventBus from the handler's success path carrying (caseInstance, workerName, bindingName, capabilityName) — one additional eventBus.publish() call, no new injected dependency. A separate ExpectationValidationHandler consumes this event and: (1) looks up expected effects from CaseDefinition + GOAP actions or producedKeys, (2) reads current world state via GoapWorldState.openWorld() on the working layer, (3) computes per-completion divergence ratio, (4) fires ExpectationViolationEvent if threshold exceeded. This follows the platform's event-driven pattern (exactly how WorkerOutcomeResolvedHandler already consumes WORKER_OUTCOME_RESOLVED for failure-path lifecycle management).
**Trade-offs:** New event type adds to the EventBus surface. The validation runs asynchronously, so doesn't add latency to the critical completion path. The handler can compare current world state against declared effects directly — it doesn't need the diff threaded through.
**Sources:** WorkflowExecutionCompletedHandler.java (879 lines, 22 injected dependencies), WorkerOutcomeResolvedEvent (precedent for completion-path EventBus events), EventBusAddresses.WORKER_OUTCOME_RESOLVED (precedent pattern), buildMetadata() (already extracts producedKeys from diff)
**Depends on:** D1 (effect source determines what the validator looks up)
**Exploration:** quick
**Status:** revised — originally event-driven handler (R1-18/19/20), then changed to inline synchronous validation (spec review R1-04: timing race with adaptation evaluator). Validator is a pure computation bean; handler fires violation events.

## D7: Thread-safety model

**Choice:** No mutable shared state — inherently thread-safe
**Alternatives:**
- ConcurrentHashMap + AtomicDouble for in-memory tracker — requires careful concurrent read/write management between @RunOnVirtualThread (completion handler) and blackboard evaluation path
- Synchronized wrapper — safe but adds contention on the hot path
**Rationale:** With on-demand computation (D3), there is no mutable in-memory state. Divergence scores are computed from EventLog queries, which are inherently thread-safe (read-only database queries). The per-completion validation handler consumes events independently. No concurrent access to shared mutable state exists by construction.
**Trade-offs:** None — this is a direct consequence of D3's on-demand computation choice.
**Sources:** D3 (on-demand computation eliminates shared state)
**Exploration:** surfaced by review (R1-22)
**Status:** captured

## D8: Failure-path completion inclusion

**Choice:** Include failure-path completions as full divergence (0 of N expected effects produced)
**Alternatives:**
- Exclude failures entirely — simpler but causes over-triggering: a binding alternating between success-with-wrong-output and failure appears as 100% divergence (every success is wrong) rather than 50% (half the completions diverge)
- Special-case failures — separate failure weighting, adds complexity without proportional benefit
**Rationale:** A failed worker produces no expected effects — that's a complete divergence. Including failures normalizes the score across all completion outcomes. The system already handles failures through dedicated mechanisms (OnFailureTrigger, OutcomePolicy, retry policies). Divergence measurement should reflect reality: if half the completions fail and half succeed-but-wrong, the score should be 100% (all completions diverge), not 100% (only measuring successes, all of which diverge). This prevents adaptation over-triggering on a mixed success/failure pattern.
**Trade-offs:** Failures may dominate the divergence score in early plan execution when transient issues are common. The windowed average (D4) naturally recovers as the window slides past early failures.
**Sources:** WorkflowExecutionCompletedHandler (success path at line 124, failure path in handleSemanticFailure at line 358), WorkerOutcomeResolvedEvent (carries bindingName on failure path)
**Exploration:** surfaced by review (R1-23)
**Status:** captured

## D9: Divergence granularity

**Choice:** Per-compound measurement
**Alternatives:**
- Per-case — coarser, blends divergence across independent compound plans
- Per-binding — finer, but a single binding's violations lack statistical significance until many completions accumulate
- Cross-compound — tracks whether compound A's output invalidates compound B's preconditions
**Rationale:** Per-compound is the natural granularity for adaptation triggering. Compounds are the unit of planning (GoapPlanningStrategy operates per compound), adaptation (AdaptationTrigger evaluates per compound), and lifecycle (CompoundCompletionEvaluator tracks per compound). Cross-compound divergence — where compound A's effects invalidate compound B's plan — is handled by the GOAP planner itself: when world state changes from compound A's output, compound B's next planning cycle sees the updated state and adjusts via normal replanning.
**Trade-offs:** Misses cases where cross-compound interaction patterns indicate systemic plan failure. This is a future concern for a global adaptation orchestrator, not for #928's per-compound expectation tracking.
**Sources:** GoapPlanningStrategy (operates per compound), AdaptationTrigger (evaluates per compound), CompoundCompletionEvaluator (tracks per compound), BlackboardRegistry (keyed per case, but planning operates per compound within)
**Exploration:** surfaced by review (R1-24)
**Status:** captured
