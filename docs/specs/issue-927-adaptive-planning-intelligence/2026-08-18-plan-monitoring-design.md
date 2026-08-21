# Plan Monitoring and Expectation Tracking — Design Spec

**Issue:** #928
**Date:** 2026-08-18
**Module:** `casehub-engine-api` (types, config), `casehub-engine-common` (event, score computation), `casehub-engine` runtime (validation handler)

---

## Summary

Validate actual context changes against declared effects after worker completion. Track cumulative divergence per compound via on-demand EventLog computation. Publish detection events when per-completion divergence exceeds a threshold. This provides the measurement foundation that #931 (progress-gated trigger) and #934 (meta-reasoning) depend on.

## Motivation

Every adaptation technique in the literature requires a measurement of "how wrong is the current state?" Without expectation tracking, the engine can only react to discrete events (completion, failure) — it cannot detect *silent* plan degradation where steps succeed but produce unexpected results.

Consider: Worker A succeeds but produces output that doesn't satisfy Worker B's preconditions. Without expectation tracking, Worker B gets dispatched, fails, gets rerouted, fails again — burning retries on a problem detectable immediately after Worker A completed.

GOAP actions already declare preconditions and effects. `@Effect` annotations map return types to context keys. `Binding.producedKeys` declares expected output keys. This build-time metadata is currently unused at runtime for validation.

---

## Architecture

### Module Placement

| Component | Module | Package |
|-----------|--------|---------|
| `ExpectedEffects` | `engine-api` | `io.casehub.engine.plan.monitoring` |
| `ViolationRecord` | `engine-api` | `io.casehub.engine.plan.monitoring` |
| `MonitoringConfig` | `engine-api` | `io.casehub.engine.plan.monitoring` |
| `ExpectedEffectResolver` | `engine-common` | `io.casehub.engine.common.internal.monitoring` |
| `DivergenceScoreComputer` | `engine-common` | `io.casehub.engine.common.internal.monitoring` |
| `ExpectationViolationEvent` | `engine-common` | `io.casehub.engine.common.internal.event` |
| `ExpectationValidator` | `runtime` | `io.casehub.engine.internal.engine.handler` |
| `ExpectationValidationResult` | `runtime` | `io.casehub.engine.internal.engine.handler` |
| `CaseHubEventType.EXPECTATION_VALIDATED` | `engine-api` | existing enum |
| `EventBusAddresses.EXPECTATION_VIOLATED` | `engine-common` | existing class |

**Boundary test (PP-20260727-5267d2):** `ExpectedEffects` and `MonitoringConfig` are consumer-referenced (case definition configuration) — engine-api. `ExpectedEffectResolver` and `DivergenceScoreComputer` are internal infrastructure — engine-common. `DivergenceScoreComputer` is in common so #931's `ProgressGatedTrigger` (planning module) can import it.

---

## 1. ExpectedEffects Record (engine-api)

```java
package io.casehub.engine.plan.monitoring;

public record ExpectedEffects(
    Map<String, Boolean> effects,
    EffectSource source
) {
  public enum EffectSource { GOAP, PRODUCED_KEYS }

  public ExpectedEffects {
    effects = effects == null ? Map.of() : Map.copyOf(effects);
    Objects.requireNonNull(source, "source");
  }

  public boolean isEmpty() { return effects.isEmpty(); }
}
```

Two sources unified into one type:
- **GOAP:** `Map<String, Boolean>` from `GoapAction.effects()` — directional (key → true means "should be present", key → false means "should be absent")
- **PRODUCED_KEYS:** `Set<String>` from `Binding.producedKeys` — converted to `Map<String, Boolean>` with all values `true` (presence-only)

---

## 2. ViolationRecord (engine-api)

```java
package io.casehub.engine.plan.monitoring;

public record ViolationRecord(
    String effectKey,
    boolean expectedValue,
    Condition actualCondition
) {}
```

