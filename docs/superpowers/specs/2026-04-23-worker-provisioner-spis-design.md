# Worker Provisioner SPIs Design

> *CaseHub defines the contracts. Claudony, Docker, Nono, and others implement them.*

---

## Overview

This spec defines four SPI interfaces in `casehub-engine/api` that allow external systems
to provision, observe, and contextualise workers for case execution. The primary implementor
is Claudony (tmux-backed Claude sessions), but the interfaces are backend-agnostic — Docker,
Nono, remote machines, and human-in-the-loop systems are all valid implementors.

All four follow the dual-stack pattern established across the ecosystem: a blocking interface
and a reactive (`Uni<>`-based) mirror. Both live in `api/src/main/java/io/casehub/api/spi/`.

---

## Design Decisions

**All four SPIs in `api/spi/`** — not `engine-model/spi/`. The provisioner SPIs are
operational concerns, not persistence concerns. They belong alongside `WorkerExecutionGuard`
and `ContextDiffStrategy`, not alongside the repositories.

**Dual-stack (blocking + reactive)** — consistent with the pattern across casehub, ledger,
qhorus, and workitems. Blocking interfaces for simple/synchronous consumers; reactive mirrors
for async/non-blocking pipelines.

**No dependency on Qhorus or Claudony** — `CaseChannel` is a rich record defined in `api`.
A Qhorus implementation sets `backendType = "qhorus"` and puts channel-specific data in
`properties`. The SPI is implementable by any channel backend.

**Future-world lineage** — `WorkerSummary.ledgerEntryId` points to the `CaseLedgerEntry`
for the prior worker's completion event. This enables new workers to set `causedByEntryId`
correctly on their own ledger entries, completing the causal chain. `EventLog` is not
referenced — `CaseLedgerEntry` (via `quarkus-ledger`) is the canonical lineage source.

---

## New Types in `api/model`

```java
// Opaque channel reference — backend-agnostic, extensible
public record CaseChannel(
    String id,
    String name,
    String purpose,
    String backendType,           // "qhorus", "slack", "in-memory", …
    Map<String, Object> properties
) {}

// Lightweight view over a CaseLedgerEntry for building worker context.
// ledgerEntryId enables the new worker to set causedByEntryId on its own
// ledger entries, completing the causal chain across workers.
public record WorkerSummary(
    String workerId,
    String workerName,
    Instant startedAt,
    Instant completedAt,
    String outputSummary,
    UUID ledgerEntryId    // ID of WORKER_EXECUTION_COMPLETED CaseLedgerEntry
) {}

// Context handed to a new worker at startup
public record WorkerContext(
    String taskDescription,
    UUID caseId,
    CaseChannel channel,
    List<WorkerSummary> priorWorkers,
    PropagationContext propagationContext,
    Map<String, Object> properties
) {}

// Input to WorkerProvisioner.provision()
public record ProvisionContext(
    UUID caseId,
    String taskType,
    WorkerContext workerContext,
    PropagationContext propagationContext
) {}
```

`ProvisioningException` — unchecked (`RuntimeException`), thrown by `provision()` when
a worker cannot be started.

Existing types reused: `Worker`, `WorkRequest`, `WorkResult`, `PropagationContext` (all
already in `api`). Zero new external dependencies.

---

## Blocking SPIs

### `WorkerProvisioner`

Called by `CaseEngine` when a PlanItem is eligible but no workers are available.

```java
public interface WorkerProvisioner {
    Worker provision(Set<String> capabilities, ProvisionContext context);
    void terminate(String workerId);
    Set<String> getCapabilities();
}
```

### `WorkerStatusListener`

Lifecycle callbacks from worker runtime to CaseEngine. Allows external provisioners
to notify CaseHub when a worker starts, completes, or stalls.

```java
public interface WorkerStatusListener {
    void onWorkerStarted(String workerId, Map<String, String> sessionMeta);
    void onWorkerCompleted(String workerId, WorkResult result);
    void onWorkerStalled(String workerId);
}
```

### `CaseChannelProvider`

Creates and manages communication channels for workers on a case. Backed by Qhorus
in the Claudony implementation; any channel backend is valid.

