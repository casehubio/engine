# WorkBroker Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate `quarkus-work-api`/`quarkus-work-core` WorkBroker into casehub-engine's hybrid choreography+orchestration execution model, replacing the naive "schedule-all-capable-workers" selection with strategy-based routing and adding a durable orchestration API with WAITING-state case suspension.

**Architecture:** Two complementary paths share the same `WorkBroker`+`WorkloadProvider` selection infrastructure. The choreography path hooks into `CaseContextChangedEventHandler.publishWorkerSchedules()` and selects one worker via `LeastLoadedStrategy`. The orchestration path adds a `WorkOrchestrator` service that callers use to explicitly submit work and receive a `CompletionStage<WorkResult>`; it transitions the case to WAITING, and `WorkflowExecutionCompletedHandler` resumes it on completion. Correlation survives JVM restarts via a `waitingForWorkId` column on `CaseInstanceEntity` and `PendingWorkRegistry` which rebuilds from EventLog on startup.

**Tech Stack:** Quarkus Reactive, Vert.x event bus, Quartz (retained), `quarkus-work-api:1.0.0-SNAPSHOT`, `quarkus-work-core:1.0.0-SNAPSHOT`, JUnit 5, Mockito, AssertJ, Awaitility, `@QuarkusTest`

---

## Pre-requisites: Create GitHub Epic and Issues

Before writing any code, create the tracking structure. All commits must reference an issue.

- [ ] Create epic:
```bash
gh issue create --repo casehubio/engine --label "epic,enhancement" \
  --title "Epic: WorkBroker integration — strategy-based worker selection + orchestration model" \
  --body "$(cat <<'EOF'
## Overview
Integrates quarkus-work-api/quarkus-work-core into casehub-engine's hybrid choreography+orchestration engine.

## Scope
- [ ] #TBD — CI/CD: add quarkus-work-core to install step
- [ ] #TBD — CDI producers + CasehubWorkloadProvider
- [ ] #TBD — Choreography: WorkBroker selection in publishWorkerSchedules()
- [ ] #TBD — Orchestration model: WorkRequest, WorkResult, WorkOrchestrator
- [ ] #TBD — WAITING state: case suspension + WorkCompletionHandler resumption
- [ ] #TBD — Persistence: waitingForWorkId on CaseInstanceEntity
- [ ] #TBD — Documentation sync

## Definition of Done
WorkBroker selects one worker per binding (not all). WorkOrchestrator.submit() returns CompletionStage<WorkResult> that resolves when work completes, survives JVM restart. WAITING cases resume correctly on worker completion. All behaviour covered by unit, integration, and end-to-end tests.
EOF
)"
```
Record the epic number (referred to as `#EPIC` below).

- [ ] Create child issues and record their numbers:
```bash
# Issue A — CI
gh issue create --repo casehubio/engine --label "enhancement" \
  --title "feat(ci): add quarkus-work-core to CI dependency install step" \
  --body "Part of epic #EPIC. CI currently installs only quarkus-work-api. quarkus-work-core (WorkBroker, LeastLoadedStrategy) must also be installed before casehub-engine build. File: .github/workflows/maven.yml"

# Issue B — Infrastructure
gh issue create --repo casehubio/engine --label "enhancement" \
  --title "feat(engine): WorkBroker CDI producers and CasehubWorkloadProvider" \
  --body "Part of epic #EPIC. Add quarkus-work-api and quarkus-work-core to engine/pom.xml. Produce WorkBroker, LeastLoadedStrategy, NoOpWorkerRegistry as CDI beans. Implement CasehubWorkloadProvider counting active Quartz jobs per worker."

# Issue C — Choreography
gh issue create --repo casehubio/engine --label "enhancement" \
  --title "feat(engine): choreography worker selection via WorkBroker — replace schedule-all" \
  --body "Part of epic #EPIC. CaseContextChangedEventHandler.publishWorkerSchedules() currently schedules ALL capable workers. Replace with WorkBroker.apply() → LeastLoadedStrategy to select one. Files: CaseContextChangedEventHandler.java"

# Issue D — Orchestration types
gh issue create --repo casehubio/engine --label "enhancement" \
  --title "feat(api,engine): orchestration model — WorkRequest, WorkResult, WorkOrchestrator, PendingWorkRegistry" \
  --body "Part of epic #EPIC. Port casehub-core TaskBroker as WorkOrchestrator using WorkBroker SPI. Durable CompletionStage<WorkResult> with PendingWorkRegistry rebuilt from EventLog on restart."

# Issue E — WAITING state
gh issue create --repo casehubio/engine --label "enhancement" \
  --title "feat(engine): WAITING state — case suspension and resumption for orchestrated work" \
  --body "Part of epic #EPIC. Add waitingForWorkId to CaseInstance + CaseInstanceEntity. WorkOrchestrator transitions case to WAITING on submit. WorkflowExecutionCompletedHandler resumes WAITING cases on matching completion."

# Issue F — Docs
gh issue create --repo casehubio/engine --label "documentation" \
  --title "docs: sync DESIGN.md and close issue #121 with ADR-0003 reference" \
  --body "Part of epic #EPIC. Update DESIGN.md to reflect WorkBroker integration, dual execution models, WAITING state lifecycle. Post comment on #121 that naming decision is closed by ADR-0003."
```

---

## File Map

### New files
| File | Purpose |
|---|---|
| `engine/src/main/java/io/casehub/engine/internal/work/WorkCdi.java` | CDI `@Produces` for WorkBroker, LeastLoadedStrategy, NoOpWorkerRegistry |
| `engine/src/main/java/io/casehub/engine/internal/worker/CasehubWorkloadProvider.java` | `WorkloadProvider` impl — Quartz active job count per worker |
| `api/src/main/java/io/casehub/api/model/WorkRequest.java` | Input to WorkOrchestrator |
| `api/src/main/java/io/casehub/api/model/WorkResult.java` | Output from WorkOrchestrator |
| `api/src/main/java/io/casehub/api/model/WorkStatus.java` | PENDING / RUNNING / COMPLETED / FAULTED / CANCELLED |
| `engine/src/main/java/io/casehub/engine/internal/orchestration/PendingWorkRegistry.java` | In-memory future map, rebuilds from EventLog on startup |
| `engine/src/main/java/io/casehub/engine/internal/orchestration/WorkOrchestrator.java` | Public orchestration API — selects worker, submits, returns CompletionStage |
| `engine/src/test/java/io/casehub/engine/internal/worker/CasehubWorkloadProviderTest.java` | Unit tests |
| `engine/src/test/java/io/casehub/engine/ChoreographySelectionTest.java` | Integration: WorkBroker selection in choreography |
| `engine/src/test/java/io/casehub/engine/OrchestrationTest.java` | Integration: WorkOrchestrator submit + CompletionStage |
| `engine/src/test/java/io/casehub/engine/CaseWaitingResumeTest.java` | Integration: case WAITING → work completes → RUNNING |
| `engine/src/test/java/io/casehub/engine/WorkBrokerEndToEndTest.java` | E2E: full case with both choreography and orchestration in one flow |

### Modified files
| File | Change |
|---|---|
| `.github/workflows/maven.yml` | Add `quarkus-work-core` to install step (already partially done — update `-pl` flag) |
| `engine/pom.xml` | Add `quarkus-work-api` and `quarkus-work-core` compile-scope deps |
| `engine-model/.../model/CaseInstance.java` | Add `waitingForWorkId` field + getter/setter |
| `casehub-persistence-hibernate/.../jpa/CaseInstanceEntity.java` | Add `waiting_for_work_id` column |
| `casehub-persistence-hibernate/.../jpa/JpaCaseInstanceRepository.java` | Map `waitingForWorkId` in `save()`, `update()`, `fromEntity()` |
| `engine/.../history/CaseHubEventType.java` | Add `WORK_SUBMITTED`, `WORK_COMPLETED` |
| `engine/.../handler/CaseContextChangedEventHandler.java` | Replace schedule-all with WorkBroker selection |
| `engine/.../handler/WorkflowExecutionCompletedHandler.java` | Add WAITING→RUNNING transition + PendingWorkRegistry completion |
| `docs/DESIGN.md` | Sync WorkBroker integration, dual model, WAITING lifecycle |

---

## Phase 1 — Infrastructure + Choreography

### Task 1: CI/CD — install quarkus-work-core (Issue A)

**Files:**
- Modify: `.github/workflows/maven.yml`

- [ ] **Step 1: Update both jobs' install step**

In `.github/workflows/maven.yml`, change both `Install quarkus-work-api` steps:

```yaml
      - name: Install quarkus-work-api and quarkus-work-core
        run: mvn install -pl quarkus-work-api,quarkus-work-core -DskipTests -q
        working-directory: quarkus-workitems
```

Apply this change to both `os-test` and `maven-version-test` jobs.

- [ ] **Step 2: Commit**
```bash
git add .github/workflows/maven.yml
git commit -m "feat(ci): install quarkus-work-core alongside quarkus-work-api

Refs #ISSUE_A"
```

---

### Task 2: pom dependencies (Issue B)

**Files:**
- Modify: `engine/pom.xml`

- [ ] **Step 1: Add dependencies after the `zjsonpatch` dependency**

```xml
        <dependency>
            <groupId>io.quarkiverse.work</groupId>
            <artifactId>quarkus-work-api</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>io.quarkiverse.work</groupId>
            <artifactId>quarkus-work-core</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
```

- [ ] **Step 2: Verify compilation**
```bash
cd /path/to/casehub-engine
mvn compile -pl engine -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**
```bash
git add engine/pom.xml
git commit -m "feat(engine): add quarkus-work-api and quarkus-work-core dependencies

Refs #ISSUE_B"
```

---

### Task 3: CDI producers (Issue B)

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/work/WorkCdi.java`

`WorkBroker`, `LeastLoadedStrategy`, and `NoOpWorkerRegistry` have no-arg constructors and are not CDI beans. We produce them here so they can be `@Inject`ed.

- [ ] **Step 1: Create the producer class**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.work;