Per-key record of what was expected vs what was found. Used in EventLog metadata for audit and on-demand divergence computation.

---

## 3. MonitoringConfig (engine-api)

Per-case configuration on `CaseDefinition`:

```java
package io.casehub.engine.plan.monitoring;

public record MonitoringConfig(
    boolean enabled,
    double perCompletionThreshold,
    int windowSize
) {
  public static final double DEFAULT_THRESHOLD = 0.5;
  public static final int DEFAULT_WINDOW_SIZE = 5;

  public MonitoringConfig {
    if (perCompletionThreshold < 0.0 || perCompletionThreshold > 1.0) {
      throw new IllegalArgumentException("perCompletionThreshold must be in [0.0, 1.0]");
    }
    if (windowSize < 1) {
      throw new IllegalArgumentException("windowSize must be >= 1");
    }
  }

  public static MonitoringConfig defaults() {
    return new MonitoringConfig(true, DEFAULT_THRESHOLD, DEFAULT_WINDOW_SIZE);
  }

  public static MonitoringConfig disabled() {
    return new MonitoringConfig(false, DEFAULT_THRESHOLD, DEFAULT_WINDOW_SIZE);
  }
}
```

**CaseDefinition** gains `monitoringConfig` (nullable `MonitoringConfig`). When null, monitoring is disabled. Builder: `.monitoring(MonitoringConfig.defaults())`. YAML:

```yaml
spec:
  monitoring:
    enabled: true
    perCompletionThreshold: 0.5
    windowSize: 5
```

---

## 4. ExpectedEffectResolver (engine-common)

`@ApplicationScoped` utility resolving expected effects for a binding:

```java
@ApplicationScoped
public class ExpectedEffectResolver {

  public ExpectedEffects resolve(CaseDefinition definition, String bindingName) {
    // 1. Try GOAP: find binding → capability name → GoapAction
    Binding binding = findBindingByName(definition, bindingName);
    if (binding == null) return new ExpectedEffects(Map.of(), EffectSource.GOAP);

    if (binding.target() instanceof CapabilityTarget ct) {
      String capabilityName = ct.capability().name();
      for (GoapAction action : definition.getGoapActions()) {
        if (action.name().equals(capabilityName) && !action.effects().isEmpty()) {
          return new ExpectedEffects(action.effects(), EffectSource.GOAP);
        }
      }
    }

    // 2. Fall back to producedKeys
    Set<String> producedKeys = binding.getProducedKeys();
    if (producedKeys != null && !producedKeys.isEmpty()) {
      Map<String, Boolean> effects = new HashMap<>();
      producedKeys.forEach(key -> effects.put(key, true));
      return new ExpectedEffects(effects, EffectSource.PRODUCED_KEYS);
    }

    return new ExpectedEffects(Map.of(), EffectSource.GOAP);
  }
}
```

**Resolution order:** GOAP action effects take precedence when both exist. In practice, GOAP bindings subsume producedKeys — the richer model includes all the information the simpler one provides. `producedKeys` is the fallback for non-GOAP bindings. A `CapabilityTarget` binding CAN have both — GOAP wins (D1).

**`findBindingByName` implementation:** `CaseDefinition` does not have a `findBindingByName` method (only `findBindingsByCapability`). The resolver iterates `definition.getBindings()` and filters by `binding.getName().equals(bindingName)`. This is a private helper in `ExpectedEffectResolver`, not a new method on `CaseDefinition` — the pattern is single-use here.

---

## 5. Validation Logic

### 5.1 Effect Validation

