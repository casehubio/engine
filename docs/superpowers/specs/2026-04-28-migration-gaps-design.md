# Migration Gaps — Design Spec

**Date:** 2026-04-28
**Issues:** casehubio/engine#193 (idempotency window), #194 (DLQ replay), #195 (SubCaseBinding)
**Status:** Approved for implementation

---

## Overview

Three bounded gaps remain from the casehub → casehub-engine migration. Each is self-contained and can be implemented and merged independently.

---

## Gap 1 — Idempotency Window (casehubio/engine#193)

### Problem

`WorkerScheduleEventHandler` deduplicates work submissions by checking the EventLog for existing `WORKER_SCHEDULED` events matching `(caseId, workerId, inputDataHash)`. This check has no time bound — the same logical work unit is permanently blocked from re-executing on a case, even intentionally (e.g. after a fix or case reset).

This follows the event-sourced pattern of using the event log itself as the idempotency store, which is correct. The only missing element is a configurable deduplication window.

### Design

**Configuration property:** `casehub.idempotency.window` (`Optional<Duration>`, default: absent = unlimited = current behaviour).

**`EventLogRepository` SPI change:** Add an optional `after` parameter to `findSchedulingEvents`:

```java
// engine-model/src/main/java/io/casehub/engine/spi/EventLogRepository.java

// Existing (kept for backward compat — delegates to new overload with null cutoff):
Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId);

// New overload:
Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId, Instant after);
```

Default implementation of the old signature delegates to the new one with `after = null`.

**`WorkerScheduleEventHandler` change:**

```java
@ConfigProperty(name = "casehub.idempotency.window")
Optional<Duration> idempotencyWindow;

// In scheduleUnderLock():
Instant after = idempotencyWindow.map(w -> Instant.now().minus(w)).orElse(null);
eventLogRepository.findSchedulingEvents(instance.getUuid(), worker.getName(), after)
```

**Repository implementations:**
- `InMemoryEventLogRepository` — filter by `eventLog.getTimestamp().isAfter(after)` when `after != null`
- `JpaEventLogRepository` — `AND e.timestamp > :after` in JPQL when `after != null`

### Invariants
- `after = null` → identical to current behaviour (no regression)
- Window applies per `(caseId, workerId)` pair — not global
- Window is checked at scheduling time, not at dedup-key creation time

### Test strategy

| Level | Test | Where |
|---|---|---|
| Unit | `findSchedulingEvents` with `after` cutoff filters correctly by timestamp | `InMemoryEventLogRepositoryTest`, `JpaEventLogRepositoryTest` |
| Unit | `after = null` returns same results as current no-cutoff call | both repository tests |
| Unit | `WorkerScheduleEventHandler` reads config and passes cutoff correctly | `WorkerScheduleEventHandlerTest` (new) |
| Integration | Duplicate blocked within window | `WorkerIdempotencyTest` — new case: submit twice within window, second is skipped |
| Integration | Re-execution allowed after window expires | `WorkerIdempotencyTest` — new case: `Clock` injection or short window + sleep |
| Correctness | `after` is computed at scheduling time, not stored | assertion on EventLog count |
| Config | `casehub.idempotency.window` absent → unlimited (no regression) | existing idempotency tests still pass |

---

## Gap 2 — DLQ Replay (casehubio/engine#194)

### Problem

`DeadLetterQueue.markReplayed()` is a state-transition stub. No code actually re-executes the failed worker. The `DeadLetterEventHandler` comment says "full input can be recovered from EventLog on replay" — but the mechanism doesn't exist. Operators have no way to retry exhausted workers without manual intervention at the engine level.

### Design

#### DeadLetterEntry changes

Add replay tracking:

```java
// New fields:
private volatile int replayAttempts = 0;
private volatile Instant lastReplayAttemptAt = null;

// New accessors:
public int replayAttempts() { return replayAttempts; }
public Instant lastReplayAttemptAt() { return lastReplayAttemptAt; }

// Package-private mutators (called by DeadLetterReplayService):
void incrementReplayAttempts() {
    replayAttempts++;
    lastReplayAttemptAt = Instant.now();
}
```

#### DeadLetterReplayService (new — casehub-resilience)

