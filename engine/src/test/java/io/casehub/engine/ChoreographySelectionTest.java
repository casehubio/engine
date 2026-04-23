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
 * capability. Previously, all capable workers were scheduled — this is the regression this test
 * guards against.
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
    twoWorkerCase.startCase(Map.of("trigger", "go")).thenAccept(caseIdRef::set);

    await().atMost(5, TimeUnit.SECONDS).until(() -> caseIdRef.get() != null);
    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(cache.get(caseIdRef.get()).getState()).isEqualTo(CaseStatus.COMPLETED));

    int totalCalls =
        TwoWorkerSameCapabilityCase.workerACount.get()
            + TwoWorkerSameCapabilityCase.workerBCount.get();
    assertThat(totalCalls).as("WorkBroker must select exactly one worker, not both").isEqualTo(1);
  }

  /** Correctness: two sequential cases each invoke exactly one worker. */
  @Test
  void twoSequentialCases_eachSelectsExactlyOneWorker() throws Exception {
    for (int i = 0; i < 2; i++) {
      // Measure delta — don't reset counters, which would race with async completions
      int countBefore =
          TwoWorkerSameCapabilityCase.workerACount.get()
              + TwoWorkerSameCapabilityCase.workerBCount.get();
      cache.clear();

      AtomicReference<UUID> caseIdRef = new AtomicReference<>();
      twoWorkerCase.startCase(Map.of("trigger", "go")).thenAccept(caseIdRef::set);

      await().atMost(5, TimeUnit.SECONDS).until(() -> caseIdRef.get() != null);
      await()
          .atMost(15, TimeUnit.SECONDS)
          .untilAsserted(
              () ->
                  assertThat(cache.get(caseIdRef.get()).getState())
                      .isEqualTo(CaseStatus.COMPLETED));

      int calls =
          TwoWorkerSameCapabilityCase.workerACount.get()
              + TwoWorkerSameCapabilityCase.workerBCount.get()
              - countBefore;
      assertThat(calls).as("Case %d: exactly one worker must run", i).isEqualTo(1);
    }
  }

  @ApplicationScoped
  public static class TwoWorkerSameCapabilityCase extends CaseHub {

    static final AtomicInteger workerACount = new AtomicInteger(0);
    static final AtomicInteger workerBCount = new AtomicInteger(0);

    private final Capability capability =
        Capability.builder()
            .name("do-work")
            .inputSchema("{ trigger: .trigger }")
            .outputSchema("{ result: \"done\" }")
            .build();

    private final Goal goal =
        Goal.builder().name("done").condition(".result == \"done\"").kind(GoalKind.SUCCESS).build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-selection")
          .name("Two Worker Selection Case")
          .version("1.0.0")
          .capabilities(capability)
          .workers(
              Worker.builder()
                  .name("worker-a")
                  .capabilities(capability)
                  .function(
                      input -> {
                        workerACount.incrementAndGet();
                        return Map.of("result", "done");
                      })
                  .build(),
              Worker.builder()
                  .name("worker-b")
                  .capabilities(capability)
                  .function(
                      input -> {
                        workerBCount.incrementAndGet();
                        return Map.of("result", "done");
                      })
                  .build())
          .bindings(
              Binding.builder()
                  .name("trigger")
                  .capability(capability)
                  .on(new ContextChangeTrigger(".trigger == \"go\""))
                  .build())
          .goals(goal)
          .completion(GoalExpression.allOf(goal))
          .build();
    }
  }
}
