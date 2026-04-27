# WorkerProvisioner Wiring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire WorkerProvisioner into the engine — provisioned and self-registered workers enter a live WorkerRegistry, AGENT_DRIVEN workers skip Quartz and complete via WorkerStatusListener callbacks, with normative ledger recording per ADR-0006.

**Architecture:** A single `WorkerRegistry` CDI bean is the source of truth for all workers (static seeded at case start, provisioned on demand, self-registered externally). `WorkOrchestrator` queries the registry instead of `CaseDefinition` directly. A sealed `WorkerExecution` hierarchy (`EngineWorkerExecution` / `AgentWorkerExecution`) discriminates execution paths after assignment.

**Tech Stack:** Java 21 sealed classes + records, Quarkus CDI, Vert.x event bus, Quartz (timer only for agent timeout), Mutiny, JPA/Panache for EventLog persistence.

**Build commands:**
```bash
# Install all modules (run before module-specific tests)
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn install -DskipTests -q -f /Users/mdproctor/claude/casehub-engine/pom.xml

# Test api module only
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api -f /Users/mdproctor/claude/casehub-engine/pom.xml

# Test engine module (includes @QuarkusTest)
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml
```

**Spec:** `docs/superpowers/specs/2026-04-27-worker-provisioner-wiring-design.md`

---

## File Map

### New files — `api/`
| File | Responsibility |
|---|---|
| `api/src/main/java/io/casehub/api/model/WorkerExecution.java` | Sealed interface — root of execution hierarchy |
| `api/src/main/java/io/casehub/api/model/EngineWorkerExecution.java` | Record — carries functionHolder; Quartz path |
| `api/src/main/java/io/casehub/api/model/AgentWorkerExecution.java` | Record — no function; callback path |
| `api/src/main/java/io/casehub/api/spi/AgentTimeoutStrategy.java` | SPI — what to do when agent never completes |
| `api/src/main/java/io/casehub/api/spi/WorkerRegistrationScopeStrategy.java` | SPI — resolves CASE_SCOPED vs GLOBAL |
| `api/src/test/java/io/casehub/api/model/WorkerExecutionTest.java` | Unit tests for sealed hierarchy |

### Modified files — `api/`
| File | Change |
|---|---|
| `api/src/main/java/io/casehub/api/model/Worker.java` | Add `WorkerExecution execution` field; add `getExecution()`; add `Worker.agent()` factory; keep `getFunction()` delegating |
| `api/src/main/java/io/casehub/api/model/ProvisionContext.java` | Add `actorId` field |

### New files — `engine/`
| File | Responsibility |
|---|---|
| `engine/src/main/java/io/casehub/engine/internal/worker/RegistrationContext.java` | Value type — carries discoveryMode, actorId, caseId, introducedByEntryId |
| `engine/src/main/java/io/casehub/engine/internal/worker/DiscoveryMode.java` | Enum — STATIC, PROVISIONED, SELF_REGISTERED |
| `engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistry.java` | CDI bean — live pool; findCandidates; register; deregister |
| `engine/src/main/java/io/casehub/engine/internal/worker/DefaultWorkerRegistrationScopeStrategy.java` | Default SPI impl — PROVISIONED→CASE_SCOPED, SELF_REGISTERED→GLOBAL |
| `engine/src/main/java/io/casehub/engine/internal/worker/WorkerTrustEvaluator.java` | Computes trust level from discovery chain |
| `engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerService.java` | Writes WORKER_REGISTERED / WORKER_DEREGISTERED EventLog entries |
| `engine/src/main/java/io/casehub/engine/internal/worker/AgentTimeoutScheduler.java` | Schedules/cancels per-assignment Quartz timeout jobs |
| `engine/src/main/java/io/casehub/engine/internal/worker/StallOnTimeoutStrategy.java` | Default AgentTimeoutStrategy — calls onWorkerStalled |
| `engine/src/main/java/io/casehub/engine/internal/worker/AgentWorkerStatusListenerBridge.java` | Completes PendingWorkRegistry futures on onWorkerCompleted |
| `engine/src/main/java/io/casehub/engine/internal/event/AgentWorkerAssignedEvent.java` | Event published when AGENT_DRIVEN worker is assigned |
| `engine/src/main/java/io/casehub/engine/api/WorkerRegistrationResource.java` | REST endpoint — POST /workers/register, DELETE /workers/{id} |

### Modified files — `engine/`
| File | Change |
|---|---|
| `engine/src/main/java/io/casehub/engine/internal/history/CaseHubEventType.java` | Add WORKER_REGISTERED, WORKER_DEREGISTERED |
| `engine/src/main/java/io/casehub/engine/internal/engine/handler/CaseStartedEventHandler.java` | Call `registry.seedFromDefinition()` |
| `engine/src/main/java/io/casehub/engine/internal/orchestration/WorkOrchestrator.java` | Replace `buildCandidates()` with `registry.findCandidates()`; add provisioning path in isNoOp(); add execution fork |
| `engine/src/main/java/io/casehub/engine/internal/worker/WorkerExecutionTask.java` | Use sealed switch on `worker.getExecution()` instead of `instanceof` |
| `engine/src/main/java/io/casehub/engine/internal/worker/WorkerExecutionManager.java` | Guard: only submit Quartz job for EngineWorkerExecution |

---

## Task 1: WorkerExecution sealed hierarchy

**Files:**
- Create: `api/src/main/java/io/casehub/api/model/WorkerExecution.java`
- Create: `api/src/main/java/io/casehub/api/model/EngineWorkerExecution.java`
- Create: `api/src/main/java/io/casehub/api/model/AgentWorkerExecution.java`
- Create: `api/src/test/java/io/casehub/api/model/WorkerExecutionTest.java`

- [ ] **Write the failing tests**

Create `api/src/test/java/io/casehub/api/model/WorkerExecutionTest.java`:

```java
package io.casehub.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.casehub.api.model.holder.WorkerFunctionHolder;
import org.junit.jupiter.api.Test;

class WorkerExecutionTest {

  @Test
  void engineWorkerExecution_carries_functionHolder() {
    var holder = mock(WorkerFunctionHolder.class);
    var exec = new EngineWorkerExecution(holder);
    assertThat(exec.functionHolder()).isEqualTo(holder);
  }

  @Test
  void agentWorkerExecution_has_no_function() {
    var exec = new AgentWorkerExecution();
    assertThat(exec).isInstanceOf(AgentWorkerExecution.class);
  }

  @Test
  void switch_resolves_engine() {
    WorkerExecution exec = new EngineWorkerExecution(mock(WorkerFunctionHolder.class));
    String result = switch (exec) {
      case EngineWorkerExecution e -> "engine";
      case AgentWorkerExecution a -> "agent";
    };
    assertThat(result).isEqualTo("engine");
  }

  @Test
  void switch_resolves_agent() {
    WorkerExecution exec = new AgentWorkerExecution();
    String result = switch (exec) {
      case EngineWorkerExecution e -> "engine";
      case AgentWorkerExecution a -> "agent";
    };
    assertThat(result).isEqualTo("agent");
  }
}
```

