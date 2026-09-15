# CbrRetrievalService: Integrate PlanEnsembleAnalyzer for Consensus Synthesis

**Issue:** engine#1051
**Consumer:** casehubio/fsitrading — overnight incident response agents need ensemble consensus to weight CBR recommendations.
**Date:** 2026-09-15

## Problem

`CbrRetrievalService` retrieves and adapts past case plans individually but never synthesizes them into a consensus view. The `PlanEnsembleAnalyzer` SPI exists in `casehub-neocortex-memory-api` with full ensemble classification semantics (UNANIMOUS, CONSENSUS, CONTESTED, MINORITY, UNIQUE per step) and a `@DefaultBean` no-op implementation (`NoOpPlanEnsembleAnalyzer`), but the engine never invokes it. Agents cannot see "3 of 5 past plans agree on reduce-exposure-first."

## Solution

Integrate `PlanEnsembleAnalyzer` into `CbrRetrievalService` and surface the consensus classification in the `CaseContext` working layer under a `cbrEnsemble` key.

## New Engine-Owned Types

All types in `api/src/main/java/io/casehub/api/spi/routing/` — same package as `RetrievedExperience`. These map neocortex types without leaking them into the engine API.

### `AgreementLevel`

Enum mirroring `io.casehub.neocortex.memory.cbr.StepAgreement`:

```java
public enum AgreementLevel {
    UNANIMOUS,   // all plans agree on this step
    CONSENSUS,   // majority agree
    CONTESTED,   // near-even split
    MINORITY,    // step appears in fewer than half
    UNIQUE       // step appears in exactly one plan
}
```

### `StepConsensusEntry`

Record mapping `StepConsensus` from neocortex:

```java
public record StepConsensusEntry(
    String bindingName,
    @Nullable String capabilityName,
    int occurrenceCount,
    int totalPlans,
    Map<String, Integer> workerDistribution,
    Map<String, Integer> outcomeDistribution,
    Map<Integer, Integer> priorityDistribution,
    List<String> contributingCaseIds,
    AgreementLevel agreement
) {
    // Constructor validates occurrenceCount >= 1, totalPlans >= 1
    // Defensive copies on maps and lists
}
```

All 9 fields from `StepConsensus` are mapped: `priorityDistribution` shows how step priority varies across retrieved plans (e.g., `{1: 3, 5: 2}` means 3 plans ran this step at priority 1, 2 at priority 5). `contributingCaseIds` identifies which specific cases contributed to each consensus step, enabling provenance tracing.

### `ConsensusScope`

Discriminator enum for the consensus mode:

```java
public enum ConsensusScope {
    STEP_LEVEL,    // step-level agreement from PlanEnsembleAnalyzer
    OUTCOME_ONLY   // aggregate outcome consistency only (no step analysis available)
}
```

### `EnsembleConsensus`

Record mapping `EnsemblePlan` from neocortex:

```java
public record EnsembleConsensus(
    ConsensusScope scope,
    List<StepConsensusEntry> stepAnalysis,
    double ensembleConfidence,
    int inputCount,
    List<String> sourceCaseIds
) {
    // Constructor validates ensembleConfidence in [0, 1], inputCount >= 0
    // Defensive copies on list fields
}
```

`scope` distinguishes the two consensus modes explicitly. Agents branch on `scope` rather than checking `stepAnalysis.isEmpty()`, which would be ambiguous (empty list could mean "analyzed but no steps" vs "no step analysis available").

`inputCount` uses a scope-neutral name: for `STEP_LEVEL` scope it represents the number of plans fed to `PlanEnsembleAnalyzer`; for `OUTCOME_ONLY` scope it represents the total number of experiences that contributed to the confidence computation. A plan-specific name like `inputPlanCount` would be semantically incorrect in OUTCOME_ONLY mode where zero plans were analyzed.

The `synthesizedPlan` field from `EnsemblePlan` is intentionally omitted. The engine's ensemble integration is focused on consensus **classification** (which steps agree, which are contested) rather than plan **synthesis**. Step-level analysis gives agents enough information to make informed decisions about which steps to trust. A synthesized plan is an opinionated output of the analyzer that constrains agent decision-making — agents should see the evidence (step analysis) and make their own selection. If a future consumer needs the synthesized plan, it can be added as an optional field without breaking existing consumers.

### `CbrRetrievalResult`

New return type for `CbrRetrievalService.retrieve()`:

```java
public record CbrRetrievalResult(
    List<RetrievedExperience> experiences,
    @Nullable EnsembleConsensus ensemble
) {
    public CbrRetrievalResult {
        Objects.requireNonNull(experiences);
        experiences = List.copyOf(experiences);
    }

    public static CbrRetrievalResult empty() {
        return new CbrRetrievalResult(List.of(), null);
    }
}
```

`ensemble` is null when:
- No CBR config present
- Fewer than 2 results retrieved (trivially UNANIMOUS — no information)
- Analyzer reported `inputCount < 2` (same D6 reasoning — catches NoOp and any analyzer that reduces to a single plan)
- Ensemble analysis failed or timed out (graceful degradation)

## CbrRetrievalService Changes

### Constructor

Add `PlanEnsembleAnalyzer` as a dependency:

```java
public CbrRetrievalService(
    JQEvaluator jqEvaluator,
    CbrCaseMemoryStore cbrStore,
    PlanAdapter planAdapter,
    PlanEnsembleAnalyzer ensembleAnalyzer,
    List<CbrCaseTypeRegistration> registrations,
    long ensembleTimeoutMs)
```

The existing package-private test constructor adds `PlanEnsembleAnalyzer` and uses a default timeout of `5000L`.

### Return type change

The `retrieve` methods change from `List<RetrievedExperience>` to `CbrRetrievalResult`:

- `retrieve(CaseDefinition, CaseInstance) → CbrRetrievalResult`
- `retrieve(CaseDefinition, CaseInstance, Class<C>) → CbrRetrievalResult`
- `retrieveInternal(...)` → `CbrRetrievalResult`

`retrieveForSelection(...)` overloads retain their `List<RetrievedExperience>` return type — they have zero production callers, cannot produce adapted plans (no `CaseDefinition` or `CbrConfig` available), and structurally cannot perform ensemble analysis. Changing their return type to `CbrRetrievalResult` with a guaranteed-null `ensemble` field would be coupling without value. A GitHub issue must be created during implementation to track adding ensemble support to `retrieveForSelection` when there is an actual consumer (title: "CBR: add ensemble analysis support to retrieveForSelection overloads").

### Refactored retrieval flow

`retrieveInternal()` restructured to collect intermediate state for ensemble analysis:

1. Extract features, resolve domain, build query (unchanged)
2. Call `cbrStore.retrieveSimilar()` to get `List<ScoredCbrCase<C>>`
3. **New: Partition `ScoredCbrCase` entries by type and collect adapted plans.** Iterate `scoredCases`: for each entry, check `instanceof ResolvedCase` to classify as plan-type vs non-plan. For plan-type entries, `adaptAndMapResolutionStep()` returns both the `AdaptedPlan` and the `List<ExperiencePlanStep>` via a new internal record `AdaptationResult(AdaptedPlan, List<ExperiencePlanStep>)`. **Partial adaptation failure:** when `planAdapter.adapt()` throws for a specific entry, `AdaptationResult` is constructed with both fallback components: (a) the `AdaptedPlan` is built from the raw `ResolutionStep` list — each step maps to `AdaptedStep` with `action = RETAINED` and `reason = null`; (b) the `List<ExperiencePlanStep>` is built via `mapResolutionStep()` from the raw `ResolutionStep` entries — these entries lack adaptation fields (`adaptationAction` and `reason` are null), matching the existing fallback behavior. Both must be produced because they serve different consumers: `AdaptedPlan` feeds `PlanEnsembleAnalyzer.analyze()`; `List<ExperiencePlanStep>` feeds `RetrievedExperience` for agent context. This preserves the 1:1 sizing invariant between `scoredCases` and `adaptedPlans` that `PlanEnsembleAnalyzer.analyze()` requires, while correctly representing unadapted steps as retained originals.
4. Build `List<RetrievedExperience>` from the adaptation results (unchanged mapping)
5. **New: Invoke ensemble analysis:**
   - If total count < 2 → `ensemble = null`
   - Partition results by type: `ResolvedCase` (plan-type) vs non-plan
   - If 2+ plan-type results:
     - Resolve `caseType` for `analyze()`: when `crossType` is false, use `config.caseType()` (falling back to `definition.getName()`). When `crossType` is true, collect distinct `caseType()` values from the plan-type `ScoredCbrCase` instances.
     - **Homogeneous case types** (all plan-type results share the same `caseType`): call `planEnsembleAnalyzer.analyze(caseType, scoredCases, adaptedPlans, features)`. **Post-analysis guard:** if the returned `EnsemblePlan.inputPlanCount() < 2`, set `ensemble = null` — same D6 reasoning applies: single-plan consensus is trivially UNANIMOUS and adds no information, regardless of how many plans were provided as input. This catches the `NoOpPlanEnsembleAnalyzer` (which always picks only the highest-scoring plan and reports `inputPlanCount=1` with all steps UNANIMOUS) without requiring SPI changes. Otherwise, map result to `EnsembleConsensus` with `scope = STEP_LEVEL`.
     - **Heterogeneous case types** (cross-type retrieval returned plans from multiple case types): fall to outcome-only consensus. Step-level ensemble analysis requires homogeneous case types because `bindingName` semantics are per-case-type — a binding "reduce-exposure" in case type A may have different step structure than in case type B. The `PlanEnsembleAnalyzer` SPI takes a single `caseType` and cannot represent cross-type step semantics.
   - If plan-type count < 2 but total count >= 2 → compute outcome-only consensus across all results (see Non-Plan Fallback below)
