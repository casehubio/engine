# CaseHub Engine — Architecture and Design

> **Status:** Active development (casehub-engine)
> **Version:** 1.0.0 (in progress)

## Overview

casehub-engine is a **hybrid choreography+orchestration coordination engine** for multi-agent work. It extends the Blackboard Architecture (Hayes-Roth, 1985) using CMMN (Case Management Model and Notation) terminology. Two complementary execution models share the same worker selection infrastructure and Quartz-based execution layer, enabling both spontaneous self-organisation (choreography) and deliberate coordination (orchestration) in the same case.

## Architecture Layers

### Core Model (`engine-model`)

Plain POJOs with no Quarkus or JPA dependencies:

- **Domain objects:** `CaseMetaModel`, `CaseInstance`, `EventLog`
- **SPI interfaces:** `CaseMetaModelRepository`, `CaseInstanceRepository`, `EventLogRepository`
- **Enums:** `CaseStatus`, `CaseHubEventType`, `EventStreamType`
- **CDI events:** `CaseLifecycleEvent` — fired via `Event.fireAsync()` by lifecycle handlers; optional modules observe this to react to transitions without coupling to the engine

### Persistence (`casehub-persistence-hibernate`, in-memory test variant)

Manages storage and retrieval of domain objects. Uses JPA/Panache for production, in-memory map for tests.

### Engine (`engine`)

Orchestrates case execution via:

- **`CaseContextChangedEventHandler`** — watches for context changes, evaluates bindings, triggers choreography
- **`WorkerScheduleEventHandler`** — calls `WorkerContextProvider.buildContext()` then schedules work in Quartz
- **`WorkflowExecutionCompletedHandler`** — processes work completion, resumes WAITING cases
- **`EventLog`** — persistent audit trail of all decisions and state changes

Lifecycle handlers fire `CaseLifecycleEvent` via `Event.fireAsync()` after their EventLog write. If no observer is registered (e.g. `casehub-ledger` absent), the event fires into the void — zero overhead.

### Audit Ledger (`casehub-ledger`, optional)

An optional module that writes an immutable, hash-chained audit record for every significant case lifecycle transition. Depends on `engine-model` (for `CaseLifecycleEvent`) and `quarkus-ledger` — no dependency on the `engine` module itself.

| Class | Role |
|---|---|
| `CaseLedgerEntry` | `LedgerEntry` subclass (JOINED inheritance) — adds `caseId`, `commandType`, `eventType`, `caseStatus` |
| `CaseLedgerEntryRepository` | Extends `JpaLedgerEntryRepository`; `@ApplicationScoped` activates it as the CDI `LedgerEntryRepository` bean |
| `CaseLedgerEventCapture` | `@ObservesAsync CaseLifecycleEvent` — writes entry in its own `@Transactional` block on a managed executor thread |

**Flyway migration:** V2000 (`case_ledger_entry` table + FK to `ledger_entry`). V1000–V1004 are reserved by quarkus-ledger.

**Observed transitions:**

| Command | Event | Notes |
|---|---|---|
| StartCase | CaseStarted | First entry — seq=1 |
| SuspendCase | CaseSuspended | Admin pause |
| ResumeCase | CaseResumed | |
| SubmitWork | WorkSubmitted | WAITING transition |
| CompleteWork | WorkCompleted | Resume from WAITING |
| SignalCase | SignalReceived | External trigger |
| ReachMilestone | MilestoneReached | |
| ReachGoal | GoalReached | |
| CompleteCase | CaseCompleted | Final entry |
| FaultCase | CaseFaulted | Error termination |
| CancelCase | CaseCancelled | |
| ExecuteWorker / WorkerExecutionStarted | Worker began | |
| ExecuteWorker / WorkerExecutionCompleted | Worker finished | |

**Actor type inference:** `"system"` or null → `SYSTEM`; versioned persona (`model:persona@version`, e.g. `claude:casehub-agent@v1`) → `AGENT`; anything else → `HUMAN`.

**Eventual consistency note:** `CaseLedgerEventCapture` runs in a separate transaction from the case state update (required by `@ObservesAsync` + reactive engine). In production the engine processes one lifecycle event per case at a time, so sequence numbers are assigned without races.

## Execution Models

casehub-engine is a **hybrid choreography+orchestration engine**. Both models share the same worker selection infrastructure (`WorkBroker`, `WorkerSelectionStrategy`, `WorkloadProvider`) and the same Quartz execution layer.

### Choreography (Binding-Driven)

