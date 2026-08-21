# Failure Taxonomy and Diagnosis Routing — Design Spec

**Issue:** #930
**Date:** 2026-08-18
**Module:** `casehub-engine-api` (types), `casehub-engine` runtime (handler changes)

---

## Summary

Classify worker failures into three categories (Transient, Knowledge, Infeasible) that determine the correct response. Thread structured failure diagnosis to rerouted agents via `WorkerContext`. Change agent exclusion behavior based on failure category — Transient failures use temporary exclusion instead of permanent.

## Motivation

All non-success outcomes currently route through the same `OutcomePolicy` path. A transient timeout permanently excludes the agent (wasting a capable resource) and gets the same REROUTE treatment as a capability mismatch (which needs replanning, not rerouting). Rerouted agents receive no context about prior failures.

**OutcomePolicy / FailureCategory interaction:** OutcomePolicy is the operator's configured intent and always takes precedence. FailureCategory is advisory — it affects exclusion behavior and provides diagnostic context, but does not override the configured REROUTE/FAULT action. The interaction model for adaptation decisions is defined by #934 (meta-reasoning).

---

## 1. FailureCategory Sealed Type (engine-api)

```java
// api/src/main/java/io/casehub/api/model/FailureCategory.java
package io.casehub.api.model;

public sealed interface FailureCategory {
  String reason();
  String categoryName();

  record Transient(String reason) implements FailureCategory {
    @Override public String categoryName() { return "transient"; }
  }
  record Knowledge(String reason, String missingContext) implements FailureCategory {
    @Override public String categoryName() { return "knowledge"; }
  }
  record Infeasible(String reason) implements FailureCategory {
    @Override public String categoryName() { return "infeasible"; }
  }
}
```

`missingContext` on `Knowledge` is nullable — populated when the classifier can identify what was missing. `categoryName()` provides stable serialization independent of Java class naming (R1-08).

---

## 2. FailureDiagnosis Record (engine-api)

Stored in `_diagnostics.<bindingName>.history[].diagnosis` and threaded to rerouted agents:

```java
// api/src/main/java/io/casehub/api/model/FailureDiagnosis.java
package io.casehub.api.model;

import java.time.Instant;

public record FailureDiagnosis(
    FailureCategory category,
    String workerId,
    String outcomeStatus,
    Instant timestamp
) {}
```

Field uses `workerId` (not `workerName`) to match codebase convention (R1-05).

---

## 3. FailureClassifier SPI (engine-api)

```java
// api/src/main/java/io/casehub/api/spi/FailureClassifier.java
package io.casehub.api.spi;

import io.casehub.api.model.FailureCategory;
import io.casehub.worker.api.WorkerOutcome;

public interface FailureClassifier {
  FailureCategory classify(WorkerOutcome<?> outcome, ClassificationContext context);
}
```

Uses `WorkerOutcome<?>` (not raw type) to avoid compiler warnings (R1-06).

```java
// api/src/main/java/io/casehub/api/spi/ClassificationContext.java
package io.casehub.api.spi;

public record ClassificationContext(
    String workerId,
    java.util.UUID caseId,
    String tenancyId,
    String bindingName,
    String capabilityName,
    int attemptCount,
    int maxRerouteAttempts
) {}
```

Carries `maxRerouteAttempts` so the classifier can detect Infeasible (R1-02).

---

## 4. DefaultFailureClassifier (runtime)

`@DefaultBean @ApplicationScoped` implementation using outcome type and reason pattern matching:

```java
@DefaultBean
@ApplicationScoped
public class DefaultFailureClassifier implements FailureClassifier {

  @Override
  public FailureCategory classify(WorkerOutcome<?> outcome, ClassificationContext context) {
    if (context.attemptCount() >= context.maxRerouteAttempts()) {
      return new FailureCategory.Infeasible(extractReason(outcome));
    }
    return switch (outcome) {
      case WorkerOutcome.Expired e -> new FailureCategory.Transient(e.reason());
      case WorkerOutcome.Declined d -> new FailureCategory.Knowledge(d.reason(), null);
      case WorkerOutcome.Failed f -> classifyFailure(f, context);
      default -> new FailureCategory.Transient("unknown");
    };
  }
}
```

**Classification rules:**

| Outcome | Default category | Override conditions |
|---------|-----------------|---------------------|
| Any + `attemptCount >= maxRerouteAttempts` | Infeasible | Checked first — exhaustion overrides outcome type |
| `Expired` | Transient | Always — timeouts are transient by definition |
| `Declined` | Knowledge | The agent explicitly said it can't do this |
| `Failed` | Heuristic | Reason text pattern matching (see below) |