```java
@ApplicationScoped
public class DeadLetterReplayService {

    // Returns the replayed entry on success, empty if not found or already replayed/discarded.
    public Optional<DeadLetterEntry> replay(String deadLetterId);

    // Replays all PENDING_REVIEW entries. Used by auto-replay scheduler.
    public List<DeadLetterEntry> replayPending();
}
```

**Replay sequence:**
1. Load `DeadLetterEntry` by ID — return empty if not found or status != `PENDING_REVIEW`
2. Query EventLog: `findByCaseAndWorkerAndType(caseId, workerId, WORKER_SCHEDULED)` filtered by `idempotencyHash` → get original `EventLog` entry
3. If EventLog not found: log WARN, leave entry `PENDING_REVIEW`, return empty
4. Load `CaseInstance` from `CaseInstanceRepository` — if FAULTED/CANCELLED, log WARN, return empty (case must be in a state that can accept work)
5. Extract `Worker` and `Capability` from `CaseDefinitionRegistry` using `CaseInstance.getCaseMetaModel()`
6. Publish `WorkerScheduleEvent(caseInstance, worker, capability)` to Vert.x event bus
7. Call `entry.incrementReplayAttempts()`, `deadLetterQueue.markReplayed(deadLetterId)`
8. Return `Optional.of(entry)`

**Dependencies:** `DeadLetterQueue`, `EventLogRepository`, `CaseInstanceRepository`, `CaseDefinitionRegistry`, Vert.x `EventBus`.

Note: `casehub-resilience` already depends on `engine` — these injections are valid.

#### DeadLetterAutoReplayJob (new — casehub-resilience)

```java
@ApplicationScoped
public class DeadLetterAutoReplayJob {
    // Scheduled via Quartz; disabled by default
    // Config-driven delays: first attempt after delay[0], second after delay[1], etc.
}
```

**Configuration:**

```properties
casehub.dlq.auto-replay.enabled=false
casehub.dlq.auto-replay.delays=30m,2h,8h
casehub.dlq.auto-replay.max-attempts=3
```

**Scheduling logic:**
- When `enabled=true`: registers a Quartz job on startup that fires every `min(delays)`
- Job calls `deadLetterReplayService.replayPending()` for entries where:
  - `status == PENDING_REVIEW`
  - `replayAttempts < max-attempts`
  - `lastReplayAttemptAt == null OR lastReplayAttemptAt < now - delays[replayAttempts]`
- Entries reaching `max-attempts` are left `PENDING_REVIEW` for manual triage (logged at WARN)

#### DeadLetterQueue change

Add `queryEligibleForAutoReplay(int maxAttempts, List<Duration> delays)` convenience query — builds a `DeadLetterQuery` predicate from the auto-replay config. Keeps auto-replay eligibility logic testable without starting Quartz.

### Test strategy

| Level | Test | Where |
|---|---|---|
| Unit | `DeadLetterEntry.incrementReplayAttempts()` increments count and sets timestamp | `DeadLetterEntryTest` (new) |
| Unit | `DeadLetterReplayService.replay()` — entry not found returns empty | `DeadLetterReplayServiceTest` (new) |
| Unit | `DeadLetterReplayService.replay()` — entry already REPLAYED returns empty | same |
| Unit | `DeadLetterReplayService.replay()` — EventLog not found leaves entry PENDING_REVIEW | same |
| Unit | `DeadLetterAutoReplayJob` eligibility predicate — correct delay logic | `DeadLetterAutoReplayJobTest` (new) |
| Integration | Worker fails → DLQ → explicit replay → worker re-executes → case resumes | `DeadLetterReplayIntegrationTest` (new) |
| Integration | Worker fails → DLQ → auto-replay scheduler fires → worker re-executes | same (short delay config) |
| Correctness | Replay on FAULTED case returns empty (case must accept work) | `DeadLetterReplayServiceTest` |
| Correctness | `max-attempts` reached → entry stays PENDING_REVIEW, not REPLAYED | `DeadLetterReplayIntegrationTest` |
| Robustness | `CaseDefinitionRegistry` returns null (case def removed) → WARN, no replay | `DeadLetterReplayServiceTest` |
| Happy path | Full E2E: failing worker → DLQ → replay → case COMPLETED | `DeadLetterQueueEndToEndTest` (extend existing) |

---