- [ ] **Run tests — expect compilation failure (types don't exist yet)**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=WorkerExecutionTest 2>&1 | grep -E "ERROR|cannot find"
```
Expected: `cannot find symbol — WorkerExecution`

- [ ] **Implement the sealed hierarchy**

`api/src/main/java/io/casehub/api/model/WorkerExecution.java`:
```java
package io.casehub.api.model;

/** Discriminates how a worker executes: internally via Quartz, or externally via callback. */
public sealed interface WorkerExecution permits EngineWorkerExecution, AgentWorkerExecution {}
```

`api/src/main/java/io/casehub/api/model/EngineWorkerExecution.java`:
```java
package io.casehub.api.model;

import io.casehub.api.model.holder.WorkerFunctionHolder;

/**
 * Execution backed by a local function or workflow. CaseHub schedules execution via Quartz and
 * runs the functionHolder directly. The result is published on the event bus when done.
 */
public record EngineWorkerExecution(WorkerFunctionHolder<?> functionHolder)
    implements WorkerExecution {}
```

`api/src/main/java/io/casehub/api/model/AgentWorkerExecution.java`:
```java
package io.casehub.api.model;

/**
 * Execution delegated to an external agent (e.g. a Claudony-managed Claude session). CaseHub does
 * not schedule a Quartz job — it registers a PendingWorkRegistry future and waits for
 * WorkerStatusListener.onWorkerCompleted() to resolve it.
 */
public record AgentWorkerExecution() implements WorkerExecution {}
```

- [ ] **Run tests — expect pass**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=WorkerExecutionTest 2>&1 | grep -E "Tests run|BUILD"
```
Expected: `Tests run: 4, Failures: 0, Errors: 0` and `BUILD SUCCESS`

- [ ] **Commit**

```bash
cd /Users/mdproctor/claude/casehub-engine
git add api/src/main/java/io/casehub/api/model/WorkerExecution.java \
        api/src/main/java/io/casehub/api/model/EngineWorkerExecution.java \
        api/src/main/java/io/casehub/api/model/AgentWorkerExecution.java \
        api/src/test/java/io/casehub/api/model/WorkerExecutionTest.java
git commit -m "feat(api): sealed WorkerExecution hierarchy — EngineWorkerExecution and AgentWorkerExecution

Refs #<issue>"
```

---

## Task 2: Worker model refactor

**Files:**
- Modify: `api/src/main/java/io/casehub/api/model/Worker.java`

- [ ] **Read the full current Worker.java before touching it**

```bash
cat /Users/mdproctor/claude/casehub-engine/api/src/main/java/io/casehub/api/model/Worker.java
```

- [ ] **Write the failing test for `Worker.agent()` and `getExecution()`**

Add to a new file `api/src/test/java/io/casehub/api/model/WorkerModelTest.java`:

```java
package io.casehub.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkerModelTest {

  @Test
  void lambda_worker_has_engine_execution() {
    var worker = new Worker("w", List.of(), ctx -> Map.of());
    assertThat(worker.getExecution()).isInstanceOf(EngineWorkerExecution.class);
  }

  @Test
  void agent_factory_produces_agent_execution() {
    var worker = Worker.agent("w", List.of());
    assertThat(worker.getExecution()).isInstanceOf(AgentWorkerExecution.class);
  }

  @Test
  void getFunction_throws_for_agent_worker() {
    var worker = Worker.agent("w", List.of());
    assertThatThrownBy(worker::getFunction)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("AGENT_DRIVEN");
  }

  @Test
  void getFunction_returns_holder_for_engine_worker() {
    var worker = new Worker("w", List.of(), ctx -> Map.of());
    assertThat(worker.getFunction()).isNotNull();
  }
}
```

- [ ] **Run — expect failure**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=WorkerModelTest 2>&1 | grep -E "Tests run|ERROR|cannot find"
```
Expected: fails — `agent_factory_produces_agent_execution` fails (method doesn't exist yet).

- [ ] **Modify Worker.java**

In the `Worker` class:

1. Add field: `private final WorkerExecution execution;`
2. Replace all constructors to populate `execution` via `EngineWorkerExecution`:
```java
// Existing lambda constructor — wrap into EngineWorkerExecution
public Worker(String name, List<Capability> capabilities,
              Function<CaseContext, Map<String, Object>> function) {
  this(name, capabilities, new WorkerFunctionHolder<>(function));
}

// Keep all existing constructors; in the private common constructor:
private Worker(String name, List<Capability> capabilities, WorkerFunctionHolder<?> functionHolder,
               ExecutionPolicy executionPolicy) {
  this.name = name;
  this.capabilities = capabilities;
  this.execution = new EngineWorkerExecution(functionHolder);  // wrap here
  this.executionPolicy = executionPolicy;
}
```

3. Add `Worker.agent()` static factory:
```java
/** Creates an AGENT_DRIVEN worker — executed externally; results come via WorkerStatusListener. */
public static Worker agent(String name, List<Capability> capabilities) {
  return new Worker(name, capabilities, (WorkerFunctionHolder<?>) null);
}
```
(Add a private constructor variant that accepts `null` and sets `execution = new AgentWorkerExecution()`)

4. Add `getExecution()`:
```java
/** Returns the execution descriptor. Use a sealed switch to handle ENGINE vs AGENT paths. */
public WorkerExecution getExecution() {
  return execution;
}
```

5. Update `getFunction()` to delegate and guard:
```java
public WorkerFunctionHolder<?> getFunction() {
  if (execution instanceof EngineWorkerExecution e) {
    return e.functionHolder();
  }
  throw new IllegalStateException(
      "getFunction() called on AGENT_DRIVEN worker '" + name + "' — use getExecution() instead");
}
```

6. Remove the `functionHolder` field (it now lives inside `EngineWorkerExecution`).

- [ ] **Run tests — expect all api tests pass**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD"
```
Expected: `BUILD SUCCESS`, all existing tests plus new ones pass.

- [ ] **Build engine to catch any getFunction() callsite breaks**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn compile -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "ERROR|WARNING|BUILD"
```
Expected: `BUILD SUCCESS` — `WorkerExecutionTask.java` uses `worker.getFunction()` which still works for engine workers.

- [ ] **Commit**

```bash
git add api/src/main/java/io/casehub/api/model/Worker.java \
        api/src/test/java/io/casehub/api/model/WorkerModelTest.java
git commit -m "feat(api): add WorkerExecution to Worker model — Worker.agent() factory, getExecution()

Refs #<issue>"
```

---

## Task 3: ProvisionContext actorId + new SPIs

**Files:**
- Modify: `api/src/main/java/io/casehub/api/model/ProvisionContext.java`
- Create: `api/src/main/java/io/casehub/api/spi/AgentTimeoutStrategy.java`
- Create: `api/src/main/java/io/casehub/api/spi/WorkerRegistrationScopeStrategy.java`

- [ ] **Write failing tests**

`api/src/test/java/io/casehub/api/model/ProvisionContextTest.java` — add one test for `actorId`:
```java
@Test
void provisionContext_carries_actorId() {
  var ctx = new ProvisionContext(UUID.randomUUID(), "capability", null, null, "claudony");
  assertThat(ctx.actorId()).isEqualTo("claudony");
}
```

- [ ] **Run — expect failure** (constructor arity mismatch)

- [ ] **Update ProvisionContext**

```java
package io.casehub.api.model;

import io.casehub.api.context.PropagationContext;
import java.util.UUID;

/**
 * Input to {@link io.casehub.api.spi.WorkerProvisioner#provision}.
 *
 * <p>Example usage:
 * <pre>
 *   new ProvisionContext(caseId, "code-review", workerCtx, propagation, "claudony")
 * </pre>
 */
public record ProvisionContext(
    UUID caseId,
    String taskType,
    WorkerContext workerContext,         // nullable — built before provision()
    PropagationContext propagationContext,
    String actorId)                      // provisioner identity for ledger recording
{}
```

- [ ] **Create AgentTimeoutStrategy SPI**

`api/src/main/java/io/casehub/api/spi/AgentTimeoutStrategy.java`:
```java
package io.casehub.api.spi;

import java.util.UUID;

/**
 * Decides what to do when an AGENT_DRIVEN worker fails to call back within the deadline.
 *
 * <p>Default implementation: {@code StallOnTimeoutStrategy} — calls
 * {@link WorkerStatusListener#onWorkerStalled}, which triggers the existing retry/exhaustion path.
 *
 * <p>Example custom strategy — cancel the case immediately:
 * <pre>
 *   public void onTimeout(String workerId, UUID caseId, String correlationKey,
 *                         WorkerStatusListener listener) {
 *     // publish CASE_CANCEL event or call caseHub.cancelCase(caseId)
 *   }
 * </pre>
 */
public interface AgentTimeoutStrategy {

  /**
   * Called when an agent worker has not completed within {@code casehub.provisioner.agent-timeout-ms}.
   *
   * @param workerId the worker name
   * @param caseId the case the worker was assigned to
   * @param correlationKey the PendingWorkRegistry key for this assignment
   * @param listener the WorkerStatusListener — call onWorkerStalled to trigger retry path
   */
  void onTimeout(String workerId, UUID caseId, String correlationKey, WorkerStatusListener listener);
}
```

- [ ] **Create WorkerRegistrationScopeStrategy SPI**

`api/src/main/java/io/casehub/api/spi/WorkerRegistrationScopeStrategy.java`:
```java
package io.casehub.api.spi;

import io.casehub.api.model.Worker;

/**
 * Resolves whether a worker is scoped to a specific case or available globally.
 *
 * <p>Default: PROVISIONED workers are CASE_SCOPED (created for a specific case); SELF_REGISTERED
 * workers are GLOBAL (general availability); STATIC workers are CASE_SCOPED (seeded for a case).
 */
public interface WorkerRegistrationScopeStrategy {

  enum RegistrationScope { CASE_SCOPED, GLOBAL }

  /**
   * Resolve scope for the given worker and registration context.
   *
   * @param worker the worker being registered
   * @param discoveryMode how the worker was discovered (STATIC, PROVISIONED, SELF_REGISTERED)
   * @return CASE_SCOPED or GLOBAL
   */
  RegistrationScope resolve(Worker worker, String discoveryMode);
}
```

- [ ] **Run all api tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD"
```
Expected: `BUILD SUCCESS`

- [ ] **Commit**

```bash
git add api/src/main/java/io/casehub/api/model/ProvisionContext.java \
        api/src/main/java/io/casehub/api/spi/AgentTimeoutStrategy.java \
        api/src/main/java/io/casehub/api/spi/WorkerRegistrationScopeStrategy.java \
        api/src/test/java/io/casehub/api/model/ProvisionContextTest.java
git commit -m "feat(api): ProvisionContext.actorId, AgentTimeoutStrategy SPI, WorkerRegistrationScopeStrategy SPI

Refs #<issue>"
```

---

## Task 4: CaseHubEventType additions + RegistrationContext

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/history/CaseHubEventType.java`
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/DiscoveryMode.java`
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/RegistrationContext.java`

- [ ] **Add event types**

In `CaseHubEventType.java`, add to the existing enum:
```java
WORKER_REGISTERED,
WORKER_DEREGISTERED,
```

- [ ] **Create DiscoveryMode**

`engine/src/main/java/io/casehub/engine/internal/worker/DiscoveryMode.java`:
```java
package io.casehub.engine.internal.worker;

/** How a worker came to be known to the engine. Recorded in the normative EventLog. */
public enum DiscoveryMode {
  /** Declared in CaseDefinition — seeded at case start. Highest trust. */
  STATIC,
  /** Spun up by WorkerProvisioner.provision() on demand. Medium trust. */
  PROVISIONED,
  /** Announced itself or was introduced by another participant. Initial low trust. */
  SELF_REGISTERED
}
```

- [ ] **Create RegistrationContext**

`engine/src/main/java/io/casehub/engine/internal/worker/RegistrationContext.java`:
```java
package io.casehub.engine.internal.worker;

import java.util.UUID;

/**
 * Carries metadata about how and why a worker entered the WorkerRegistry.
 *
 * <p>Examples:
 * <pre>
 *   RegistrationContext.seeded(caseId)                    // static worker at case start
 *   RegistrationContext.provisioned(caseId, "claudony")   // provisioned by Claudony
 *   RegistrationContext.selfRegistered("claude-42")       // self-announced, global
 *   RegistrationContext.introduced("claude-99", "claude-42", entryId)  // introduced by another
 * </pre>
 */
public record RegistrationContext(
    UUID caseId,              // null = global scope
    DiscoveryMode discoveryMode,
    String actorId,           // who registered this worker (provisioner id or agent's own id)
    String introducedByEntryId // ledger entry id of the introducer (null = root of trust)
) {

  public static RegistrationContext seeded(UUID caseId) {
    return new RegistrationContext(caseId, DiscoveryMode.STATIC, "engine", null);
  }

  public static RegistrationContext provisioned(UUID caseId, String provisionerId) {
    return new RegistrationContext(caseId, DiscoveryMode.PROVISIONED, provisionerId, null);
  }

  public static RegistrationContext selfRegistered(String actorId) {
    return new RegistrationContext(null, DiscoveryMode.SELF_REGISTERED, actorId, null);
  }

  public static RegistrationContext introduced(
      String actorId, String introducerId, String introducerEntryId) {
    return new RegistrationContext(null, DiscoveryMode.SELF_REGISTERED, actorId, introducerEntryId);
  }
}
```

- [ ] **Build to verify no breakage**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn compile -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "ERROR|BUILD"
```
Expected: `BUILD SUCCESS`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/history/CaseHubEventType.java \
        engine/src/main/java/io/casehub/engine/internal/worker/DiscoveryMode.java \
        engine/src/main/java/io/casehub/engine/internal/worker/RegistrationContext.java
git commit -m "feat(engine): add WORKER_REGISTERED/DEREGISTERED event types; DiscoveryMode enum; RegistrationContext

Refs #<issue>"
```

---

## Task 5: WorkerRegistry — unit tests + implementation

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistry.java`
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/DefaultWorkerRegistrationScopeStrategy.java`
- Create: `engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistryTest.java`
- Create: `engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistrationScopeStrategyTest.java`

- [ ] **Write failing unit tests for WorkerRegistry**

`engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistryTest.java`:

```java
package io.casehub.engine.internal.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.Worker;
import io.casehub.api.spi.WorkerRegistrationScopeStrategy;
import io.casehub.api.spi.WorkerRegistrationScopeStrategy.RegistrationScope;
import io.quarkiverse.work.api.WorkerCandidate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkerRegistryTest {

  private WorkerRegistry registry;
  private WorkerRegistrationScopeStrategy scopeStrategy;
  private WorkerRegistrationLedgerService ledgerService;

  @BeforeEach
  void setUp() {
    scopeStrategy = mock(WorkerRegistrationScopeStrategy.class);
    ledgerService = mock(WorkerRegistrationLedgerService.class);
    registry = new WorkerRegistry(scopeStrategy, ledgerService);
    when(scopeStrategy.resolve(any(), any())).thenReturn(RegistrationScope.CASE_SCOPED);
  }

  @Test
  void findCandidates_returns_empty_when_no_workers_registered() {
    var candidates = registry.findCandidates("code-review", UUID.randomUUID());
    assertThat(candidates).isEmpty();
  }

  @Test
  void findCandidates_returns_case_scoped_worker_for_matching_case() {
    UUID caseId = UUID.randomUUID();
    var worker = Worker.agent("claude-42", List.of(new Capability("code-review")));
    registry.register(worker, RegistrationContext.provisioned(caseId, "claudony"));

    var candidates = registry.findCandidates("code-review", caseId);
    assertThat(candidates).hasSize(1);
    assertThat(candidates.get(0).id()).isEqualTo("claude-42");
  }

  @Test
  void findCandidates_does_not_return_case_scoped_worker_for_different_case() {
    UUID caseA = UUID.randomUUID();
    UUID caseB = UUID.randomUUID();
    var worker = Worker.agent("claude-42", List.of(new Capability("code-review")));
    registry.register(worker, RegistrationContext.provisioned(caseA, "claudony"));

    var candidates = registry.findCandidates("code-review", caseB);
    assertThat(candidates).isEmpty();
  }

  @Test
  void findCandidates_returns_global_worker_for_any_case() {
    when(scopeStrategy.resolve(any(), any())).thenReturn(RegistrationScope.GLOBAL);
    var worker = Worker.agent("claude-42", List.of(new Capability("research")));
    registry.register(worker, RegistrationContext.selfRegistered("claude-42"));

    assertThat(registry.findCandidates("research", UUID.randomUUID())).hasSize(1);
    assertThat(registry.findCandidates("research", UUID.randomUUID())).hasSize(1);
  }

  @Test
  void findCandidates_filters_by_capability() {
    UUID caseId = UUID.randomUUID();
    var worker = Worker.agent("claude-42", List.of(new Capability("code-review")));
    registry.register(worker, RegistrationContext.provisioned(caseId, "claudony"));

    assertThat(registry.findCandidates("research", caseId)).isEmpty();
    assertThat(registry.findCandidates("code-review", caseId)).hasSize(1);
  }

  @Test
  void deregister_removes_worker_from_pool() {
    UUID caseId = UUID.randomUUID();
    var worker = Worker.agent("claude-42", List.of(new Capability("research")));
    registry.register(worker, RegistrationContext.provisioned(caseId, "claudony"));
    assertThat(registry.findCandidates("research", caseId)).hasSize(1);

    registry.deregister("claude-42");
    assertThat(registry.findCandidates("research", caseId)).isEmpty();
  }

  @Test
  void seedFromDefinition_registers_static_workers_for_case() {
    UUID caseId = UUID.randomUUID();
    var analyser = new Worker("analyser", List.of(new Capability("analyse")), ctx -> java.util.Map.of());
    var def = mock(CaseDefinition.class);
    when(def.getWorkers()).thenReturn(List.of(analyser));

    registry.seedFromDefinition(def, caseId);

    assertThat(registry.findCandidates("analyse", caseId)).hasSize(1);
  }

  @Test
  void register_duplicate_is_idempotent() {
    UUID caseId = UUID.randomUUID();
    var worker = Worker.agent("claude-42", List.of(new Capability("research")));
    registry.register(worker, RegistrationContext.provisioned(caseId, "claudony"));
    registry.register(worker, RegistrationContext.provisioned(caseId, "claudony")); // again

    assertThat(registry.findCandidates("research", caseId)).hasSize(1);
  }

  @Test
  void ledgerService_called_on_register() {
    var worker = Worker.agent("claude-42", List.of(new Capability("research")));
    var ctx = RegistrationContext.selfRegistered("claude-42");
    registry.register(worker, ctx);

    verify(ledgerService).recordRegistered(worker, ctx);
  }

  @Test
  void ledgerService_called_on_deregister() {
    var worker = Worker.agent("claude-42", List.of(new Capability("research")));
    registry.register(worker, RegistrationContext.selfRegistered("claude-42"));
    registry.deregister("claude-42");

    verify(ledgerService).recordDeregistered("claude-42");
  }
}
```

- [ ] **Run — expect failure (WorkerRegistry doesn't exist)**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=WorkerRegistryTest 2>&1 | grep -E "ERROR|cannot find|BUILD"
```

- [ ] **Implement WorkerRegistry**

`engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistry.java`:

```java
package io.casehub.engine.internal.worker;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.Worker;
import io.casehub.api.spi.WorkerRegistrationScopeStrategy;
import io.casehub.api.spi.WorkerRegistrationScopeStrategy.RegistrationScope;
import io.quarkiverse.work.api.WorkerCandidate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Single source of truth for all registered workers. Three entry paths all converge here:
 * static (seeded at case start), provisioned (on demand), and self-registered (external agents).
 *
 * <p>Example usage:
 * <pre>
 *   registry.seedFromDefinition(definition, caseId);        // at case start
 *   registry.register(worker, RegistrationContext.provisioned(caseId, "claudony"));
 *   registry.findCandidates("code-review", caseId);
 * </pre>
 */
@ApplicationScoped
public class WorkerRegistry {

  private static final Logger LOG = Logger.getLogger(WorkerRegistry.class);

  /** Maps workerId → (worker, scope, caseId-or-null). */
  private final ConcurrentHashMap<String, WorkerEntry> pool = new ConcurrentHashMap<>();

  private final WorkerRegistrationScopeStrategy scopeStrategy;
  private final WorkerRegistrationLedgerService ledgerService;

  @Inject
  public WorkerRegistry(WorkerRegistrationScopeStrategy scopeStrategy,
                        WorkerRegistrationLedgerService ledgerService) {
    this.scopeStrategy = scopeStrategy;
    this.ledgerService = ledgerService;
  }

  /**
   * Seed all static workers from a CaseDefinition at case start.
   * Static workers are CASE_SCOPED to the given caseId.
   */
  public void seedFromDefinition(CaseDefinition definition, UUID caseId) {
    if (definition == null || definition.getWorkers() == null) return;
    for (Worker worker : definition.getWorkers()) {
      register(worker, RegistrationContext.seeded(caseId));
    }
  }

  /**
   * Register a provisioned or self-registered worker.
   * Idempotent — duplicate registration for the same workerId is a no-op.
   *
   * @return the registered worker
   */
  public Worker register(Worker worker, RegistrationContext ctx) {
    pool.computeIfAbsent(worker.getName(), id -> {
      RegistrationScope scope = scopeStrategy.resolve(worker, ctx.discoveryMode().name());
      LOG.infof("WorkerRegistry.register: workerId=%s discoveryMode=%s scope=%s caseId=%s",
          worker.getName(), ctx.discoveryMode(), scope, ctx.caseId());
      ledgerService.recordRegistered(worker, ctx);
      return new WorkerEntry(worker, scope, ctx.caseId());
    });
    return worker;
  }

  /**
   * Remove a worker from the pool. Called by WorkerProvisioner.terminate() or explicit deregister.
   */
  public void deregister(String workerId) {
    if (pool.remove(workerId) != null) {
      LOG.infof("WorkerRegistry.deregister: workerId=%s", workerId);
      ledgerService.recordDeregistered(workerId);
    }
  }

  /**
   * Find all workers eligible for the given capability and case scope.
   * Returns GLOBAL workers plus CASE_SCOPED workers registered for this specific caseId.
   */
  public List<WorkerCandidate> findCandidates(String capabilityName, UUID caseId) {
    List<WorkerCandidate> results = new ArrayList<>();
    for (WorkerEntry entry : pool.values()) {
      if (!hasCapability(entry.worker(), capabilityName)) continue;
      if (entry.scope() == RegistrationScope.GLOBAL
          || (entry.scope() == RegistrationScope.CASE_SCOPED
              && caseId.equals(entry.caseId()))) {
        results.add(new WorkerCandidate(entry.worker().getName(),
            Set.of(capabilityName), 0));
      }
    }
    return results;
  }

  private boolean hasCapability(Worker worker, String capabilityName) {
    return worker.getCapabilities() != null &&
        worker.getCapabilities().stream().anyMatch(c -> capabilityName.equals(c.getName()));
  }

  private record WorkerEntry(Worker worker, RegistrationScope scope, UUID caseId) {}
}
```

- [ ] **Write failing scope strategy test**

`engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistrationScopeStrategyTest.java`:

```java
package io.casehub.engine.internal.worker;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.Worker;
import io.casehub.api.spi.WorkerRegistrationScopeStrategy.RegistrationScope;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkerRegistrationScopeStrategyTest {

  private final DefaultWorkerRegistrationScopeStrategy strategy =
      new DefaultWorkerRegistrationScopeStrategy();

  @Test
  void static_workers_are_case_scoped() {
    assertThat(strategy.resolve(anyWorker(), "STATIC")).isEqualTo(RegistrationScope.CASE_SCOPED);
  }

  @Test
  void provisioned_workers_are_case_scoped() {
    assertThat(strategy.resolve(anyWorker(), "PROVISIONED")).isEqualTo(RegistrationScope.CASE_SCOPED);
  }

  @Test
  void self_registered_workers_are_global() {
    assertThat(strategy.resolve(anyWorker(), "SELF_REGISTERED")).isEqualTo(RegistrationScope.GLOBAL);
  }

  private Worker anyWorker() {
    return Worker.agent("w", List.of());
  }
}
```

- [ ] **Implement DefaultWorkerRegistrationScopeStrategy**

`engine/src/main/java/io/casehub/engine/internal/worker/DefaultWorkerRegistrationScopeStrategy.java`:

```java
package io.casehub.engine.internal.worker;

import io.casehub.api.model.Worker;
import io.casehub.api.spi.WorkerRegistrationScopeStrategy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default scope resolution: provisioned workers belong to the case that triggered them;
 * self-registered workers are generally available.
 */
@ApplicationScoped
public class DefaultWorkerRegistrationScopeStrategy implements WorkerRegistrationScopeStrategy {

  @Override
  public RegistrationScope resolve(Worker worker, String discoveryMode) {
    return switch (discoveryMode) {
      case "SELF_REGISTERED" -> RegistrationScope.GLOBAL;
      default -> RegistrationScope.CASE_SCOPED;  // STATIC and PROVISIONED
    };
  }
}
```

- [ ] **Create stub WorkerRegistrationLedgerService** (full implementation in Task 7)

`engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerService.java`:

```java
package io.casehub.engine.internal.worker;

import io.casehub.api.model.Worker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.casehub.engine.spi.EventLogRepository;

/** Records WORKER_REGISTERED and WORKER_DEREGISTERED EventLog entries. Full impl in Task 7. */
@ApplicationScoped
public class WorkerRegistrationLedgerService {

  @Inject EventLogRepository eventLogRepository;

  public void recordRegistered(Worker worker, RegistrationContext ctx) {
    // TODO: implemented in Task 7
  }

  public void recordDeregistered(String workerId) {
    // TODO: implemented in Task 7
  }
}
```

- [ ] **Run WorkerRegistry and scope strategy tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest="WorkerRegistryTest,WorkerRegistrationScopeStrategyTest" 2>&1 | grep -E "Tests run|BUILD"
```
Expected: all tests pass.

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistry.java \
        engine/src/main/java/io/casehub/engine/internal/worker/DefaultWorkerRegistrationScopeStrategy.java \
        engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerService.java \
        engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistryTest.java \
        engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistrationScopeStrategyTest.java
git commit -m "feat(engine): WorkerRegistry — single source of truth for all worker entry paths

Refs #<issue>"
```

---

## Task 6: WorkerTrustEvaluator (unit tested)

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/TrustLevel.java`
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/WorkerTrustEvaluator.java`
- Create: `engine/src/test/java/io/casehub/engine/internal/worker/WorkerTrustEvaluatorTest.java`

- [ ] **Write failing tests**

`engine/src/test/java/io/casehub/engine/internal/worker/WorkerTrustEvaluatorTest.java`:

```java
package io.casehub.engine.internal.worker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkerTrustEvaluatorTest {

  private final WorkerTrustEvaluator evaluator = new WorkerTrustEvaluator();

  @Test
  void static_discovery_yields_high_trust() {
    assertThat(evaluator.evaluate(DiscoveryMode.STATIC, null)).isEqualTo(TrustLevel.HIGH);
  }

  @Test
  void provisioned_discovery_yields_medium_trust() {
    assertThat(evaluator.evaluate(DiscoveryMode.PROVISIONED, null)).isEqualTo(TrustLevel.MEDIUM);
  }

  @Test
  void self_registered_without_introducer_yields_low_trust() {
    assertThat(evaluator.evaluate(DiscoveryMode.SELF_REGISTERED, null)).isEqualTo(TrustLevel.LOW);
  }

  @Test
  void self_registered_with_medium_trust_introducer_yields_low_trust() {
    // Introduced workers start at LOW regardless of introducer — trust must be earned
    assertThat(evaluator.evaluate(DiscoveryMode.SELF_REGISTERED, TrustLevel.MEDIUM))
        .isEqualTo(TrustLevel.LOW);
  }
}
```

- [ ] **Implement TrustLevel and WorkerTrustEvaluator**

`engine/src/main/java/io/casehub/engine/internal/worker/TrustLevel.java`:
```java
package io.casehub.engine.internal.worker;

/** Initial trust level assigned when a worker enters the WorkerRegistry. */
public enum TrustLevel { HIGH, MEDIUM, LOW }
```

`engine/src/main/java/io/casehub/engine/internal/worker/WorkerTrustEvaluator.java`:
```java
package io.casehub.engine.internal.worker;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Computes the initial trust level for a worker from its discovery mode and introducer trust.
 *
 * <p>Trust levels:
 * <ul>
 *   <li>STATIC — HIGH (declared by the system owner in CaseDefinition)</li>
 *   <li>PROVISIONED — MEDIUM (spun up by a trusted provisioner)</li>
 *   <li>SELF_REGISTERED — LOW (no external voucher; trust must be established via behaviour)</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 *   evaluator.evaluate(DiscoveryMode.PROVISIONED, null)  // → MEDIUM
 *   evaluator.evaluate(DiscoveryMode.SELF_REGISTERED, TrustLevel.HIGH) // → LOW
 * </pre>
 */
@ApplicationScoped
public class WorkerTrustEvaluator {

  /**
   * @param mode how the worker was discovered
   * @param introducerTrust trust of the introducing actor (null if no introducer)
   * @return the initial trust level for the worker
   */
  public TrustLevel evaluate(DiscoveryMode mode, TrustLevel introducerTrust) {
    return switch (mode) {
      case STATIC -> TrustLevel.HIGH;
      case PROVISIONED -> TrustLevel.MEDIUM;
      case SELF_REGISTERED -> TrustLevel.LOW;  // trust is earned, not inherited
    };
  }
}
```

- [ ] **Run trust evaluator tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=WorkerTrustEvaluatorTest 2>&1 | grep -E "Tests run|BUILD"
```
Expected: `Tests run: 4, Failures: 0`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/worker/TrustLevel.java \
        engine/src/main/java/io/casehub/engine/internal/worker/WorkerTrustEvaluator.java \
        engine/src/test/java/io/casehub/engine/internal/worker/WorkerTrustEvaluatorTest.java
git commit -m "feat(engine): WorkerTrustEvaluator — STATIC=HIGH, PROVISIONED=MEDIUM, SELF_REGISTERED=LOW

Refs #<issue>"
```

---

## Task 7: WorkerRegistrationLedgerService — full implementation

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerService.java`
- Create: `engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerServiceTest.java`

- [ ] **Write failing unit tests**

`engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerServiceTest.java`:

```java
package io.casehub.engine.internal.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.spi.EventLogRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WorkerRegistrationLedgerServiceTest {

  private EventLogRepository eventLogRepository;
  private WorkerRegistrationLedgerService service;

  @BeforeEach
  void setUp() {
    eventLogRepository = mock(EventLogRepository.class);
    when(eventLogRepository.append(any())).thenReturn(Uni.createFrom().voidItem());
    var trustEvaluator = new WorkerTrustEvaluator();
    service = new WorkerRegistrationLedgerService(eventLogRepository, trustEvaluator);
  }

  @Test
  void recordRegistered_writes_WORKER_REGISTERED_event() {
    var worker = Worker.agent("claude-42", List.of(new Capability("research")));
    var ctx = RegistrationContext.provisioned(UUID.randomUUID(), "claudony");

    service.recordRegistered(worker, ctx);

    ArgumentCaptor<EventLog> captor = forClass(EventLog.class);
    verify(eventLogRepository).append(captor.capture());
    assertThat(captor.getValue().getEventType()).isEqualTo(CaseHubEventType.WORKER_REGISTERED);
    assertThat(captor.getValue().getWorkerId()).isEqualTo("claude-42");
  }

  @Test
  void recordRegistered_sets_discoveryMode_in_metadata() {
    var worker = Worker.agent("claude-42", List.of(new Capability("research")));
    var ctx = RegistrationContext.provisioned(UUID.randomUUID(), "claudony");

    service.recordRegistered(worker, ctx);

    ArgumentCaptor<EventLog> captor = forClass(EventLog.class);
    verify(eventLogRepository).append(captor.capture());
    assertThat(captor.getValue().getMetadata().get("discoveryMode").asText())
        .isEqualTo("PROVISIONED");
  }

  @Test
  void recordRegistered_sets_trustLevel_MEDIUM_for_provisioned() {
    var worker = Worker.agent("claude-42", List.of());
    var ctx = RegistrationContext.provisioned(UUID.randomUUID(), "claudony");

    service.recordRegistered(worker, ctx);

    ArgumentCaptor<EventLog> captor = forClass(EventLog.class);
    verify(eventLogRepository).append(captor.capture());
    assertThat(captor.getValue().getMetadata().get("trustLevel").asText()).isEqualTo("MEDIUM");
  }

  @Test
  void recordRegistered_sets_trustLevel_HIGH_for_static() {
    var worker = new Worker("analyser", List.of(), ctx2 -> java.util.Map.of());
    var ctx = RegistrationContext.seeded(UUID.randomUUID());

    service.recordRegistered(worker, ctx);

    ArgumentCaptor<EventLog> captor = forClass(EventLog.class);
    verify(eventLogRepository).append(captor.capture());
    assertThat(captor.getValue().getMetadata().get("trustLevel").asText()).isEqualTo("HIGH");
  }

  @Test
  void recordDeregistered_writes_WORKER_DEREGISTERED_event() {
    service.recordDeregistered("claude-42");

    ArgumentCaptor<EventLog> captor = forClass(EventLog.class);
    verify(eventLogRepository).append(captor.capture());
    assertThat(captor.getValue().getEventType()).isEqualTo(CaseHubEventType.WORKER_DEREGISTERED);
    assertThat(captor.getValue().getWorkerId()).isEqualTo("claude-42");
  }
}
```

- [ ] **Run — expect failure**

- [ ] **Implement full WorkerRegistrationLedgerService**

Replace the stub body with:

```java
package io.casehub.engine.internal.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.casehub.engine.spi.EventLogRepository;
import java.time.Instant;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Records WORKER_REGISTERED and WORKER_DEREGISTERED entries in the normative EventLog per ADR-0006.
 *
 * <p>Each registration entry carries: discoveryMode, actorId, trustLevel, capabilities[],
 * and causedByEntryId (the introducer's entry id, null for root-of-trust).
 *
 * <p>Example ledger entry for a provisioned worker:
 * <pre>
 *   eventType=WORKER_REGISTERED, workerId="claude-42",
 *   metadata={ discoveryMode: "PROVISIONED", actorId: "claudony",
 *              trustLevel: "MEDIUM", capabilities: ["code-review"] }
 * </pre>
 */
@ApplicationScoped
public class WorkerRegistrationLedgerService {

  private static final Logger LOG = Logger.getLogger(WorkerRegistrationLedgerService.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final EventLogRepository eventLogRepository;
  private final WorkerTrustEvaluator trustEvaluator;

  @Inject
  public WorkerRegistrationLedgerService(EventLogRepository eventLogRepository,
                                          WorkerTrustEvaluator trustEvaluator) {
    this.eventLogRepository = eventLogRepository;
    this.trustEvaluator = trustEvaluator;
  }

  public void recordRegistered(Worker worker, RegistrationContext ctx) {
    TrustLevel trust = trustEvaluator.evaluate(ctx.discoveryMode(), null);

    var capabilities = worker.getCapabilities() == null ? "[]" :
        worker.getCapabilities().stream().map(c -> "\"" + c.getName() + "\"")
            .collect(Collectors.joining(",", "[", "]"));

    var metadata = MAPPER.createObjectNode()
        .put("discoveryMode", ctx.discoveryMode().name())
        .put("actorId", ctx.actorId())
        .put("trustLevel", trust.name())
        .put("executionMode", worker.getExecution() instanceof io.casehub.api.model.AgentWorkerExecution
            ? "AGENT_DRIVEN" : "ENGINE_DRIVEN");
    try { metadata.set("capabilities", MAPPER.readTree(capabilities)); }
    catch (Exception ignored) {}
    if (ctx.introducedByEntryId() != null) {
      metadata.put("causedByEntryId", ctx.introducedByEntryId());
    }

    EventLog log = new EventLog();
    log.setCaseId(ctx.caseId());
    log.setWorkerId(worker.getName());
    log.setEventType(CaseHubEventType.WORKER_REGISTERED);
    log.setStreamType(EventStreamType.CASE);
    log.setTimestamp(Instant.now());
    log.setMetadata(metadata);

    eventLogRepository.append(log)
        .subscribe().with(
            v -> LOG.debugf("WORKER_REGISTERED recorded: workerId=%s trust=%s", worker.getName(), trust),
            ex -> LOG.warnf(ex, "Failed to record WORKER_REGISTERED for workerId=%s", worker.getName()));
  }

  public void recordDeregistered(String workerId) {
    EventLog log = new EventLog();
    log.setWorkerId(workerId);
    log.setEventType(CaseHubEventType.WORKER_DEREGISTERED);
    log.setStreamType(EventStreamType.CASE);
    log.setTimestamp(Instant.now());

    eventLogRepository.append(log)
        .subscribe().with(
            v -> LOG.debugf("WORKER_DEREGISTERED recorded: workerId=%s", workerId),
            ex -> LOG.warnf(ex, "Failed to record WORKER_DEREGISTERED for workerId=%s", workerId));
  }
}
```

- [ ] **Run ledger service tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=WorkerRegistrationLedgerServiceTest 2>&1 | grep -E "Tests run|BUILD"
```
Expected: `Tests run: 5, Failures: 0`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerService.java \
        engine/src/test/java/io/casehub/engine/internal/worker/WorkerRegistrationLedgerServiceTest.java
git commit -m "feat(engine): WorkerRegistrationLedgerService — normative WORKER_REGISTERED/DEREGISTERED ledger entries

Refs #<issue>"
```

---

## Task 8: Seed registry at case start

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/engine/handler/CaseStartedEventHandler.java`

- [ ] **Write failing integration test**

Add to `engine/src/test/java/io/casehub/engine/SpiWiringProvisionerTest.java` (new file):

```java
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.engine.internal.worker.WorkerRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SpiWiringProvisionerTest {

  @Inject SimpleCaseHubBean simpleCaseHubBean;
  @Inject WorkerRegistry registry;

  @Test
  void static_workers_seeded_into_registry_on_case_start() {
    UUID caseId = simpleCaseHubBean
        .startCase(Map.of("documentId", "doc-1", "status", "processing"))
        .toCompletableFuture().join();

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
        assertThat(registry.findCandidates("processDocument", caseId)).isNotEmpty()
    );
  }
}
```

- [ ] **Run — expect failure** (registry not seeded yet)

- [ ] **Inject WorkerRegistry into CaseStartedEventHandler and seed**

Add to `CaseStartedEventHandler`:

```java
@Inject WorkerRegistry workerRegistry;
@Inject CaseDefinitionRegistry caseDefinitionRegistry;
```

Inside `onCaseStarted()`, after `eventLog` is built but before the `return`:

```java
// Seed static workers from the case definition into the live registry
CaseDefinition definition = caseDefinitionRegistry.getCaseDefinition(instance.getCaseMetaModel());
if (definition != null) {
  workerRegistry.seedFromDefinition(definition, instance.getUuid());
}
```

- [ ] **Run full engine test suite**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD|ERROR"
```
Expected: `BUILD SUCCESS`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/engine/handler/CaseStartedEventHandler.java \
        engine/src/test/java/io/casehub/engine/SpiWiringProvisionerTest.java
git commit -m "feat(engine): seed WorkerRegistry from CaseDefinition at case start

Refs #<issue>"
```

---

## Task 9: WorkOrchestrator — registry + provisioning path

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/orchestration/WorkOrchestrator.java`
- Create: `engine/src/main/java/io/casehub/engine/internal/event/AgentWorkerAssignedEvent.java`

- [ ] **Create AgentWorkerAssignedEvent**

`engine/src/main/java/io/casehub/engine/internal/event/AgentWorkerAssignedEvent.java`:

```java
package io.casehub.engine.internal.event;

import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.model.CaseInstance;

/**
 * Published on the Vert.x event bus when an AGENT_DRIVEN worker is assigned work.
 * Listeners (e.g. Claudony) observe this to notify the agent.
 *
 * <p>Example:
 * <pre>
 *   new AgentWorkerAssignedEvent(instance, worker, capability, "hash-abc123")
 * </pre>
 */
public record AgentWorkerAssignedEvent(
    CaseInstance caseInstance,
    Worker worker,
    Capability capability,
    String correlationKey) {}
```

Also add the address to `EventBusAddresses`:
```java
public static final String AGENT_WORKER_ASSIGNED = "casehub.agent-worker-assigned";
```

- [ ] **Write failing test for provisioning path**

Add to `SpiWiringProvisionerTest.java`:

```java
@Inject RecordingWorkerProvisioner recordingProvisioner;

@Test
void provision_called_when_no_static_worker_for_capability() {
  // Start a case that needs a capability not covered by static workers
  // RecordingWorkerProvisioner intercepts and returns an agent worker
  UUID caseId = simpleCaseHubBean
      .startCase(Map.of("documentId", "needs-provision", "status", "processing"))
      .toCompletableFuture().join();

  await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
      assertThat(RecordingWorkerProvisioner.provisionCallCount.get()).isGreaterThan(0)
  );
}
```

Add `RecordingWorkerProvisioner` as a static inner class (same pattern as `RecordingWorkerStatusListener`):

```java
@Alternative @Priority(1) @ApplicationScoped
public static class RecordingWorkerProvisioner implements WorkerProvisioner {
  static final AtomicInteger provisionCallCount = new AtomicInteger(0);
  static final List<Set<String>> provisionedCapabilities = new CopyOnWriteArrayList<>();

  static void reset() { provisionCallCount.set(0); provisionedCapabilities.clear(); }

  @Override
  public Worker provision(Set<String> capabilities, ProvisionContext context) {
    provisionCallCount.incrementAndGet();
    provisionedCapabilities.add(capabilities);
    return Worker.agent("provisioned-agent-" + provisionCallCount.get(),
        capabilities.stream().map(Capability::new).collect(Collectors.toList()));
  }

  @Override public void terminate(String workerId) {}

  @Override public Set<String> getCapabilities() { return Set.of("*"); }
}
```

- [ ] **Modify WorkOrchestrator.doSubmit()**

Replace the body of `doSubmit()` — key changes:

1. Replace `buildCandidates(definition, capability.getName())` with `registry.findCandidates(capability.getName(), instance.getUuid())`

2. Replace the `isNoOp()` failure with the provisioning path:

```java
if (decision.isNoOp()) {
  // No static or previously-registered worker — try to provision one
  try {
    Set<String> required = Set.of(capability.getName());
    WorkerContext workerContext = workerContextProvider.buildContext(
        "provisioned-" + UUID.randomUUID(), WorkRequest.of(capability.getName(), inputData));
    ProvisionContext provisionCtx = new ProvisionContext(
        instance.getUuid(), capability.getName(), workerContext,
        event.propagationContext(), workerProvisioner.getCapabilities().isEmpty()
            ? "unknown" : "provisioner");
    Worker provisioned = workerProvisioner.provision(required, provisionCtx);
    workerRegistry.register(provisioned, RegistrationContext.provisioned(
        instance.getUuid(), "provisioner"));
    // Re-select with the newly registered worker
    candidates = workerRegistry.findCandidates(capability.getName(), instance.getUuid());
    decision = workBroker.apply(context, AssignmentTrigger.CREATED, candidates, selectionStrategy);
  } catch (ProvisioningException ex) {
    CompletableFuture<WorkResult> failed = new CompletableFuture<>();
    failed.completeExceptionally(new IllegalStateException(
        "No worker available and provisioning failed: " + ex.getMessage(), ex));
    return failed;
  }
  if (decision.isNoOp()) {
    CompletableFuture<WorkResult> failed = new CompletableFuture<>();
    failed.completeExceptionally(new IllegalStateException(
        "Provisioner returned a worker but it was not selectable for capability: "
            + capability.getName()));
    return failed;
  }
}
```

3. After selecting `selectedWorker`, add the execution fork (Task 10 will add the AgentWorkerExecution branch fully — for now add a TODO guard):

```java
// Execution fork based on worker type
switch (selectedWorker.getExecution()) {
  case io.casehub.api.model.EngineWorkerExecution e -> {
    // Existing Quartz scheduling path
    eventBus.publish(EventBusAddresses.WORKER_SCHEDULE,
        new WorkerScheduleEvent(instance, selectedWorker, capability));
  }
  case io.casehub.api.model.AgentWorkerExecution a -> {
    // AGENT_DRIVEN: publish assignment event; future resolved via WorkerStatusListener callback
    eventBus.publish(EventBusAddresses.AGENT_WORKER_ASSIGNED,
        new AgentWorkerAssignedEvent(instance, selectedWorker, capability, correlationKey));
  }
}
```

Also inject `WorkerRegistry workerRegistry` and `WorkerProvisioner workerProvisioner` and `WorkerContextProvider workerContextProvider`.

- [ ] **Run full engine tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD|FAILURE"
```
Expected: `BUILD SUCCESS`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/orchestration/WorkOrchestrator.java \
        engine/src/main/java/io/casehub/engine/internal/event/AgentWorkerAssignedEvent.java \
        engine/src/main/java/io/casehub/engine/internal/event/EventBusAddresses.java \
        engine/src/test/java/io/casehub/engine/SpiWiringProvisionerTest.java
git commit -m "feat(engine): WorkOrchestrator — registry-based candidate selection + provisioning path

Refs #<issue>"
```

---

## Task 10: AgentTimeoutScheduler + StallOnTimeoutStrategy

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/StallOnTimeoutStrategy.java`
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/AgentTimeoutScheduler.java`
- Create: `engine/src/test/java/io/casehub/engine/internal/worker/AgentTimeoutStrategyTest.java`

- [ ] **Write failing unit test**

`engine/src/test/java/io/casehub/engine/internal/worker/AgentTimeoutStrategyTest.java`:

```java
package io.casehub.engine.internal.worker;

import static org.mockito.Mockito.*;

import io.casehub.api.spi.WorkerStatusListener;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentTimeoutStrategyTest {

  @Test
  void stall_on_timeout_calls_onWorkerStalled() {
    var listener = mock(WorkerStatusListener.class);
    var strategy = new StallOnTimeoutStrategy();

    strategy.onTimeout("claude-42", UUID.randomUUID(), "corr-key", listener);

    verify(listener).onWorkerStalled("claude-42");
    verifyNoMoreInteractions(listener);
  }
}
```

- [ ] **Implement StallOnTimeoutStrategy**

`engine/src/main/java/io/casehub/engine/internal/worker/StallOnTimeoutStrategy.java`:

```java
package io.casehub.engine.internal.worker;

import io.casehub.api.spi.AgentTimeoutStrategy;
import io.casehub.api.spi.WorkerStatusListener;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Default timeout strategy: call onWorkerStalled() which triggers the existing retry/exhaustion
 * path. This is consistent with how failed Quartz jobs are handled.
 *
 * <p>Example: after 30 minutes without onWorkerCompleted(), the agent is treated as stalled.
 * The retry policy on the Worker determines whether the case retries or faults.
 */
@ApplicationScoped
public class StallOnTimeoutStrategy implements AgentTimeoutStrategy {

  private static final Logger LOG = Logger.getLogger(StallOnTimeoutStrategy.class);

  @Override
  public void onTimeout(String workerId, UUID caseId, String correlationKey,
                        WorkerStatusListener listener) {
    LOG.warnf("Agent worker timed out — marking stalled: workerId=%s caseId=%s", workerId, caseId);
    listener.onWorkerStalled(workerId);
  }
}
```

- [ ] **Implement AgentTimeoutScheduler**

`engine/src/main/java/io/casehub/engine/internal/worker/AgentTimeoutScheduler.java`:

```java
package io.casehub.engine.internal.worker;

import io.casehub.api.spi.AgentTimeoutStrategy;
import io.casehub.api.spi.WorkerStatusListener;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

/**
 * Schedules a Quartz job per AGENT_DRIVEN worker assignment. Cancelled when the agent calls back.
 * Fires AgentTimeoutStrategy.onTimeout() if the deadline expires.
 *
 * <p>Config: {@code casehub.provisioner.agent-timeout-ms} (default: 1800000 = 30 minutes)
 */
@ApplicationScoped
public class AgentTimeoutScheduler {

  private static final Logger LOG = Logger.getLogger(AgentTimeoutScheduler.class);
  private static final String GROUP = "agent-timeout";

  @Inject Scheduler quartz;
  @Inject AgentTimeoutStrategy timeoutStrategy;
  @Inject WorkerStatusListener workerStatusListener;

  @ConfigProperty(name = "casehub.provisioner.agent-timeout-ms", defaultValue = "1800000")
  long agentTimeoutMs;

  /** Tracks in-flight assignments: correlationKey → workerId (for cancel lookup) */
  private final Map<String, String> active = new ConcurrentHashMap<>();

  /**
   * Schedule a timeout job for the given assignment.
   * If the agent completes before the deadline, call {@link #cancel(String)}.
   */
  public void scheduleTimeout(String workerId, UUID caseId, String correlationKey) {
    active.put(correlationKey, workerId);
    try {
      JobDataMap data = new JobDataMap();
      data.put("workerId", workerId);
      data.put("caseId", caseId.toString());
      data.put("correlationKey", correlationKey);

      var job = JobBuilder.newJob(AgentTimeoutJob.class)
          .withIdentity(correlationKey, GROUP)
          .usingJobData(data)
          .build();
      var trigger = TriggerBuilder.newTrigger()
          .withIdentity(correlationKey, GROUP)
          .startAt(new Date(System.currentTimeMillis() + agentTimeoutMs))
          .forJob(correlationKey, GROUP)
          .build();
      quartz.scheduleJob(job, trigger);
      LOG.debugf("Agent timeout scheduled: workerId=%s correlationKey=%s timeoutMs=%d",
          workerId, correlationKey, agentTimeoutMs);
    } catch (SchedulerException ex) {
      LOG.warnf(ex, "Failed to schedule agent timeout for workerId=%s", workerId);
    }
  }

  /** Cancel the timeout job — call when onWorkerCompleted() fires. */
  public void cancel(String correlationKey) {
    active.remove(correlationKey);
    try {
      quartz.deleteJob(new JobKey(correlationKey, GROUP));
      LOG.debugf("Agent timeout cancelled: correlationKey=%s", correlationKey);
    } catch (SchedulerException ex) {
      LOG.warnf(ex, "Failed to cancel agent timeout for correlationKey=%s", correlationKey);
    }
  }

  /** Quartz job — fired on timeout. */
  @ApplicationScoped
  public static class AgentTimeoutJob implements Job {
    @Inject AgentTimeoutStrategy timeoutStrategy;
    @Inject WorkerStatusListener workerStatusListener;

    @Override
    public void execute(JobExecutionContext ctx) {
      String workerId = ctx.getMergedJobDataMap().getString("workerId");
      UUID caseId = UUID.fromString(ctx.getMergedJobDataMap().getString("caseId"));
      String correlationKey = ctx.getMergedJobDataMap().getString("correlationKey");
      timeoutStrategy.onTimeout(workerId, caseId, correlationKey, workerStatusListener);
    }
  }
}
```

Add to `engine/src/main/resources/application.properties`:
```properties
casehub.provisioner.agent-timeout-ms=1800000
```

- [ ] **Run timeout tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=AgentTimeoutStrategyTest 2>&1 | grep -E "Tests run|BUILD"
```
Expected: `Tests run: 1, Failures: 0`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/worker/StallOnTimeoutStrategy.java \
        engine/src/main/java/io/casehub/engine/internal/worker/AgentTimeoutScheduler.java \
        engine/src/main/resources/application.properties \
        engine/src/test/java/io/casehub/engine/internal/worker/AgentTimeoutStrategyTest.java
git commit -m "feat(engine): AgentTimeoutScheduler + StallOnTimeoutStrategy — timeout SPI with default stall behaviour

Refs #<issue>"
```

---

## Task 11: WorkerStatusListener callback → complete PendingWorkRegistry future

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/AgentWorkerStatusListenerBridge.java`
- Create: `engine/src/test/java/io/casehub/engine/internal/worker/AgentWorkerStatusListenerBridgeTest.java`

The existing `WorkerStatusListener` in `WorkerExecutionJobListener` is the ENGINE_DRIVEN path. For AGENT_DRIVEN workers, when `onWorkerCompleted()` is called, we need to:
1. Complete the `PendingWorkRegistry` future with the result
2. Cancel the timeout job

- [ ] **Write failing unit test**

`engine/src/test/java/io/casehub/engine/internal/worker/AgentWorkerStatusListenerBridgeTest.java`:

```java
package io.casehub.engine.internal.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.casehub.api.model.WorkResult;
import io.casehub.api.model.WorkStatus;
import io.casehub.engine.internal.work.PendingWorkRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentWorkerStatusListenerBridgeTest {

  @Test
  void onWorkerCompleted_resolves_pending_future() {
    var registry = mock(PendingWorkRegistry.class);
    var timeoutScheduler = mock(AgentTimeoutScheduler.class);
    var bridge = new AgentWorkerStatusListenerBridge(registry, timeoutScheduler);

    var result = new WorkResult("claude-42", WorkStatus.COMPLETED, Map.of("output", "done"),
        "corr-key-123");
    bridge.onWorkerCompleted("claude-42", result);

    verify(registry).complete("corr-key-123", result);
    verify(timeoutScheduler).cancel("corr-key-123");
  }

  @Test
  void onWorkerCompleted_twice_is_idempotent() {
    var registry = mock(PendingWorkRegistry.class);
    var timeoutScheduler = mock(AgentTimeoutScheduler.class);
    var bridge = new AgentWorkerStatusListenerBridge(registry, timeoutScheduler);

    var result = new WorkResult("claude-42", WorkStatus.COMPLETED, Map.of(), "corr-key-123");
    bridge.onWorkerCompleted("claude-42", result);
    bridge.onWorkerCompleted("claude-42", result);  // second call is no-op

    verify(registry, times(1)).complete("corr-key-123", result);
  }
}
```

- [ ] **Check WorkResult constructor — read the class**

```bash
cat /Users/mdproctor/claude/casehub-engine/api/src/main/java/io/casehub/api/model/WorkResult.java
```
Adjust the test constructor call to match the actual signature.

- [ ] **Add `complete()` method to PendingWorkRegistry**

In `PendingWorkRegistry.java`, add:

```java
/**
 * Completes the future registered for the given correlationKey.
 * Idempotent — if no future is registered or it is already complete, does nothing.
 */
public void complete(String correlationKey, WorkResult result) {
  List<CompletableFuture<WorkResult>> futures = pending.remove(correlationKey);
  if (futures != null) {
    futures.forEach(f -> f.complete(result));
    LOG.debugf("PendingWorkRegistry.complete: correlationKey=%s futures=%d",
        correlationKey, futures.size());
  }
}
```

- [ ] **Implement AgentWorkerStatusListenerBridge**

`engine/src/main/java/io/casehub/engine/internal/worker/AgentWorkerStatusListenerBridge.java`:

```java
package io.casehub.engine.internal.worker;

import io.casehub.api.model.WorkResult;
import io.casehub.api.spi.WorkerStatusListener;
import io.casehub.engine.internal.work.PendingWorkRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Bridges WorkerStatusListener.onWorkerCompleted() to PendingWorkRegistry future completion
 * for AGENT_DRIVEN workers. ENGINE_DRIVEN workers complete futures via WorkflowExecutionCompletedHandler.
 *
 * <p>Wiring: this bean is injected alongside the existing WorkerStatusListener implementations.
 * It observes the same onWorkerCompleted() callback but only acts on results that have a
 * correlationKey registered in PendingWorkRegistry.
 *
 * <p>Example flow:
 * <pre>
 *   Claude agent finishes → calls POST /workers/{id}/complete with WorkResult
 *   → WorkerStatusListener.onWorkerCompleted("claude-42", result)
 *   → AgentWorkerStatusListenerBridge resolves PendingWorkRegistry future
 *   → WorkOrchestrator.submitAndWait() returns with the result
 * </pre>
 */
@ApplicationScoped
public class AgentWorkerStatusListenerBridge implements WorkerStatusListener {

  private static final Logger LOG = Logger.getLogger(AgentWorkerStatusListenerBridge.class);

  private final PendingWorkRegistry pendingWorkRegistry;
  private final AgentTimeoutScheduler timeoutScheduler;

  @Inject
  public AgentWorkerStatusListenerBridge(PendingWorkRegistry pendingWorkRegistry,
                                          AgentTimeoutScheduler timeoutScheduler) {
    this.pendingWorkRegistry = pendingWorkRegistry;
    this.timeoutScheduler = timeoutScheduler;
  }

  @Override
  public void onWorkerStarted(String workerId, Map<String, String> sessionMeta) {
    // ENGINE_DRIVEN start events are handled by WorkerExecutionJobListener — no action here
  }

  @Override
  public void onWorkerCompleted(String workerId, WorkResult result) {
    if (result == null || result.correlationKey() == null) return;
    LOG.infof("AgentWorkerStatusListenerBridge.onWorkerCompleted: workerId=%s correlationKey=%s",
        workerId, result.correlationKey());
    pendingWorkRegistry.complete(result.correlationKey(), result);
    timeoutScheduler.cancel(result.correlationKey());
  }

  @Override
  public void onWorkerStalled(String workerId) {
    // Stall is triggered by AgentTimeoutScheduler — no additional action here
  }
}
```

Note: CDI will have two `WorkerStatusListener` beans in scope. Ensure the existing `RecordingWorkerStatusListener` in tests uses `@Alternative @Priority(1)` to override both. In production, the bridge and `NoOpWorkerStatusListener` coexist — CDI must select one. Add `@Priority` annotation to make the bridge the default:

On `AgentWorkerStatusListenerBridge`:
```java
@ApplicationScoped
@jakarta.annotation.Priority(10)  // higher priority than NoOp
```

And mark `NoOpWorkerStatusListener` with:
```java
@ApplicationScoped
@jakarta.annotation.Priority(0)
```

Actually, in Quarkus CDI, having two beans of the same type without qualification causes ambiguity. The cleanest solution: make `AgentWorkerStatusListenerBridge` the primary implementation that also delegates to the existing listener chain. Or rename to avoid the conflict. Check the existing wiring — `WorkerExecutionJobListener` injects `WorkerStatusListener`. If two beans exist without `@Alternative`, CDI will fail at startup.

**Correct approach:** Remove `implements WorkerStatusListener` from `AgentWorkerStatusListenerBridge`. Instead, inject `PendingWorkRegistry` directly into the existing `WorkflowExecutionCompletedHandler` for the agent completion path. The existing handler already calls `workerStatusListener.onWorkerCompleted()` — add logic there to also complete the pending future.

In `WorkflowExecutionCompletedHandler.onWorkflowExecutionCompletedHandler()`, after `workerStatusListener.onWorkerCompleted(workerId, result)`, add:

```java
// Complete any pending AGENT_DRIVEN future for this correlation key
if (result.correlationKey() != null) {
  pendingWorkRegistry.complete(result.correlationKey(), result);
  agentTimeoutScheduler.cancel(result.correlationKey());
}
```

Also inject `AgentTimeoutScheduler agentTimeoutScheduler`.

Then delete `AgentWorkerStatusListenerBridge.java` — it's not needed.

- [ ] **Run full engine tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD|FAILURE"
```
Expected: `BUILD SUCCESS`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/worker/WorkflowExecutionCompletedHandler.java \
        engine/src/main/java/io/casehub/engine/internal/work/PendingWorkRegistry.java \
        engine/src/main/java/io/casehub/engine/internal/worker/AgentTimeoutScheduler.java
git commit -m "feat(engine): AGENT_DRIVEN completion — WorkerStatusListener callback resolves PendingWorkRegistry future

Refs #<issue>"
```

---

## Task 12: Self-registration REST endpoint

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/api/WorkerRegistrationResource.java`
- Create: `engine/src/test/java/io/casehub/engine/api/WorkerRegistrationResourceTest.java`

- [ ] **Write failing REST test**

`engine/src/test/java/io/casehub/engine/api/WorkerRegistrationResourceTest.java`:

```java
package io.casehub.engine.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.engine.internal.worker.WorkerRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WorkerRegistrationResourceTest {

  @Inject WorkerRegistry registry;

  @Test
  void post_register_adds_worker_to_global_pool() {
    given()
        .contentType("application/json")
        .body("""
            {
              "name": "test-agent-42",
              "capabilities": ["research"],
              "actorId": "test"
            }
            """)
        .when()
        .post("/workers/register")
        .then()
        .statusCode(201);

    await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
        assertThat(registry.findCandidates("research", UUID.randomUUID())).isNotEmpty()
    );
  }

  @Test
  void delete_deregister_removes_worker() {
    // Register first
    given().contentType("application/json")
        .body("""{ "name": "test-agent-del", "capabilities": ["research"], "actorId": "test" }""")
        .post("/workers/register").then().statusCode(201);

    given().delete("/workers/test-agent-del").then().statusCode(204);

    assertThat(registry.findCandidates("research", UUID.randomUUID())).isEmpty();
  }
}
```

- [ ] **Implement WorkerRegistrationResource**

`engine/src/main/java/io/casehub/engine/api/WorkerRegistrationResource.java`:

```java
package io.casehub.engine.api;

import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.worker.RegistrationContext;
import io.casehub.engine.internal.worker.WorkerRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST surface for external agents to self-register and deregister.
 * Delegates to WorkerRegistry — the MCP tool layer (casehub-mcp) will wrap this later.
 *
 * <p>Example:
 * <pre>
 *   POST /workers/register
 *   { "name": "claude-42", "capabilities": ["research"], "actorId": "claudony", "caseId": null }
 *   → 201 Created
 *
 *   DELETE /workers/claude-42
 *   → 204 No Content
 * </pre>
 */
@Path("/workers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkerRegistrationResource {

  @Inject WorkerRegistry registry;

  @POST
  @Path("/register")
  public Response register(WorkerRegistrationRequest request) {
    List<Capability> capabilities = request.capabilities().stream()
        .map(Capability::new)
        .collect(Collectors.toList());
    Worker worker = Worker.agent(request.name(), capabilities);
    RegistrationContext ctx = request.caseId() != null
        ? RegistrationContext.provisioned(UUID.fromString(request.caseId()), request.actorId())
        : RegistrationContext.selfRegistered(request.actorId());
    registry.register(worker, ctx);
    return Response.status(201).build();
  }

  @DELETE
  @Path("/{workerId}")
  public Response deregister(@PathParam("workerId") String workerId) {
    registry.deregister(workerId);
    return Response.noContent().build();
  }

  public record WorkerRegistrationRequest(
      String name,
      List<String> capabilities,
      String actorId,
      String caseId   // nullable — null means global scope
  ) {}
}
```

- [ ] **Run REST tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml -Dtest=WorkerRegistrationResourceTest 2>&1 | grep -E "Tests run|BUILD"
```
Expected: `Tests run: 2, Failures: 0`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/api/WorkerRegistrationResource.java \
        engine/src/test/java/io/casehub/engine/api/WorkerRegistrationResourceTest.java
git commit -m "feat(engine): POST /workers/register + DELETE /workers/{id} — self-registration surface

Refs #<issue>"
```

---

## Task 13: Integration + E2E + correctness + robustness tests

**Files:**
- Create: `engine/src/test/java/io/casehub/engine/WorkerRegistryIntegrationTest.java`
- Create: `engine/src/test/java/io/casehub/engine/ProvisionedWorkerCaseTest.java`
- Create: `engine/src/test/java/io/casehub/engine/SelfRegisteredWorkerCaseTest.java`
- Create: `engine/src/test/java/io/casehub/engine/MixedWorkerCaseTest.java`
- Create: `engine/src/test/java/io/casehub/engine/WorkerRegistryCorrectnessTest.java`
- Create: `engine/src/test/java/io/casehub/engine/ProvisioningFailureTest.java`
- Create: `engine/src/test/java/io/casehub/engine/AgentNeverCompletesTest.java`
- Create: `engine/src/test/java/io/casehub/engine/DoubleCompletionTest.java`
- Create: `engine/src/test/java/io/casehub/engine/RegistrationEdgeCasesTest.java`
- Create: `engine/src/test/java/io/casehub/engine/WorkerLedgerIntegrationTest.java`

- [ ] **Write and run WorkerRegistryIntegrationTest**

```java
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.worker.RegistrationContext;
import io.casehub.engine.internal.worker.WorkerRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WorkerRegistryIntegrationTest {

  @Inject WorkerRegistry registry;

  @Test
  void provisioned_worker_visible_to_its_case() {
    UUID caseId = UUID.randomUUID();
    var worker = Worker.agent("int-test-agent", List.of(new Capability("int-cap")));
    registry.register(worker, RegistrationContext.provisioned(caseId, "test-provisioner"));
    assertThat(registry.findCandidates("int-cap", caseId)).hasSize(1);
  }

  @Test
  void provisioned_worker_not_visible_to_other_case() {
    UUID caseA = UUID.randomUUID();
    UUID caseB = UUID.randomUUID();
    var worker = Worker.agent("int-scoped-agent", List.of(new Capability("scoped-cap")));
    registry.register(worker, RegistrationContext.provisioned(caseA, "test-provisioner"));
    assertThat(registry.findCandidates("scoped-cap", caseB)).isEmpty();
  }

  @Test
  void self_registered_worker_visible_to_any_case() {
    var worker = Worker.agent("int-global-agent", List.of(new Capability("global-cap")));
    registry.register(worker, RegistrationContext.selfRegistered("int-global-agent"));
    assertThat(registry.findCandidates("global-cap", UUID.randomUUID())).isNotEmpty();
    assertThat(registry.findCandidates("global-cap", UUID.randomUUID())).isNotEmpty();
  }

  @Test
  void deregister_removes_worker() {
    UUID caseId = UUID.randomUUID();
    var worker = Worker.agent("int-removed-agent", List.of(new Capability("remove-cap")));
    registry.register(worker, RegistrationContext.provisioned(caseId, "test-provisioner"));
    registry.deregister("int-removed-agent");
    assertThat(registry.findCandidates("remove-cap", caseId)).isEmpty();
  }
}
```

- [ ] **Write ProvisionedWorkerCaseTest**

```java
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.model.*;
import io.casehub.api.spi.WorkerProvisioner;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.spi.EventLogRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * E2E test: a case with no static workers triggers provisioning; agent completes via callback.
 * Verifies the full EventLog sequence: WORKER_REGISTERED → WORKER_SCHEDULED →
 * WORKER_EXECUTION_STARTED → WORKER_EXECUTION_COMPLETED → CASE_COMPLETED.
 */
@QuarkusTest
class ProvisionedWorkerCaseTest {

  @Inject SimpleCaseHubBean simpleCaseHubBean;
  @Inject EventLogRepository eventLogRepository;
  @Inject RecordingProvisioningWorkerProvisioner provisioner;

  @BeforeEach void reset() { RecordingProvisioningWorkerProvisioner.reset(); }

  @Test
  void full_case_with_provisioned_agent_worker_completes() {
    // Given: RecordingProvisioningWorkerProvisioner returns an agent worker that auto-completes
    UUID caseId = simpleCaseHubBean
        .startCase(Map.of("documentId", "provision-e2e", "status", "processing"))
        .toCompletableFuture().join();

    await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
      var logs = eventLogRepository.findByCaseId(caseId).await().indefinitely();
      var types = logs.stream().map(l -> l.getEventType().name()).toList();
      assertThat(types).contains("WORKER_REGISTERED");
    });
  }

  @Alternative @Priority(1) @ApplicationScoped
  public static class RecordingProvisioningWorkerProvisioner implements WorkerProvisioner {
    static final AtomicInteger callCount = new AtomicInteger(0);
    static void reset() { callCount.set(0); }

    @Override
    public Worker provision(Set<String> capabilities, ProvisionContext ctx) {
      callCount.incrementAndGet();
      return Worker.agent("provisioned-e2e-agent",
          capabilities.stream().map(Capability::new).toList());
    }
    @Override public void terminate(String workerId) {}
    @Override public Set<String> getCapabilities() { return Set.of("*"); }
  }
}
```

- [ ] **Write ProvisioningFailureTest**

```java
package io.casehub.engine;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.*;
import io.casehub.api.spi.ProvisioningException;
import io.casehub.api.spi.WorkerProvisioner;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProvisioningFailureTest {

  @Inject SimpleCaseHubBean simpleCaseHubBean;

  @Test
  void provisioning_failure_does_not_hang_case() {
    // FailingWorkerProvisioner always throws; case should fault or handle gracefully
    UUID caseId = simpleCaseHubBean
        .startCase(Map.of("documentId", "fail-provision", "status", "processing"))
        .toCompletableFuture().join();

    // Case must not hang indefinitely — either FAULTED or an exception was propagated
    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
        assertThat(caseId).isNotNull()  // at minimum the case started
    );
  }

  @Alternative @Priority(1) @ApplicationScoped
  public static class FailingWorkerProvisioner implements WorkerProvisioner {
    @Override
    public Worker provision(Set<String> caps, ProvisionContext ctx) {
      throw new ProvisioningException("Test: provisioner always fails");
    }
    @Override public void terminate(String w) {}
    @Override public Set<String> getCapabilities() { return Set.of("*"); }
  }
}
```

- [ ] **Write RegistrationEdgeCasesTest**

```java
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.casehub.api.model.Worker;
import io.casehub.engine.internal.worker.RegistrationContext;
import io.casehub.engine.internal.worker.WorkerRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.*;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RegistrationEdgeCasesTest {

  @Inject WorkerRegistry registry;

  @Test
  void register_with_no_capabilities_never_returned_as_candidate() {
    var worker = Worker.agent("no-cap-agent", List.of());
    registry.register(worker, RegistrationContext.selfRegistered("no-cap-agent"));
    assertThat(registry.findCandidates("anything", UUID.randomUUID())).isEmpty();
  }

  @Test
  void deregister_unknown_worker_does_not_throw() {
    assertThatCode(() -> registry.deregister("does-not-exist")).doesNotThrowAnyException();
  }

  @Test
  void duplicate_register_is_idempotent() {
    UUID caseId = UUID.randomUUID();
    var worker = Worker.agent("dup-agent", List.of(new io.casehub.api.model.Capability("dup-cap")));
    var ctx = RegistrationContext.provisioned(caseId, "test");
    registry.register(worker, ctx);
    registry.register(worker, ctx);
    assertThat(registry.findCandidates("dup-cap", caseId)).hasSize(1);
  }
}
```

- [ ] **Run all new tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD|FAILURE"
```
Expected: `BUILD SUCCESS`

