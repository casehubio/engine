# Decisions — #937 Learned Action Costs from CBR Traces

## D1: Cost signal — success rate only

**Choice:** Use `1.0 / successRate` as the learned cost multiplier. No duration data.
**Alternatives:**
- Add duration to PlanTrace — requires cross-repo change to neocortex-memory-api
- EventLog-based duration — adds query cost at planning time, complex
**Rationale:** ExperiencePlanStep carries RoutingOutcome but not duration. Success rate captures the key insight (unreliable actions are more expensive) without cross-repo changes. Duration is a future refinement.
**Trade-offs:** Misses latency signal — a 95% success action taking 30s looks the same as one taking 2s.
**Sources:** ExperiencePlanStep.java:21, PlanTrace (neocortex-memory-api jar), CbrCaseRetainObserver.java:54
**Exploration:** quick
**Status:** captured

## D2: Data source — CBR similarity retrieval

**Choice:** At planning time, retrieve similar cases via CbrRetrievalService, compute per-action success rates from their plan traces.
**Alternatives:**
- Aggregate cache — O(1) lookup but loses similarity weighting
- Hybrid — most complex, marginal benefit over similarity-only
**Rationale:** Similarity-weighted costs adjust based on how similar the current case is to past cases. A case similar to past failures should see inflated costs for failing actions. This is the "closed learning loop" described in the research.
**Trade-offs:** Adds CBR query latency (~10-50ms) to decomposition. Mitigated by case-lifetime caching on CbrRetrievalService.
**Sources:** CbrRetrievalService.java:140, research/2026-08-18-adaptive-planning-intelligence.md sec 3.10
**Exploration:** quick
**Status:** captured

## D3: SPI placement — static utility on ExperienceAnalyser

**Choice:** Add `actionSuccessRates()` static method to ExperienceAnalyser alongside existing `workerSuccessRates()`.
**Alternatives:**
- SPI in engine-common/spi/ — adds types, requires EngineStrategyResolver update per GE-20260704-d6aacc
- Concrete bean in runtime — less discoverable, no extensibility
**Rationale:** Follows the existing pattern exactly. ExperienceAnalyser already aggregates plan trace data. No new types, no CDI wiring, no EngineStrategyResolver changes.
**Trade-offs:** Not extensible to non-CBR cost sources. Acceptable for v1 — can extract to SPI if a second cost source emerges.
**Sources:** ExperienceAnalyser.java:31, GE-20260704-d6aacc, GE-20260810-b53fd8
**Exploration:** quick
**Status:** captured

## D4: Config location — CbrConfig on CaseDefinition

**Choice:** Add `minCostSamples` field to CbrConfig. YAML: `cbr: { minCostSamples: 5 }`.
**Alternatives:**
- Quarkus config property — deployment-wide, not per-case
- Both — adds complexity for marginal benefit
**Rationale:** CbrConfig already governs CBR retrieval. Per-case-definition control lets different case types have different thresholds based on their action space size and data availability.
**Trade-offs:** No deployment-wide default override. Acceptable — the default (5) is sensible.
**Sources:** CbrConfig.java (api/model/cbr/)
**Exploration:** quick
**Status:** captured

## D5: Scope — both GOAP strategies

**Choice:** Apply learned costs to both GoapDecompositionStrategy (upfront plan) and GoapPlanningStrategy (dispatch-time).
**Alternatives:**
- Decomposition only — simpler scope, defers dispatch-time
**Rationale:** PlanExecutionContext already carries `List<RetrievedExperience>` at dispatch time (populated at CaseContextChangedEventHandler:244). Both strategies share the same cost composition logic — extracting it to ExperienceAnalyser means both benefit with minimal extra plumbing.
**Trade-offs:** Slightly larger scope. Mitigated by shared utility method.
**Sources:** PlanExecutionContext.java:44, CaseContextChangedEventHandler.java:244
**Exploration:** quick
**Status:** captured