## Gap 3 — SubCaseBinding (casehubio/engine#195)

### Problem

`SubCase` is a data model only. `Binding` has no way to reference a `SubCase`. `CaseContextChangedEventHandler` cannot spawn child cases. `SubCaseCompletionStrategy` exists but is never invoked. The engine has no parent-child case relationship at runtime.

### Design

#### SubCase model changes (casehub-blackboard)

Add execution control fields:

```java
private final boolean waitForCompletion;  // default: true
private final String inputMapping;         // JQ, default: "." (identity)
private final String outputMapping;        // JQ, default: null (no propagation)
```

`inputMapping` is evaluated against the parent `CaseContext` snapshot to produce the child case's initial context. `outputMapping` is evaluated against the child's final context snapshot; the result is merged into the parent context keys.

#### Binding model changes (api)

```java
// New optional field — mutually exclusive with capability at validation time:
private SubCase subCase;

// Builder additions:
public Builder subCase(SubCase subCase) { ... }

// Accessor:
public SubCase getSubCase() { return subCase; }
```

`Binding.Builder.build()` validates: `capability` and `subCase` cannot both be set.

#### New event: SubCaseScheduleEvent (casehub-blackboard)

```java
public record SubCaseScheduleEvent(
    CaseInstance parentInstance,
    SubCase subCase,
    Map<String, Object> childInitialContext  // result of inputMapping evaluation
) {}
```

Event bus address: `EventBusAddresses.SUBCASE_SCHEDULE = "casehub.subcase.schedule"`.

#### CaseContextChangedEventHandler change (engine)

In `publishWorkerSchedules()`, when a binding has `subCase != null`:

```java
if (binding.getSubCase() != null) {
    return publishSubCaseSchedule(caseInstance, binding);
}
// existing worker path
```

`publishSubCaseSchedule()` evaluates `inputMapping` against parent context and publishes `SubCaseScheduleEvent`.

#### SubCaseExecutionHandler (casehub-blackboard)

```java
@ApplicationScoped
public class SubCaseExecutionHandler {

    @Inject CaseHubRuntime runtime;
    @Inject EventLogRepository eventLogRepository;
    @Inject CaseInstanceRepository caseInstanceRepository;
    @Inject PendingWorkRegistry pendingWorkRegistry;
    @Inject EventBus eventBus;

    @ConsumeEvent(EventBusAddresses.SUBCASE_SCHEDULE)
    public Uni<Void> onSubCaseSchedule(SubCaseScheduleEvent event);
}
```

**Execution sequence (`waitForCompletion=true`):**
1. Resolve child `CaseDefinition` from `CaseDefinitionRegistry` (by namespace/name/version)
2. Start child case: `runtime.startCase(childDefinition, childInitialContext)` → `childCaseId`
3. Write `SUBCASE_STARTED` EventLog entry on parent (metadata: `childCaseId`, `waitForCompletion=true`, `outputMapping`)
4. Register future: `pendingWorkRegistry.register(childCaseId.toString(), future)`
5. Transition parent to `WAITING` with `waitingForWorkId = childCaseId.toString()`
6. Persist updated parent state

**Execution sequence (`waitForCompletion=false`):**
1–3 same as above (metadata: `waitForCompletion=false`)
4. No future registration, parent stays `RUNNING`

#### SubCaseCompletionListener (casehub-blackboard)

```java
@ApplicationScoped
public class SubCaseCompletionListener {

    // Observes all CaseLifecycleEvent — filters for terminal states
    public void onCaseLifecycle(@ObservesAsync CaseLifecycleEvent event);
}
```

**Completion sequence:**
1. Check if `event.commandType()` is `CompleteCase`, `FaultCase`, or `CancelCase`
2. Query EventLog for `SUBCASE_STARTED` entry where `metadata.childCaseId == event.caseId()`
3. If not found: no-op (this case is not a child of any waiting parent)
4. Load parent `CaseInstance`; load child final context from child's EventLog (`CASE_COMPLETED` payload)
5. Evaluate `outputMapping` against child's final context → write resulting keys to parent context
6. If `waitForCompletion=true`:
   - Transition parent `WAITING → RUNNING` atomically via `caseInstanceRepository.updateStateAndAppendEvent()` (same pattern as `WorkflowExecutionCompletedHandler.resumeIfWaiting()` — extracted to a shared `CaseResumptionService` to avoid duplication)
   - Write `SUBCASE_COMPLETED` EventLog entry on parent (metadata: `childCaseId`, `childFinalStatus`)
   - Complete any registered `PendingWorkRegistry` future for `childCaseId.toString()`
   - Publish `CONTEXT_CHANGED` on parent so bindings re-evaluate
