# Reflexion-Style Failure Critique — Design Spec

**Issue:** #932
**Date:** 2026-08-19
**Module:** `casehub-engine-api` (record change), `casehub-engine` runtime (service, handler), `casehub-engine-planning` (revision prompt)

---

## Summary

Add a verbal critique layer on worker failure that explains *why* a step failed, not just *that* it failed and its category. LLM-generated for Knowledge failures when ChatModelProvider is available; classical fallback uses the diagnosis reason string directly. Critique flows to ForwardReplanRevision prompts and rerouted agents automatically.

## Motivation

Reflexion (Shinn et al., NeurIPS 2023) — verbal self-reflection stored in episodic memory before retry dramatically outperforms scalar reward for plan revision. A natural-language critique of "what went wrong and what should change" produces better revised plans than reporting "step 3 failed with category Knowledge."

Current state after #930: `_diagnostics.<bindingName>` stores structured telemetry — `status`, `attempts`, `history[]` with `category` and `missingContext`, `excludedAgents`, `latestDiagnosis`. `ForwardReplanRevision` sees only that something failed (via `AdaptationCause.StepFailed(stepId, reason)` where reason is `"FAULTED"`). Rerouted agents receive `List<FailureDiagnosis>` with category and reason but no verbal analysis.

---

## Architecture

### Module Placement

| Component | Module | Package |
|-----------|--------|---------|
| `FailureDiagnosis.critique` | `engine-api` | `io.casehub.api.model` |
| `FailureCritiqueService` | `runtime` | `io.casehub.engine.internal.worker` |
| Handler integration | `runtime` | `io.casehub.engine.internal.engine.handler` |
| Revision prompt enrichment | `planning` | `io.casehub.engine.planning.adaptation` |

---

## 1. FailureDiagnosis Record Change (engine-api)

Add nullable `String critique` as the 5th record component:

```java
public record FailureDiagnosis(
    FailureCategory category,
    String workerId,
    String outcomeStatus,
    Instant timestamp,
    String critique
) {}
```

Backward-compatible 4-arg constructor passes `null` critique. Existing call sites (dispatch-time deserialization from `_diagnostics` history, WorkerContext population) continue to work — entries without critique produce `FailureDiagnosis` with null critique.

---

## 2. FailureCritiqueService (runtime)

`@ApplicationScoped` internal service — NOT an SPI. The `FailureClassifier` SPI from #930 is the consumer-extensible layer; critique is the engine's own reflection on top.

```java
@ApplicationScoped
public class FailureCritiqueService {

  @Inject Instance<ChatModelProvider> chatModelProviders;

  public String generateCritique(
      FailureDiagnosis diagnosis, JsonNode workingLayer, CaseDefinition definition) {

    if (!(diagnosis.category() instanceof FailureCategory.Knowledge)) {
      return diagnosis.category().reason();
    }

    if (chatModelProviders.isUnsatisfied()) {
      return diagnosis.category().reason();
    }

    // LLM critique for Knowledge failures
    String systemPrompt = "You are a failure analyst. Given a worker failure and "
        + "the current case context, produce a single sentence explaining what "
        + "went wrong and what should change on retry. Be specific and actionable.";

    String userPrompt = buildUserPrompt(diagnosis, workingLayer);

    var agent = Agent.builder()
        .systemPrompt(systemPrompt)
        .model(chatModelProviders.get().get())
        .build();

    try {
      var result = agent.execute(Map.of("prompt", userPrompt));
      var output = result.output();
      return output instanceof Map<?,?> m && m.containsKey("result")
          ? m.get("result").toString()
          : output.toString();
    } catch (Exception e) {
      // LLM failure → fall back to classical
      return diagnosis.category().reason();
    }
  }
}
```

**LLM prompt context:** The user prompt includes the binding name, failure category, reason, missingContext (if available), and a truncated snapshot of the working layer (max 1000 chars — enough for local context, not full case state).

**Error handling:** LLM failure is caught and falls back to the classical reason string. Critique generation never blocks case progression.

---

## 3. Handler Integration (runtime)

