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
<<<<<<< feat/casehub-ledger-integration
- **CDI events:** `CaseLifecycleEvent` — fired via `Event.fireAsync()` by lifecycle handlers; optional modules observe this to react to transitions without coupling to the engine
=======
>>>>>>> main

### Persistence (`casehub-persistence-hibernate`, in-memory test variant)

Manages storage and retrieval of domain objects. Uses JPA/Panache for production, in-memory map for tests.

### Engine (`engine`)

Orchestrates case execution via:

- **`CaseContextChangedEventHandler`** — watches for context changes, evaluates bindings, triggers choreography
- **`WorkerScheduleEventHandler`** — schedules work in Quartz
- **`WorkflowExecutionCompletedHandler`** — processes work completion, resumes WAITING cases
- **`EventLog`** — persistent audit trail of all decisions and state changes

<<<<<<< feat/casehub-ledger-integration
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

**Actor type inference:** `"system"` or null → `SYSTEM`; versioned persona (`model:persona@version`, e.g. `claude:casehub-agent@v1`) → `AGENT`; anything else → `HUMAN`.

**Eventual consistency note:** `CaseLedgerEventCapture` runs in a separate transaction from the case state update (required by `@ObservesAsync` + reactive engine). In production the engine processes one lifecycle event per case at a time, so sequence numbers are assigned without races.

=======
>>>>>>> main
## Execution Models

casehub-engine is a **hybrid choreography+orchestration engine**. Both models share the same worker selection infrastructure (`WorkBroker`, `WorkerSelectionStrategy`, `WorkloadProvider`) and the same Quartz execution layer.

### Choreography (Binding-Driven)

Context changes trigger binding evaluations. When a binding's condition is met, `CaseContextChangedEventHandler` builds a `WorkerCandidate` list from capable workers, calls `WorkBroker.apply()` with `LeastLoadedStrategy`, and publishes a `WorkerScheduleEvent` for the selected worker. The case remains `RUNNING` throughout. A configurable `casehub.idempotency.window` bounds how far back this check looks — absent means permanent dedup (default).

