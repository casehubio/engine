# Contingent Planning Branches — Design Spec

**Issue:** casehubio/engine#938
**Epic:** #927 (Adaptive Planning Intelligence) — Phase D
**Date:** 2026-08-21

---

## Summary

Pre-compute alternative execution branches at decomposition time for predictable failure modes. When a DagNode fails, the DagDriver activates a pre-attached contingency sub-plan before escalating to reactive adaptation. This is the proactive complement to the reactive adaptation system built in #929–#937.

---

## Architecture

### Execution levels and contingency scope

The engine has two execution levels with different failure-handling strategies:

| Level | Executor | Failure handling | Contingency role |
|-------|----------|-----------------|-----------------|
| **DagDriver** (engine-common) | In-process, concurrent DAG execution. Used by blocks patterns. | `propagateFailures()` — mark dependents as Skipped | **Primary**: pre-computed contingency activated on node exception |
| **Engine PlanItem** (planning) | Distributed worker dispatch via `PlanningStrategyLoopControl` | Adaptation system (meta-reasoning → repair/optimization → concede) | **None**: adaptation system IS the engine-level contingency mechanism |

Contingencies are a DagDriver-level mechanism only. When decomposition produces a DagPlan with contingencies and it's materialized as PlanItems, contingencies are stored as metadata on the compound for audit and inspection but are not executed by PlanItem dispatch.

### Data flow

```
                    YAML binding                    CBR history
                    contingency:                    (experiences)
                    [alt-a, alt-b]                      │
                         │                              │
                         ▼                              ▼
              ┌─────────────────────┐    ┌──────────────────────────┐
              │ DefaultGoalDecomposer│    │ GoapDecompositionStrategy │
              │ (post-decomposition  │    │ (during decomposition     │
              │  attachment)         │    │  from CBR failure rates)  │
              └─────────┬───────────┘    └──────────┬───────────────┘
                        │                           │
                        ▼                           ▼
                ┌──────────────────────────────────────┐
                │  DagPlan<LeafTask<JsonNode>>           │
                │  nodes with optional contingency      │
                └──────────────────┬───────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    ▼                             ▼
           ┌──────────────┐             ┌──────────────────┐
           │  DagDriver    │             │  PlanItem         │
           │  (blocks)     │             │  materialization  │
           │               │             │  (engine)         │
           │  Contingency  │             │                   │
           │  ACTIVATED    │             │  Contingency as   │
           │  on failure   │             │  audit metadata   │
           └──────────────┘             └──────────────────┘
```

Two sources produce contingencies:
1. **YAML-declared** — `Binding.getContingency()` is a `List<String>` of capability names. `DefaultGoalDecomposer` attaches these AFTER the decomposition strategy returns the primary DagPlan, matching DagNodes to bindings via `findBindingsByCapability()`.
2. **Strategy-generated** — `GoapDecompositionStrategy` queries CBR failure rates from `GoalDecompositionContext.experiences()` and generates contingency sub-plans for actions exceeding the failure threshold. `LlmDecompositionStrategy` can generate contingencies when prompted with known failure modes.

YAML contingencies and strategy-generated contingencies can overlap — strategy-generated takes precedence (it has CBR context; YAML is a manual fallback declaration).

---

## Component changes

### 1. DagNode (engine-api)

```java
public record DagNode<T>(
    String id,
    T task,
    Set<String> dependsOn,
    JoinType joinType,
    DagPlan<T> contingency   // nullable — alternative sub-plan on primary failure
) {
  public DagNode {
    Objects.requireNonNull(id, "id required");
    Objects.requireNonNull(task, "task required");
    dependsOn = dependsOn != null ? Set.copyOf(dependsOn) : Set.of();
    if (joinType == null) joinType = JoinType.ALL_OF;
    if (contingency != null && contingency.exitNodeIds().size() > 1) {
      throw new IllegalArgumentException(
          "Contingency plan must have a single exit node, got " + contingency.exitNodeIds().size());
    }
  }

  // Backward-compatible 4-arg constructor (no contingency)
  public DagNode(String id, T task, Set<String> dependsOn, JoinType joinType) {
    this(id, task, dependsOn, joinType, null);
  }
}
```

**Single-exit validation:** Contingency plans must have exactly one exit node. This ensures deterministic result extraction without ambiguity. Multi-step contingencies use `DagPlan.sequence()` which naturally produces a single exit. Decomposition strategies enforce this by construction.