**Failed reason heuristics (R1-04 — defined precedence):**

Evaluation order: Transient patterns first, then Knowledge patterns. First match wins. Matching is case-insensitive. Rationale: Transient first because it's the cheaper response (retry vs. replan).

| Priority | Pattern (case-insensitive) | Category | Rationale |
|----------|---------------------------|----------|-----------|
| 1 | Contains "timeout", "timed out", "connection refused", "503", "429", "retry" | Transient | Infrastructure/network issues |
| 2 | Contains "not found", "missing", "unsupported", "invalid schema" | Knowledge | The approach/data is wrong |
| 3 | Default | Transient | Fail-safe: retry is cheaper than replanning |

Note: "cannot" removed from Knowledge patterns — too generic, matches infrastructure errors like "cannot connect". Consumer classifiers can add domain-specific patterns.

**Infeasible detection:** The Infeasible heuristic (`attemptCount >= maxRerouteAttempts`) is checked BEFORE the outcome switch. This aligns with the handler's existing exhaustion check rather than duplicating it — both use the same threshold. The classifier produces `Infeasible` and the handler produces `EXHAUSTED` disposition in the same cycle (R1-02).

---

## 5. handleSemanticFailure Changes (runtime)

### 5.1 Classification call site (D1)

Classification runs AFTER `attempts` is computed (after the existing `_diagnostics` JSON node is built, ~line 432 in current code), not "at the top" (R1-03):

```java
// After: final int attempts = ... (line 432)
// After: final boolean exhausted = ... (line 433)

final ClassificationContext classificationCtx = new ClassificationContext(
    worker.name(), caseInstance.getUuid(), caseInstance.tenancyId,
    bindingName, capabilityName, attempts, policy.maxRerouteAttempts());
final FailureCategory category = classifier.classify(event.outcome(), classificationCtx);
```

### 5.2 Category-aware agent exclusion (D2, R1-01)

Replace the unconditional `excluded.add(worker.name())` with category-aware logic. Transient failures use a single-attempt exclusion — the agent is added to `excludedAgents` but also to a separate `transientExcluded` set. On the NEXT successful completion or context change cycle, `transientExcluded` agents are removed from `excludedAgents` (they get one more chance):

```java
if (category instanceof FailureCategory.Transient) {
  // Add to excludedAgents (prevents immediate re-selection on this cycle)
  // but also mark as transient so the exclusion is cleared on next cycle
  excluded.add(worker.name());
  bindingOutcome.set("excludedAgents", excluded);
  ArrayNode transientExcluded = bindingOutcome.has("transientExcluded")
      ? (ArrayNode) bindingOutcome.get("transientExcluded")
      : OBJECT_MAPPER.createArrayNode();
  transientExcluded.add(worker.name());
  bindingOutcome.set("transientExcluded", transientExcluded);
} else {
  // Permanent exclusion for Knowledge and Infeasible
  excluded.add(worker.name());
  bindingOutcome.set("excludedAgents", excluded);
}
```

The `CaseContextChangedEventHandler` exclusion filter gains a pre-step: before filtering by `excludedAgents`, remove any agents listed in `transientExcluded` from `excludedAgents` and clear `transientExcluded`. This gives transiently-failed agents one dispatch cycle of exclusion (preventing immediate same-cycle retry) before they become eligible again.

### 5.3 Diagnosis storage (R1-07, R1-08)

Enrich each `history[]` entry with the category and missingContext, rather than maintaining a separate `diagnosis` field that overwrites. This preserves cumulative failure context:

```java
historyEntry.put("category", category.categoryName());
if (category instanceof FailureCategory.Knowledge k && k.missingContext() != null) {
  historyEntry.put("missingContext", k.missingContext());
}
```

The latest diagnosis is also stored as `_diagnostics.<bindingName>.latestDiagnosis` for quick access at dispatch time (avoids scanning the history array).

### 5.4 Category in EventLog metadata

```java
metadata.put("failureCategory", category.categoryName());
```

Uses `categoryName()` method instead of reflection (R1-08).

### 5.5 Category on WorkerOutcomeResolvedEvent (R1-05)

Add `FailureCategory category` using the existing field naming convention (`workerId`):

```java
public record WorkerOutcomeResolvedEvent(
    CaseInstance caseInstance,
    String workerId,
    String bindingName,
    String capabilityName,
    OutcomeDisposition disposition,
    FailureCategory category  // nullable for backward compat
) {}
```

Existing 5-arg constructor delegates with `null` category.

---

## 6. WorkerContext Diagnosis Threading (D4)