Context changes trigger binding evaluations. When a binding's condition is met, `CaseContextChangedEventHandler` builds a `WorkerCandidate` list from capable workers, calls `WorkBroker.apply()` with `LeastLoadedStrategy`, and publishes a `WorkerScheduleEvent` for the selected worker. If no pre-defined workers match a capability, the engine calls `tryProvision()` to attempt dynamic provisioning via the registered `WorkerProvisioner` SPI. The case remains `RUNNING` throughout.

```
CaseContext change
  → CaseContextChangedEventHandler.publishWorkerSchedules()
  → WorkBroker.apply(SelectionContext, CREATED, candidates, LeastLoadedStrategy)
  → AssignmentDecision.assignTo(workerId)                [candidates found]
  → WorkerScheduleEvent → WorkerScheduleEventHandler
      → WorkerContextProvider.buildContext()             [always called]
      → Quartz
  → WorkflowExecutionCompleted → CaseContext updated → next binding fires

  → tryProvision(caseInstance, capability)               [no candidates]
      → WorkerProvisioner.provision() if capability advertised
      → ProvisioningException caught; binding stays eligible
```

**Semantics:**
- No case suspension. Work flows continuously.
- Bindings are passive (triggered by context change), not imperative.
- Worker order emerges from dependency, not direction.
- All capable workers compete for selection; LeastLoadedStrategy picks the least-loaded.

### Orchestration (Explicit Work Submission)

`WorkOrchestrator.submit(CaseInstance, WorkRequest)` selects a worker via `WorkBroker`, publishes a `WorkerScheduleEvent`, and returns a `CompletionStage<WorkResult>`. `WorkOrchestrator.submitAndWait()` additionally suspends the case to `WAITING`; `WorkflowExecutionCompletedHandler` resumes it when the matching worker completes.

```
WorkOrchestrator.submitAndWait(instance, request)
  → WorkBroker selects worker
  → WORK_SUBMITTED written to EventLog (durable)
  → case transitions to WAITING, waitingForWorkId persisted
  → WorkerScheduleEvent → Quartz executes worker
  → WorkflowExecutionCompleted fires
  → WorkflowExecutionCompletedHandler: WAITING → RUNNING, WORK_COMPLETED written
  → PendingWorkRegistry.complete() → CompletionStage<WorkResult> resolves
  → CONTEXT_CHANGED fires (case now RUNNING) → bindings re-evaluate
```

**Semantics:**
- Explicit caller initiates work (not a binding).
- Caller receives a `CompletionStage<WorkResult>` to wait on externally.
- Optionally transitions the case to WAITING (for critical milestones).
- Survives JVM restart: correlation key persists, futures re-registered on startup.

### Worker Selection (Shared Infrastructure)

| Component | Role |
|---|---|
| `WorkBroker` (quarkus-work-core) | Trigger gate + capability filter + strategy dispatch |
| `LeastLoadedStrategy` (quarkus-work-core) | Selects worker with fewest active Quartz jobs |
| `CasehubWorkloadProvider` | Counts active Quartz jobs per worker name |
| `NoOpWorkerRegistry` (quarkus-work-core) | Group resolution (no-op; workers come from CaseDefinition) |

All selection paths converge on `WorkBroker.apply()`:
- **Input:** `SelectionContext` (workload type, filters), `AssignmentTrigger` (CREATED), `WorkerCandidate` list (capability-filtered workers with load counts), `WorkerSelectionStrategy` (LeastLoadedStrategy)
- **Output:** `AssignmentDecision` (either `assignTo(workerId)` or `noChange()`)

### Durability (Orchestration Only)

`PendingWorkRegistry` survives JVM restarts by scanning the EventLog on startup for `WORK_SUBMITTED` events without `WORK_COMPLETED` and re-registering futures. `WorkerExecutionRecoveryService` replays the Quartz jobs; both mechanisms work together to restore in-flight orchestrated work.

The `waitingForWorkId` column on `CaseInstanceEntity` persists the correlation between a WAITING case and its in-flight work, enabling WAITING→RUNNING resumption after restart.

**EventLog entries for orchestrated work:**
- `WORK_SUBMITTED` — when `WorkOrchestrator.submit()` is called (metadata: `correlationKey`, `capability`)
- `WORK_COMPLETED` — when `WorkflowExecutionCompletedHandler` resumes a WAITING case (metadata: `correlationKey`, old/new status)

## Worker Execution Lifecycle

Full sequence from Quartz job fire to case context update:

```
Quartz fires job
  → WorkerExecutionJobListener.jobToBeExecuted()
      → WorkerStatusListener.onWorkerStarted(workerId, {caseId})
      → EventLog: WORKER_EXECUTION_STARTED (async persist)
  → WorkerExecutionTask.execute()
      → load EventLog by ID
      → load CaseInstance (cache or restore)
      → resolve Worker and Capability from CaseDefinition
      → execute worker function (Workflow or Function<Map,Map>)
      → publish WorkflowExecutionCompleted → WORKER_EXECUTION_FINISHED bus
  → WorkflowExecutionCompletedHandler.onWorkflowExecutionCompletedHandler()
      → snapshot CaseContext (before)
      → apply output with conflict resolution strategy
      → snapshot CaseContext (after), compute contextDiff
      → EventLog: WORKER_EXECUTION_COMPLETED (payload=output, metadata=inputDataHash+contextChanges)
      → resumeIfWaiting() — if case WAITING and correlationKey matches:
          → case RUNNING, EventLog: WORK_COMPLETED, PendingWorkRegistry.complete()
      → WorkerStatusListener.onWorkerCompleted(workerId, WorkResult)
      → CaseLifecycleEvent: WorkerExecutionCompleted (async CDI)
      → publish CONTEXT_CHANGED → bindings re-evaluate
```

**Conflict resolution on output:** Each output key is written through the `ContextDiffStrategy`-selected resolver configured on the binding (`LAST_WRITER_WINS` default, `FIRST_WRITER_WINS`, or `FAIL`).

**Idempotency:** `WorkerScheduleEventHandler` holds a Vert.x local lock on `(caseId, workerId, inputDataHash)` and checks for existing `WORKER_SCHEDULED / WORKER_EXECUTION_STARTED / WORKER_EXECUTION_COMPLETED` events before submitting to Quartz. Duplicate `CONTEXT_CHANGED` events that arrive while a worker is in flight are silently dropped.

## Failure and Retry Lifecycle

When a Quartz job throws, the retry-or-fault sequence is:

```
WorkerExecutionTask.execute() throws
  → WorkerExecutionJobListener.jobWasExecuted(context, exception)
      → EventLog: WORKER_EXECUTION_FAILED (metadata=inputDataHash+errorMessage)
      → maybeRescheduleJob()
          → load RetryPolicy from Worker.executionPolicy.retries
          → count WORKER_EXECUTION_FAILED events for (caseId, workerId, inputDataHash)
          → if failureCount < retryPolicy.maxAttempts:
              → compute delay (FIXED | EXPONENTIAL | EXPONENTIAL_WITH_JITTER)
              → WorkerExecutionScheduler.scheduleRetry()
          → else:
              → publish WorkerRetriesExhaustedEvent → WORKER_RETRIES_EXHAUSTED bus
  → WorkerRetriesExhaustedEventHandler.onWorkerRetriesExhaustedEvent()
      → set CaseInstance.state = FAULTED
      → caseInstanceRepository.updateStateAndAppendEvent()    [atomic]
          → EventLog: CASE_FAULTED
      → WorkerStatusListener.onWorkerStalled(workerId)
      → publish CASE_STATUS_CHANGED
  → CaseStatusChangedHandler.onCaseStatusChangedHandler()
      → CaseChannelProvider.closeChannel() for all open channels
      → SchedulerService.cancelAllTriggers(caseId)
      → publish CASE_FAULTED bus address
```

**Retry count source:** `EventLogRepository.findByCaseAndWorkerAndType(WORKER_EXECUTION_FAILED)` filtered by `inputDataHash` metadata — counts distinct failed attempts for the same logical invocation.

**Backoff strategies:**
- `FIXED` — constant `delayMs`
- `EXPONENTIAL` — `delayMs × 2^(attempt-1)`, capped at 30 s
- `EXPONENTIAL_WITH_JITTER` — random in `[0, exponential cap]`

**Guard quarantine:** `WorkerExecutionGuard.isBlocked()` is checked in `WorkerScheduleEventHandler` before event log creation. Quarantined workers immediately emit `WorkerRetriesExhaustedEvent` without scheduling a Quartz job.

## EventLog Event Sequence

Every significant decision is recorded with full provenance. The table below shows events in emission order for a complete successful case, plus the variants for failure and orchestration.

### Successful choreography case

| # | EventLog type | Writer | Notes |
|---|---|---|---|
| 1 | `CASE_STARTED` | `CaseStartedEventHandler` | Payload: initial context snapshot |
| 2 | `WORKER_SCHEDULED` | `WorkerScheduleEventHandler` | Metadata: workerName, capabilityName, inputDataHash |
| 3 | `WORKER_EXECUTION_STARTED` | `WorkerExecutionJobListener` | Metadata: inputDataHash |
| 4 | `WORKER_EXECUTION_COMPLETED` | `WorkflowExecutionCompletedHandler` | Payload: output; metadata: inputDataHash, contextChanges |
| 5 | `CASE_STATUS_CHANGED` → `CASE_COMPLETED` | `CaseStatusChangedHandler` | Written when goal expression satisfied |