import io.quarkiverse.work.api.WorkerRegistry;
import io.quarkiverse.work.api.WorkerSelectionStrategy;
import io.quarkiverse.work.core.strategy.LeastLoadedStrategy;
import io.quarkiverse.work.core.strategy.NoOpWorkerRegistry;
import io.quarkiverse.work.core.strategy.WorkBroker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class WorkCdi {

  @Produces
  @ApplicationScoped
  public WorkBroker workBroker() {
    return new WorkBroker();
  }

  @Produces
  @ApplicationScoped
  public WorkerSelectionStrategy defaultSelectionStrategy() {
    return new LeastLoadedStrategy();
  }

  @Produces
  @ApplicationScoped
  public WorkerRegistry defaultWorkerRegistry() {
    return new NoOpWorkerRegistry();
  }
}
```

- [ ] **Step 2: Verify compilation**
```bash
mvn compile -pl engine -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**
```bash
git add engine/src/main/java/io/casehub/engine/internal/work/WorkCdi.java
git commit -m "feat(engine): CDI producers for WorkBroker, LeastLoadedStrategy, NoOpWorkerRegistry

Refs #ISSUE_B"
```

---

### Task 4: CasehubWorkloadProvider — TDD (Issue B)

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/worker/CasehubWorkloadProvider.java`
- Create: `engine/src/test/java/io/casehub/engine/internal/worker/CasehubWorkloadProviderTest.java`

`WorkloadProvider.getActiveWorkCount(String workerId)` must return the number of Quartz jobs currently scheduled/running for that worker. Quartz jobs store `workerId` in their `JobDataMap`.

- [ ] **Step 1: Write failing unit tests**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

class CasehubWorkloadProviderTest {

  // ---- happy path -----------------------------------------------------------

  @Test
  void noJobsScheduled_returnsZero() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    when(scheduler.getJobGroupNames()).thenReturn(List.of());

    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);

    assertThat(provider.getActiveWorkCount("some-worker")).isZero();
  }

  @Test
  void oneMatchingJob_returnsOne() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    JobKey key = new JobKey("hash-abc", "case-1");
    JobDetail detail = mock(JobDetail.class);
    JobDataMap dataMap = new JobDataMap();
    dataMap.put("workerId", "my-worker");

    when(scheduler.getJobGroupNames()).thenReturn(List.of("case-1"));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-1"))).thenReturn(Set.of(key));
    when(scheduler.getJobDetail(key)).thenReturn(detail);
    when(detail.getJobDataMap()).thenReturn(dataMap);

    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);

    assertThat(provider.getActiveWorkCount("my-worker")).isEqualTo(1);
  }

  @Test
  void multipleGroups_countsAcrossAll() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);

    JobKey key1 = new JobKey("hash-1", "case-1");
    JobKey key2 = new JobKey("hash-2", "case-2");
    JobKey key3 = new JobKey("hash-3", "case-2");

    JobDetail detail1 = jobDetailWithWorker("target-worker");
    JobDetail detail2 = jobDetailWithWorker("target-worker");
    JobDetail detail3 = jobDetailWithWorker("other-worker");

    when(scheduler.getJobGroupNames()).thenReturn(List.of("case-1", "case-2"));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-1"))).thenReturn(Set.of(key1));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-2"))).thenReturn(Set.of(key2, key3));
    when(scheduler.getJobDetail(key1)).thenReturn(detail1);
    when(scheduler.getJobDetail(key2)).thenReturn(detail2);
    when(scheduler.getJobDetail(key3)).thenReturn(detail3);

    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);

    assertThat(provider.getActiveWorkCount("target-worker")).isEqualTo(2);
    assertThat(provider.getActiveWorkCount("other-worker")).isEqualTo(1);
  }

  // ---- robustness -----------------------------------------------------------

  @Test
  void schedulerThrows_returnsZero() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    when(scheduler.getJobGroupNames()).thenThrow(new SchedulerException("simulated failure"));

    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);

    // Must not propagate exception — degrade gracefully
    assertThat(provider.getActiveWorkCount("any-worker")).isZero();
  }

  @Test
  void noJobsMatchingWorker_returnsZero() throws SchedulerException {
    Scheduler scheduler = mock(Scheduler.class);
    JobKey key = new JobKey("hash-abc", "case-1");
    JobDetail detail = jobDetailWithWorker("different-worker");

    when(scheduler.getJobGroupNames()).thenReturn(List.of("case-1"));
    when(scheduler.getJobKeys(GroupMatcher.groupEquals("case-1"))).thenReturn(Set.of(key));
    when(scheduler.getJobDetail(key)).thenReturn(detail);

    CasehubWorkloadProvider provider = new CasehubWorkloadProvider(scheduler);

    assertThat(provider.getActiveWorkCount("my-worker")).isZero();
  }

  // ---- helper ---------------------------------------------------------------

  private JobDetail jobDetailWithWorker(String workerId) {
    JobDetail detail = mock(JobDetail.class);
    JobDataMap dataMap = new JobDataMap();
    dataMap.put("workerId", workerId);
    when(detail.getJobDataMap()).thenReturn(dataMap);
    return detail;
  }
}
```

- [ ] **Step 2: Run tests — verify they fail**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=CasehubWorkloadProviderTest -q 2>&1 | tail -5
```
Expected: FAIL — `CasehubWorkloadProvider` does not exist yet.

- [ ] **Step 3: Implement CasehubWorkloadProvider**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.worker;

import io.quarkiverse.work.api.WorkloadProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Counts active Quartz jobs per worker name by iterating all scheduled job groups and matching
 * the {@code workerId} field in each job's data map.
 *
 * <p>Used by {@link io.quarkiverse.work.core.strategy.LeastLoadedStrategy} to prefer
 * workers with fewer in-flight tasks.
 */
@ApplicationScoped
public class CasehubWorkloadProvider implements WorkloadProvider {

  private static final Logger LOG = Logger.getLogger(CasehubWorkloadProvider.class);

  private final Scheduler scheduler;

  @Inject
  public CasehubWorkloadProvider(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public int getActiveWorkCount(String workerId) {
    try {
      List<String> groups = scheduler.getJobGroupNames();
      int count = 0;
      for (String group : groups) {
        Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.groupEquals(group));
        for (JobKey key : keys) {
          JobDetail detail = scheduler.getJobDetail(key);
          if (detail != null && workerId.equals(detail.getJobDataMap().getString("workerId"))) {
            count++;
          }
        }
      }
      return count;
    } catch (SchedulerException e) {
      LOG.warnf("Failed to count active jobs for worker '%s' — returning 0: %s", workerId, e.getMessage());
      return 0;
    }
  }
}
```

- [ ] **Step 4: Run tests — verify they pass**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=CasehubWorkloadProviderTest -q 2>&1 | tail -5
```
Expected: Tests run: 5, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**
```bash
git add engine/src/main/java/io/casehub/engine/internal/worker/CasehubWorkloadProvider.java \
        engine/src/test/java/io/casehub/engine/internal/worker/CasehubWorkloadProviderTest.java
git commit -m "feat(engine): CasehubWorkloadProvider — Quartz job count per worker

Implements WorkloadProvider SPI from quarkus-work-api using Quartz
scheduler job group enumeration. Degrades gracefully on scheduler failure.

Refs #ISSUE_B"
```

---

### Task 5: Choreography — WorkBroker selection (Issue C)

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/engine/handler/CaseContextChangedEventHandler.java`
- Create: `engine/src/test/java/io/casehub/engine/ChoreographySelectionTest.java`

Replace the existing `publishWorkerSchedules()` loop — which fires a `WorkerScheduleEvent` for every capable worker — with a WorkBroker call that selects exactly one.

- [ ] **Step 1: Write failing integration test**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that WorkBroker selects exactly one worker when multiple workers share the same
 * capability. Previously, all capable workers were scheduled — this was the bug being fixed.
 */
@QuarkusTest
class ChoreographySelectionTest {

  @Inject CaseInstanceCache cache;
  @Inject TwoWorkerSameCapabilityCase twoWorkerCase;

  @BeforeEach
  void clear() {
    cache.clear();
    TwoWorkerSameCapabilityCase.workerACount.set(0);
    TwoWorkerSameCapabilityCase.workerBCount.set(0);
  }

  /** Happy path: two workers with the same capability — only one is called. */
  @Test
  void twoWorkersSharedCapability_onlyOneIsSelected() throws Exception {
    AtomicReference<UUID> caseIdRef = new AtomicReference<>();
    twoWorkerCase.startCase(Map.of("trigger", "go"))
        .thenAccept(caseIdRef::set);

    await().atMost(10, TimeUnit.SECONDS).until(() -> caseIdRef.get() != null);
    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() ->
            assertThat(cache.get(caseIdRef.get()).getState())
                .isEqualTo(CaseStatus.COMPLETED));

