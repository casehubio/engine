# Design: Plan Repair vs Plan Optimization Separation (#935)

Parent epic: #927 (Adaptive Planning Intelligence)
Depends on: #934 (Persist / Refine / Concede Meta-Reasoning)

## Problem

`ForwardReplanRevision` conflates two distinct concerns: restoring a broken plan to validity (repair) and improving a working plan's quality (optimization). The IJCAI 2024 Plan Optimization Survey draws a clear distinction — repair is triggered by failure, targets minimal change, and prioritises speed; optimization is triggered by context drift, accepts higher cost, and targets quality.

#934 introduced `AdaptationDecision.Refine(RefineScope scope, String reason)` with `LOCAL` and `COMPOUND` scopes, but `DefaultPlanAdaptationEvaluator` ignores the scope — always resolving the single `PlanRevisionStrategy` from `config.revision()`. LOCAL falls back to ForwardReplanRevision (full LLM replan), which is correct but wasteful. This issue delivers the differentiated execution that #934's decision vocabulary prepared for.

## Design

### Marker Sub-Interfaces

Introduce two marker sub-interfaces of `PlanRevisionStrategy` in `engine-api` (`io.casehub.engine.plan.adaptation`):

```java
public interface RepairStrategy extends PlanRevisionStrategy {
}

public interface OptimizationStrategy extends PlanRevisionStrategy {
}
```

No default `id()` on the marker interfaces — implementations must declare their own. The parent `PlanRevisionStrategy` default (`"forward-replan"`) is inherited but collides with `ForwardReplanRevision`, causing a loud startup failure if a custom implementation forgets to override.

Both inherit `revise(RevisionContext) → RevisedPlan` — zero API duplication. The type distinction enables `EngineStrategyResolver` to resolve by type+id: `resolve(RepairStrategy.class, "goap-repair")` vs `resolve(OptimizationStrategy.class, "forward-replan")`. A strategy that implements `RepairStrategy` cannot be misconfigured as an optimization strategy — the resolver enforces the pairing.

`PlanRevisionStrategy` remains as the common base. `RevisionContext` is unchanged — no `RefineScope` field. Strategies know their role by their type, not by inspecting context.

### AdaptationConfig Evolution

`AdaptationConfig` record changes:

```java
public record AdaptationConfig(
    String trigger,
    String optimization,    // renamed from `revision`
    Double threshold,
    String metaReasoner,
    String repair           // new, nullable
) {
  // Backward-compatible factory — `revision` param maps to `optimization`
  public static AdaptationConfig of(String trigger, String optimization) {
    return new AdaptationConfig(trigger, optimization, null, null, null);
  }

  public String effectiveRepair(CaseDefinition definition) {
    if (repair != null) return repair;
    if (definition.getGoapActions() != null && !definition.getGoapActions().isEmpty()) {
      return "goap-repair";
    }
    return "llm-repair";
  }

  public String effectiveMetaReasoner() {
    return metaReasoner != null ? metaReasoner : "cost-ceiling";
  }
}
```

- `optimization` (non-null, required) — renamed from `revision`. Default "forward-replan".
- `repair` (nullable) — when null, auto-detected via `effectiveRepair(CaseDefinition)`: GOAP actions present → "goap-repair", else → "llm-repair".
- `effectiveRepair()` is a pure function on AdaptationConfig — the evaluator calls it, the config decides.

### YAML Schema

Explicit configuration:
```yaml
adaptation:
  trigger: progress
  optimization: forward-replan
  repair: goap-repair
  metaReasoner: cost-ceiling
  threshold: 0.3
```

Presets updated:
- `adaptation: adaptive` → `{trigger: every-step, optimization: forward-replan, repair: null}` (auto-detect)
- `adaptation: conservative` → `{trigger: on-failure, optimization: forward-replan, repair: null}`
- `adaptation: off` → no adaptation

`CaseDefinitionYamlMapper` accepts both `revision:` (backward compat) and `optimization:` keys — `revision` maps to `optimization`. Both parsed, `optimization` takes precedence if both present.

### Pipeline Integration

`DefaultPlanAdaptationEvaluator.performAdaptation()` — scope-aware routing:

```java
case AdaptationDecision.Refine r -> {
  PlanRevisionStrategy strategy = switch (r.scope()) {
    case LOCAL -> strategyResolver.resolve(
        RepairStrategy.class, config.effectiveRepair(definition));
    case COMPOUND -> strategyResolver.resolve(
        OptimizationStrategy.class, config.optimization());
  };
  // ... build RevisionContext, call strategy.revise(), apply result
}
```

This replaces the current single-strategy resolution at line 284: `strategyResolver.resolve(PlanRevisionStrategy.class, config.revision())`.

### Built-in Strategies

#### GoapRepairStrategy (id="goap-repair")

`@ApplicationScoped`, implements `RepairStrategy`. Standalone `GoapPlanner` instance — does not delegate to `GoapDecompositionStrategy`.

Algorithm:
1. Extract failed binding name from `AdaptationCause.StepFailed`
2. Build `GoapWorldState` from `AdaptationContext.currentContext()` (JsonNode working layer) — same pattern as `GoapDecompositionStrategy.buildOpenWorldState()`: call `GoapWorldState.openWorld(jsonNode)`, then close unknown precondition keys to `FALSE` by iterating each action's preconditions
3. Resolve available `GoapAction`s from `definition.getGoapActions()`, filter to available capabilities from `RevisionContext.capabilities()`
4. Blacklist the failed action via `PlannerConfig`
5. Resolve goal conditions from definition goals
6. Invoke `GoapPlanner.plan()` with the blacklisted config
7. Map `GoapAction` results to `PlanStepDescriptor` list in `RevisedPlan`

