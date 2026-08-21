# Design: Annotations module @Cost and enhanced GOAP support

**Issue:** casehubio/engine#939
**Parent epic:** casehubio/engine#927 (Adaptive Planning Intelligence)
**Date:** 2026-08-21
**Scale:** S | **Complexity:** Med

## Summary

Extend the annotations module to expose dynamic cost computation via `@Cost` for the enhanced GOAP planner. Validate that `@SoftDependency` ternary mapping and action blacklisting identity already work correctly end-to-end.

## Scope

**In scope:**
- New `@Cost` annotation with build-time validation and runtime wiring
- `GoapActionDescriptor` extension for cost method reference
- `CaseDefinitionRecorder` wiring of `CostFunction` from reflective method invocation
- Bug fix: `inferGoapAction` must use capability name (not method name) for action identity
- Validation tests confirming `@SoftDependency` ternary behavior
- Validation tests confirming action name blacklisting identity

**Out of scope:**
- Changes to the GOAP planner itself (delivered in #929)
- Changes to `GoapWorldState` or `Condition` (delivered in #929)
- Changes to `CostFunction` interface (delivered in #929)

## Design

### 1. `@Cost` Annotation

New annotation in `annotations/runtime`:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cost {
    String value(); // resolved worker identity (method name or @Worker.capability())
}
```

**Method contract:**
- Exactly one parameter of type `io.casehub.engine.plan.goap.GoapWorldState`
- Return type `double`
- Must be a `default` method on the `@Case` interface (same as `@Worker` methods)
- `value()` references a worker by its resolved identity — the processor matches against both method names and explicit `@Worker.capability()`/`@Worker.value()` names

**Example usage:**

```java
@Case(namespace = "legal", name = "ContractReview", planning = PlanningMode.GOAP)
public interface ContractReviewCase {

    @Worker(capability = "assessRisk", cost = 0.5)
    default RiskReport assessRisk(AnalysisResult analysis, ClauseList clauses) {
        return new RiskReport("LOW");
    }

    @Cost("assessRisk")
    default double assessRiskCost(GoapWorldState state) {
        return state.get("priorReview") == Condition.TRUE ? 0.2 : 0.8;
    }
}
```

**Interaction with `@Worker.cost()`:** Both can coexist. The static `@Worker.cost()` value is stored on `GoapAction.cost` (used as the A* heuristic base via `effectiveCost()`). The `@Cost` method produces the `CostFunction` stored on `GoapAction.costFunction` (used at planning time via `effectiveCost(GoapWorldState)`). When only `@Worker.cost()` is declared, `costFunction` is null and `effectiveCost()` uses the static value — existing behavior preserved.

### 2. Descriptor and Recorder Changes

**`GoapActionDescriptor`** gains a 7th field:

```java
public record GoapActionDescriptor(
    String name,
    Map<String, Boolean> preconditions,
    Map<String, Boolean> effects,
    double cost,
    double benefit,
    Map<String, Boolean> softPreconditions,
    String costMethodName) {}  // nullable — null when no @Cost declared
```

**`CaseDefinitionRecorder.createCaseDefinition()`** — when `gad.costMethodName()` is non-null, creates a `CostFunction` lambda that reflectively invokes the cost method on the Gizmo-generated impl class instance:

```java
CostFunction costFn = null;
if (gad.costMethodName() != null) {
    Class<?> implClass = Thread.currentThread().getContextClassLoader()
        .loadClass(descriptor.implClassName());
    Object instance = implClass.getDeclaredConstructor().newInstance();
    Method costMethod = implClass.getMethod(gad.costMethodName(), GoapWorldState.class);
    costFn = state -> {
        try {
            return (double) costMethod.invoke(instance, state);
        } catch (Exception e) {
            return gad.cost(); // fallback to static cost on error
        }
    };
}
goapActions.add(new GoapAction(
    gad.name(), gad.preconditions(), gad.effects(),
    gad.cost(), gad.benefit(), gad.softPreconditions(), costFn));
```

The impl class instance is created once per `CaseDefinition` build (not per invocation) — same pattern as `@Completion` method invocation.

### 3. Deployment Processor Changes

**Bug fix — action identity:** `inferGoapAction(method, method.name(), ...)` currently uses the Java method name as the GOAP action name. But `GoapDecompositionStrategy.decompose()` filters actions by capability name. When `@Worker(capability = "custom")` is on method `doWork`, the action `doWork` is silently dropped because `"doWork" ∉ availableCapabilities`. Fix: pass the resolved capability name (from `resolveCapabilityName()`) as the action name instead of `method.name()`.

**`EngineAnnotationsProcessor.processWorkerMethod()`** — change the `inferGoapAction` call:
```java
// Before: goapActions.add(inferGoapAction(method, method.name(), cost, benefit));
// After:
goapActions.add(inferGoapAction(method, capabilityName, cost, benefit));
```

**`EngineAnnotationsProcessor.buildDescriptor()`** — after processing all `@Worker` methods, scan for `@Cost` methods. For each `@Cost` method:
1. Extract `value()` — the target worker identity (method name or capability name)
2. Find the matching `GoapActionDescriptor` in the already-built list (match by name)
3. Replace it with a copy that has `costMethodName` set

**`@Cost` resolution:** Build a map of `workerIdentity → GoapActionDescriptor` during worker processing. Worker identity is the resolved capability name (from `resolveCapabilityName()`). `@Cost.value()` is matched against this map. Build error if no match found.

**New DotName:** `COST = DotName.createSimple("io.casehub.engine.annotations.Cost")`

### 4. Build-Time Validation

**`AnnotationValidationStep`** gains:

- `@Cost` method must have exactly one parameter of type `GoapWorldState` and return `double`
- `@Cost.value()` must reference a `@Worker` method on the same interface (build error if not found)
- `@Cost` without `@Case(planning = GOAP)` or `@Case(planning = ADAPTIVE)`: warning (no effect in EXPLICIT mode)
- `@Cost` on a method that also has `@Worker`: build error (conflicting annotations — cost method and worker method are separate)

### 5. @SoftDependency Ternary Mapping (Validation Only)

No code changes. The ternary pipeline works for **soft preconditions**:
- `@SoftDependency` on a parameter → `GoapActionDescriptor.softPreconditions` entry
- `GoapWorldState.openWorld()` → absent keys return `Condition.UNKNOWN`
- `GoapPlanner.softPenalty()` → penalizes UNKNOWN soft preconditions during A* scoring

**Note on hard preconditions:** `GoapDecompositionStrategy.buildOpenWorldState()` closes hard preconditions to FALSE before planning — the optimistic UNKNOWN handling in `GoapAction.isApplicable()` does not fire in the decomposition path for hard preconditions. Only soft preconditions exercise the full ternary pipeline at decomposition time.

**Deliverable:** Integration test in `annotations/deployment` confirming that a GOAP-annotated case with `@SoftDependency` parameters produces correct `GoapAction` soft precondition entries and that the planner applies ternary soft-penalty scoring correctly.

### 6. Action Blacklisting Identity (Validation Only)

After the bug fix in §3, `GoapAction.name()` = resolved capability name from `@Worker`:
- `@Worker(capability = "assessRisk")` → `GoapAction("assessRisk", ...)`
- `GoapDecompositionStrategy.replan()` → `PlannerConfig.blacklistedActions("assessRisk")`

**Deliverable:** Unit test confirming that `GoapActionDescriptor.name()` from annotations matches the resolved capability name used by the decomposition filter and replan blacklisting path.

## Files Changed

| Module | File | Change |
|--------|------|--------|
| `annotations/runtime` | `Cost.java` | New annotation |
| `annotations/runtime` | `GoapActionDescriptor.java` | Add `costMethodName` field |
| `annotations/runtime` | `CaseDefinitionRecorder.java` | Wire `CostFunction` from reflective invocation |
| `annotations/deployment` | `EngineAnnotationsProcessor.java` | Fix action name bug, scan `@Cost` methods, build cost method references |
| `annotations/deployment` | `AnnotationValidationStep.java` | Validate `@Cost` signature and references |
| `examples/goap-case-annotated` | `GoapAnnotatedCase.java` | Add `@Cost` usage example |
| `annotations/deployment` | `*Test.java` | Tests for all of the above |

## Test Plan

1. **@Cost wiring test** — `@Case` with `@Cost` method produces `GoapAction` with non-null `costFunction` that returns context-dependent values
2. **@Cost validation tests** — wrong signature, missing worker reference, wrong planning mode
3. **@Cost + static cost coexistence** — both `@Worker(cost=X)` and `@Cost` method on same worker
4. **@SoftDependency ternary test** — annotated case with soft dependency produces correct soft preconditions, planner handles UNKNOWN state
5. **Action identity bug fix test** — `@Worker(capability = "custom")` on method `doWork` produces `GoapAction("custom", ...)`, not `GoapAction("doWork", ...)`
6. **Action identity blacklisting test** — annotated worker capability name matches `GoapAction.name()` used by decomposition filter and replan blacklisting

## References

- `api/src/main/java/io/casehub/engine/plan/goap/GoapAction.java` — canonical constructor with `CostFunction`
- `api/src/main/java/io/casehub/engine/plan/goap/CostFunction.java` — `@FunctionalInterface`
- `api/src/main/java/io/casehub/engine/plan/goap/GoapWorldState.java` — ternary world state
- `annotations/runtime/.../CaseDefinitionRecorder.java` — runtime wiring site
- `annotations/deployment/.../EngineAnnotationsProcessor.java` — build-time processing
- `planning/.../GoapCostEnricher.java` — learned cost composition on top of CostFunction
- `planning/.../GoapDecompositionStrategy.java` — action consumption and replan blacklisting
- GE-20260818-534e70 — ternary world state semantics
- GE-20260820-114e9a — annotation composition pitfalls
- GE-20260613-095ce5 — Jandex valueWithDefault() for defaulted attributes