```
CaseContext change
  → CaseContextChangedEventHandler.publishWorkerSchedules()
  → WorkBroker.apply(SelectionContext, CREATED, candidates, LeastLoadedStrategy)
  → AssignmentDecision.assignTo(workerId)
  → WorkerScheduleEvent → WorkerScheduleEventHandler → Quartz
  → WorkflowExecutionCompleted → CaseContext updated → next binding fires
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

## EventLog and Lineage

Every significant decision is recorded with full provenance:

- **CASE_STARTED** — case created
- **CASE_COMPLETED** — all goals satisfied
- **CASE_FAULTED** — error occurred
- **CASE_CANCELLED** — explicit cancellation
- **CASE_STATUS_CHANGED** — transition (e.g., RUNNING → WAITING → RUNNING)
- **WORKER_SCHEDULED** — binding triggered, worker selected
- **WORKER_EXECUTION_STARTED** — Quartz job started
- **WORKER_EXECUTION_COMPLETED** — worker finished, output applied
- **WORKER_EXECUTION_FAILED** — worker threw
- **WORK_SUBMITTED** — orchestrated work submitted (includes correlation key)
- **WORK_COMPLETED** — orchestrated work completed (case resumes if WAITING)
- **SIGNAL_RECEIVED** — external signal (human input, webhook, etc.)
- **MILESTONE_REACHED** — intermediate goal achieved
- **GOAL_REACHED** — final goal satisfied

The EventLog is append-only and immutable — a complete audit trail of the case's decision history.

## Dependencies and SPI

The engine defines clean extension points via SPIs:

- **`CaseInstanceRepository`** — persist and retrieve case state
- **`EventLogRepository`** — persist and query the event log
- **`CaseMetaModelRepository`** — retrieve case definitions

External systems implement these SPIs to provide storage. The engine depends only on the SPIs, not on specific storage backends.

<<<<<<< feat/casehub-ledger-integration
### Worker Provisioner SPIs

Four dual-stack SPI interfaces (blocking + reactive) enable external systems to provision workers, observe lifecycle events, create channels for inter-worker communication, and build worker startup context from case lineage.

| Blocking SPI | Reactive Mirror | Purpose |
|---|---|---|
| `WorkerProvisioner` | `ReactiveWorkerProvisioner` | Provision/terminate workers when a `PlanItem` is eligible but no workers are available; responds with the registered `Worker` |
| `WorkerStatusListener` | `ReactiveWorkerStatusListener` | Lifecycle callbacks: `started()`, `completed()`, `stalled()` for observing worker state transitions |
| `CaseChannelProvider` | `ReactiveCaseChannelProvider` | Open/close/post to backend-agnostic channels (Qhorus, Slack, email, etc.) for inter-worker communication |
| `WorkerContextProvider` | `ReactiveWorkerContextProvider` | Build startup context from `CaseLedgerEntry` lineage (not `EventLog`) — includes prior worker summaries, causal chain metadata |

**New model types** in `api/model/`:
- `CaseChannel` — backend-agnostic channel reference with extensible `properties` map
- `WorkerSummary` — prior worker's execution summary, includes `ledgerEntryId` (UUID of the `WORKER_EXECUTION_COMPLETED` ledger entry)
- `WorkerContext` — startup context for a newly provisioned worker, includes `priorWorkers` list and channel references
- `ProvisionContext` — input to `WorkerProvisioner.provision()`, contains the work request and case metadata

**Default implementations** in `engine/internal/worker/`:
- `NoOpWorkerProvisioner` — throws `ProvisioningException` to flag misconfiguration
- `NoOpWorkerStatusListener` — silently ignores all lifecycle events
- `NoOpCaseChannelProvider` — returns sentinel channels with `backendType = "none"`
- `EmptyWorkerContextProvider` — returns minimal context with empty `priorWorkers` list
- Four `@Alternative` reactive mirrors for optional reactive pipeline use (selected via `quarkus.arc.selected-alternatives`)

**Causal chain:** When a worker completes, `CaseLedgerEventCapture` writes a `WORKER_EXECUTION_COMPLETED` entry to the ledger. The allocated `WorkerSummary` includes this entry's UUID as `ledgerEntryId`. New workers set `causedByEntryId` on their own ledger entries to this value, completing the causal chain across workers on a case. This enables `WorkerContextProvider.buildContext()` to reconstruct the full prior-worker history and surface it in the startup context.

**SPI placement rule:** Operational SPIs (worker provisioning, lifecycle, channels) go in `api/spi/`; persistence SPIs (`CaseMetaModelRepository`, etc.) go in `engine-model/spi/`. This distinction clarifies intent: operational SPIs are about external system integration; persistence SPIs are about data durability.

=======
>>>>>>> main
## Configuration

Configuration uses the `casehub.` prefix. Key properties:

```properties
# Quartz (RAM store, no JDBC)
quarkus.quartz.store-type=ram

# Schema management (Hibernate drop-and-create, no migrations)
quarkus.hibernate-orm.schema-management.strategy=drop-and-create

# Event bus (Vert.x)
quarkus.vertx.event-loops=16

# Idempotency window (optional) — limits how far back the EventLog dedup check looks.
# Absent = permanent dedup (default, safest). Example: 7d
# casehub.idempotency.window=7d
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
<<<<<<< feat/casehub-ledger-integration
- ✅ Immutable audit ledger (`casehub-ledger`, Q2 2026)
=======
>>>>>>> main
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
<<<<<<< feat/casehub-ledger-integration
- **casehubio/engine#145** — quarkus-ledger integration epic
- **mdproctor/quarkus-ledger#39** — CaseLedgerEntry tracking issue
=======
>>>>>>> main