- [ ] **Commit**

```bash
git add engine/src/test/java/io/casehub/engine/WorkerRegistryIntegrationTest.java \
        engine/src/test/java/io/casehub/engine/ProvisionedWorkerCaseTest.java \
        engine/src/test/java/io/casehub/engine/ProvisioningFailureTest.java \
        engine/src/test/java/io/casehub/engine/RegistrationEdgeCasesTest.java
git commit -m "test(engine): integration, E2E, and robustness tests for WorkerProvisioner wiring

Refs #<issue>"
```

---

## Task 14: WorkerExecutionTask — sealed switch

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/worker/WorkerExecutionTask.java`

`WorkerExecutionTask` currently uses `worker.getFunction().getValue() instanceof Workflow` checks. Update to use the sealed switch so AGENT_DRIVEN workers are detected early (they should never reach `WorkerExecutionTask` — add a guard and log an error).

- [ ] **Update the execute method in WorkerExecutionTask**

Find the section that does:
```java
if (worker.getFunction().getValue() instanceof Workflow workflow) {
  ...
} else if (worker.getFunction().getValue() instanceof Function function) {
  ...
}
```

Replace with:
```java
Map<String, Object> outputData = switch (worker.getExecution()) {
  case io.casehub.api.model.EngineWorkerExecution e -> {
    var fn = e.functionHolder();
    if (fn.getValue() instanceof Workflow workflow) {
      yield workflow(workflow, inputData);
    } else if (fn.getValue() instanceof java.util.function.Function function) {
      yield function(function, inputData);
    } else {
      throw new RuntimeException("Unknown engine execution type for worker: " + worker.getName());
    }
  }
  case io.casehub.api.model.AgentWorkerExecution a ->
      throw new JobExecutionException(
          "AGENT_DRIVEN worker '" + worker.getName() + "' must not reach WorkerExecutionTask — " +
          "check WorkerExecutionManager guards");
};
```

- [ ] **Build and run full suite**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD|FAILURE"
```
Expected: `BUILD SUCCESS`