6. Return `CbrRetrievalResult(experiences, ensemble)`

### Mapping EnsemblePlan → EnsembleConsensus

```java
private EnsembleConsensus mapEnsemblePlan(EnsemblePlan plan) {
    List<StepConsensusEntry> entries = plan.stepAnalysis().stream()
        .map(sc -> new StepConsensusEntry(
            sc.bindingName(),
            sc.capabilityName(),
            sc.occurrenceCount(),
            sc.totalPlans(),
            sc.workerDistribution(),
            sc.outcomeDistribution(),
            sc.priorityDistribution(),
            sc.contributingCaseIds(),
            AgreementLevel.valueOf(sc.agreement().name())))
        .toList();
    return new EnsembleConsensus(
        ConsensusScope.STEP_LEVEL,
        entries,
        plan.ensembleConfidence(),
        plan.inputPlanCount(),
        plan.sourceCaseIds());
}
```

### Non-plan fallback

For `FeatureVectorCbrCase` and `ResolutionGuide` results (no plan traces), or for cross-type retrieval with heterogeneous plan types:

```java
private EnsembleConsensus buildOutcomeOnlyConsensus(
    List<RetrievedExperience> experiences,
    List<? extends ScoredCbrCase<?>> scoredCases) {
    double confidence = ExperienceAnalyser.outcomeConsistency(experiences);
    List<String> caseIds = scoredCases.stream()
        .map(ScoredCbrCase::caseId)
        .filter(Objects::nonNull)
        .toList();
    return new EnsembleConsensus(
        ConsensusScope.OUTCOME_ONLY,
        List.of(), confidence, experiences.size(), caseIds);
}
```

`scope = OUTCOME_ONLY` tells agents explicitly that step-level consensus is unavailable — they still get the aggregate confidence. The `ScoredCbrCase` list provides caseIds directly (rather than sourcing from `RetrievedExperience.caseId()` which may not be populated in all mapping paths).

The confidence computation delegates to `ExperienceAnalyser.outcomeConsistency()` — see ExperienceAnalyser Changes below.

### Error handling

Ensemble analysis is bounded by a configurable timeout. The `analyze()` call executes via `Executors.newVirtualThreadPerTaskExecutor()` with `future.get(timeoutMs, TimeUnit.MILLISECONDS)`, following the same pattern as `PortfolioDecompositionStrategy.executeWithTimeout()`. On `TimeoutException`, the future is cancelled and `ensemble = null` (graceful degradation).

Config property: `casehub.engine.cbr.ensemble-timeout-ms` (default: `5000`). The 5-second default is generous for `NoOpPlanEnsembleAnalyzer` (which runs in microseconds) but provides a safety net for production implementations that may perform statistical analysis or LLM-backed synthesis.

Any exception from `analyze()` (including timeout) is caught and logged at WARN level. The method returns `CbrRetrievalResult(experiences, null)` — experiences are still available. Ensemble analysis never blocks case progression. Same pattern as existing CBR failure handling. For CASE_LIFETIME caching implications, see §Caching.

### Caching

`CASE_LIFETIME` cache type changes from `ConcurrentHashMap<UUID, List<RetrievedExperience>>` to `ConcurrentHashMap<UUID, CbrRetrievalResult>`. Ensemble consensus is cached alongside experiences.

