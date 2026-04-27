# WorkerProvisioner Wiring — Design Specification
**Date:** 2026-04-27
**Status:** Approved
**Epic:** Worker Provisioner SPIs wired into engine (#152 extended)
**Supersedes:** ADR-0005 (SPI placement), ADR-0006 (normative registration) — this spec implements them

---

## 1. Problem Statement

`WorkerProvisioner.provision()` is never called. The `isNoOp()` branch in `WorkOrchestrator` fails immediately when no statically-declared worker has the required capability. The full worker lifecycle — provisioning on demand, self-registration by external agents, trust-tracked ledger recording, AGENT_DRIVEN callback completion, timeout recovery — is designed but unimplemented.

Three entry paths exist conceptually; only static (declared in `CaseDefinition`) works today.

---

## 2. Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Registry architecture | Approach A — single `WorkerRegistry` | One source of truth; normative ledger has one instrumentation point; evolution to Approach C (multi-source) is an internal refactor (Issue #187) |
| Execution mode discriminator | Sealed `WorkerExecution` hierarchy | No nullable `functionHolder`; compiler-enforced exhaustive switch; clean data model |
| Agent timeout | `AgentTimeoutStrategy` SPI, default = timeout + stall | Consistent with `ClaimSlaPolicy` SPI pattern (ADR-0004) |
| Self-registration surface | `WorkerRegistry` CDI bean + REST endpoint (MCP tool later) | Clean separation of mechanics from transport |
| Registry scope | `WorkerRegistrationScopeStrategy` SPI | PROVISIONED→CASE_SCOPED, SELF_REGISTERED→GLOBAL by default |
| Ledger recording | Implement now (ADR-0006 full) | Discovery lineage is a normative act; defer = incomplete ADR |

---

## 3. Model Changes (`api/`)

### 3.1 `WorkerExecution` sealed hierarchy

```java
// api/src/main/java/io/casehub/api/model/WorkerExecution.java
public sealed interface WorkerExecution
    permits EngineWorkerExecution, AgentWorkerExecution {}

// Engine executes via Quartz + WorkflowExecutor
public record EngineWorkerExecution(WorkerFunctionHolder<?> functionHolder)
    implements WorkerExecution {}

// External agent executes; engine waits for WorkerStatusListener callback
public record AgentWorkerExecution()
    implements WorkerExecution {}
```

**Example — creating each type:**
```java
// Engine-driven (existing paths, unchanged externally)
Worker.lambda("analyser", caps, ctx -> Map.of("result", analyse(ctx)))
  // → EngineWorkerExecution(functionHolder)

Worker.workflow("processor", caps, workflowDef)
  // → EngineWorkerExecution(functionHolder)

// Agent-driven (new)
Worker.agent("claude-session-42", caps)
  // → AgentWorkerExecution()
```

### 3.2 `Worker` model

- `functionHolder` field removed as direct field; moved into `EngineWorkerExecution`
- Existing static factory methods (`lambda`, `workflow`, `file`) unchanged externally; internally wrap into `EngineWorkerExecution`
- New factory: `Worker.agent(String name, List<Capability> capabilities)`
- `getExecution()` returns `WorkerExecution`

### 3.3 `ProvisionContext`

Gains `actorId` (the provisioner's identity for ledger recording):

```java
public record ProvisionContext(
    UUID caseId,
    String taskType,
    WorkerContext workerContext,      // nullable — built before provision()
    PropagationContext propagationContext,
    String actorId)                   // NEW — provisioner identity
{}
```

---

## 4. WorkerRegistry (`engine/internal/worker/`)

Single CDI bean. All three entry paths converge here.

```java
@ApplicationScoped
public class WorkerRegistry {

  /** Seed static workers from a CaseDefinition at case start. */
  void seedFromDefinition(CaseDefinition definition, UUID caseId);

  /** Register a provisioned or self-registered worker. */
  Worker register(Worker worker, RegistrationContext ctx);

  /** Remove a worker (on terminate() or explicit deregister). */
  void deregister(String workerId);

  /** Find eligible candidates for a capability + case scope. */
  List<WorkerCandidate> findCandidates(String capabilityName, UUID caseId);
}
```

### 4.1 `RegistrationContext`

```java
public record RegistrationContext(
    UUID caseId,              // null = global
    DiscoveryMode discoveryMode,  // STATIC | PROVISIONED | SELF_REGISTERED
    String actorId,           // who registered this worker
    String introducedByEntryId // ledger entry id of introducer (null = root-of-trust)
) {
  static RegistrationContext seeded(UUID caseId) { ... }        // STATIC, actorId="engine"
  static RegistrationContext provisioned(UUID caseId, String provisionerId) { ... }
  static RegistrationContext selfRegistered(String actorId) { ... }  // caseId=null
  static RegistrationContext introduced(String actorId, String introducerEntryId) { ... }
}
```

### 4.2 `WorkerRegistrationScopeStrategy` SPI (new, `api/spi/`)

```java
public interface WorkerRegistrationScopeStrategy {
  RegistrationScope resolve(Worker worker, RegistrationContext ctx);
}
public enum RegistrationScope { CASE_SCOPED, GLOBAL }
```

**Default implementation:**
- `PROVISIONED` → `CASE_SCOPED` (provisioned for a specific case)
- `SELF_REGISTERED` → `GLOBAL` (general availability)
- `STATIC` → `CASE_SCOPED` (seeded for a specific case)

**Example — scope resolution:**
```java
// Claudony provisions a worker for Case X:
registry.register(worker, RegistrationContext.provisioned(caseX, "claudony"))
→ scope = CASE_SCOPED → worker only visible to Case X's findCandidates()

// Claude session self-registers:
registry.register(worker, RegistrationContext.selfRegistered("claude-42"))
→ scope = GLOBAL → worker visible to any case's findCandidates()
```

### 4.3 Seeding at case start

`CaseStartedEventHandler` (already fires) calls:

```java
registry.seedFromDefinition(definition, caseId);
```

Static workers enter the registry scoped to that case. `WorkOrchestrator` no longer queries `CaseDefinition.getWorkers()` directly — it calls `registry.findCandidates(capability, caseId)`.

---

## 5. Orchestration Changes (`engine/internal/orchestration/`)

### 5.1 Provisioning path in `WorkOrchestrator.doSubmit()`

Where `decision.isNoOp()` currently throws, it now:

1. Calls `workerContextProvider.buildContext(generatedWorkerId, request)` → `WorkerContext`
2. Constructs `ProvisionContext(caseId, capability, workerContext, propagation, provisionerId)`
3. Calls `workerProvisioner.provision(capabilities, context)` → `Worker` (AgentWorkerExecution). The provisioner's contract requires the returned worker to declare all requested capabilities — `provision()` must not return a worker that cannot handle the assigned capability. If it cannot satisfy the request it must throw `ProvisioningException`.
4. Calls `registry.register(worker, RegistrationContext.provisioned(caseId, provisionerId))`
5. Re-runs `registry.findCandidates()` — finds the newly registered worker
6. Continues with normal assignment

**Example — provisioning flow:**
```
Case X, capability "code-review", no static workers
→ buildContext("claude-session-42", WorkRequest("code-review", inputData))
→ provision({"code-review"}, ProvisionContext(caseX, "code-review", ctx, prop, "claudony"))
  [Claudony starts tmux session, returns Worker.agent("claude-session-42", ["code-review"])]
→ registry.register(worker, RegistrationContext.provisioned(caseX, "claudony"))
→ findCandidates("code-review", caseX) → [claude-session-42]
→ WorkBroker selects → assignment proceeds
```

### 5.2 Execution fork after assignment

```java
switch (selectedWorker.getExecution()) {
  case EngineWorkerExecution e ->
    // Existing Quartz path — unchanged
    workflowExecutionManager.submit(eventLogId, instance, worker, capability, inputData);

  case AgentWorkerExecution a -> {
    // New path — no Quartz job; register future; publish assignment event
    CompletableFuture<WorkResult> future = pendingWorkRegistry.register(correlationKey);
    eventBus.publish(AGENT_WORKER_ASSIGNED,
        new AgentWorkerAssignedEvent(instance, worker, capability, correlationKey));
    agentTimeoutScheduler.scheduleTimeout(worker.getName(), caseId, correlationKey);
    // Future resolved when WorkerStatusListener.onWorkerCompleted() fires
  }
}
```

### 5.3 `WorkerStatusListener.onWorkerCompleted()` completing the future

`NoOpWorkerStatusListener.onWorkerCompleted()` currently does nothing. Now:

```java
// Wired in WorkflowExecutionCompletedHandler (engine-driven) — already done
// For agent-driven, WorkerStatusListenerBridge (new) handles the callback:

public void onWorkerCompleted(String workerId, WorkResult result) {
  String correlationKey = result.correlationKey();
  pendingWorkRegistry.complete(correlationKey, result);  // resolves the CompletableFuture
  agentTimeoutScheduler.cancel(workerId);
}
```

---

## 6. Self-Registration Surface

### 6.1 `WorkerRegistry.register()` — CDI entry point

Any in-process code calls `register()` directly with a `RegistrationContext`. Claudony's SPI implementations use this path.

### 6.2 REST endpoint

```
POST /workers/register
{
  "name": "claude-session-42",
  "capabilities": ["research", "java"],
  "caseId": "uuid-or-null",
  "actorId": "claudony"
}
→ 201 Created

DELETE /workers/{workerId}
→ 204 No Content
```

Delegates to `WorkerRegistry.register()` / `deregister()`.

**Example — external agent self-registers:**
```
Claude session starts independently (Claudony-managed but not provisioned by CaseHub)
→ POST /workers/register { name: "claude-42", capabilities: ["research"], caseId: null }
→ scope = GLOBAL
→ Case Y needs "research" → findCandidates() finds claude-42 → assigns
```

**Example — agent introduces another agent:**
```
claude-42 (trusted, already registered) introduces claude-99
→ POST /workers/register {
    name: "claude-99", capabilities: ["review"],
    actorId: "claude-42",
    introducedByEntryId: "<claude-42's WORKER_REGISTERED ledger entry id>"
  }
→ Trust chain: claude-99 inherits discovery lineage from claude-42
```

---

## 7. Agent Timeout (`api/spi/` + `engine/internal/worker/`)

### 7.1 `AgentTimeoutStrategy` SPI

```java
public interface AgentTimeoutStrategy {
  void onTimeout(String workerId, UUID caseId, String correlationKey,
                 WorkerStatusListener listener);
}
```

**Default implementation** — `StallOnTimeoutStrategy`:

```java
public void onTimeout(String workerId, UUID caseId, String correlationKey,
                      WorkerStatusListener listener) {
  listener.onWorkerStalled(workerId);
  // Existing retry/exhaustion path takes over
}
```

**Configuration:** `casehub.provisioner.agent-timeout-ms` (default: 1,800,000 = 30 minutes)

### 7.2 `AgentTimeoutScheduler` (engine-internal)

Quartz job per AGENT_DRIVEN assignment. Cancelled on `onWorkerCompleted()`. Fires `AgentTimeoutStrategy.onTimeout()` on expiry.

**Example — timeout flow:**
```
claude-session-42 assigned at T+0
→ Quartz timeout job scheduled for T+30min
→ No onWorkerCompleted() received by T+30min
→ AgentTimeoutStrategy.onTimeout("claude-session-42", caseX, key, listener)
→ Default: listener.onWorkerStalled("claude-session-42")
→ Existing WorkerRetriesExhaustedEventHandler → retry or FAULTED
```

---

## 8. Normative Ledger Recording (ADR-0006)

### 8.1 New `CaseHubEventType` values

```java
WORKER_REGISTERED,
WORKER_DEREGISTERED,
```

### 8.2 `WorkerRegistrationLedgerService` (engine-internal)

Called by `WorkerRegistry.register()` and `deregister()`. Writes `EventLog` entries:

| Field | Value |
|---|---|
| `eventType` | `WORKER_REGISTERED` |
| `workerId` | worker name |
| `caseId` | from scope (global workers use a `GLOBAL_SCOPE` sentinel UUID) |
| `metadata` | `discoveryMode`, `actorId`, `executionMode`, `capabilities[]`, `trustLevel` |
| `causedByEntryId` | registering actor's `WORKER_REGISTERED` entry id (null = root of trust) |

### 8.3 `WorkerTrustEvaluator` (engine-internal)

Computes initial trust level from discovery chain:

| Discovery mode | Default trust |
|---|---|
| `STATIC` | `HIGH` |
| `PROVISIONED` | `MEDIUM` (inherits from provisioner) |
| `SELF_REGISTERED` (no introducer) | `LOW` |
| `SELF_REGISTERED` (introduced by another) | derived from introducer's trust |

**Example — full causal chain:**

```
Static worker at case start:
  EventLog { type=WORKER_REGISTERED, workerId="analyser",
             discoveryMode=STATIC, trustLevel=HIGH, causedByEntryId=null }

Provisioned by ClaudonyWorkerProvisioner:
  EventLog { type=WORKER_REGISTERED, workerId="claude-42",
             discoveryMode=PROVISIONED, actorId="claudony",
             trustLevel=MEDIUM, causedByEntryId=null }

Self-registered, introduced by claude-42:
  EventLog { type=WORKER_REGISTERED, workerId="claude-99",
             discoveryMode=SELF_REGISTERED, actorId="claude-99",
             introducerActorId="claude-42",
             trustLevel=LOW,
             causedByEntryId=<claude-42's WORKER_REGISTERED entry id> }

Chain traversal: claude-99 → claude-42 → root (no further cause)
```

---

## 9. Testing Strategy

### 9.1 Unit tests (no CDI, no DB)

| Test class | What it verifies |
|---|---|
| `WorkerRegistryTest` | register/deregister/findCandidates for each entry path; scope resolution; case-scoped vs global pool; capability matching; duplicate registration idempotency |
| `WorkerExecutionTest` | Sealed hierarchy: EngineWorkerExecution carries functionHolder; AgentWorkerExecution has none; switch exhaustiveness (compiler + runtime) |
| `WorkerTrustEvaluatorTest` | STATIC=HIGH, PROVISIONED=MEDIUM, SELF_REGISTERED=LOW; chain depth inheritance; null causedByEntryId = root |
| `WorkerRegistrationScopeStrategyTest` | Default strategy: PROVISIONED→CASE_SCOPED, SELF_REGISTERED→GLOBAL, STATIC→CASE_SCOPED |
| `AgentTimeoutStrategyTest` | Default calls onWorkerStalled after configured ms; cancels on completion; no stall if completed in time |
| `WorkerRegistrationLedgerServiceTest` | Correct EventLog for each discoveryMode; causedByEntryId chain correct; metadata fields populated |

### 9.2 Integration tests (`@QuarkusTest`, H2, `casehub-testing`)

| Test class | What it verifies |
|---|---|
| `WorkerRegistryIntegrationTest` | Seed from CaseDefinition; findCandidates returns seeded workers; provision path end-to-end with recording stub; self-register via REST, candidate found; deregister removes from pool |
| `WorkerOrchestratorProvisioningTest` | doSubmit() with recording provisioner: provision() called when no candidates; AgentWorkerExecution → no Quartz job; PendingWorkRegistry future registered; onWorkerCompleted() resolves future |
| `AgentTimeoutIntegrationTest` | Agent assigned, no completion within short test-override timeout; onWorkerStalled fired; retry path triggered |
| `WorkerLedgerIntegrationTest` | EventLog entries written for STATIC (case start), PROVISIONED, SELF_REGISTERED; causedByEntryId chain traversable; WORKER_DEREGISTERED written on terminate() |
| `SpiWiringProvisionerTest` | Recording WorkerProvisioner wired via `@Alternative @Priority(1)`; provision() called with correct ProvisionContext; buildContext() called before provision() |

### 9.3 Happy path E2E tests

| Test class | What it verifies |
|---|---|
| `ProvisionedWorkerCaseTest` | Full case: no static workers → provision() → agent completes → case completes; EventLog sequence: WORKER_REGISTERED → WORKER_SCHEDULED → WORKER_EXECUTION_STARTED → WORKER_EXECUTION_COMPLETED → CASE_COMPLETED |
| `SelfRegisteredWorkerCaseTest` | Self-register before case starts → case assigned → completes |
| `MixedWorkerCaseTest` | Two bindings: first static ENGINE_DRIVEN, second provisioned AGENT_DRIVEN; both complete; correct execution path used for each |

### 9.4 Correctness tests

| Test class | What it verifies |
|---|---|
| `WorkerRegistryCorrectnessTest` | Concurrent registrations don't corrupt pool; case-scoped worker not returned for different case; global worker returned for any case; deregister mid-assignment doesn't cause NPE |
| `PendingWorkRegistryCompletionTest` | Future completes exactly once even if onWorkerCompleted called twice; future fails cleanly if stall fires before completion |
| `TrustChainCorrectnessTest` | Chain traversal handles max depth gracefully; chain of depth 5 resolves correctly |

### 9.5 Robustness tests

| Test class | What it verifies |
|---|---|
| `ProvisioningFailureTest` | provision() throws ProvisioningException; case transitions to FAULTED; in-progress workers unaffected; EventLog records failure |
| `AgentNeverCompletesTest` | Agent assigned, onWorkerCompleted never fires; timeout fires; stall → retry → exhaustion → FAULTED |
| `DoubleCompletionTest` | onWorkerCompleted called twice; second call is no-op; future not double-completed; no exception |
| `RegistrationWithNoCapabilitiesTest` | Register worker with empty capabilities; findCandidates never returns it; no NPE |
| `ProvisionContextNullWorkerContextTest` | buildContext() returns null; provision() called with null workerContext; no NPE downstream |

---

## 10. Documentation Updates

### 10.1 New

- `adr/0007-worker-registry-as-single-source-of-truth.md` — Approach A decision, evolution path to Approach C (Issue #187), sealed WorkerExecution rationale
- This spec

### 10.2 Updated alongside each PR

- `DESIGN.md` — Worker Provisioner section: replace "SPIs defined but not wired" with full architecture; add sequence diagrams for provisioned and self-registered flows
- `CLAUDE.md` — WorkerRegistry pattern for tests; AGENT_DRIVEN callback path explanation
- `adr/INDEX.md` — ADR-0007 entry
- Migration plan — mark Worker Provisioner wiring complete; update "What Remains"

### 10.3 Staleness sweep

- `DESIGN.md` — full pass: class names, package structure table, remove milestone handler references deleted upstream
- Ecosystem design doc — Worker Provisioner SPI section updated to reflect actual implementation shape
- All new public types — Javadoc with responsibility, contract, one-line usage example

---

## 11. Issue Structure

| Issue | Concern |
|---|---|
| New issue | `WorkerExecution` sealed hierarchy + `Worker` model refactor |
| New issue | `WorkerRegistry` + `WorkerRegistrationScopeStrategy` SPI + seeding |
| New issue | Provisioning path in `WorkOrchestrator` + `AgentTimeoutStrategy` SPI |
| New issue | AGENT_DRIVEN execution fork + `WorkerStatusListener` callback completion |
| New issue | Self-registration REST endpoint |
| New issue | Normative ledger recording (`WorkerRegistrationLedgerService`, `WorkerTrustEvaluator`) |
| New issue | ADR-0007 + full documentation sweep |
| #152 (existing) | Closed when all above issues are merged |

Each issue gets a focused PR from a fork branch, linked, with CI green before review.