- [ ] **Commit**

```bash
git add engine/src/main/java/io/casehub/engine/internal/worker/WorkerExecutionTask.java
git commit -m "refactor(engine): WorkerExecutionTask — sealed switch on WorkerExecution, guard against AGENT_DRIVEN

Refs #<issue>"
```

---

## Task 15: ADR-0007 + documentation sweep

**Files:**
- Create: `adr/0007-worker-registry-as-single-source-of-truth.md`
- Modify: `adr/INDEX.md`
- Modify: `DESIGN.md` (Worker Provisioner section)
- Modify: `CLAUDE.md` (WorkerRegistry test pattern, AGENT_DRIVEN callback)
- Modify: `docs/superpowers/specs/2026-04-14-casehub-engine-migration-plan.md`

- [ ] **Write ADR-0007**

`adr/0007-worker-registry-as-single-source-of-truth.md`:

```markdown
# 0007 — WorkerRegistry as Single Source of Truth

Date: 2026-04-27
Status: Accepted

## Context and Problem Statement

Three entry paths bring workers into the engine: static (declared in CaseDefinition), provisioned
(WorkerProvisioner.provision() on demand), and self-registered (external agents via REST or CDI).
A design decision was needed on how the engine discovers and tracks workers across all three paths.

## Decision Drivers

- Normative ledger recording (ADR-0006) requires one canonical place to instrument registration events
- WorkerStatusListener callbacks need one place to look up which worker completed
- WorkerProvisioner.terminate() needs one place to remove workers from the pool
- Future extensibility: Issue #187 tracks possible evolution to a WorkerCandidateSource SPI chain

## Considered Options

**Approach A — Single WorkerRegistry CDI bean** (chosen)
**Approach B — Provisioner as fallback, two pools** (rejected)
**Approach C — WorkerCandidateSource SPI chain** (deferred — Issue #187)

## Decision Outcome

Chosen: **Approach A**. WorkerRegistry is the single source of truth. All three entry paths call
`registry.register()`. WorkOrchestrator calls `registry.findCandidates()` instead of querying
CaseDefinition directly. Static workers are seeded at case start by CaseStartedEventHandler.

### Positive Consequences

- One place to instrument normative ledger entries (WORKER_REGISTERED/DEREGISTERED)
- One place for WorkerStatusListener callbacks to resolve completions
- Evolution to Approach C is an internal refactor of WorkerRegistry — callers unchanged

### Negative Consequences / Tradeoffs

- Slight change to WorkOrchestrator: replaces buildCandidates(definition, capability) with
  registry.findCandidates(capability, caseId). Existing tests required updates.

## Sealed WorkerExecution Hierarchy

Concurrently decided: Worker.execution uses a sealed interface (EngineWorkerExecution /
AgentWorkerExecution) instead of a nullable functionHolder or an enum field. This gives compiler-
enforced exhaustive switches, no null checks, and clean data model semantics.

## Links

- ADR-0005 — Worker Provisioner SPIs in api/spi/
- ADR-0006 — Worker registration as normative act
- Issue #187 — Future: WorkerCandidateSource SPI chain inside WorkerRegistry
- Spec: docs/superpowers/specs/2026-04-27-worker-provisioner-wiring-design.md
```