**Timeout degradation is permanent per case lifetime.** If `analyze()` times out or fails on the first retrieval for a CASE_LIFETIME case, the result `CbrRetrievalResult(experiences, null)` is cached. All subsequent retrievals for that case return the cached result — the case permanently loses ensemble data for its lifetime. This is an accepted trade-off: re-attempting ensemble analysis on cache hit would require caching intermediate state (`scoredCases`, `adaptedPlans`, features) alongside the result, significantly increasing memory footprint per cached entry. Since ensemble data is supplementary (agents always have full experiences and the case never blocks on ensemble), and CASE_LIFETIME caching exists to avoid redundant SPI calls, permanent degradation for a single case is acceptable. New cases are unaffected.

## CDI Producer Updates

### RuntimeBeans (Quarkus)

```java
@Produces
@ApplicationScoped
CbrRetrievalService cbrRetrievalService(
    JQEvaluator jqEvaluator,
    CbrCaseMemoryStore cbrStore,
    PlanAdapter planAdapter,
    PlanEnsembleAnalyzer ensembleAnalyzer,
    @All Instance<CbrCaseTypeRegistration> registrations,
    @ConfigProperty(name = "casehub.engine.cbr.ensemble-timeout-ms",
                    defaultValue = "5000") long ensembleTimeoutMs) {
  return new CbrRetrievalService(
      jqEvaluator, cbrStore, planAdapter, ensembleAnalyzer,
      StreamSupport.stream(registrations.spliterator(), false).toList(),
      ensembleTimeoutMs);
}
```

### RuntimeManualConfig (Spring)

```java
@Bean
public CbrRetrievalService cbrRetrievalService(
    JQEvaluator jqEvaluator,
    CbrCaseMemoryStore cbrStore,
    PlanAdapter planAdapter,
    PlanEnsembleAnalyzer ensembleAnalyzer,
    List<CbrCaseTypeRegistration> registrations,
    @Value("${casehub.engine.cbr.ensemble-timeout-ms:5000}") long ensembleTimeoutMs) {
  return new CbrRetrievalService(
      jqEvaluator, cbrStore, planAdapter, ensembleAnalyzer, registrations,
      ensembleTimeoutMs);
}
```

## ExperienceAnalyser Changes

Add a static method to `ExperienceAnalyser` (`api/spi/routing/`) to consolidate the outcome-consistency computation:

```java
public static double outcomeConsistency(List<RetrievedExperience> experiences) {
    Map<String, Long> freq = experiences.stream()
        .map(RetrievedExperience::outcome)
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    if (freq.isEmpty()) {
        return 0.0;
    }
    return (double) Collections.max(freq.values()) / experiences.size();
}
```

This consolidates the computation currently duplicated in `CaseStartedEventHandler.computeOutcomeConsistency()`. After this change, `CaseStartedEventHandler` replaces its private `computeOutcomeConsistency()` with a call to `ExperienceAnalyser.outcomeConsistency()`, and `CbrRetrievalService.buildOutcomeOnlyConsensus()` also delegates to it.

## Caller Updates

All 6 production call sites of `retrieve()` change mechanically. Most callers only need `.experiences()`. `retrieveForSelection()` callers are unaffected (return type unchanged).

| Caller | Change |
|--------|--------|
| `CaseStartedEventHandler.injectCbrExperiences()` | Use `result.experiences()` for existing logic + write `result.ensemble()` to context; replace `computeOutcomeConsistency()` with `ExperienceAnalyser.outcomeConsistency()` |
| `CaseContextChangedEventHandler.rules()` | `result.experiences()` |
| `DefaultGoalDecomposer.decomposeGoal()` | `result.experiences()` |
| `DeeperDecompositionHandler.tryDecompose()` | `result.experiences()` |
| `DefaultPlanAdaptationEvaluator.retrieveExperiences()` | `result.experiences()` |
| `DefaultWorkOrchestrator.doSubmit()` | `result.experiences()` |

## Context Surfacing

In `CaseStartedEventHandler.injectCbrExperiences()`, after existing `engineSet` calls:

```java
CbrRetrievalResult result = cbrRetrievalService.retrieve(definition, instance);
List<RetrievedExperience> experiences = result.experiences();
if (!experiences.isEmpty()) {
    // ... existing engineSet calls for cbrExperiences, cbrBestSimilarity, etc.
    if (result.ensemble() != null) {
        Map<String, Object> ensembleMap = OBJECT_MAPPER.convertValue(
            result.ensemble(), new TypeReference<Map<String, Object>>() {});
        layer.engineSet("cbrEnsemble", ensembleMap);
    }
}
```