### 2. DagDriver (engine-common)

**Cancellation model:** The `volatile boolean cancelled` field is replaced with an `AtomicBoolean` that can be shared between outer and nested drivers:

```java
private final AtomicBoolean cancelSignal;

public DagDriver(DagPlan<T> plan, DispatchMode mode, List<DagEventListener<T, R>> listeners) {
  this(plan, mode, listeners, new AtomicBoolean(false));
}

// Package-private — used by contingency execution only
DagDriver(DagPlan<T> plan, DispatchMode mode, List<DagEventListener<T, R>> listeners,
          AtomicBoolean cancelSignal) {
  // ...
  this.cancelSignal = cancelSignal;
}
```

**Max contingency depth:** `DagDriver` gains a `maxContingencyDepth` field (default 3) and tracks the current depth. Prevents unbounded recursive blocking from misconfigured or auto-generated plans where contingency nodes themselves carry contingencies.

**Contingency execution in `executeNode()`:**

```java
private void executeNode(String nodeId, DagNode<T> node, Function<T, R> taskExecutor) {
  try {
    R result = taskExecutor.apply(node.task());
    states.put(nodeId, new NodeState.Completed<>(result));
    fireNodeCompleted(nodeId, node.task(), result);
  } catch (Exception e) {
    if (node.contingency() != null && !cancelSignal.get() && contingencyDepth < maxContingencyDepth) {
      // Empty listener list — onContingencyActivated is the sole observability channel
      var contingencyDriver = new DagDriver<>(
          node.contingency(), mode, List.of(), cancelSignal);
      contingencyDriver.contingencyDepth = this.contingencyDepth + 1;
      contingencyDriver.maxContingencyDepth = this.maxContingencyDepth;
      DagResult<R> contingencyResult = contingencyDriver.execute(taskExecutor);
      fireContingencyActivated(nodeId, node.task(), contingencyResult);

      if (cancelSignal.get()) {
        states.put(nodeId, new NodeState.Cancelled<>());
        fireNodeCancelled(nodeId, node.task());
      } else if (contingencyResult.allSucceeded()) {
        R fallbackResult = extractExitResult(contingencyResult, node.contingency());
        states.put(nodeId, new NodeState.Completed<>(fallbackResult));
        fireNodeCompleted(nodeId, node.task(), fallbackResult);
      } else {
        states.put(nodeId, new NodeState.Failed<>(e.getMessage(), e));
        fireNodeFailed(nodeId, node.task(), e.getMessage(), e);
      }
    } else {
      states.put(nodeId, new NodeState.Failed<>(e.getMessage(), e));
      fireNodeFailed(nodeId, node.task(), e.getMessage(), e);
    }
  }
}

private R extractExitResult(DagResult<R> result, DagPlan<T> plan) {
  // Single exit enforced by DagNode validation — exactly one exit node
  String exitId = plan.exitNodeIds().iterator().next();
  return result.completedResults().get(exitId);
}
```

**Key behaviors:**
- **Activation trigger:** Any exception from `taskExecutor.apply()`. DagDriver is a simple executor — it doesn't differentiate failure types.
- **Cancellation:** Shared `AtomicBoolean cancelSignal` between outer and nested drivers. If the outer driver is cancelled during contingency execution, the nested driver observes the same flag and stops dispatching new nodes. Post-contingency, the outer checks the signal and marks the node Cancelled.
- **Listener isolation:** The nested driver receives an empty listener list. The outer driver's `fireContingencyActivated()` provides the full `DagResult` — the sole observability channel. This prevents stateful listeners (e.g., `SnapshotCapturingDagEventListener`) from mixing contingency node events with outer plan events.
- **Depth limit:** `maxContingencyDepth` (default 3) prevents unbounded recursive blocking. When depth is exceeded, the node is marked Failed without attempting the contingency.
- **Result:** Single exit node's result (validated at DagNode construction — single-exit enforced). Contingency success → node Completed. Contingency failure → node Failed with the original exception.
- **Timeout:** Contingency execution time counts against the outer driver's global latch timeout (10 min). No special handling — a slow contingency is the same as a slow primary task from the outer latch's perspective.

### 3. DagEventListener (engine-common)

New callback:

```java
default void onContingencyActivated(
    String nodeId, T task, DagResult<R> contingencyResult) {}
```