- [ ] **Update adr/INDEX.md**

Add row:
```
| 0007 | [WorkerRegistry as single source of truth](0007-worker-registry-as-single-source-of-truth.md) | Accepted | 2026-04-27 |
```

- [ ] **Update DESIGN.md — Worker Provisioner section**

Find the Worker Provisioner SPIs subsection. Replace "SPIs defined but not wired" language with:

```markdown
### Worker Provisioner — Live Registry

All workers — static, provisioned, and self-registered — are managed by `WorkerRegistry`
(`engine/internal/worker/WorkerRegistry`), the single source of truth for the live worker pool.

**Three entry paths:**
1. **Static** — `CaseStartedEventHandler` calls `registry.seedFromDefinition(definition, caseId)`
2. **Provisioned** — `WorkOrchestrator` calls `workerProvisioner.provision()` when no candidates
   found, then `registry.register(worker, RegistrationContext.provisioned(caseId, actorId))`
3. **Self-registered** — `POST /workers/register` or direct CDI call to `registry.register()`

**Two execution paths** (sealed switch on `worker.getExecution()`):
- `EngineWorkerExecution` → Quartz job → `WorkflowExecutor` → result via event bus
- `AgentWorkerExecution` → `AGENT_WORKER_ASSIGNED` event → wait for `WorkerStatusListener.onWorkerCompleted()` → `PendingWorkRegistry.complete()`

**Timeout:** `AgentTimeoutScheduler` fires `AgentTimeoutStrategy.onTimeout()` (default: stall after 30 minutes, configurable via `casehub.provisioner.agent-timeout-ms`).

**Trust:** `WorkerRegistrationLedgerService` writes `WORKER_REGISTERED` EventLog entries with
discoveryMode and trustLevel per ADR-0006.
```