In `WorkflowExecutionCompletedHandler.handleSemanticFailure()`, after classification (which runs after `attempts` is computed — per #930 spec §5.1):

```java
// After: FailureCategory category = classifier.classify(...)
String critique = failureCritiqueService.generateCritique(
    new FailureDiagnosis(category, worker.name(), outcome.getClass().getSimpleName(),
        Instant.now(), null),
    workingLayer, definition);

// Enrich the FailureDiagnosis with critique
var diagnosis = new FailureDiagnosis(category, worker.name(),
    outcome.getClass().getSimpleName(), Instant.now(), critique);
```

### 3.1 Context Storage

```java
// Top-level critique (latest only)
bindingOutcome.put("critique", critique);

// Per-attempt in history[] entry
historyEntry.put("critique", critique);
```

### 3.2 FailureDiagnosis serialization in latestDiagnosis

`latestDiagnosis` (existing per #930) is enriched with the critique field. Deserialization at dispatch time reconstructs `FailureDiagnosis` with critique.

---

## 4. ForwardReplanRevision Prompt Enrichment (planning)

`ForwardReplanRevision.revise()` reads failure context from `adaptationContext.currentContext()`. When the cause is `StepFailed`, extract `_diagnostics` and append critique to the prompt:

```java
// In buildCompletedHistory or as separate method
if (context.cause() instanceof AdaptationCause.StepFailed failed) {
  String critique = extractCritique(adaptCtx.currentContext(), failed.stepId());
  if (critique != null) {
    userPrompt += "\n\nFailure analysis for step '" + failed.stepId() + "':\n" + critique;
  }
}
```

`extractCritique()` reads `_diagnostics.<bindingName>.critique` from the JSON context. Returns null if absent.

---

## 5. Rerouted Agent Context

No additional work needed. `WorkerContext.failureDiagnoses` already threads `List<FailureDiagnosis>` to rerouted agents (per #930 spec §6). The new `critique` field on `FailureDiagnosis` flows automatically. Workers access via `((WorkerRuntime) scope).context().failureDiagnoses()`.

---

## 6. Graceful Degradation

| Condition | Behavior |
|-----------|----------|
| No ChatModelProvider | Classical: `category.reason()` used as critique |
| LLM call fails | Classical fallback: `category.reason()` |
| Transient/Infeasible failure | Classical: `category.reason()` — no LLM call |
| Knowledge failure + LLM available | LLM one-sentence critique |
| No `_diagnostics` in context for binding | ForwardReplanRevision prompt has no critique block |

---

## 7. Testing Strategy

### 7.1 FailureDiagnosis Record Tests (engine-api)

- 5-arg constructor stores critique
- 4-arg backward-compatible constructor has null critique
- Record equality with and without critique

### 7.2 FailureCritiqueService Unit Tests (runtime)

- Transient failure → returns reason directly (no LLM call)
- Infeasible failure → returns reason directly
- Knowledge failure + no ChatModelProvider → returns reason
- Knowledge failure + ChatModelProvider → returns LLM result
- Knowledge failure + LLM throws → falls back to reason
- Null workingLayer handled gracefully

### 7.3 Handler Integration Tests (runtime)

- Critique stored in `_diagnostics.<bindingName>.critique`
- Critique stored in `history[]` entry
- latestDiagnosis enriched with critique

### 7.4 ForwardReplanRevision Tests (planning)

- StepFailed cause with critique in context → prompt includes critique
- StepCompleted cause → no critique block in prompt
- StepFailed but no `_diagnostics` in context → prompt has no critique block

---

## 8. Scope Boundaries

**In scope:**
- `FailureDiagnosis.critique` field (nullable String)
- `FailureCritiqueService` (runtime, @ApplicationScoped)
- `handleSemanticFailure` integration — generate and store critique
- `ForwardReplanRevision` prompt enrichment
- Classical fallback for non-Knowledge failures and absent LLM

**Out of scope:**
- Consumer-extensible critique generation (SPI) — current design is internal; revisit if consumer need emerges
- Critique for success outcomes (expectation violations) — different path, covered by #928/#931 divergence tracking
- Multi-attempt critique aggregation/summarization — cumulative history is available; summarization is a future concern
- Critique in GOAP replanning (#929's `GoapDecompositionStrategy.replan()`) — only ForwardReplanRevision in scope

---

## References

- `api/src/main/java/io/casehub/api/model/FailureDiagnosis.java` — existing record
- `api/src/main/java/io/casehub/api/model/FailureCategory.java` — sealed type with Knowledge.missingContext
- `api/src/main/java/io/casehub/api/spi/FailureClassifier.java` — classification SPI
- `runtime/.../WorkflowExecutionCompletedHandler.java` — handleSemanticFailure integration point
- `planning/.../ForwardReplanRevision.java` — revision prompt construction
- `specs/issue-927-adaptive-planning-intelligence/2026-08-18-failure-taxonomy-design.md` — #930 design
- `specs/issue-927-adaptive-planning-intelligence/decisions-932.md` — design decisions
- `research/2026-08-18-adaptive-planning-intelligence.md` §3 — Reflexion literature
- GitHub #932 — focal issue
- GitHub #930 — prerequisite (failure taxonomy)