### 6.1 WorkerContext field (R1-07)

Add `failureDiagnoses` as the 9th field — a list to carry cumulative failure context:

```java
public record WorkerContext(
    String taskDescription,
    UUID caseId,
    List<CaseChannel> channels,
    List<WorkerSummary> priorWorkers,
    PropagationContext propagationContext,
    Map<String, Object> properties,
    List<RetrievedExperience> experiences,
    List<RetrievedMemory> memories,
    List<FailureDiagnosis> failureDiagnoses  // immutable, default List.of()
) { ... }
```

Existing 8-arg, 7-arg, and 6-arg constructors pass `List.of()`.

### 6.2 Population at dispatch time

In `WorkerScheduleEventHandler`, after input projection, read `_diagnostics.<bindingName>.history[]` from the case context. Extract entries that have a `category` field, deserialize each to `FailureDiagnosis`, and store as a list in EventLog metadata (key `"failureDiagnoses"`). `QuartzWorkerExecutionJob` reads from metadata and passes to `WorkerContext`.

Same threading pattern as experiences and memories: EventLog metadata is the serialization boundary.

---

## 7. Testing Strategy

### 7.1 FailureCategory Unit Tests (engine-api)

- Sealed type construction and pattern matching
- Knowledge with null missingContext
- categoryName() returns stable strings

### 7.2 DefaultFailureClassifier Unit Tests (runtime)

- Expired → Transient
- Declined → Knowledge
- Failed with "timeout" → Transient
- Failed with "not found" → Knowledge
- Failed with unknown reason → Transient (fail-safe)
- attemptCount >= maxRerouteAttempts → Infeasible (regardless of outcome type)
- Transient pattern checked before Knowledge pattern (precedence)

### 7.3 handleSemanticFailure Integration (runtime)

- Transient failure adds to excludedAgents AND transientExcluded
- Knowledge failure adds to excludedAgents only (permanent)
- Diagnosis enriched in history[] entries
- Category in EventLog metadata (via categoryName())
- Category on WorkerOutcomeResolvedEvent

### 7.4 Transient Exclusion Clearing (runtime)

- CaseContextChangedEventHandler clears transientExcluded agents from excludedAgents on next cycle
- Agent becomes re-eligible after one exclusion cycle

### 7.5 WorkerContext Threading (runtime)

- List<FailureDiagnosis> populated from _diagnostics history at dispatch time
- Empty list when no prior failures exist
- Backward-compatible constructors

---

## 8. Scope Boundaries

**In scope:**
- `FailureCategory` sealed type with three variants and `categoryName()`
- `FailureDiagnosis` record
- `FailureClassifier` SPI + `DefaultFailureClassifier` with defined precedence
- `handleSemanticFailure` category-aware routing and transient exclusion
- `_diagnostics` enrichment with diagnosis in history entries
- Transient exclusion clearing in `CaseContextChangedEventHandler`
- `WorkerContext` diagnosis list + dispatch-time threading
- `WorkerOutcomeResolvedEvent` category field

**Out of scope:**
- LLM-powered failure critique (#932 — Reflexion-style critique)
- Adaptation trigger wiring from Knowledge failures (#934 — meta-reasoning reads the category)
- Infeasible → GoalAbandonmentEvaluator wiring (#934 — meta-reasoning owns the Concede decision)
- Consumer-provided FailureClassifier implementations (domain-specific)
- OutcomePolicy / FailureCategory interaction formalization (#934 — meta-reasoning)

---

## References

- `api/src/main/java/io/casehub/api/model/OutcomePolicy.java` — per-binding outcome routing
- `api/src/main/java/io/casehub/api/model/OutcomeAction.java` — REROUTE/FAULT enum
- `api/src/main/java/io/casehub/api/model/WorkerContext.java` — worker execution context
- `runtime/src/main/java/io/casehub/engine/internal/engine/handler/WorkflowExecutionCompletedHandler.java:357-538` — handleSemanticFailure
- `runtime/src/main/java/io/casehub/engine/internal/engine/handler/CaseContextChangedEventHandler.java:430-456` — excludedAgents filter
- `common/src/main/java/io/casehub/engine/common/internal/event/WorkerOutcomeResolvedEvent.java` — outcome event
- `runtime/src/main/java/io/casehub/engine/internal/routing/GoalAbandonmentEvaluator.java` — threshold-based abandonment
- `research/2026-08-18-adaptive-planning-intelligence.md` §Issue 3 — failure taxonomy design
- Design review: `/Users/mdproctor/reviews/casehub-slots/930-failure-taxonomy-20260818-124331/` — 9 issues addressed