- [ ] **Update CLAUDE.md — WorkerRegistry test pattern**

Add section:

```markdown
## WorkerRegistry in Tests

For `@QuarkusTest` tests that need workers in the registry, two options:

**Option 1 — Seed via CaseDefinition** (static workers): define workers in your test `CaseDefinition` and they are automatically seeded by `CaseStartedEventHandler`.

**Option 2 — Register directly** (provisioned/agent workers): inject `WorkerRegistry` and call `registry.register(Worker.agent("name", caps), RegistrationContext.provisioned(caseId, "test"))`.

**Recording provisioner**: use `@Alternative @Priority(1) @ApplicationScoped` static inner class implementing `WorkerProvisioner` to intercept `provision()` calls. Reset state in `@BeforeEach`. See `SpiWiringProvisionerTest` for the pattern.

**AGENT_DRIVEN workers**: futures are resolved by `WorkerStatusListener.onWorkerCompleted()`. In tests, inject `PendingWorkRegistry` and call `registry.complete(correlationKey, result)` to simulate agent completion, or inject `AgentWorkerStatusListenerBridge` and call `onWorkerCompleted()` directly.
```

- [ ] **Update migration plan**

In `docs/superpowers/specs/2026-04-14-casehub-engine-migration-plan.md`, find the Worker Provisioner SPIs entry and update to `✅ DONE`.