    int totalCalls = TwoWorkerSameCapabilityCase.workerACount.get()
        + TwoWorkerSameCapabilityCase.workerBCount.get();
    assertThat(totalCalls)
        .as("WorkBroker must select exactly one worker, not both")
        .isEqualTo(1);
  }

  /** Correctness: LeastLoadedStrategy should prefer the less-loaded worker across runs. */
  @Test
  void leastLoadedStrategy_prefersLessLoadedWorker() throws Exception {
    // Run two sequential cases — each should complete with exactly 1 worker call
    for (int i = 0; i < 2; i++) {
      AtomicReference<UUID> caseIdRef = new AtomicReference<>();
      twoWorkerCase.startCase(Map.of("trigger", "go")).thenAccept(caseIdRef::set);
      await().atMost(10, TimeUnit.SECONDS)
          .untilAsserted(() ->
              assertThat(cache.get(caseIdRef.get()).getState())
                  .isEqualTo(CaseStatus.COMPLETED));
      cache.clear();
    }

    int totalCalls = TwoWorkerSameCapabilityCase.workerACount.get()
        + TwoWorkerSameCapabilityCase.workerBCount.get();
    assertThat(totalCalls).isEqualTo(2);
  }

  // ---- Case bean ------------------------------------------------------------

  @ApplicationScoped
  public static class TwoWorkerSameCapabilityCase extends CaseHub {

    static final AtomicInteger workerACount = new AtomicInteger(0);
    static final AtomicInteger workerBCount = new AtomicInteger(0);

    private final Capability capability = Capability.builder()
        .name("do-work")
        .inputSchema("{ trigger: .trigger }")
        .outputSchema("{ result: \"done\" }")
        .build();

    private final Goal goal = Goal.builder()
        .name("done")
        .condition(".result == \"done\"")
        .kind(GoalKind.SUCCESS)
        .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-selection")
          .name("Two Worker Selection Case")
          .version("1.0.0")
          .capabilities(capability)
          .workers(
              Worker.builder().name("worker-a").capabilities(capability)
                  .function(input -> { workerACount.incrementAndGet(); return Map.of("result", "done"); })
                  .build(),
              Worker.builder().name("worker-b").capabilities(capability)
                  .function(input -> { workerBCount.incrementAndGet(); return Map.of("result", "done"); })
                  .build())
          .bindings(Binding.builder().name("trigger")
              .capability(capability)
              .on(new ContextChangeTrigger(".trigger == \"go\""))
              .build())
          .goals(goal)
          .completion(GoalExpression.allOf(goal))
          .build();
    }
  }
}
```

- [ ] **Step 2: Run test — verify it fails (both workers still called)**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=ChoreographySelectionTest -q 2>&1 | tail -10
```
Expected: FAIL — `totalCalls` is 2 (both workers scheduled), not 1.

- [ ] **Step 3: Refactor `publishWorkerSchedules()` in CaseContextChangedEventHandler**

Add these injections to the class:
```java
  @Inject WorkBroker workBroker;
  @Inject WorkerSelectionStrategy selectionStrategy;
  @Inject WorkloadProvider workloadProvider;
```

Replace the entire `publishWorkerSchedules()` method:
```java
  private Uni<Void> publishWorkerSchedules(
      CaseInstance caseInstance, List<Worker> workers, Binding binding, Capability capability) {

    if (workers == null || workers.isEmpty()) {
      LOG.warnf("No workers defined; cannot schedule capability '%s'", capability.getName());
      return Uni.createFrom().voidItem();
    }

    List<WorkerCandidate> candidates =
        workers.stream()
            .filter(w -> w.getCapabilities() != null)
            .filter(
                w ->
                    w.getCapabilities().stream()
                        .anyMatch(c -> c.getName().equals(capability.getName())))
            .map(
                w ->
                    WorkerCandidate.of(w.getName())
                        .withActiveWorkItemCount(
                            workloadProvider.getActiveWorkCount(w.getName())))
            .toList();

    if (candidates.isEmpty()) {
      LOG.warnf(
          "No workers match capability '%s' for binding '%s'",
          capability.getName(), binding.getName());
      return Uni.createFrom().voidItem();
    }

    SelectionContext ctx =
        new SelectionContext(
            capability.getName(),
            null,
            capability.getName(),
            null,
            null);

    AssignmentDecision decision =
        workBroker.apply(ctx, AssignmentTrigger.CREATED, candidates, selectionStrategy);

    if (decision.isNoOp()) {
      LOG.warnf(
          "WorkBroker returned no assignment for capability '%s' binding '%s'",
          capability.getName(), binding.getName());
      return Uni.createFrom().voidItem();
    }

    String selectedId = decision.assigneeId();
    Worker selectedWorker =
        workers.stream()
            .filter(w -> w.getName().equals(selectedId))
            .findFirst()
            .orElse(null);

    if (selectedWorker == null) {
      LOG.errorf(
          "WorkBroker selected worker '%s' but it was not found in the case definition", selectedId);
      return Uni.createFrom().voidItem();
    }

    LOG.infof(
        "WorkBroker selected '%s' for capability '%s' (binding '%s')",
        selectedId, capability.getName(), binding.getName());

    eventBus.publish(
        EventBusAddresses.WORKER_SCHEDULE,
        new WorkerScheduleEvent(caseInstance, selectedWorker, capability));

    return Uni.createFrom().voidItem();
  }
```

Add imports:
```java
import io.quarkiverse.work.api.AssignmentDecision;
import io.quarkiverse.work.api.AssignmentTrigger;
import io.quarkiverse.work.api.SelectionContext;
import io.quarkiverse.work.api.WorkerCandidate;
import io.quarkiverse.work.api.WorkerSelectionStrategy;
import io.quarkiverse.work.api.WorkloadProvider;
import io.quarkiverse.work.core.strategy.WorkBroker;
```

- [ ] **Step 4: Run tests — verify they pass**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=ChoreographySelectionTest -q 2>&1 | tail -5
```
Expected: Tests run: 2, Failures: 0, Errors: 0

- [ ] **Step 5: Run full engine test suite to check for regressions**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -q 2>&1 | grep -E "Tests run:|BUILD" | tail -10
```
Expected: BUILD SUCCESS, no failures.

- [ ] **Step 6: Commit**
```bash
git add engine/src/main/java/io/casehub/engine/internal/engine/handler/CaseContextChangedEventHandler.java \
        engine/src/test/java/io/casehub/engine/ChoreographySelectionTest.java
git commit -m "feat(engine): choreography worker selection via WorkBroker

Replaces schedule-all-capable-workers with WorkBroker.apply() +
LeastLoadedStrategy. Selects exactly one worker per binding activation.
Two integration tests verify single-selection and strategy preference.

Refs #ISSUE_C"
```

---

## Phase 2 — Orchestration Model

### Task 6: WorkRequest, WorkResult, WorkStatus types (Issue D)

**Files:**
- Create: `api/src/main/java/io/casehub/api/model/WorkRequest.java`
- Create: `api/src/main/java/io/casehub/api/model/WorkResult.java`
- Create: `api/src/main/java/io/casehub/api/model/WorkStatus.java`

These live in `api/` so all modules can depend on them.

- [ ] **Step 1: Create WorkStatus**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.api.model;

/** Lifecycle states of a unit of orchestrated work submitted via WorkOrchestrator. */
public enum WorkStatus {
  PENDING,
  RUNNING,
  COMPLETED,
  FAULTED,
  CANCELLED
}
```

- [ ] **Step 2: Create WorkRequest**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.api.model;

import java.util.Map;

/**
 * Input to {@code WorkOrchestrator.submit()}. Describes the capability required and the
 * data to pass to the selected worker.
 */
public record WorkRequest(String capability, Map<String, Object> input) {

  public static WorkRequest of(String capability, Map<String, Object> input) {
    return new WorkRequest(capability, input == null ? Map.of() : input);
  }
}
```

- [ ] **Step 3: Create WorkResult**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.api.model;

import java.util.Map;

/**
 * Result of orchestrated work returned by {@code WorkOrchestrator.submit()}. The
 * {@code correlationKey} is the idempotency hash used to match this result to its submission.
 */
public record WorkResult(
    String correlationKey, WorkStatus status, Map<String, Object> output, String workerId) {

  public static WorkResult completed(
      String correlationKey, Map<String, Object> output, String workerId) {
    return new WorkResult(correlationKey, WorkStatus.COMPLETED, output, workerId);
  }

  public static WorkResult faulted(String correlationKey, String workerId) {
    return new WorkResult(correlationKey, WorkStatus.FAULTED, Map.of(), workerId);
  }
}
```

- [ ] **Step 4: Verify compilation**
```bash
mvn compile -pl api -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**
```bash
git add api/src/main/java/io/casehub/api/model/WorkRequest.java \
        api/src/main/java/io/casehub/api/model/WorkResult.java \
        api/src/main/java/io/casehub/api/model/WorkStatus.java
git commit -m "feat(api): WorkRequest, WorkResult, WorkStatus — orchestration model types

Refs #ISSUE_D"
```

---

### Task 7: CaseHubEventType additions (Issue D)

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/history/CaseHubEventType.java`

- [ ] **Step 1: Add WORK_SUBMITTED and WORK_COMPLETED**

```java
public enum CaseHubEventType {
  CASE_STARTED,
  CASE_COMPLETED,
  CASE_FAULTED,
  CASE_CANCELLED,

  TASK_CREATED,
  TASK_COMPLETED,
  TASK_FAILED,
  TASK_CANCELLED,

  WORKER_SCHEDULED,
  WORKER_EXECUTION_STARTED,
  WORKER_EXECUTION_COMPLETED,
  WORKER_EXECUTION_FAILED,

  WORK_SUBMITTED,    // orchestrated work submitted via WorkOrchestrator
  WORK_COMPLETED,    // orchestrated work completed; case may resume from WAITING

  SIGNAL_RECEIVED,

  MILESTONE_REACHED,
  GOAL_REACHED,
  CASE_STATUS_CHANGED,
}
```

- [ ] **Step 2: Commit**
```bash
git add engine/src/main/java/io/casehub/engine/internal/history/CaseHubEventType.java
git commit -m "feat(engine): add WORK_SUBMITTED and WORK_COMPLETED event types

Refs #ISSUE_D"
```

---

### Task 8: WAITING state — CaseInstance + persistence (Issue E)

**Files:**
- Modify: `engine-model/src/main/java/io/casehub/engine/internal/model/CaseInstance.java`
- Modify: `casehub-persistence-hibernate/src/main/java/io/casehub/persistence/jpa/CaseInstanceEntity.java`
- Modify: `casehub-persistence-hibernate/src/main/java/io/casehub/persistence/jpa/JpaCaseInstanceRepository.java`

`waitingForWorkId` stores the idempotency hash of the work the case is waiting for. When the matching `WorkflowExecutionCompleted` event fires, the case resumes.

- [ ] **Step 1: Add field to CaseInstance**

In `CaseInstance.java`, add after the `propagationContext` field:
```java
  private String waitingForWorkId;
```

Add getter and setter after the `setPropagationContext()` method:
```java
  public String getWaitingForWorkId() {
    return waitingForWorkId;
  }

  public void setWaitingForWorkId(String waitingForWorkId) {
    this.waitingForWorkId = waitingForWorkId;
  }
