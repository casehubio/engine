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
package io.casehub.blackboard.it;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Worker;
import io.casehub.blackboard.event.PlanItemCompletedEvent;
import io.casehub.blackboard.plan.PlanItem.PlanItemStatus;
import io.casehub.blackboard.registry.BlackboardRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * R4: Two bindings (different capabilities) fire on independent triggers. Both workers complete
 * exactly once. Verifies coexistence of multiple concurrent PlanItems. See casehubio/engine#76.
 *
 * <p>Design: each binding has its own disjoint trigger key ({@code phaseA} / {@code phaseB}) so the
 * workers self-falsify their own trigger condition. This prevents re-trigger loops — each worker
 * runs exactly once and reaches COMPLETED.
 *
 * <p>Test strategy: event-driven via {@link PlanItemCompletedEvent} CDI async events. The {@link
 * WorkerCompletionObserver} holds a race-free {@code CompletableFuture} per worker using {@code
 * ConcurrentHashMap.computeIfAbsent} — safe regardless of whether the event fires before or after
 * the test registers the future.
 */
@QuarkusTest
class MixedWorkersBlackboardTest {

  @Inject BlackboardRegistry registry;
  @Inject MixedCaseBean mixedCase;
  @Inject WorkerCompletionObserver observer;

  @Test
  void two_workers_with_different_capabilities_both_complete() throws Exception {
    UUID caseId =
        mixedCase
            .startCase(Map.of("phaseA", "start", "phaseB", "start"))
            .toCompletableFuture()
            .join();

    // Register completion futures — race-free: if events already fired, futures are already done.
    CompletableFuture<Void> aFuture = observer.awaitWorker(caseId, "worker-a");
    CompletableFuture<Void> bFuture = observer.awaitWorker(caseId, "worker-b");

    CompletableFuture.allOf(aFuture, bFuture).get(30, TimeUnit.SECONDS);

    // Verify registry state (guaranteed by the time events fired — events fire after markCompleted)
    var plan = registry.get(caseId);
    assertThat(plan).isPresent();

    var itemAId = registry.getPlanItemId(caseId, "worker-a");
    var itemBId = registry.getPlanItemId(caseId, "worker-b");

    assertThat(itemAId).as("worker-a must be indexed").isPresent();
    assertThat(itemBId).as("worker-b must be indexed").isPresent();

    assertThat(plan.get().getPlanItem(itemAId.get()).map(i -> i.getStatus()))
        .as("worker-a PlanItem must be COMPLETED")
        .contains(PlanItemStatus.COMPLETED);
    assertThat(plan.get().getPlanItem(itemBId.get()).map(i -> i.getStatus()))
        .as("worker-b PlanItem must be COMPLETED")
        .contains(PlanItemStatus.COMPLETED);
  }

  /**
   * Observes {@link PlanItemCompletedEvent} and signals per-worker {@link CompletableFuture}s.
   * Race-free: {@code computeIfAbsent} ensures the future exists whether the event fires before or
   * after the test calls {@link #awaitWorker}.
   */
  @ApplicationScoped
  public static class WorkerCompletionObserver {

    private final ConcurrentHashMap<String, CompletableFuture<Void>> futures =
        new ConcurrentHashMap<>();

    void onPlanItemCompleted(@ObservesAsync PlanItemCompletedEvent event) {
      futures
          .computeIfAbsent(key(event.caseId(), event.workerName()), k -> new CompletableFuture<>())
          .complete(null);
    }

    /**
     * Returns a future that completes when the given worker's PlanItem is COMPLETED. Safe to call
     * before or after the event fires.
     */
    public CompletableFuture<Void> awaitWorker(UUID caseId, String workerName) {
      return futures.computeIfAbsent(key(caseId, workerName), k -> new CompletableFuture<>());
    }

    private static String key(UUID caseId, String workerName) {
      return caseId + ":" + workerName;
    }
  }

  /**
   * Case with two distinct capabilities and bindings. Each binding fires on its own disjoint
   * trigger key so each worker self-falsifies its own trigger condition (phaseA "start" → "done";
   * phaseB "start" → "done"). Workers complete exactly once with no re-trigger.
   */
  @ApplicationScoped
  public static class MixedCaseBean extends CaseHub {

    private final Capability capA =
        Capability.builder()
            .name("cap-a")
            .inputSchema("{ phaseA: .phaseA }")
            .outputSchema("{ phaseA: .phaseA }")
            .build();

    private final Capability capB =
        Capability.builder()
            .name("cap-b")
            .inputSchema("{ phaseB: .phaseB }")
            .outputSchema("{ phaseB: .phaseB }")
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("blackboard-it")
          .name("Mixed Workers Case")
          .version("1.0.0")
          .capabilities(capA, capB)
          .workers(
              Worker.builder()
                  .name("worker-a")
                  .capabilities(capA)
                  .function(input -> Map.of("phaseA", "done"))
                  .build(),
              Worker.builder()
                  .name("worker-b")
                  .capabilities(capB)
                  .function(input -> Map.of("phaseB", "done"))
                  .build())
          .bindings(
              Binding.builder()
                  .name("trigger-a")
                  .capability(capA)
                  .on(new ContextChangeTrigger(".phaseA == \"start\""))
                  .build(),
              Binding.builder()
                  .name("trigger-b")
                  .capability(capB)
                  .on(new ContextChangeTrigger(".phaseB == \"start\""))
                  .build())
          .build();
    }
  }
}