### Relationship to existing `cbrOutcomeConsistency`

After this change, the working layer context contains both:
- `cbrOutcomeConsistency` — raw outcome consistency (max-outcome-frequency / total). Always based on outcome distribution regardless of case type. Computed via `ExperienceAnalyser.outcomeConsistency()`.
- `cbrEnsemble.ensembleConfidence` — ensemble confidence. For `STEP_LEVEL` scope: reflects step-level agreement from `PlanEnsembleAnalyzer`. For `OUTCOME_ONLY` scope: identical value to `cbrOutcomeConsistency`.

These are **complementary, not redundant** for plan-type cases: `cbrOutcomeConsistency` answers "how consistent were the case outcomes?" while `cbrEnsemble.ensembleConfidence` answers "how much did the plan steps agree?" For non-plan cases, both values are identical (both measure outcome consistency).

`cbrOutcomeConsistency` is **not deprecated** — it remains the stable, simple outcome metric. `cbrEnsemble` provides the richer signal when step-level analysis is available.

### Agent-facing context shape

```json
{
  "cbrEnsemble": {
    "scope": "STEP_LEVEL",
    "stepAnalysis": [
      {
        "bindingName": "reduce-exposure",
        "capabilityName": "risk-mitigation",
        "occurrenceCount": 3,
        "totalPlans": 5,
        "workerDistribution": {"risk-analyst-1": 2, "risk-analyst-2": 1},
        "outcomeDistribution": {"SUCCESS": 3},
        "priorityDistribution": {"1": 3},
        "contributingCaseIds": ["case-a", "case-b", "case-d"],
        "agreement": "CONSENSUS"
      },
      {
        "bindingName": "escalate-to-desk",
        "capabilityName": "escalation",
        "occurrenceCount": 2,
        "totalPlans": 5,
        "workerDistribution": {"desk-manager": 2},
        "outcomeDistribution": {"SUCCESS": 1, "DECLINED": 1},
        "priorityDistribution": {"3": 1, "5": 1},
        "contributingCaseIds": ["case-c", "case-e"],
        "agreement": "CONTESTED"
      }
    ],
    "ensembleConfidence": 0.72,
    "inputCount": 5,
    "sourceCaseIds": ["case-a", "case-b", "case-c", "case-d", "case-e"]
  }
}
```

Agents query this with JQ:
- `.cbrEnsemble | select(.scope == "STEP_LEVEL") | .stepAnalysis[] | select(.agreement == "UNANIMOUS")` — steps all past plans agree on (only when step analysis is available)
- `.cbrEnsemble.stepAnalysis[] | select(.agreement == "CONTESTED")` — steps with disagreement
- `.cbrEnsemble.ensembleConfidence` — overall confidence level
- `.cbrEnsemble.scope` — check consensus mode before querying step-level data

## Testing

### Unit tests

- `CbrRetrievalServiceTest` — new tests:
  - `ensemble_invoked_for_plan_type_with_multiple_results` — verify `PlanEnsembleAnalyzer.analyze()` called, result mapped to `EnsembleConsensus` with `scope = STEP_LEVEL`
  - `ensemble_skipped_for_single_result` — verify `ensemble` is null when < 2 results
  - `ensemble_skipped_for_empty_results` — verify `CbrRetrievalResult.empty()` returned
  - `ensemble_failure_returns_null_ensemble` — verify graceful degradation
  - `non_plan_type_gets_outcome_only_consensus` — verify fallback path with `scope = OUTCOME_ONLY`
  - `outcome_only_confidence_computed_correctly` — verify ratio logic via `ExperienceAnalyser.outcomeConsistency()`
  - `mixed_crossType_results_fall_to_outcome_only` — verify mixed `ResolvedCase` + `ResolutionGuide` with < 2 plans uses outcome-only fallback
  - `mixed_crossType_with_enough_plans_uses_ensemble` — verify mixed results with 2+ `ResolvedCase` (same caseType) uses `PlanEnsembleAnalyzer` on the plan subset
  - `crossType_heterogeneous_caseTypes_fall_to_outcome_only` — verify cross-type retrieval with plans from different case types uses outcome-only consensus
  - `ensemble_cached_with_case_lifetime` — verify cache stores `CbrRetrievalResult`
  - `concurrent_retrieve_with_case_lifetime_caching` — two threads call `retrieve()` concurrently for the same case; verify both get a valid result, at most one result is stored (via `synchronized cacheIfUnderBound` + `putIfAbsent`), and subsequent single-threaded retrievals return the cached result. Note: both threads may independently compute the full pipeline including ensemble analysis — `putIfAbsent` prevents double **storage**, not double **computation**
  - `partial_adaptation_failure_preserves_sizing_invariant` — verify that when `planAdapter.adapt()` throws for some `ResolvedCase` entries, fallback `AdaptedPlan` (RETAINED steps) is produced for those entries, and `analyze()` receives equal-sized `scoredCases` and `adaptedPlans` lists
  - `ensemble_timeout_returns_null_ensemble` — verify that when `analyze()` exceeds `ensemble-timeout-ms`, ensemble is null and experiences are still returned
  - `analyzer_reporting_single_plan_returns_null_ensemble` — verify that when `PlanEnsembleAnalyzer.analyze()` returns `inputCount < 2` (as `NoOpPlanEnsembleAnalyzer` does), ensemble is null despite 2+ plan inputs
  - `step_consensus_maps_all_nine_fields` — verify `priorityDistribution` and `contributingCaseIds` mapped from `StepConsensus`