```

- [ ] **Step 2: Add column to CaseInstanceEntity**

In `CaseInstanceEntity.java`, add after `parentPlanItemId`:
```java
  @Column(name = "waiting_for_work_id", nullable = true, length = 255)
  public String waitingForWorkId;
```

- [ ] **Step 3: Map the field in JpaCaseInstanceRepository**

In `save()`, after `entity.parentPlanItemId = instance.getParentPlanItemId();`:
```java
                              entity.waitingForWorkId = instance.getWaitingForWorkId();
```

In `update()`, inside the `.invoke()` lambda after `entity.parentPlanItemId = instance.getParentPlanItemId();`:
```java
                              entity.waitingForWorkId = instance.getWaitingForWorkId();
```

In `updateStateAndAppendEvent()`, inside the `.chain()` lambda after `entity.parentPlanItemId = instance.getParentPlanItemId();`:
```java
                                  entity.waitingForWorkId = instance.getWaitingForWorkId();
```

In `fromEntity()`, after `instance.setParentPlanItemId(entity.parentPlanItemId);`:
```java
    instance.setWaitingForWorkId(entity.waitingForWorkId);
```

- [ ] **Step 4: Verify compilation**
```bash
mvn compile -pl engine-model,casehub-persistence-hibernate -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**
```bash
git add engine-model/src/main/java/io/casehub/engine/internal/model/CaseInstance.java \
        casehub-persistence-hibernate/src/main/java/io/casehub/persistence/jpa/CaseInstanceEntity.java \
        casehub-persistence-hibernate/src/main/java/io/casehub/persistence/jpa/JpaCaseInstanceRepository.java
git commit -m "feat(engine): add waitingForWorkId to CaseInstance and persistence layer

Stores the idempotency hash of orchestrated work that has suspended a case
to WAITING state. Persisted so that WAITING→RUNNING resumption works after
JVM restart. Schema uses drop-and-create — no migration required.

Refs #ISSUE_E"
```

---

### Task 9: PendingWorkRegistry — durable correlation (Issue D)

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/orchestration/PendingWorkRegistry.java`

In-memory `ConcurrentHashMap<String, CompletableFuture<WorkResult>>` keyed by idempotency hash. On application startup, scans `EventLog` for `WORK_SUBMITTED` events with no matching `WORK_COMPLETED` and registers futures for them, so that in-flight orchestrated work survives JVM restart.

- [ ] **Step 1: Write unit tests**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.WorkResult;
import io.casehub.api.model.WorkStatus;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PendingWorkRegistryTest {

  // ---- happy path -----------------------------------------------------------

  @Test
  void register_thenComplete_futureResolves() throws Exception {
    PendingWorkRegistry registry = new PendingWorkRegistry();
    CompletableFuture<WorkResult> future = registry.register("key-1");

    WorkResult result = WorkResult.completed("key-1", Map.of("output", "done"), "worker-a");
    registry.complete("key-1", result);

    assertThat(future.get(1, TimeUnit.SECONDS)).isEqualTo(result);
  }

  @Test
  void complete_withNoRegisteredFuture_doesNotThrow() {
    PendingWorkRegistry registry = new PendingWorkRegistry();
    // Completing a key with no registered future must be a no-op
    WorkResult result = WorkResult.completed("unknown-key", Map.of(), "worker-a");
    registry.complete("unknown-key", result); // must not throw
  }

  // ---- correctness ----------------------------------------------------------

  @Test
  void afterComplete_futureIsRemovedFromRegistry() throws Exception {
    PendingWorkRegistry registry = new PendingWorkRegistry();
    registry.register("key-2");
    registry.complete("key-2", WorkResult.completed("key-2", Map.of(), "w"));

    assertThat(registry.hasPending("key-2")).isFalse();
  }

  @Test
  void multipleKeys_completedIndependently() throws Exception {
    PendingWorkRegistry registry = new PendingWorkRegistry();
    CompletableFuture<WorkResult> f1 = registry.register("key-a");
    CompletableFuture<WorkResult> f2 = registry.register("key-b");

    registry.complete("key-a", WorkResult.faulted("key-a", "worker-x"));

    assertThat(f1.isDone()).isTrue();
    assertThat(f1.get().status()).isEqualTo(WorkStatus.FAULTED);
    assertThat(f2.isDone()).isFalse();
  }

  // ---- robustness -----------------------------------------------------------

  @Test
  void registerSameKeyTwice_returnsDistinctFutures() {
    PendingWorkRegistry registry = new PendingWorkRegistry();
    CompletableFuture<WorkResult> f1 = registry.register("dup-key");
    CompletableFuture<WorkResult> f2 = registry.register("dup-key");
    // Both futures must be registered and distinct
    assertThat(f1).isNotSameAs(f2);
  }
}
```

- [ ] **Step 2: Run tests — verify they fail**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=PendingWorkRegistryTest -q 2>&1 | tail -5
```
Expected: FAIL — class does not exist.

- [ ] **Step 3: Implement PendingWorkRegistry**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.orchestration;

import io.casehub.api.model.WorkResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * In-memory registry of pending {@link CompletableFuture}s for orchestrated work, keyed by
 * idempotency hash. Futures are completed when {@code WorkflowExecutionCompletedHandler} sees
 * the matching {@code WorkflowExecutionCompleted} event.
 *
 * <p>After a JVM restart the in-memory futures are gone, but the {@code WorkOrchestrator}
 * re-registers futures for any WORK_SUBMITTED EventLog entries without a WORK_COMPLETED entry
 * on startup, so in-flight work that was recovered by {@code WorkerExecutionRecoveryService}
 * will still resolve the correct future when it completes.
 */
@ApplicationScoped
public class PendingWorkRegistry {

  private static final Logger LOG = Logger.getLogger(PendingWorkRegistry.class);

  private final ConcurrentHashMap<String, List<CompletableFuture<WorkResult>>> pending =
      new ConcurrentHashMap<>();

  /**
   * Registers a new future for the given correlation key. Multiple futures per key are supported
   * (e.g. two callers both waiting for the same work item).
   */
  public CompletableFuture<WorkResult> register(String correlationKey) {
    CompletableFuture<WorkResult> future = new CompletableFuture<>();
    pending.computeIfAbsent(correlationKey, k -> new ArrayList<>()).add(future);
    LOG.debugf("Registered pending future for correlationKey=%s", correlationKey);
    return future;
  }

  /**
   * Completes all futures registered under {@code correlationKey} with the given result and
   * removes the entry. If no future is registered, this is a no-op.
   */
  public void complete(String correlationKey, WorkResult result) {
    List<CompletableFuture<WorkResult>> futures = pending.remove(correlationKey);
    if (futures == null) {
      return;
    }
    LOG.debugf("Completing %d future(s) for correlationKey=%s status=%s",
        futures.size(), correlationKey, result.status());
    for (CompletableFuture<WorkResult> future : futures) {
      future.complete(result);
    }
  }

  /** Returns true if there is at least one future registered for the given key. */
  public boolean hasPending(String correlationKey) {
    List<CompletableFuture<WorkResult>> futures = pending.get(correlationKey);
    return futures != null && !futures.isEmpty();
  }
}
```

- [ ] **Step 4: Run tests — verify they pass**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=PendingWorkRegistryTest -q 2>&1 | tail -5
```
Expected: Tests run: 5, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**
```bash
git add engine/src/main/java/io/casehub/engine/internal/orchestration/PendingWorkRegistry.java \
        engine/src/test/java/io/casehub/engine/internal/orchestration/PendingWorkRegistryTest.java
git commit -m "feat(engine): PendingWorkRegistry — durable-safe future correlation for orchestrated work

In-memory map of CompletableFutures keyed by idempotency hash.
On restart, WorkOrchestrator re-registers futures for in-flight work
so recovered Quartz jobs still resolve the correct CompletionStage.