Validation uses `Condition` semantics from the ternary world state (#929):

**For GOAP effects** (`Map<String, Boolean>`):
```java
GoapWorldState worldState = GoapWorldState.openWorld(workingLayer);
for (Map.Entry<String, Boolean> effect : expectedEffects.entrySet()) {
  Condition expected = Condition.fromBoolean(effect.getValue());
  Condition actual = worldState.get(effect.getKey());
  if (actual != expected) {
    violations.add(new ViolationRecord(effect.getKey(), effect.getValue(), actual));
  }
}
```

UNKNOWN is treated as a violation — if an effect declares `key: true` and the key is absent (UNKNOWN in the working layer), the effect is unsatisfied. This is the correct closed-world semantic for post-completion validation: the worker has run and its output has been applied — any declared effect that isn't present in the working layer is genuinely missing, not unknowable.

**For producedKeys** (`Set<String>`, all `true`):
Same logic — `Condition.fromBoolean(true)` vs `worldState.get(key)`. Presence-only validation is a subset of value-aware validation.

### 5.2 Per-Completion Divergence Ratio

```java
double ratio = violations.size() / (double) expectedEffects.size();
```

Normalized to [0.0, 1.0]. 0.0 = all effects satisfied. 1.0 = no effects satisfied.

---

## 6. Inline Validation and Event Integration

### 6.1 Synchronous Validation in Success Path

Validation runs **synchronously** inside `WorkflowExecutionCompletedHandler`, BEFORE `CONTEXT_CHANGED` is published. This ensures validation EventLog entries are available when `PlanAdaptationEvaluator` runs (triggered by `CONTEXT_CHANGED` → PlanItem completion → adaptation evaluation). An async validation handler would race with the adaptation evaluator — the validation EventLog might not be written before the trigger queries it.

The handler delegates to `ExpectationValidator` (injected `@ApplicationScoped` bean) — one new injected dependency:

```java
// In onWorkflowExecutionCompletedHandler(), after contextOutputApplier.apply():
String compoundId = resolveCompoundId(caseInstance, bindingName);
ExpectationValidationResult validationResult =
    expectationValidator.validate(caseInstance, definition, bindingName, compoundId);
```

**Compound ID resolution:** `resolveCompoundId()` reads PlanItem metadata from EventLog — the `WORKER_SCHEDULED` entry for this binding carries `compoundId` in its metadata (written by `WorkerScheduleEventHandler`). Query: most recent `WORKER_SCHEDULED` EventLog for this `caseId + bindingName`. Null when no compound owns the binding (case-level validation). This avoids coupling the handler to `BlackboardRegistry` (planning module).

### 6.2 ExpectationViolationEvent (engine-common)

Published asynchronously by the handler when per-completion divergence exceeds threshold:

```java
public record ExpectationViolationEvent(
    UUID caseId,
    String tenancyId,
    String compoundId,
    String bindingName,
    String workerName,
    double divergenceRatio,
    List<ViolationRecord> violations
) {}
```

Published on `EventBusAddresses.EXPECTATION_VIOLATED`. Fire-and-forget observability/alerting signal — no direct consumer in #931. `ProgressGatedTrigger` queries EventLog divergence scores via `DivergenceScoreComputer`, not this event. Consumers: monitoring dashboards, alerting hooks, audit observers.

### 6.3 Handler Integration Point

In `WorkflowExecutionCompletedHandler.onWorkflowExecutionCompletedHandler()`, the validation call is inserted after `contextOutputApplier.apply()` and the `recordSuccessOutcome()` call, but BEFORE `eventBus.publish(CONTEXT_CHANGED)`:

```java
// Validate expectations (synchronous — must complete before CONTEXT_CHANGED)
String compoundId = resolveCompoundId(caseInstance, bindingName);
ExpectationValidationResult validationResult =
    expectationValidator.validate(caseInstance, definition, bindingName, compoundId);

// Enrich EventLog metadata with validation results
if (validationResult != null) {
  enrichMetadataWithValidation(metadata, validationResult);
}

// ... existing: build EventLog, append, fire CONTEXT_CHANGED
```

---

## 7. ExpectationValidator (runtime)

`@ApplicationScoped` bean encapsulating all validation logic:

```java
@ApplicationScoped
public class ExpectationValidator {

  @Inject ExpectedEffectResolver effectResolver;
  @Inject EventLogRepository eventLogRepository;
  @Inject EventBus eventBus;

  public ExpectationValidationResult validate(
      CaseInstance instance, CaseDefinition definition,
      String bindingName, String compoundId) {

    MonitoringConfig config = definition.getMonitoringConfig();
    if (config == null || !config.enabled()) return null;

    ExpectedEffects expected = effectResolver.resolve(definition, bindingName);
    if (expected.isEmpty()) return null;

    // Validate against current working layer
    JsonNode workingLayer = instance.getCaseContext()
        .layer(ContextLayer.WORKING).asJsonNode();
    GoapWorldState worldState = GoapWorldState.openWorld(workingLayer);

    List<ViolationRecord> violations = new ArrayList<>();
    for (var entry : expected.effects().entrySet()) {
      Condition expectedCondition = Condition.fromBoolean(entry.getValue());
      Condition actual = worldState.get(entry.getKey());
      if (actual != expectedCondition) {
        violations.add(new ViolationRecord(
            entry.getKey(), entry.getValue(), actual));
      }
    }

    double ratio = violations.size() / (double) expected.effects().size();

    var result = new ExpectationValidationResult(
        bindingName, compoundId, expected, violations, ratio);

    // Fire violation event asynchronously if threshold exceeded
    if (ratio > config.perCompletionThreshold()) {
      eventBus.publish(
          EventBusAddresses.EXPECTATION_VIOLATED,
          new ExpectationViolationEvent(
              instance.getUuid(), instance.tenancyId, compoundId,
              bindingName, instance.getWorkerName(),
              ratio, violations));
    }

    return result;
  }
}
```

**`ExpectationValidationResult`** (runtime-internal record):
```java
record ExpectationValidationResult(
    String bindingName,
    String compoundId,
    ExpectedEffects expected,
    List<ViolationRecord> violations,
    double divergenceRatio
) {}
```

**EventLog metadata enrichment** — validation results are written to the existing completion EventLog (not a separate entry), enriching `buildMetadata()`:

```json
{
  "inputDataHash": "abc123",
  "contextChanges": { ... },
  "producedKeys": ["entityResolved", "riskScore"],
  "expectationValidation": {
    "compoundId": "investigation-compound",
    "totalExpectedEffects": 3,
    "violatedEffectCount": 1,
    "divergenceRatio": 0.333,
    "effectSource": "GOAP",
    "adaptationGeneration": 0,
    "violations": [
      {"key": "entityResolved", "expected": true, "actual": "UNKNOWN"}
    ]
  }
}
```

The `adaptationGeneration` field tracks which compound generation this validation belongs to. After plan adaptation (#803), the generation increments. `DivergenceScoreComputer` filters by generation to avoid post-adaptation window contamination — pre-adaptation violations are not included in the new plan's divergence score.

---

## 8. On-Demand Divergence Score Computation (engine-common)

### 8.1 DivergenceScoreComputer

Pure utility with no CDI dependencies — takes EventLog records as input:

```java
public final class DivergenceScoreComputer {

  private DivergenceScoreComputer() {}

  public static double computeForCompound(
      List<EventLog> recentValidations, int windowSize, int adaptationGeneration) {
    List<EventLog> filtered = recentValidations.stream()
        .filter(e -> {
          JsonNode meta = e.getMetadata();
          if (meta == null || !meta.has("expectationValidation")) return false;
          JsonNode validation = meta.get("expectationValidation");
          int gen = validation.has("adaptationGeneration")
              ? validation.get("adaptationGeneration").asInt() : 0;
          return gen == adaptationGeneration;
        })
        .toList();

    if (filtered.isEmpty()) return 0.0;

    List<EventLog> window = filtered.size() <= windowSize
        ? filtered
        : filtered.subList(filtered.size() - windowSize, filtered.size());

    double totalRatio = 0.0;
    for (EventLog entry : window) {
      JsonNode validation = entry.getMetadata().get("expectationValidation");
      totalRatio += validation.get("divergenceRatio").asDouble();
    }
    return totalRatio / window.size();
  }
}
```

**Generation filtering:** After plan adaptation (#803), the compound's `adaptationGeneration` increments. `computeForCompound` filters EventLog entries to only those at the current generation, ensuring each adapted plan starts with a clean divergence baseline. Pre-adaptation violations are excluded — they triggered the adaptation and should not re-trigger it.

**Query pattern for #931:** `ProgressGatedTrigger` queries `EventLogRepository` for `WORKER_EXECUTION_COMPLETED` events filtered by `caseId`, reads `expectationValidation` from metadata, and passes the results to `DivergenceScoreComputer.computeForCompound()` with the current `adaptationGeneration` from `AdaptationContext`.

### 8.2 Failure-Path Inclusion (D8)

Failed workers produce no expected effects — that is complete divergence. The `handleSemanticFailure` path in `WorkflowExecutionCompletedHandler` also publishes `WorkerCompletionValidationEvent`. The validation handler treats a failure outcome as `ratio = 1.0` (0 of N effects produced) and writes an `EXPECTATION_VALIDATED` EventLog entry. This normalizes the divergence score across all completion outcomes.

The failure path in `WorkflowExecutionCompletedHandler` also calls `expectationValidator.validate()`. The validation runs normally — a failed worker's output was never applied, so its declared effects are genuinely missing in the working layer. The validation logic handles this correctly without special-casing: the presence check against `GoapWorldState.openWorld()` reports all expected effects as violations, producing ratio 1.0.

**Reroute interaction:** Intermediate reroute failures contribute ratio 1.0 to the divergence score. If a binding succeeds after rerouting (Worker A fails → Worker B succeeds), the window contains both the failure (1.0) and success (0.0). With `windowSize=5` and only 2 completions, the cumulative score is 0.5. This is intentional: divergence measures plan health at the planning level, not execution health at the rerouting level. A binding that requires rerouting IS evidence that the plan's assumption (that the first-choice agent would succeed) was wrong. The windowed average naturally recovers as successful completions push failure entries out of the window.

---

## 9. CaseDefinition Integration

`CaseDefinition` gains:

```java
private MonitoringConfig monitoringConfig;

public MonitoringConfig getMonitoringConfig() { return monitoringConfig; }
```

Builder: `.monitoring(MonitoringConfig.defaults())`.

**CaseDefinitionYamlMapper** parses `monitoring:` block under `spec:`:

```yaml
spec:
  monitoring:
    enabled: true
    perCompletionThreshold: 0.5
    windowSize: 5
```

---

## 10. Graceful Degradation

| Condition | Behavior |
|-----------|----------|
| No `MonitoringConfig` on definition | No validation, no events |
| `monitoring.enabled = false` | No validation, no events |
| No GOAP actions AND no producedKeys for binding | `ExpectedEffects.isEmpty()` → skip validation |
| Some bindings have effects, others don't | Validate only bindings with declared effects |
| No compound for binding | Validate at case level, `compoundId = null` |

No false positives when effects are undeclared. Monitoring adds zero overhead to cases without `MonitoringConfig`.

---

## 11. Testing Strategy

### 11.1 ExpectedEffectResolver Unit Tests (engine-common)

- GOAP action effects resolved by capability name
- producedKeys fallback when no GOAP action matches
- GOAP takes precedence over producedKeys when both exist
- Empty effects for binding with neither GOAP action nor producedKeys
- Null/absent binding returns empty

### 11.2 Validation Logic Unit Tests (runtime)

- All effects satisfied → ratio 0.0, no violations
- One of three effects missing → ratio 0.333
- FALSE effect (key should be absent) correctly detected
- UNKNOWN (absent key) treated as violation for `true` expectation
- UNKNOWN treated as violation for `false` expectation (worker failed to explicitly set key to FALSE — `GoapAction.applyTo()` calls `with(key, false)`, producing `Condition.FALSE`, not absent)
- Produced keys validation (presence-only)
- Failure-path inclusion: ratio 1.0 when no effects satisfied

### 11.3 DivergenceScoreComputer Unit Tests (engine-common)

- Empty validations → 0.0
- Single validation → its ratio
- Window caps at windowSize
- Mixed ratios averaged correctly
- Window slides: oldest entries dropped

### 11.4 ExpectationValidator Unit Tests (runtime)

- Returns null when MonitoringConfig disabled
- Returns null when no expected effects
- Correct violation detection and ratio computation
- Threshold not exceeded → no ExpectationViolationEvent
- Threshold exceeded → ExpectationViolationEvent published
- Compound ID resolved from WORKER_SCHEDULED EventLog metadata

### 11.5 WorkflowExecutionCompletedHandler Integration Tests (runtime)

- End-to-end: worker completes → validation metadata on EventLog entry
- Failure path: validation metadata shows ratio 1.0
- MonitoringConfig absent → no validation metadata on EventLog

### 11.6 DivergenceScoreComputer Generation Tests (engine-common)

- Filters by adaptationGeneration correctly
- Post-adaptation entries only: ignores pre-adaptation violations
- Mixed generations: returns score only for requested generation

---

## 12. Scope Boundaries

**In scope:**
- `ExpectedEffects`, `ViolationRecord`, `MonitoringConfig` types (engine-api)
- `ExpectedEffectResolver` with GOAP + producedKeys resolution (engine-common)
- `ExpectationValidator` inline in success/failure paths (runtime)
- `DivergenceScoreComputer` for on-demand windowed average with generation filtering (engine-common)
- `ExpectationViolationEvent` (engine-common)
- Event bus address `EXPECTATION_VIOLATED` in `EventBusAddresses`
- `MonitoringConfig` on `CaseDefinition` with YAML/builder support
- EventLog metadata enrichment with validation results
- Failure-path validation inclusion

**Out of scope:**
- `ProgressGatedTrigger` AdaptationTrigger implementation (#931)
- Meta-reasoning integration (#934)
- Promoting `producedKeys` semantics (currently audit-only → behavioral contract) — this is a semantic promotion documented here, not a structural API change
- Reflexion-style failure critique enrichment of validation results (#932)
- Cross-compound divergence tracking (D9 — future concern)

---

## References

- `api/src/main/java/io/casehub/engine/plan/goap/GoapAction.java` — effects `Map<String, Boolean>`
- `api/src/main/java/io/casehub/engine/plan/goap/GoapWorldState.java` — ternary world state, `openWorld()`, `Condition`
- `api/src/main/java/io/casehub/engine/plan/goap/Condition.java` — TRUE/FALSE/UNKNOWN enum
- `api/src/main/java/io/casehub/engine/plan/adaptation/AdaptationTrigger.java` — SPI that #931 implements
- `api/src/main/java/io/casehub/engine/plan/adaptation/AdaptationContext.java` — trigger evaluation context
- `runtime/.../WorkflowExecutionCompletedHandler.java:180` — `contextOutputApplier.apply()` call site
- `runtime/.../WorkflowExecutionCompletedHandler.java:689` — `buildMetadata()` with existing `producedKeys`
- `common/.../event/EventBusAddresses.java` — event bus address constants
- `common/.../history/CaseHubEventType.java` — event type enum
- GE-20260706-56a75c — WorkerOutcomeResolvedEvent is failure-only; use WorkflowExecutionCompleted for all outcomes
- GE-20260605-fa1a51 — PlanItemCompletedEvent only fires for worker completions
- PP-20260727-5267d2 — plan-definition types in engine-api; execution types in engine-common
- `research/2026-08-18-adaptive-planning-intelligence.md` §1 (expectation tracking gap), §2.3 (divergence-triggered adaptation)
- Design review: decisions-928.md — 9 decisions, D1-D9