- `EnsembleConsensusTest` — record validation tests (including `ConsensusScope` field)
- `StepConsensusEntryTest` — record validation tests (including `priorityDistribution` and `contributingCaseIds`)
- `CbrRetrievalResultTest` — record validation, `empty()` factory

- `ExperienceAnalyserTest` — new test:
  - `outcomeConsistency_computed_correctly` — verify ratio matches existing `computeOutcomeConsistency` behavior

### Integration tests

- `CaseStartedEventHandlerTest` — verify `cbrEnsemble` written to context when ensemble present, not written when null

## CLAUDE.md Updates

Add to `## CBR Retrieval Bridge` section:

```
`EnsembleConsensus` (`api/spi/routing/`) — engine-owned read model for neocortex `EnsemblePlan`.
Fields: `scope` (ConsensusScope: STEP_LEVEL | OUTCOME_ONLY), `stepAnalysis` (List<StepConsensusEntry>),
`ensembleConfidence` (double), `inputCount` (int), `sourceCaseIds` (List<String>).
`StepConsensusEntry` carries per-step consensus: `bindingName`, `capabilityName`, `occurrenceCount`,
`totalPlans`, `workerDistribution`, `outcomeDistribution`, `priorityDistribution`, `contributingCaseIds`,
`agreement` (AgreementLevel enum: UNANIMOUS/CONSENSUS/CONTESTED/MINORITY/UNIQUE).
`CbrRetrievalResult` (`api/spi/routing/`) wraps `List<RetrievedExperience>` and nullable
`EnsembleConsensus`. `retrieve()` overloads return `CbrRetrievalResult`; `retrieveForSelection()`
retains `List<RetrievedExperience>`. Ensemble analysis invoked for plan-type with 2+ results (homogeneous
caseType); cross-type with heterogeneous types and non-plan types get outcome-only consensus.
`ExperienceAnalyser.outcomeConsistency()` consolidates outcome ratio computation.
Context surfacing: `cbrEnsemble` key in working layer via `CaseStartedEventHandler`.
`cbrOutcomeConsistency` remains (complementary, not deprecated).
Config: `casehub.engine.cbr.ensemble-timeout-ms` (default 5000) bounds `PlanEnsembleAnalyzer.analyze()`.
Partial adaptation failures produce fallback `AdaptedPlan` (RETAINED steps) to preserve sizing invariant.
CASE_LIFETIME caching: timeout/failure degradation is permanent per case — ensemble is supplementary.
Refs engine#1051.
```

## References

- `runtime-core/.../CbrRetrievalService.java:56` — retrieval service (integration point)
- `runtime/.../CaseStartedEventHandler.java:175` — context injection site
- `casehub-neocortex-memory-api` — `PlanEnsembleAnalyzer`, `EnsemblePlan`, `StepConsensus`, `StepAgreement` SPI types
- `casehub-neocortex-memory` — `NoOpPlanEnsembleAnalyzer` @DefaultBean
- `api/.../RetrievedExperience.java` — existing engine-owned CBR type (pattern reference)
- `api/.../ExperienceAnalyser.java` — existing CBR analysis utility (pattern reference)
- casehubio/fsitrading#36 — consumer issue noting the gap