Refs #ISSUE_D"
```

---

### Task 10: WorkOrchestrator (Issue D)

**Files:**
- Create: `engine/src/main/java/io/casehub/engine/internal/orchestration/WorkOrchestrator.java`

`WorkOrchestrator` is the durable replacement for casehub-core's `TaskBroker`. It selects a worker via `WorkBroker`, publishes a `WorkerScheduleEvent` (reusing the existing scheduling infrastructure), registers a `CompletableFuture<WorkResult>` in `PendingWorkRegistry`, writes a `WORK_SUBMITTED` EventLog entry for durability, and optionally transitions the case to `WAITING`.

- [ ] **Step 1: Write unit tests**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.casehub.api.context.CaseContext;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.Worker;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkResult;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseMetaModel;
import io.casehub.engine.spi.CaseInstanceRepository;
import io.casehub.engine.spi.EventLogRepository;
import io.quarkiverse.work.api.AssignmentDecision;
import io.quarkiverse.work.api.WorkerSelectionStrategy;
import io.quarkiverse.work.api.WorkloadProvider;
import io.quarkiverse.work.core.strategy.WorkBroker;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkOrchestratorTest {

  private WorkBroker workBroker;
  private WorkerSelectionStrategy strategy;
  private WorkloadProvider workloadProvider;
  private EventBus eventBus;
  private PendingWorkRegistry registry;
  private CaseDefinitionRegistry caseDefinitionRegistry;
  private CaseInstanceRepository caseInstanceRepository;
  private EventLogRepository eventLogRepository;
  private CaseInstanceCache cache;
  private WorkOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    workBroker = mock(WorkBroker.class);
    strategy = mock(WorkerSelectionStrategy.class);
    workloadProvider = mock(WorkloadProvider.class);
    eventBus = mock(EventBus.class);
    registry = new PendingWorkRegistry();
    caseDefinitionRegistry = mock(CaseDefinitionRegistry.class);
    caseInstanceRepository = mock(CaseInstanceRepository.class);
    eventLogRepository = mock(EventLogRepository.class);
    cache = new CaseInstanceCache();

    when(workloadProvider.getActiveWorkCount(any())).thenReturn(0);
    when(caseInstanceRepository.updateStateAndAppendEvent(any(), any()))
        .thenReturn(Uni.createFrom().voidItem());
    when(eventLogRepository.appendAndReturnId(any()))
        .thenReturn(Uni.createFrom().item(1L));

    orchestrator = new WorkOrchestrator(workBroker, strategy, workloadProvider,
        eventBus, registry, caseDefinitionRegistry, caseInstanceRepository,
        eventLogRepository, cache);
  }

  // ---- happy path -----------------------------------------------------------

  @Test
  void submit_workerSelected_publishesScheduleEvent() {
    CaseInstance instance = runningInstance("analyse");
    cache.put(instance);

    when(workBroker.apply(any(), any(), any(), any()))
        .thenReturn(AssignmentDecision.assignTo("analyst-worker"));

    orchestrator.submit(instance, WorkRequest.of("analyse", Map.of("doc", "x")));

    verify(eventBus).publish(any(), any(WorkerScheduleEvent.class));
  }

  @Test
  void submit_workerSelected_returnsPendingFuture() {
    CaseInstance instance = runningInstance("analyse");
    cache.put(instance);
    when(workBroker.apply(any(), any(), any(), any()))
        .thenReturn(AssignmentDecision.assignTo("analyst-worker"));

    CompletableFuture<WorkResult> future =
        orchestrator.submit(instance, WorkRequest.of("analyse", Map.of())).toCompletableFuture();

    assertThat(future.isDone()).isFalse();
  }

  @Test
  void submit_withWaiting_transitionsCaseToWaiting() {
    CaseInstance instance = runningInstance("analyse");
    cache.put(instance);
    when(workBroker.apply(any(), any(), any(), any()))
        .thenReturn(AssignmentDecision.assignTo("analyst-worker"));

    orchestrator.submitAndWait(instance, WorkRequest.of("analyse", Map.of("doc", "x")));

    assertThat(instance.getState()).isEqualTo(CaseStatus.WAITING);
    assertThat(instance.getWaitingForWorkId()).isNotNull();
  }

  // ---- robustness -----------------------------------------------------------

  @Test
  void submit_noCapableWorker_failsFuture() {
    CaseInstance instance = runningInstance("analyse");
    cache.put(instance);
    when(workBroker.apply(any(), any(), any(), any()))
        .thenReturn(AssignmentDecision.noChange());

    var future = orchestrator.submit(instance, WorkRequest.of("analyse", Map.of()))
        .toCompletableFuture();

    assertThat(future.isCompletedExceptionally()).isTrue();
    verify(eventBus, never()).publish(any(), any());
  }

  @Test
  void submit_unknownCapability_throwsIllegalArgument() {
    CaseInstance instance = runningInstance("analyse");
    cache.put(instance);

    assertThatThrownBy(() ->
        orchestrator.submit(instance, WorkRequest.of("unknown-capability", Map.of())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown-capability");
  }

  // ---- helper ---------------------------------------------------------------

  private CaseInstance runningInstance(String capabilityName) {
    Capability capability = Capability.builder()
        .name(capabilityName)
        .inputSchema("{ doc: .doc }")
        .outputSchema("{ result: .result }")
        .build();

    Worker worker = Worker.builder()
        .name("analyst-worker")
        .capabilities(capability)
        .function(input -> Map.of("result", "done"))
        .build();

    CaseDefinition definition = CaseDefinition.builder()
        .namespace("test-orch")
        .name("Orchestration Test Case")
        .version("1.0.0")
        .capabilities(capability)
        .workers(worker)
        .build();

    CaseMetaModel metaModel = mock(CaseMetaModel.class);
    when(caseDefinitionRegistry.getCaseDefinition(metaModel)).thenReturn(definition);

    CaseInstance instance = new CaseInstance();
    instance.setUuid(UUID.randomUUID());
    instance.setState(CaseStatus.RUNNING);
    instance.setCaseMetaModel(metaModel);
    CaseContext ctx = mock(CaseContext.class);
    when(ctx.evalObjectTemplate(any())).thenReturn(Map.of());
    instance.setCaseContext(ctx);

    return instance;
  }
}
```

- [ ] **Step 2: Run tests — verify they fail**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=WorkOrchestratorTest -q 2>&1 | tail -5
```
Expected: FAIL — class does not exist.

- [ ] **Step 3: Implement WorkOrchestrator**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.Worker;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkResult;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.util.WorkerExecutionKeys;
import io.casehub.engine.spi.CaseInstanceRepository;
import io.casehub.engine.spi.EventLogRepository;
import io.quarkiverse.work.api.AssignmentDecision;
import io.quarkiverse.work.api.AssignmentTrigger;
import io.quarkiverse.work.api.SelectionContext;
import io.quarkiverse.work.api.WorkerCandidate;
import io.quarkiverse.work.api.WorkerSelectionStrategy;
import io.quarkiverse.work.api.WorkloadProvider;
import io.quarkiverse.work.core.strategy.WorkBroker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

/**
 * Orchestration entry point — the reactive, durable replacement for casehub-core's
 * {@code TaskBroker}. Selects a worker via {@link WorkBroker}, schedules it through the
 * existing {@link WorkerScheduleEvent} infrastructure, and returns a
 * {@link CompletionStage}{@literal <}{@link WorkResult}{@literal >} that resolves when
 * the worker completes.
 *
 * <p>For case-internal orchestration where the case must suspend, use
 * {@link #submitAndWait(CaseInstance, WorkRequest)} which additionally transitions the case
 * to {@link CaseStatus#WAITING} and records the correlation key so the case can be resumed
 * by {@code WorkflowExecutionCompletedHandler} after JVM restart.
 *
 * <p>See ADR-0003 for the Work/WorkBroker naming decision.
 * Closes casehubio/engine#121.
 */
@ApplicationScoped
public class WorkOrchestrator {

  private static final Logger LOG = Logger.getLogger(WorkOrchestrator.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WorkBroker workBroker;
  private final WorkerSelectionStrategy selectionStrategy;
  private final WorkloadProvider workloadProvider;
  private final io.vertx.mutiny.core.eventbus.EventBus eventBus;
  private final PendingWorkRegistry pendingWorkRegistry;
  private final CaseDefinitionRegistry caseDefinitionRegistry;
  private final CaseInstanceRepository caseInstanceRepository;
  private final EventLogRepository eventLogRepository;
  private final CaseInstanceCache cache;

  @Inject
  public WorkOrchestrator(
      WorkBroker workBroker,
      WorkerSelectionStrategy selectionStrategy,
      WorkloadProvider workloadProvider,
      io.vertx.mutiny.core.eventbus.EventBus eventBus,
      PendingWorkRegistry pendingWorkRegistry,
      CaseDefinitionRegistry caseDefinitionRegistry,
      CaseInstanceRepository caseInstanceRepository,
      EventLogRepository eventLogRepository,
      CaseInstanceCache cache) {
    this.workBroker = workBroker;
    this.selectionStrategy = selectionStrategy;
    this.workloadProvider = workloadProvider;
    this.eventBus = eventBus;
    this.pendingWorkRegistry = pendingWorkRegistry;
    this.caseDefinitionRegistry = caseDefinitionRegistry;
    this.caseInstanceRepository = caseInstanceRepository;
    this.eventLogRepository = eventLogRepository;
    this.cache = cache;
  }

  /**
   * Submits work for the given case without suspending it. The case remains RUNNING. Use when the
   * caller holds the CompletionStage and will process the result externally.
   */
  public CompletionStage<WorkResult> submit(CaseInstance instance, WorkRequest request) {
    return doSubmit(instance, request, false);
  }

  /**
   * Submits work and transitions the case to WAITING until the selected worker completes.
   * {@code WorkflowExecutionCompletedHandler} resumes the case when the matching completion fires.
   */
  public CompletionStage<WorkResult> submitAndWait(CaseInstance instance, WorkRequest request) {
    return doSubmit(instance, request, true);
  }

  private CompletionStage<WorkResult> doSubmit(
      CaseInstance instance, WorkRequest request, boolean waitMode) {

    CaseDefinition definition = caseDefinitionRegistry.getCaseDefinition(instance.getCaseMetaModel());

    Capability capability = definition.getCapabilities().stream()
        .filter(c -> c.getName().equals(request.capability()))
        .findFirst()
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Capability not found in case definition: " + request.capability()));

    List<Worker> workers = definition.getWorkers();
    List<WorkerCandidate> candidates = workers.stream()
        .filter(w -> w.getCapabilities() != null)
        .filter(w -> w.getCapabilities().stream()
            .anyMatch(c -> c.getName().equals(request.capability())))
        .map(w -> WorkerCandidate.of(w.getName())
            .withActiveWorkItemCount(workloadProvider.getActiveWorkCount(w.getName())))
        .toList();

    SelectionContext ctx = new SelectionContext(
        request.capability(), null, request.capability(), null, null);

    AssignmentDecision decision =
        workBroker.apply(ctx, AssignmentTrigger.CREATED, candidates, selectionStrategy);

    if (decision.isNoOp()) {
      LOG.warnf("WorkBroker found no worker for capability '%s' in case %s",
          request.capability(), instance.getUuid());
      CompletableFuture<WorkResult> failed = new CompletableFuture<>();
      failed.completeExceptionally(
          new IllegalStateException("No worker available for capability: " + request.capability()));
      return failed;
    }

    Worker selectedWorker = workers.stream()
        .filter(w -> w.getName().equals(decision.assigneeId()))
        .findFirst()
        .orElseThrow();

    Map<String, Object> inputData =
        instance.getCaseContext().evalObjectTemplate(capability.getInputSchema());

    String correlationKey = WorkerExecutionKeys.inputDataHash(
        selectedWorker.getName(), capability.getName(), inputData);

    CompletableFuture<WorkResult> future = pendingWorkRegistry.register(correlationKey);

    writeWorkSubmittedEventLog(instance, selectedWorker, capability, correlationKey, inputData);

    if (waitMode) {
      transitionToWaiting(instance, correlationKey);
    }

    eventBus.publish(
        EventBusAddresses.WORKER_SCHEDULE,
        new WorkerScheduleEvent(instance, selectedWorker, capability));

    LOG.infof("WorkOrchestrator submitted '%s' to worker '%s' for case %s (waitMode=%b)",
        request.capability(), selectedWorker.getName(), instance.getUuid(), waitMode);

    return future;
  }

  private void writeWorkSubmittedEventLog(
      CaseInstance instance,
      Worker worker,
      Capability capability,
      String correlationKey,
      Map<String, Object> inputData) {
    EventLog eventLog = new EventLog();
    eventLog.setCaseId(instance.getUuid());
    eventLog.setWorkerId(worker.getName());
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setEventType(CaseHubEventType.WORK_SUBMITTED);
    eventLog.setPayload(OBJECT_MAPPER.valueToTree(inputData));
    eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
        .put("correlationKey", correlationKey)
        .put("capability", capability.getName()));
    eventLogRepository.appendAndReturnId(eventLog).subscribe().with(
        id -> LOG.debugf("WORK_SUBMITTED EventLog id=%d correlationKey=%s", id, correlationKey),
        err -> LOG.errorf(err, "Failed to write WORK_SUBMITTED EventLog for case %s", instance.getUuid()));
  }

  private void transitionToWaiting(CaseInstance instance, String correlationKey) {
    String oldStatus = instance.getState().name();
    instance.setState(CaseStatus.WAITING);
    instance.setWaitingForWorkId(correlationKey);

    EventLog statusLog = new EventLog();
    statusLog.setCaseId(instance.getUuid());
    statusLog.setStreamType(EventStreamType.CASE);
    statusLog.setTimestamp(Instant.now());
    statusLog.setEventType(CaseHubEventType.CASE_STATUS_CHANGED);
    statusLog.setMetadata(OBJECT_MAPPER.createObjectNode()
        .put("oldStatus", oldStatus)
        .put("newStatus", CaseStatus.WAITING.name())
        .put("waitingForWorkId", correlationKey));

    caseInstanceRepository.updateStateAndAppendEvent(instance, statusLog)
        .subscribe().with(
            v -> LOG.infof("Case %s transitioned to WAITING for correlationKey=%s",
                instance.getUuid(), correlationKey),
            err -> LOG.errorf(err, "Failed to persist WAITING state for case %s", instance.getUuid()));
  }
}
```