7. If `waitForCompletion=false`:
   - Write `SUBCASE_COMPLETED` EventLog entry on parent
   - Publish `CONTEXT_CHANGED` on parent
8. Apply `SubCaseCompletionStrategy.mapToStageItemStatus(childCaseStatus)` to determine if parent should fault

**`CaseResumptionService` (new — engine):** Extracts the WAITING→RUNNING transition from `WorkflowExecutionCompletedHandler.resumeIfWaiting()` into an injectable service, usable by both the Quartz worker completion path and the sub-case completion path without duplication.

**Cancellation propagation:** When parent transitions to `CANCELLED`, `CaseStatusChangedHandler` also cancels any active child cases found via `SUBCASE_STARTED` EventLog entries.

#### EventLog entries

| Type | Written by | Metadata |
|---|---|---|
| `SUBCASE_STARTED` | `SubCaseExecutionHandler` | `childCaseId`, `waitForCompletion`, `outputMapping` |
| `SUBCASE_COMPLETED` | `SubCaseCompletionListener` | `childCaseId`, `childFinalStatus` |

Add `SUBCASE_STARTED` and `SUBCASE_COMPLETED` to `CaseHubEventType`.

### Circular sub-case detection

`SubCaseExecutionHandler` checks: if the child case definition key (namespace/name/version) matches the parent's own definition key, reject with a `SubCaseCircularDependencyException` (extends `RuntimeException`). Does not detect deeper cycles — that is an accepted limitation documented in code.

### Test strategy

| Level | Test | Where |
|---|---|---|
| Unit | `SubCase.Builder` — `waitForCompletion` default true, fields set correctly | `SubCaseTest` (extend existing) |
| Unit | `Binding.Builder` — `subCase` and `capability` mutually exclusive | `BindingTest` (new) |
| Unit | `inputMapping` evaluation against parent context snapshot | `SubCaseExecutionHandlerTest` (new) |
| Unit | `outputMapping` evaluation against child final context | same |
| Unit | Circular detection: same namespace/name/version rejects | same |
| Integration (waitForCompletion=true) | Parent goes WAITING, child runs, parent resumes with child output | `SubCaseIntegrationTest` (new) |
| Integration (waitForCompletion=false) | Parent stays RUNNING, child runs independently, parent context updated on child completion | same |
| Correctness | Parent context updated only with keys returned by outputMapping | same |
| Correctness | Child FAULTED → parent FAULTED (via DefaultSubCaseCompletionStrategy) | same |
| Correctness | Parent CANCELLED → child case cancelled | same |
| Correctness | `SUBCASE_STARTED` EventLog written with correct childCaseId | same |
| Robustness | Child definition not found → error log, parent stays RUNNING (not faulted) | `SubCaseExecutionHandlerTest` |
| Robustness | outputMapping null → no parent context update, parent still resumes | `SubCaseIntegrationTest` |
| Happy path | Full E2E: parent starts → child spawned → child completes → parent completes | `SubCaseIntegrationTest` |

---

## Documentation impact

All three gaps require updates to:

- **`docs/DESIGN.md`** — Worker Execution Lifecycle section, EventLog Event Sequence section, Dependencies and SPI section, Roadmap
- **`CLAUDE.md`** — casehub-resilience module description (DLQ replay), casehub-blackboard module description (SubCaseBinding)
- **Migration plan** — mark all three gaps as resolved
- **`SubCase.java`** Javadoc — remove "future epic" note, update to reference #195

## Commit discipline

Every commit references its issue:
- `Refs #193` / `Closes #193` — idempotency window
- `Refs #194` / `Closes #194` — DLQ replay
- `Refs #195` / `Closes #195` — SubCaseBinding

All work goes through the fork (`mdproctor/engine`) via separate branches, one per gap. PRs created in order: #193 first (smallest, no dependencies), #194 second, #195 third.