Learned cost enrichment (future): `RevisionContext` does not currently carry `RetrievedExperience` — enrichment via `GoapCostEnricher` is deferred until `RevisionContext` gains an `experiences` field. Learned costs are already applied at decomposition time, so repair plans benefit indirectly via the initial decomposition.

Failure: throws on empty plan result (caller catches and continues with existing plan).

#### LlmRepairStrategy (id="llm-repair")

`@ApplicationScoped`, implements `RepairStrategy`. Uses `ChatModelProvider` (via `Instance<>` with `isResolvable()` guard).

Distinct from `ForwardReplanRevision` in prompt focus:
- System prompt targets **one failed step** — "Repair this plan by replacing or modifying the failed step. Minimise changes to the rest of the plan."
- User prompt includes: the specific failed step (from `AdaptationCause.StepFailed`), failure critique (from `_diagnostics.<bindingName>.critique`), available capabilities, and the current plan state
- Does NOT include full completed history or planning constraints (those are optimization concerns)

Transparent no-op when `ChatModelProvider` absent (throws, caller catches).

#### ForwardReplanRevision (id="forward-replan")

Unchanged implementation. Now implements `OptimizationStrategy` instead of `PlanRevisionStrategy` directly:

```java
@ApplicationScoped
public class ForwardReplanRevision implements OptimizationStrategy {
  // ... existing code unchanged
  @Override public String id() { return "forward-replan"; }
}
```

### EngineStrategyResolver Registration

Add three explicit `Instance<>` parameters (two for new sub-interfaces, one for existing SPI that relied on catch-all):

```java
@Any Instance<RepairStrategy> repairStrategies,
@Any Instance<OptimizationStrategy> optimizationStrategies,
@Any Instance<AdaptationTrigger> adaptationTriggers,          // existing, was on catch-all
```

`Instance<PlanRevisionStrategy>` is NOT added — `resolveStrategyTypes()` walks the interface hierarchy, so `GoapRepairStrategy` discovered via `Instance<RepairStrategy>` is automatically registered under both `RepairStrategy` and `PlanRevisionStrategy` types. Adding a separate `Instance<PlanRevisionStrategy>` would cause duplicate-id registration failures. `AdaptationMetaReasoner` already has an explicit `Instance<>` (added in #934).

Per GE-20260810-b53fd8, explicit registration is required for reliable Quarkus ARC build-time discovery.

### EventLog Audit

`PLAN_ADAPTED` metadata gains two fields:
- `revisionScope` — "LOCAL" or "COMPOUND" (from `RefineScope`)
- `resolvedStrategy` — the actual strategy ID that was resolved (e.g., "goap-repair", "llm-repair", "forward-replan")

These supplement the existing `revisionStrategy` field which stays as-is for EventLog backward compat (captures the config-level optimization strategy name). The `resolvedStrategy` field captures the actual strategy used, distinguishing auto-detected repair from explicitly configured.

### D7 Fallback Removal

#934 D7 established: "LOCAL scope falls back to ForwardReplanRevision until #935." This fallback is explicitly removed by this work. LOCAL now routes to `RepairStrategy` (via config or auto-detect). The fallback existed because no repair strategies existed; now three are available.

## Testing

### Unit Tests

- **GoapRepairStrategy:** Verify blacklisting of failed action, world state construction from JsonNode via `openWorld()` + close unknown preconditions, plan generation, empty-plan exception
- **LlmRepairStrategy:** Verify repair-focused prompt (targets failed step, includes critique), transparent no-op without ChatModelProvider, PlanStepDescriptor parsing
- **AdaptationConfig:** `effectiveRepair()` auto-detect — GOAP actions → "goap-repair", no GOAP → "llm-repair", explicit config overrides both
- **DefaultPlanAdaptationEvaluator:** Scope-based routing — LOCAL resolves RepairStrategy, COMPOUND resolves OptimizationStrategy, auto-detect integration
- **EngineStrategyResolver:** RepairStrategy and OptimizationStrategy beans discovered via explicit Instance<>, AdaptationTrigger no longer on catch-all, no duplicate registration from hierarchy walking

### Integration

- **End-to-end adaptation with GOAP repair:** Case with GOAP actions, Knowledge failure triggers LOCAL scope, GoapRepairStrategy produces valid plan, plan applied and case continues
- **Fallback to LLM repair:** Case without GOAP actions, Knowledge failure triggers LOCAL, LlmRepairStrategy invoked
- **Optimization path unchanged:** Context drift triggers COMPOUND scope, ForwardReplanRevision invoked (existing behavior preserved)

## References

- `PlanRevisionStrategy.java` — base SPI interface
- `DefaultPlanAdaptationEvaluator.java:259-290` — scope switch site
- `ForwardReplanRevision.java` — existing optimization strategy
- `CostCeilingMetaReasoner.java:54-56` — scope selection by failure category
- `GoapDecompositionStrategy.java:86-122` — existing GOAP replan (reference, not delegated to)
- `GoapPlanningStrategy.java` — world state construction pattern
- `GoapCostEnricher.java` — learned cost enrichment pattern
- `EngineStrategyResolver.java:60-78` — explicit Instance<> registration pattern
- `AdaptationConfig.java` — current config record
- `CaseDefinitionYamlMapper.java` — YAML parsing for adaptation block
- GE-20260810-b53fd8 — EngineStrategyResolver explicit Instance<> required
- GE-20260814-d2b419 — AdaptationCause sealed type constraints
- decisions-934.md D2, D7 — scope routing deferred to #935, LOCAL fallback
- IJCAI 2024 Plan Optimization Survey — repair vs optimization distinction