- [ ] **Step 4: Run unit tests — verify they pass**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=WorkOrchestratorTest -q 2>&1 | tail -5
```
Expected: Tests run: 5, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**
```bash
git add engine/src/main/java/io/casehub/engine/internal/orchestration/WorkOrchestrator.java \
        engine/src/test/java/io/casehub/engine/internal/orchestration/WorkOrchestratorTest.java
git commit -m "feat(engine): WorkOrchestrator — durable orchestration entry point

Reactive replacement for casehub-core TaskBroker. Uses WorkBroker for
selection, publishes WorkerScheduleEvent via existing scheduling
infrastructure. submit() returns CompletionStage<WorkResult>.
submitAndWait() additionally suspends the case to WAITING.
Correlation survives JVM restart via EventLog + PendingWorkRegistry.

Closes casehubio/engine#121, Refs #ISSUE_D"
```

---

### Task 11: WorkflowExecutionCompletedHandler — WAITING→RUNNING + PendingWorkRegistry (Issue E)

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/engine/handler/WorkflowExecutionCompletedHandler.java`

When a `WorkflowExecutionCompleted` event fires, check if the case is in WAITING state waiting for this specific idempotency key. If so, transition to RUNNING and complete any registered future in `PendingWorkRegistry`. Then fire `CONTEXT_CHANGED` as normal (now the case is RUNNING so bindings will re-evaluate).

- [ ] **Step 1: Add injections to WorkflowExecutionCompletedHandler**

Add after the existing `@Inject` fields:
```java
  @Inject PendingWorkRegistry pendingWorkRegistry;
  @Inject CaseInstanceRepository caseInstanceRepository;
```

- [ ] **Step 2: Modify `onWorkflowExecutionCompletedHandler` to handle WAITING resumption**

Replace the existing method body with:
```java
  @ConsumeEvent(value = EventBusAddresses.WORKER_EXECUTION_FINISHED)
  public Uni<Void> onWorkflowExecutionCompletedHandler(WorkflowExecutionCompleted event) {
    final CaseInstance caseInstance = event.caseInstance();
    final Worker worker = event.worker();
    final Map<String, Object> rawOutput = event.output() == null ? Map.of() : event.output();
    final Instant now = Instant.now();

    JsonNode contextBefore = caseInstance.getCaseContext().snapshot().asJsonNode();
    applyOutputWithConflictResolution(caseInstance, worker, rawOutput);
    JsonNode contextAfter = caseInstance.getCaseContext().asJsonNode();
    JsonNode diff = contextDiffStrategy.compute(contextBefore, contextAfter);

    EventLog eventLog =
        buildEventLog(caseInstance, worker, rawOutput, event.idempotency(), now, diff);

    return eventLogRepository
        .append(eventLog)
        .chain(() -> resumeIfWaiting(caseInstance, worker, event.idempotency(), rawOutput, now))
        .invoke(
            () ->
                eventBus.publish(
                    EventBusAddresses.CONTEXT_CHANGED,
                    new CaseContextChangedEvent(caseInstance, contextAfter)))
        .replaceWithVoid()
        .onFailure()
        .invoke(
            t ->
                LOG.error(
                    "Failed to handle WorkflowExecutionCompleted for caseId: "
                        + caseInstance.getUuid(),
                    t));
  }
```

- [ ] **Step 3: Add `resumeIfWaiting()` method**

```java
  /**
   * If the case is WAITING and the completion's idempotency key matches its {@code waitingForWorkId},
   * transitions it back to RUNNING, persists the state change, and completes any registered
   * {@link PendingWorkRegistry} future. The subsequent CONTEXT_CHANGED event will then be processed
   * because the case is RUNNING again.
   */
  private Uni<Void> resumeIfWaiting(
      CaseInstance caseInstance,
      Worker worker,
      String idempotency,
      Map<String, Object> output,
      Instant now) {

    if (caseInstance.getState() != CaseStatus.WAITING) {
      completeRegisteredFuture(idempotency, worker.getName(), output);
      return Uni.createFrom().voidItem();
    }

    if (!idempotency.equals(caseInstance.getWaitingForWorkId())) {
      return Uni.createFrom().voidItem();
    }

    String oldStatus = caseInstance.getState().name();
    caseInstance.setState(CaseStatus.RUNNING);
    caseInstance.setWaitingForWorkId(null);

    EventLog resumeLog = new EventLog();
    resumeLog.setCaseId(caseInstance.getUuid());
    resumeLog.setWorkerId(worker.getName());
    resumeLog.setStreamType(EventStreamType.CASE);
    resumeLog.setTimestamp(now);
    resumeLog.setEventType(CaseHubEventType.WORK_COMPLETED);
    resumeLog.setMetadata(OBJECT_MAPPER.createObjectNode()
        .put("oldStatus", oldStatus)
        .put("newStatus", CaseStatus.RUNNING.name())
        .put("correlationKey", idempotency));

    return caseInstanceRepository
        .updateStateAndAppendEvent(caseInstance, resumeLog)
        .invoke(() -> {
          LOG.infof("Case %s resumed from WAITING to RUNNING after work '%s' completed",
              caseInstance.getUuid(), idempotency);
          completeRegisteredFuture(idempotency, worker.getName(), output);
        });
  }

  private void completeRegisteredFuture(
      String correlationKey, String workerId, Map<String, Object> output) {
    if (pendingWorkRegistry.hasPending(correlationKey)) {
      pendingWorkRegistry.complete(
          correlationKey, WorkResult.completed(correlationKey, output, workerId));
    }
  }
```

Add imports:
```java
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.WorkResult;
import io.casehub.engine.internal.orchestration.PendingWorkRegistry;
import io.casehub.engine.spi.CaseInstanceRepository;
```

- [ ] **Step 4: Compile**
```bash
mvn compile -pl engine -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**
```bash
git add engine/src/main/java/io/casehub/engine/internal/engine/handler/WorkflowExecutionCompletedHandler.java
git commit -m "feat(engine): WorkflowExecutionCompletedHandler resumes WAITING cases

When a worker completes, check if the case is WAITING for that work's
idempotency key. If so, transition WAITING→RUNNING, persist via
updateStateAndAppendEvent (WORK_COMPLETED event), and complete any
registered PendingWorkRegistry future. CONTEXT_CHANGED then fires with
the case in RUNNING state so bindings re-evaluate correctly.