```java
public interface CaseChannelProvider {
    CaseChannel openChannel(UUID caseId, String purpose);
    void postToChannel(CaseChannel channel, String from, String content);
    void closeChannel(CaseChannel channel);
    List<CaseChannel> listChannels(UUID caseId);
}
```

### `WorkerContextProvider`

Builds startup context for a new worker. In the future-world architecture the
implementor queries `CaseLedgerEntryRepository` (not `EventLog`) for prior worker
history, constructing `WorkerSummary` entries with `ledgerEntryId` for causal linking.

```java
public interface WorkerContextProvider {
    WorkerContext buildContext(String workerId, WorkRequest task);
}
```

---

## Reactive SPIs

Mirror of the blocking SPIs returning `Uni<>`. All in `api/spi/`.

```java
public interface ReactiveWorkerProvisioner {
    Uni<Worker> provision(Set<String> capabilities, ProvisionContext context);
    Uni<Void> terminate(String workerId);
    Uni<Set<String>> getCapabilities();
}

public interface ReactiveCaseChannelProvider {
    Uni<CaseChannel> openChannel(UUID caseId, String purpose);
    Uni<Void> postToChannel(CaseChannel channel, String from, String content);
    Uni<Void> closeChannel(CaseChannel channel);
    Uni<List<CaseChannel>> listChannels(UUID caseId);
}

public interface ReactiveWorkerContextProvider {
    Uni<WorkerContext> buildContext(String workerId, WorkRequest task);
}

public interface ReactiveWorkerStatusListener {
    Uni<Void> onWorkerStarted(String workerId, Map<String, String> sessionMeta);
    Uni<Void> onWorkerCompleted(String workerId, WorkResult result);
    Uni<Void> onWorkerStalled(String workerId);
}
```

---

## Test Strategy

**Unit tests** — no-op / always-allow default implementations for each SPI. Verify
correct method signatures compile and CDI `@Alternative` selection works.

**Contract tests** — abstract base test class per SPI (following the pattern from
`quarkus-qhorus-testing`). Concrete test classes run against in-memory implementations.

**Integration tests** — `@QuarkusTest` with in-memory implementations activated via
`quarkus.arc.selected-alternatives`. Verify CaseEngine calls `WorkerProvisioner.provision()`
when a PlanItem is eligible and no workers are registered.

**Happy path** — provision a worker, receive `onWorkerStarted`, complete work, receive
`onWorkerCompleted`. Channel opens on provision, closes on completion.

**Correctness** — `WorkerContextProvider.buildContext()` returns `priorWorkers` in
execution order (ascending `seq` from CaseLedgerEntry). `ledgerEntryId` matches the
actual `CaseLedgerEntry` UUID for the prior WORKER_EXECUTION_COMPLETED event.

**Robustness** — `provision()` throws `ProvisioningException`; CaseEngine handles
gracefully (PlanItem stays eligible, retried on next control loop tick). `terminate()`
on unknown workerId is a no-op. `buildContext()` with no prior workers returns empty
`priorWorkers` list, not null.

---

## What Does NOT Change

- `EventLog` — not removed; still written by the engine for backward compatibility.
  `WorkerContextProvider` implementors use `CaseLedgerEntry` for lineage, not `EventLog`.
- `WorkerExecutionGuard`, `ContextDiffStrategy` — unchanged existing SPIs.
- Persistence SPIs (`CaseInstanceRepository`, `EventLogRepository`) — unchanged.
- `casehub-engine` module structure — no new modules needed; all new types/interfaces
  in the existing `api` module.

---

## Out of Scope

- Claudony's implementation (`ClaudonyWorkerProvisioner` etc.) — that lives in
  `claudony-casehub` module in the Claudony repo.
- CaseHub → Qhorus named datasource dependency — Claudony's `CaseChannelProvider`
  implementation connects to Qhorus; CaseHub has no Qhorus dependency.
- Causal chain wiring between `WorkItemLedgerEntry` and `CaseLedgerEntry` — a
  separate workstream; `WorkerSummary.ledgerEntryId` is the hook point but the
  cross-system linking is not implemented here.
- Default implementations beyond no-ops — real implementations are in Claudony.