Receives the full `DagResult` from the nested contingency execution. `SnapshotCapturingDagEventListener` uses this to capture contingency execution traces (node durations, success/failure states) alongside the main plan snapshot.

### 4. DagNodeSnapshot (engine-api)

```java
public record DagNodeSnapshot(
    String id,
    String taskId,
    String taskDescription,
    String executorName,
    Set<String> dependsOn,
    JoinType joinType,
    DagPlanSnapshot contingency  // nullable
) {}
```

Recursive structure: `DagNodeSnapshot` → `DagPlanSnapshot` → `List<DagNodeSnapshot>`. Jackson handles this natively. The `from()` factory extracts `TaskDescriptor` fields from the generic task type, recursively building the contingency snapshot when present.

### 5. NodeStateSnapshot (engine-common)

Gains optional `DagResultSnapshot contingencyResult` — populated when a contingency was activated (regardless of whether it succeeded or failed). Null when no contingency was triggered.

### 6. AdaptationConfig (engine-api)

`AdaptationConfig` is a record. New 6th component `contingencyThreshold`:

```java
public record AdaptationConfig(
    String trigger,
    String optimization,
    Double threshold,
    String metaReasoner,
    String repair,
    Double contingencyThreshold  // nullable — default 0.15
) {
  public static final double DEFAULT_CONTINGENCY_THRESHOLD = 0.15;

  // Existing compact constructor unchanged (trigger, optimization required)

  // Backward-compatible 5-arg factory
  public static AdaptationConfig of(String trigger, String optimization,
      Double threshold, String metaReasoner, String repair) {
    return new AdaptationConfig(trigger, optimization, threshold, metaReasoner, repair, null);
  }

  public double effectiveContingencyThreshold() {
    return contingencyThreshold != null ? contingencyThreshold : DEFAULT_CONTINGENCY_THRESHOLD;
  }
}
```

YAML: `adaptation: { contingencyThreshold: 0.15 }` or implicit default via presets.

### 7. Binding (engine-api)

```java
// New field
private final List<String> contingency; // nullable — alternative capability names

// Builder
public Builder contingency(List<String> capabilities) { ... }
public Builder contingency(String... capabilities) { ... }
```

YAML: `contingency: [manual-review, escalate-to-human]` on binding definitions.

### 8. DefaultGoalDecomposer (planning)

After `strategy.decompose()` returns the primary DagPlan, the decomposer iterates each node and checks for YAML-declared contingencies:

```java
private DagPlan<TaskNode.LeafTask<JsonNode>> attachYamlContingencies(
    DagPlan<TaskNode.LeafTask<JsonNode>> plan, CaseDefinition definition) {

  Map<String, DagNode<TaskNode.LeafTask<JsonNode>>> updatedNodes = new LinkedHashMap<>();
  for (var entry : plan.nodes().entrySet()) {
    DagNode<TaskNode.LeafTask<JsonNode>> node = entry.getValue();

    // Strategy-generated contingency takes precedence
    if (node.contingency() != null) {
      updatedNodes.put(entry.getKey(), node);
      continue;
    }

    // Extract capability name from the task — GoalStep carries capabilityName
    String capabilityName = (node.task() instanceof GoalStep step)
        ? step.capabilityName() : null;
    if (capabilityName == null) {
      updatedNodes.put(entry.getKey(), node);
      continue;
    }

    var bindings = definition.findBindingsByCapability(capabilityName);
    if (!bindings.isEmpty() && bindings.get(0).getContingency() != null) {
      DagPlan<TaskNode.LeafTask<JsonNode>> contingencyPlan =
          buildContingencyFromCapabilities(bindings.get(0).getContingency());
      updatedNodes.put(entry.getKey(),
          new DagNode<>(node.id(), node.task(), node.dependsOn(), node.joinType(), contingencyPlan));
    } else {
      updatedNodes.put(entry.getKey(), node);
    }
  }
  return new DagPlan<>(updatedNodes);
}

private DagPlan<TaskNode.LeafTask<JsonNode>> buildContingencyFromCapabilities(
    List<String> capabilityNames) {
  List<TaskNode.LeafTask<JsonNode>> steps = capabilityNames.stream()
      .map(name -> (TaskNode.LeafTask<JsonNode>) new GoalStep(
          UUID.randomUUID(), name, name, Instant.now()))
      .toList();
  return DagPlan.sequence(steps);
}
```