### Failure variant (retries exhausted)

After step 3 above, for each failed attempt:

| # | EventLog type | Writer | Notes |
|---|---|---|---|
| 4a | `WORKER_EXECUTION_FAILED` | `WorkerExecutionJobListener` | Metadata: inputDataHash, errorMessage |
| (repeat 4a per attempt until maxAttempts) | | | |
| 5a | `CASE_FAULTED` | `WorkerRetriesExhaustedEventHandler` | Atomic with state transition |

### Orchestration variant (WAITING/RUNNING)

Between steps 2 and 3 above, `WorkOrchestrator.submitAndWait()` inserts:

| # | EventLog type | Writer | Notes |
|---|---|---|---|
| 2a | `WORK_SUBMITTED` | `WorkOrchestrator` | Metadata: correlationKey, capability |
| (case → WAITING) | `CASE_STATUS_CHANGED` | `CaseStatusChangedHandler` | |
| (after step 4) | `WORK_COMPLETED` | `WorkflowExecutionCompletedHandler.resumeIfWaiting()` | Metadata: correlationKey |
| (case → RUNNING) | `CASE_STATUS_CHANGED` | `CaseStatusChangedHandler` | |

The EventLog is append-only and immutable — a complete audit trail of the case's decision history.

## Naming Conventions

See **ADR-0003** (`adr/0003-work-workitem-task-naming.md`) for the formal decision. Summary:

| Term | Meaning |
|---|---|
| **Work** | Generalized assignable unit (automated or human) — the top-level concept |
| **WorkBroker** | Routes Work to the right worker (quarkus-work-api SPI) |
| **WorkItem** | Human-inbox specialisation of Work (requires claim/inbox semantics) |
| **Task** | Sub-steps within a Work unit (lowest granularity) |

casehub-engine uses `WorkBroker` from `quarkus-work-api` (shared SPI with quarkus-workitems) and `WorkOrchestrator` as the top-level orchestration API. This replaces the casehub-core `TaskBroker` (retired terminology).

## Case Lifecycle

```
PENDING (case created, not yet started)
  → RUNNING (bindings evaluated, work flows)
    → WAITING (orchestrated work in flight, case suspended; only from RUNNING via submitAndWait)
      → RUNNING (work completed, case resumes)
    → COMPLETED (all goals reached, case successful)
    → FAULTED (binding threw, work failed, or explicit error)
    → CANCELLED (case cancelled explicitly)
```

Only orchestration transitions a case to WAITING. Choreography keeps the case RUNNING unless an error occurs.

## Dependencies and SPI

The engine defines clean extension points via SPIs:

- **`CaseInstanceRepository`** — persist and retrieve case state
- **`EventLogRepository`** — persist and query the event log
- **`CaseMetaModelRepository`** — retrieve case definitions

External systems implement these SPIs to provide storage. The engine depends only on the SPIs, not on specific storage backends.

### Worker Provisioner SPIs

Four dual-stack SPI interfaces (blocking + reactive) enable external systems to provision workers, observe lifecycle events, create channels for inter-worker communication, and build worker startup context from case lineage.

| Blocking SPI | Reactive Mirror | Purpose |
|---|---|---|
| `WorkerProvisioner` | `ReactiveWorkerProvisioner` | Provision/terminate workers when no pre-defined workers match a capability |
| `WorkerStatusListener` | `ReactiveWorkerStatusListener` | Lifecycle callbacks: `started()`, `completed()`, `stalled()` |
| `CaseChannelProvider` | `ReactiveCaseChannelProvider` | Open/close/post to backend-agnostic channels (Qhorus, Slack, etc.) |
| `WorkerContextProvider` | `ReactiveWorkerContextProvider` | Build startup context from `CaseLedgerEntry` lineage — includes prior worker summaries, causal chain metadata |

**Model types** in `api/model/`:
- `CaseChannel` — backend-agnostic channel reference with extensible `properties` map
- `WorkerSummary` — prior worker's execution summary, includes `ledgerEntryId` (UUID of the `WORKER_EXECUTION_COMPLETED` ledger entry)
- `WorkerContext` — startup context for a newly provisioned worker, includes `priorWorkers` list and channel references
- `ProvisionContext` — input to `WorkerProvisioner.provision()`, contains the work request and case metadata