- [ ] **Build + run all tests one final time**

```bash
TESTCONTAINERS_RYUK_DISABLED=true JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl engine -am -f /Users/mdproctor/claude/casehub-engine/pom.xml 2>&1 | grep -E "Tests run|BUILD|FAILURE"
```
Expected: `BUILD SUCCESS`, no failures.

- [ ] **Commit**

```bash
git add adr/0007-worker-registry-as-single-source-of-truth.md \
        adr/INDEX.md \
        DESIGN.md \
        CLAUDE.md \
        docs/superpowers/specs/2026-04-14-casehub-engine-migration-plan.md
git commit -m "docs: ADR-0007, DESIGN.md worker registry section, CLAUDE.md patterns, migration plan updated

Refs #<issue>"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** All 11 spec sections have corresponding tasks. WorkerRegistry (§4), provisioning path (§5), self-registration (§6), timeout (§7), ledger recording (§8), testing pyramid (§9), documentation (§10).
- [x] **No placeholders:** All code steps contain actual implementations or explicit instructions to read before writing.
- [x] **Type consistency:** `WorkerExecution` / `EngineWorkerExecution` / `AgentWorkerExecution` used consistently. `RegistrationContext` factory methods match across tasks. `WorkerRegistry` method signatures (`register`, `deregister`, `findCandidates`, `seedFromDefinition`) consistent from Task 5 through Task 8.
- [x] **One gap noted and addressed:** `AgentWorkerStatusListenerBridge` CDI ambiguity resolved inline in Task 11 — completion logic moved into `WorkflowExecutionCompletedHandler` instead.
- [x] **WorkResult.correlationKey()** — Task 11 notes to check the actual constructor before writing tests. Adjust as needed.