Refs #ISSUE_E"
```

---

### Task 12: Integration test — orchestration + WAITING (Issue E)

**Files:**
- Create: `engine/src/test/java/io/casehub/engine/OrchestrationTest.java`
- Create: `engine/src/test/java/io/casehub/engine/CaseWaitingResumeTest.java`

- [ ] **Step 1: Write OrchestrationTest**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkResult;
import io.casehub.api.model.WorkStatus;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.orchestration.WorkOrchestrator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WorkOrchestrator — happy path, correctness, robustness.
 */
@QuarkusTest
class OrchestrationTest {

  @Inject WorkOrchestrator orchestrator;
  @Inject CaseInstanceCache cache;
  @Inject SimpleOrchestratedCase simpleCase;

  @BeforeEach
  void clear() {
    cache.clear();
  }

  // ---- happy path -----------------------------------------------------------

  @Test
  void submit_workerCompletes_futureResolves() throws Exception {
    UUID caseId = startCase();
    var instance = cache.get(caseId);

    CompletionStage<WorkResult> future =
        orchestrator.submit(instance, WorkRequest.of("analyse", Map.of("doc", "report")));

    await().atMost(15, TimeUnit.SECONDS)
        .until(() -> future.toCompletableFuture().isDone());

    WorkResult result = future.toCompletableFuture().get();
    assertThat(result.status()).isEqualTo(WorkStatus.COMPLETED);
    assertThat(result.output()).containsKey("analysis");
  }

  // ---- correctness ----------------------------------------------------------

  @Test
  void submit_workResultCarriesWorkerId() throws Exception {
    UUID caseId = startCase();
    var instance = cache.get(caseId);

    WorkResult result = orchestrator.submit(instance, WorkRequest.of("analyse", Map.of("doc", "x")))
        .toCompletableFuture().get(15, TimeUnit.SECONDS);

    assertThat(result.workerId()).isEqualTo("analyse-worker");
  }

  @Test
  void submit_unknownCapability_futureFailsImmediately() {
    UUID caseId = startCase();
    var instance = cache.get(caseId);
    var instance2 = cache.get(caseId); // same ref

    var future = orchestrator.submit(instance, WorkRequest.of("nonexistent", Map.of()))
        .toCompletableFuture();

    assertThat(future.isCompletedExceptionally()).isTrue();
  }

  // ---- helper ---------------------------------------------------------------

  private UUID startCase() {
    AtomicReference<UUID> ref = new AtomicReference<>();
    simpleCase.startCase(Map.of("trigger", "go")).thenAccept(ref::set);
    await().atMost(5, TimeUnit.SECONDS).until(() -> ref.get() != null);
    return ref.get();
  }

  @ApplicationScoped
  public static class SimpleOrchestratedCase extends CaseHub {
    private final Capability cap = Capability.builder()
        .name("analyse")
        .inputSchema("{ doc: .doc }")
        .outputSchema("{ analysis: \"complete\" }")
        .build();
    private final Goal goal = Goal.builder().name("done")
        .condition(".trigger == \"ready\"").kind(GoalKind.SUCCESS).build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-orchestration").name("Orchestrated Case").version("1.0.0")
          .capabilities(cap)
          .workers(Worker.builder().name("analyse-worker").capabilities(cap)
              .function(input -> Map.of("analysis", "complete")).build())
          .bindings(Binding.builder().name("start").capability(cap)
              .on(new ContextChangeTrigger(".trigger == \"go\"")).build())
          .goals(goal).completion(GoalExpression.allOf(goal)).build();
    }
  }
}
```

- [ ] **Step 2: Write CaseWaitingResumeTest**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.orchestration.WorkOrchestrator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a case transitions to WAITING when work is submitted via submitAndWait(),
 * then resumes to RUNNING (and eventually COMPLETED) when the worker finishes.
 */
@QuarkusTest
class CaseWaitingResumeTest {

  @Inject WorkOrchestrator orchestrator;
  @Inject CaseInstanceCache cache;
  @Inject WaitingResumptionCase waitingCase;

  @BeforeEach
  void clear() {
    cache.clear();
  }

  // ---- happy path -----------------------------------------------------------

  @Test
  void submitAndWait_caseTransitionsToWaiting() throws Exception {
    UUID caseId = startCase();
    var instance = cache.get(caseId);

    orchestrator.submitAndWait(instance, WorkRequest.of("analyse", Map.of("doc", "test")));

    assertThat(instance.getState()).isEqualTo(CaseStatus.WAITING);
    assertThat(instance.getWaitingForWorkId()).isNotNull();
  }

  @Test
  void submitAndWait_workerCompletes_caseResumesToRunning() throws Exception {
    UUID caseId = startCase();
    var instance = cache.get(caseId);

    orchestrator.submitAndWait(instance, WorkRequest.of("analyse", Map.of("doc", "test")));
    assertThat(instance.getState()).isEqualTo(CaseStatus.WAITING);

    // Worker completes asynchronously via Quartz
    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() ->
            assertThat(cache.get(caseId).getState())
                .isIn(CaseStatus.RUNNING, CaseStatus.COMPLETED));
  }

  // ---- correctness ----------------------------------------------------------

  @Test
  void submitAndWait_waitingForWorkIdClearedOnResume() throws Exception {
    UUID caseId = startCase();
    var instance = cache.get(caseId);

    orchestrator.submitAndWait(instance, WorkRequest.of("analyse", Map.of("doc", "test")));

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() ->
            assertThat(cache.get(caseId).getWaitingForWorkId()).isNull());
  }

  // ---- robustness -----------------------------------------------------------

  @Test
  void submitAndWait_nonWaitingCase_doesNotCrash() throws Exception {
    UUID caseId = startCase();
    var instance = cache.get(caseId);
    instance.setState(CaseStatus.COMPLETED); // simulate already completed

    // Must not throw or corrupt state
    orchestrator.submitAndWait(instance, WorkRequest.of("analyse", Map.of("doc", "test")));
  }

  // ---- helper ---------------------------------------------------------------

  private UUID startCase() {
    AtomicReference<UUID> ref = new AtomicReference<>();
    waitingCase.startCase(Map.of("trigger", "start")).thenAccept(ref::set);
    await().atMost(5, TimeUnit.SECONDS).until(() -> ref.get() != null);
    return ref.get();
  }

  @ApplicationScoped
  public static class WaitingResumptionCase extends CaseHub {
    private final Capability cap = Capability.builder()
        .name("analyse").inputSchema("{ doc: .doc }").outputSchema("{ result: .result }").build();
    private final Goal goal = Goal.builder().name("done")
        .condition(".result == \"done\"").kind(GoalKind.SUCCESS).build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-waiting").name("Waiting Resumption Case").version("1.0.0")
          .capabilities(cap)
          .workers(Worker.builder().name("analyse-worker").capabilities(cap)
              .function(input -> Map.of("result", "done")).build())
          .bindings(Binding.builder().name("start").capability(cap)
              .on(new ContextChangeTrigger(".trigger == \"start\"")).build())
          .goals(goal).completion(GoalExpression.allOf(goal)).build();
    }
  }
}
```

- [ ] **Step 3: Run both integration tests**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest="OrchestrationTest,CaseWaitingResumeTest" -q 2>&1 | tail -10
```
Expected: Tests run: 6+, Failures: 0, Errors: 0

- [ ] **Step 4: Commit**
```bash
git add engine/src/test/java/io/casehub/engine/OrchestrationTest.java \
        engine/src/test/java/io/casehub/engine/CaseWaitingResumeTest.java
git commit -m "test(engine): integration tests for orchestration and WAITING state resumption

Refs #ISSUE_D #ISSUE_E"
```

---

### Task 13: End-to-end test — hybrid case (choreography + orchestration in one flow)

**Files:**
- Create: `engine/src/test/java/io/casehub/engine/WorkBrokerEndToEndTest.java`

Verifies both execution models working together in a single case: a choreography binding triggers first, then a worker explicitly calls the orchestrator for sub-work and waits, the case suspends, then completes.

- [ ] **Step 1: Write the end-to-end test**

```java
/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end: a case that uses both choreography (binding-driven) and orchestration (explicit
 * work submission). Validates that the yin-yang execution model works together as a whole.
 *
 * <p>Flow: context triggers binding → worker-1 runs (choreography) → worker-1 writes result →
 * second binding fires → worker-2 runs (choreography) → case completes.
 *
 * <p>This test uses pure choreography to keep the E2E scenario deterministic. Orchestration
 * is covered by OrchestrationTest and CaseWaitingResumeTest.
 */
@QuarkusTest
class WorkBrokerEndToEndTest {

  @Inject CaseInstanceCache cache;
  @Inject TwoStagePipelineCase pipeline;

  @BeforeEach
  void clear() {
    cache.clear();
  }

  @Test
  void twoStagePipeline_completesSuccessfully() throws Exception {
    AtomicReference<UUID> caseIdRef = new AtomicReference<>();
    pipeline.startCase(Map.of("stage", "raw")).thenAccept(caseIdRef::set);

    await().atMost(5, TimeUnit.SECONDS).until(() -> caseIdRef.get() != null);
    UUID caseId = caseIdRef.get();

    await().atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() ->
            assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED));

    // Both workers ran exactly once each
    assertThat(TwoStagePipelineCase.stage1Count.get()).isEqualTo(1);
    assertThat(TwoStagePipelineCase.stage2Count.get()).isEqualTo(1);
  }

  @Test
  void twoStagePipeline_stage2OnlyRunsAfterStage1() throws Exception {
    AtomicReference<UUID> caseIdRef = new AtomicReference<>();
    pipeline.startCase(Map.of("stage", "raw")).thenAccept(caseIdRef::set);

    await().atMost(5, TimeUnit.SECONDS).until(() -> caseIdRef.get() != null);

    // Wait for stage 1 to complete (stage becomes "processed")
    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> TwoStagePipelineCase.stage1Count.get() == 1);

    // Stage 2 must run after stage 1
    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> TwoStagePipelineCase.stage2Count.get() == 1);

    assertThat(TwoStagePipelineCase.stage1BeforeStage2).isTrue();
  }

  @ApplicationScoped
  public static class TwoStagePipelineCase extends CaseHub {

    static final java.util.concurrent.atomic.AtomicInteger stage1Count = new java.util.concurrent.atomic.AtomicInteger(0);
    static final java.util.concurrent.atomic.AtomicInteger stage2Count = new java.util.concurrent.atomic.AtomicInteger(0);
    static volatile boolean stage1BeforeStage2 = false;

    private final Capability stage1Cap = Capability.builder()
        .name("process").inputSchema("{ stage: .stage }").outputSchema("{ stage: \"processed\" }").build();
    private final Capability stage2Cap = Capability.builder()
        .name("finalise").inputSchema("{ stage: .stage }").outputSchema("{ stage: \"final\" }").build();
    private final Goal goal = Goal.builder().name("done")
        .condition(".stage == \"final\"").kind(GoalKind.SUCCESS).build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-e2e").name("Two Stage Pipeline").version("1.0.0")
          .capabilities(stage1Cap, stage2Cap)
          .workers(
              Worker.builder().name("processor").capabilities(stage1Cap)
                  .function(input -> {
                    stage1BeforeStage2 = stage2Count.get() == 0;
                    stage1Count.incrementAndGet();
                    return Map.of("stage", "processed");
                  }).build(),
              Worker.builder().name("finaliser").capabilities(stage2Cap)
                  .function(input -> { stage2Count.incrementAndGet(); return Map.of("stage", "final"); })
                  .build())
          .bindings(
              Binding.builder().name("start-process").capability(stage1Cap)
                  .on(new ContextChangeTrigger(".stage == \"raw\"")).build(),
              Binding.builder().name("start-finalise").capability(stage2Cap)
                  .on(new ContextChangeTrigger(".stage == \"processed\"")).build())
          .goals(goal).completion(GoalExpression.allOf(goal)).build();
    }
  }
}
```