**Default implementations** in `engine/internal/worker/`:
- `NoOpWorkerProvisioner` — throws `ProvisioningException` (never called unless provisioner advertises capabilities)
- `NoOpWorkerStatusListener` — silently ignores all lifecycle events
- `NoOpCaseChannelProvider` — returns sentinel channels with `backendType = "none"`
- `EmptyWorkerContextProvider` — returns minimal context with empty `priorWorkers` list
- Four `@Alternative` reactive mirrors for optional reactive pipeline use

**Causal chain:** When a worker completes, `CaseLedgerEventCapture` writes a `WORKER_EXECUTION_COMPLETED` ledger entry. The `WorkerSummary` for that worker carries this entry's UUID as `ledgerEntryId`. New workers set `causedByEntryId` on their own ledger entries to this value, completing the causal chain across workers on a case.

**SPI placement rule:** Operational SPIs (worker provisioning, lifecycle, channels) go in `api/spi/`; persistence SPIs (`CaseMetaModelRepository`, etc.) go in `engine-model/spi/`.

### SPI Call Sites

All seven engine SPI call sites, in lifecycle order:

| SPI method | Called in | When |
|---|---|---|
| `CaseChannelProvider.openChannel` | `CaseStartedEventHandler.onCaseStarted` | Case transitions to RUNNING |
| `WorkerContextProvider.buildContext` | `WorkerScheduleEventHandler.onWorkerScheduleEventHandler` | Before Quartz job is submitted |
| `WorkerProvisioner.provision` | `CaseContextChangedEventHandler.tryProvision` | No pre-defined workers match capability AND provisioner advertises it |
| `WorkerStatusListener.onWorkerStarted` | `WorkerExecutionJobListener.jobToBeExecuted` | Quartz job begins execution |
| `WorkerStatusListener.onWorkerCompleted` | `WorkflowExecutionCompletedHandler` | Worker function returns successfully |
| `WorkerStatusListener.onWorkerStalled` | `WorkerRetriesExhaustedEventHandler` | All retries exhausted; case transitions to FAULTED |
| `CaseChannelProvider.closeChannel` | `CaseStatusChangedHandler` | Case reaches terminal state (COMPLETED / FAULTED / CANCELLED) |

`WorkerProvisioner.provision()` is guarded by `getCapabilities()` — the no-op default returns empty set, so it is never called unless a real provisioner is wired in. `ProvisioningException` is caught and logged; the binding stays eligible for the next tick.

## Configuration

Configuration uses the `casehub.` prefix. Key properties:

```properties
# Quartz (RAM store, no JDBC)
quarkus.quartz.store-type=ram

# Schema management (Hibernate drop-and-create, no migrations)
quarkus.hibernate-orm.schema-management.strategy=drop-and-create

# Event bus (Vert.x)
quarkus.vertx.event-loops=16
```

See `src/main/resources/application.properties` for all available options.

## Testing

Tests use `@QuarkusTest` (never `*IT` classes). In-memory SPI implementations are provided for tests, eliminating Docker/database dependencies.

**Build and test:**

```bash
# Full suite
TESTCONTAINERS_RYUK_DISABLED=true mvn clean test

# Single module
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine

# Single test class
TESTCONTAINERS_RYUK_DISABLED=true mvn test -Dtest=ChoreographySelectionTest
```

## Roadmap

**Near term:**
- ✅ Hybrid choreography+orchestration (Q2 2026)
- ✅ WAITING state durability (Q2 2026)
- ✅ Immutable audit ledger (`casehub-ledger`, Q2 2026)
- ✅ Worker Provisioner SPI wiring — all 4 blocking SPIs integrated (Q2 2026)
- [ ] Human worker integration (Q2/Q3 2026)
- [ ] Escalation rules and thresholds (Q3 2026)

**Medium term:**
- [ ] Lineage-driven planning (learns from history)
- [ ] Cascade/parallel binding support (advanced scenarios)
- [ ] Sub-case delegation (nested cases)

**Long term:**
- [ ] Integration with Claudony (session management + dashboard)
- [ ] Integration with Qhorus (inter-agent communication)

## Further Reading

- **ADR-0001** — Blackboard model and terminology alignment
- **ADR-0002** — Binding evaluation strategy
- **ADR-0003** — Work/WorkItem/Task naming hierarchy
- **casehubio/engine#121** — Original design discussion (closed by ADR-0003)
- **casehubio/engine#131** — WorkBroker integration epic
- **casehubio/engine#145** — quarkus-ledger integration epic
- **casehubio/engine#191** — Worker Provisioner SPI wiring
- **mdproctor/quarkus-ledger#39** — CaseLedgerEntry tracking issue
