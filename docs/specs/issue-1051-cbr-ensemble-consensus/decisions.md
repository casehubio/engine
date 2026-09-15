## D1: Scope — plan-type vs all CBR types

**Choice:** All types with fallback
**Alternatives:**
- Plan-type only — skip ensemble for non-plan cbrTypes. Simpler but leaves feature-vector and textual cases without any consensus signal.
**Rationale:** Consumer (fsitrading) may use mixed retrieval types. A degraded consensus signal (outcome agreement without step analysis) is still valuable for non-plan types.
**Trade-offs:** Slightly more complex implementation — need a fallback path for non-plan types that computes outcome-level consensus without step traces.
**Sources:** PlanEnsembleAnalyzer SPI (neocortex-memory-api), CbrRetrievalService (runtime-core)
**Exploration:** quick
**Status:** captured

## D2: Context surfacing location

**Choice:** Top-level `cbrEnsemble` key in the working layer
**Alternatives:**
- Nested inside `cbrExperiences` — keeps all CBR data in one place but mixes per-experience and aggregate data, making JQ queries harder.
**Rationale:** Clean separation. Agents query `.cbrEnsemble.stepAnalysis` directly without navigating mixed structures. Consistent with existing `cbrBestSimilarity`, `cbrMatchCount` as separate top-level keys.
**Trade-offs:** One more top-level key in the working layer.
**Sources:** CaseStartedEventHandler.injectCbrExperiences() (runtime)
**Exploration:** quick
**Status:** captured

## D3: Integration point

**Choice:** CbrRetrievalService — invoke ensemble inside the retrieval service
**Alternatives:**
- CaseStartedEventHandler only — simpler change but routing strategies and decomposition don't see consensus programmatically.
**Rationale:** All consumers (routing strategies, decomposition, adaptation, context injection) benefit from programmatic access. The adapted plans and scored cases are already in scope inside `retrieveInternal()`.
**Trade-offs:** All callers of `retrieve()` must update to the new return type (mechanical change).
**Sources:** CbrRetrievalService call sites (CaseStartedEventHandler, CaseContextChangedEventHandler, DefaultGoalDecomposer, DeeperDecompositionHandler, DefaultPlanAdaptationEvaluator, DefaultWorkOrchestrator)
**Exploration:** quick
**Status:** captured

## D4: Return type design

**Choice:** New `CbrRetrievalResult` record wrapping `List<RetrievedExperience>` and nullable `EnsembleConsensus`
**Alternatives:**
- Parallel method (no return type change) — avoids breaking callers but requires re-computation of adapted plans or internal caching, since they're discarded after mapping.
- Decorator pattern — non-breaking but requires interface extraction and a side-channel registry.
**Rationale:** Mechanical caller update (`.experiences()`) vs re-computation problem or unnecessary indirection. The adapted plans are computed inside `retrieveInternal()` and discarded — a parallel method would need to redo the work.
**Trade-offs:** Breaking change to `retrieve()` return type.
**Sources:** CbrRetrievalService.retrieveInternal(), adaptAndMapResolutionStep()
**Exploration:** quick
**Status:** captured

## D5: Non-plan fallback mechanism

**Choice:** Outcome-only consensus
**Alternatives:**
- Skip for non-plan — return null EnsembleConsensus, contradicts D1 "all types" decision.
- Separate non-plan SPI — over-engineering for v1.
**Rationale:** Reuses existing cbrOutcomeConsistency computation pattern. Agents see "4 of 5 past cases succeeded" without step breakdown. Meaningful signal with minimal complexity.
**Trade-offs:** No step-level analysis for non-plan types — agents only see aggregate outcome agreement.
**Depends on:** D1 (all types with fallback)
**Sources:** CaseStartedEventHandler.computeOutcomeConsistency()
**Exploration:** quick
**Status:** captured

## D6: Minimum plan count threshold

**Choice:** Skip ensemble analysis for < 2 retrieved plans
**Alternatives:**
- Always invoke — more uniform but wastes a call for the trivial single-plan case.
**Rationale:** Single-plan consensus is trivially UNANIMOUS and adds no information. Saves the SPI call and avoids meaningless data in the context.
**Trade-offs:** Agents see null ensemble for single-plan retrievals — must handle the null case.
**Depends on:** D3 (integration point in CbrRetrievalService)
**Sources:** NoOpPlanEnsembleAnalyzer (neocortex-memory)
**Exploration:** quick
**Status:** captured