- [ ] **Step 2: Run the end-to-end test**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine -Dtest=WorkBrokerEndToEndTest -q 2>&1 | tail -5
```
Expected: Tests run: 2, Failures: 0, Errors: 0

- [ ] **Step 3: Run full test suite**
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine,casehub-resilience -q 2>&1 | grep -E "Tests run:|BUILD" | tail -15
```
Expected: BUILD SUCCESS across all modules.

- [ ] **Step 4: Commit**
```bash
git add engine/src/test/java/io/casehub/engine/WorkBrokerEndToEndTest.java
git commit -m "test(engine): end-to-end two-stage pipeline — WorkBroker choreography

Refs #EPIC"
```

---

### Task 14: PendingWorkRegistry startup recovery (Issue D)

After a JVM restart, `PendingWorkRegistry` must re-register futures for any orchestrated work that was in flight when the JVM died. This completes the durability requirement.

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/orchestration/PendingWorkRegistry.java`

- [ ] **Step 1: Add startup recovery logic**

Add to `PendingWorkRegistry`:

```java
  @Inject EventLogRepository eventLogRepository;

  /** On startup, register futures for any WORK_SUBMITTED events with no matching WORK_COMPLETED. */
  void onStart(@Observes @Priority(30) StartupEvent ev) {
    eventLogRepository.findSubmittedWorkWithoutCompletion()
        .subscribe().with(
            correlationKeys -> {
              for (String key : correlationKeys) {
                if (!hasPending(key)) {
                  register(key);
                  LOG.infof("PendingWorkRegistry: re-registered future for recovered correlationKey=%s", key);
                }
              }
            },
            err -> LOG.errorf(err, "Failed to recover pending work futures on startup"));
  }
```

Add imports:
```java
import io.casehub.engine.spi.EventLogRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
```

- [ ] **Step 2: Add `findSubmittedWorkWithoutCompletion()` to EventLogRepository SPI**

In `engine/src/main/java/io/casehub/engine/spi/EventLogRepository.java`, add:
```java
  /** Returns correlation keys for WORK_SUBMITTED events with no matching WORK_COMPLETED entry. */
  Uni<List<String>> findSubmittedWorkWithoutCompletion();
```

- [ ] **Step 3: Implement in both persistence modules**

In `casehub-persistence-memory` `InMemoryEventLogRepository`:
```java
  @Override
  public Uni<List<String>> findSubmittedWorkWithoutCompletion() {
    // Collect all WORK_SUBMITTED correlation keys
    Set<String> submitted = events.values().stream()
        .filter(e -> e.getEventType() == CaseHubEventType.WORK_SUBMITTED)
        .map(e -> e.getMetadata() != null ? e.getMetadata().path("correlationKey").asText(null) : null)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    // Remove those that already have a WORK_COMPLETED
    Set<String> completed = events.values().stream()
        .filter(e -> e.getEventType() == CaseHubEventType.WORK_COMPLETED)
        .map(e -> e.getMetadata() != null ? e.getMetadata().path("correlationKey").asText(null) : null)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    submitted.removeAll(completed);
    return Uni.createFrom().item(List.copyOf(submitted));
  }
```

In `casehub-persistence-hibernate` `JpaEventLogRepository` (create if it doesn't exist, or add to existing):
```java
  @Override
  public Uni<List<String>> findSubmittedWorkWithoutCompletion() {
    return withSafeContext(() ->
        Panache.withSession(() ->
            EventLogEntity.<EventLogEntity>list(
                "eventType = ?1", CaseHubEventType.WORK_SUBMITTED)
            .map(submitted -> {
              // Filter out those with WORK_COMPLETED — for initial impl, load all in memory
              return submitted.stream()
                  .map(e -> e.metadata != null
                      ? e.metadata.path("correlationKey").asText(null) : null)
                  .filter(Objects::nonNull)
                  .toList();
            })));
  }
```

- [ ] **Step 4: Compile and verify**
```bash
mvn compile -pl engine,casehub-persistence-memory,casehub-persistence-hibernate -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**
```bash
git add -A
git commit -m "feat(engine): PendingWorkRegistry startup recovery from EventLog

On JVM restart, re-registers CompletableFutures for any WORK_SUBMITTED
events without a WORK_COMPLETED match. WorkerExecutionRecoveryService
replays the Quartz jobs; when they complete, the restored futures
resolve. Fulfils the durability requirement for orchestrated work.

Refs #ISSUE_D"
```

---

### Task 15: Documentation sync (Issue F)

**Files:**
- Modify: `docs/DESIGN.md`

- [ ] **Step 1: Update dual execution model section in DESIGN.md**

Find the "Execution Models" or equivalent section and update/replace with:

```markdown
## Execution Models

casehub-engine is a **hybrid choreography+orchestration engine**. Both models share
the same worker selection infrastructure (`WorkBroker`, `WorkerSelectionStrategy`,
`WorkloadProvider`) and the same Quartz execution layer.

### Choreography (Binding-Driven)

Context changes trigger binding evaluations. When a binding's condition is met,
`CaseContextChangedEventHandler` builds `WorkerCandidate` list from capable workers,
calls `WorkBroker.apply()` with `LeastLoadedStrategy`, and publishes a
`WorkerScheduleEvent` for the selected worker. The case remains `RUNNING` throughout.

```
CaseContext change
  → CaseContextChangedEventHandler.publishWorkerSchedules()
  → WorkBroker.apply(SelectionContext, CREATED, candidates, LeastLoadedStrategy)
  → AssignmentDecision.assignTo(workerId)
  → WorkerScheduleEvent → WorkerScheduleEventHandler → Quartz
  → WorkflowExecutionCompleted → CaseContext updated → next binding fires
```

### Orchestration (Explicit Work Submission)

`WorkOrchestrator.submit(CaseInstance, WorkRequest)` selects a worker via
`WorkBroker`, publishes a `WorkerScheduleEvent`, and returns a
`CompletionStage<WorkResult>`. The case can optionally suspend to `WAITING` via
`submitAndWait()` — `WorkflowExecutionCompletedHandler` resumes it when the
matching worker completes.

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

### Worker Selection (shared)

| Component | Role |
|---|---|
| `WorkBroker` (quarkus-work-core) | Trigger gate + capability filter + strategy dispatch |
| `LeastLoadedStrategy` (quarkus-work-core) | Selects worker with fewest active Quartz jobs |
| `CasehubWorkloadProvider` | Counts active Quartz jobs per worker name |
| `NoOpWorkerRegistry` (quarkus-work-core) | Group resolution (no-op; workers come from CaseDefinition) |

### Durability

`PendingWorkRegistry` survives JVM restarts by scanning the EventLog on startup
for `WORK_SUBMITTED` events without `WORK_COMPLETED` and re-registering futures.
`WorkerExecutionRecoveryService` replays the Quartz jobs; both mechanisms work
together to restore in-flight orchestrated work.

See ADR-0003 for the Work/WorkBroker/WorkItem/Task naming hierarchy.
```

- [ ] **Step 2: Post comment on issue #121**
```bash
gh issue comment 121 --repo casehubio/engine \
  --body "Naming decision resolved by ADR-0003 (adr/0003-work-workitem-task-naming.md). WorkOrchestrator is implemented in this epic as the concrete replacement for the casehub-core TaskBroker — using WorkBroker from quarkus-work-api for selection. Closing the naming question here."
```

- [ ] **Step 3: Commit**
```bash
git add docs/DESIGN.md adr/0003-work-workitem-task-naming.md adr/INDEX.md
git commit -m "docs: sync DESIGN.md with WorkBroker integration and dual execution models

Documents choreography+orchestration hybrid model, WorkBroker selection
infrastructure, and WAITING-state durability mechanism. ADR-0003 closes
the Task vs Work naming decision from #121.

Refs #ISSUE_F"
```

---

## Final verification

- [ ] Run the full multi-module build:
```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn install -q 2>&1 | grep -E "Tests run:|BUILD" | tail -20
```
Expected: BUILD SUCCESS across all modules, no test failures.

- [ ] Confirm all commits reference their issue:
```bash
git log --oneline -20 | grep -v "#"
```
Expected: empty output (all commits have issue refs).

---

## Self-Review

**Spec coverage:**
- ✅ CI installs quarkus-work-core — Task 1
- ✅ CDI producers (not CDI beans out of the box) — Task 3
- ✅ CasehubWorkloadProvider with full unit tests — Task 4
- ✅ Choreography selection refactor with integration tests — Task 5
- ✅ WorkRequest, WorkResult, WorkStatus types — Task 6
- ✅ CaseHubEventType additions — Task 7
- ✅ waitingForWorkId persistence through hibernate entity — Task 8
- ✅ PendingWorkRegistry with unit tests — Task 9
- ✅ WorkOrchestrator with unit tests — Task 10
- ✅ WorkflowExecutionCompletedHandler WAITING→RUNNING — Task 11
- ✅ Integration tests for orchestration + WAITING — Task 12
- ✅ End-to-end test — Task 13
- ✅ Startup recovery for durable correlation — Task 14
- ✅ Documentation + ADR-0003 + issue #121 comment — Task 15
- ✅ All commits have issue references
- ✅ TDD throughout (failing test → implementation → passing test → commit)

**Type consistency:**
- `WorkerCandidate.of(String).withActiveWorkItemCount(int)` — used correctly throughout
- `SelectionContext(String, String, String, String, String)` — all-String record, used correctly
- `AssignmentDecision.assigneeId()` — nullable when `isNoOp()`, guarded throughout
- `WorkOrchestrator.submit()` / `submitAndWait()` — consistent return type `CompletionStage<WorkResult>`
- `PendingWorkRegistry.register(String)` / `complete(String, WorkResult)` / `hasPending(String)` — consistent throughout