**Helper methods:**
- **Capability name extraction:** `GoalStep.capabilityName()` — works for GOAP and LLM decomposition outputs (both produce `GoalStep` instances). Non-GoalStep tasks are skipped (no YAML contingency attachment).
- **Contingency plan construction:** `buildContingencyFromCapabilities()` creates a `GoalStep` per capability name and chains them with `DagPlan.sequence()`, producing a single-exit sequential fallback plan.

### 9. GoapDecompositionStrategy (planning)

After building the primary plan, checks CBR failure rates for each action:

```java
private DagPlan<TaskNode.LeafTask<JsonNode>> attachCbrContingencies(
    DagPlan<TaskNode.LeafTask<JsonNode>> plan,
    List<GoapAction> allActions,
    DecompositionContext<JsonNode> context,
    CaseDefinition definition) {

  List<RetrievedExperience> experiences = extractExperiences(context);
  if (experiences.isEmpty()) return plan;

  double threshold = definition.getAdaptationConfig() != null
      ? definition.getAdaptationConfig().effectiveContingencyThreshold()
      : AdaptationConfig.DEFAULT_CONTINGENCY_THRESHOLD;
  int minSamples = GoapCostEnricher.resolveMinCostSamples(definition.getCbrConfig());

  Map<String, Double> failureRates =
      ExperienceAnalyser.actionFailureRates(experiences, minSamples);

  Map<String, DagNode<TaskNode.LeafTask<JsonNode>>> updatedNodes = new LinkedHashMap<>(plan.nodes());
  for (var entry : plan.nodes().entrySet()) {
    DagNode<TaskNode.LeafTask<JsonNode>> node = entry.getValue();
    if (node.contingency() != null) continue; // already has contingency
    String actionName = (node.task() instanceof GoalStep step) ? step.capabilityName() : null;
    if (actionName == null) continue;

    Double failureRate = failureRates.get(actionName);
    if (failureRate != null && failureRate >= threshold) {
      // Blacklist the primary action and replan
      var config = new PlannerConfig(PlannerConfig.DEFAULT_MAX_ITERATIONS,
          Set.of(actionName), true, true);
      List<GoapAction> altPlan = planner.plan(worldState, goalConditions, allActions, config);
      if (!altPlan.isEmpty()) {
        DagPlan<TaskNode.LeafTask<JsonNode>> contingencyPlan = buildDagPlan(altPlan);
        updatedNodes.put(entry.getKey(),
            new DagNode<>(node.id(), node.task(), node.dependsOn(), node.joinType(), contingencyPlan));
      }
    }
  }
  return new DagPlan<>(updatedNodes);
}
```

**Pipeline:** `ExperienceAnalyser.actionFailureRates(experiences, minSamples)` is a new method that computes per-capability failure rates from CBR history: `failureRate = 1 - successRate`. Returns `Map<String, Double>` keyed by capability name. Below `minSamples` threshold, the capability is omitted from the map (no contingency generation on insufficient data).

The contingency is generated by running `GoapPlanner.plan()` with the primary action blacklisted — the same mechanism as `replan()` but pre-computed at decomposition time.

### 10. LlmDecompositionStrategy (planning)

When the prompt context includes known failure modes (from CBR data or binding declarations), the LLM response schema is extended to support optional contingencies per step:

```json
{
  "steps": [
    {
      "id": "step-1",
      "description": "Verify identity via API",
      "capabilityName": "identity-check",
      "contingency": ["manual-review", "escalate-to-human"]
    },
    {
      "id": "step-2",
      "description": "Process payment",
      "capabilityName": "payment"
    }
  ]
}
```

The `contingency` field is optional per step — an array of alternative capability names. The parser creates `GoalStep` instances from the list and wraps them in `DagPlan.sequence()`, attaching to the corresponding `DagNode`. Steps without `contingency` produce nodes with null contingency (existing behavior).

**Fallback:** When the LLM does not produce contingency declarations (the common case until prompted), nodes have no contingencies. YAML contingencies are attached post-decomposition by `DefaultGoalDecomposer` (§8) as a fallback. No error when contingencies are absent.

### 11. CaseDefinitionYamlMapper (engine)

Parses `contingency:` on binding definitions:

```yaml
bindings:
  - name: verify-identity
    capability: identity-check
    contingency:
      - manual-review
      - escalate-to-human
```

The YAML array maps to `Binding.contingency` as `List<String>`. Each string is a capability name. The list is stored on the Binding and consumed by `DefaultGoalDecomposer` post-decomposition (see §8).

### 12. EventLog audit

New event type: `CaseHubEventType.CONTINGENCY_ACTIVATED`

Metadata:
- `nodeId` — the primary node that failed
- `originalFailureReason` — exception message from the primary failure
- `contingencyNodeCount` — number of nodes in the contingency sub-plan
- `contingencySucceeded` — boolean
- `contingencyDurationMs` — execution time of the contingency sub-plan
- `activatedContingencyNodeIds` — list of contingency node IDs that executed

Fired by `SnapshotCapturingDagEventListener.onContingencyActivated()` (or equivalent listener in the blocks adapter).

---

## Interaction with existing adaptation

| Scenario | What happens |
|----------|-------------|
| DagNode fails, has contingency, contingency succeeds | Node marked Completed. No adaptation triggered. |
| DagNode fails, has contingency, contingency fails | Node marked Failed. `propagateFailures()` runs. Engine adaptation evaluates (if in compound context). |
| DagNode fails, no contingency | Node marked Failed. `propagateFailures()` runs. Engine adaptation evaluates (if in compound context). |
| Engine PlanItem fails (no DagDriver) | Adaptation system handles directly (meta-reasoning → repair/optimization → concede). |

Contingency is the first line of defense (pre-computed, fast). Adaptation is the second line (reactive, may invoke LLM). The two never compete — contingency exhaustion PRECEDES adaptation evaluation.

---

## Constraints

- **Single-exit contingency plans:** Validated at DagNode construction. Multi-exit contingency plans are rejected. This ensures deterministic result extraction.
- **No retry interaction:** DagDriver doesn't know about retries. All exceptions trigger contingency. The engine's retry/reroute mechanism operates at a higher level.
- **No per-node timeout:** DagDriver has a global execution timeout (10 min latch) but no per-node deadline. Timeout-based contingency activation is out of scope.
- **Max contingency depth:** DagDriver limits recursive contingency nesting to 3 (configurable). Prevents unbounded recursive blocking from auto-generated plans where contingency nodes themselves carry contingencies.
- **Pre-release:** DagNode record change is source-breaking. All caller sites need updating with the new 5-arg constructor or backward-compatible 4-arg constructor.

---

## Testing strategy

1. **DagNode construction** — validate single-exit contingency, null contingency, backward-compatible constructor
2. **DagDriver contingency activation** — primary fails + contingency succeeds → Completed; primary fails + contingency fails → Failed; primary succeeds → no contingency activation; cancellation during contingency
3. **DagDriver propagation** — contingency success doesn't propagate failure to dependents; contingency failure does
4. **DagEventListener** — `onContingencyActivated` receives correct DagResult
5. **GoapDecompositionStrategy** — contingencies generated above threshold, not generated below, not generated below sample minimum
6. **DefaultGoalDecomposer** — YAML contingencies attached post-decomposition; strategy-generated take precedence
7. **CaseDefinitionYamlMapper** — `contingency:` parsed correctly on bindings
8. **Snapshot serialization** — DagNodeSnapshot with recursive contingency, NodeStateSnapshot with contingencyResult

---

## References

- [research/2026-08-18-adaptive-planning-intelligence.md §2.3] — HQCP 2025, contingent planning under partial observability
- [GE-20260818-6546f0] — DagNode dependency edge building from GOAP
- [GE-20260818-dd3e91] — DagPlan.fromNodes empty rejection
- [GE-20260714-aa950f] — DagDriver diamond inference
- [GE-20260808-47dc40] — CasePlanModel decomposition vs adaptation structural differences
- [DagNode.java] — engine-api, current 4-component record
- [DagDriver.java] — engine-common, executeNode/propagateFailures/computeReadySet
- [DagEventListener.java] — engine-common, observation callbacks
- [GoapDecompositionStrategy.java] — planning, buildDagPlan/replan
- [DefaultPlanAdaptationEvaluator.java] — planning, adaptation pipeline
- [DefaultGoalDecomposer] — planning, post-decomposition binding resolution
- [engine#938] — issue spec
- [decisions-938.md] — D1–D7, light decision review with revisions
