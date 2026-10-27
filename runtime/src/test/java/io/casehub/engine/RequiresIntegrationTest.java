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
import io.casehub.api.model.BindingRequires;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RequiresIntegrationTest {

  @Inject QuorumCaseHub quorumCaseHub;
  @Inject CaseInstanceCache caseInstanceCache;

  @Test
  void aggregatorDispatchesAfterQuorumMet() {
    UUID caseId = quorumCaseHub.startCase(Map.of("status", "go"));

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(caseId);
              assertThat(instance).isNotNull();
              assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
            });
  }

  @ApplicationScoped
  public static class QuorumCaseHub extends CaseHub {

    private final Capability capA =
        Capability.builder().name("step-a").inputSchema(".").outputSchema(".").build();
    private final Capability capB =
        Capability.builder().name("step-b").inputSchema(".").outputSchema(".").build();
    private final Capability capC =
        Capability.builder().name("step-c").inputSchema(".").outputSchema(".").build();

    private final Goal successGoal =
        Goal.builder().name("done").condition(".aggregated == true").kind(GoalKind.SUCCESS).build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-requires")
          .name("Quorum Case")
          .version("1.0.0")
          .capabilities(capA, capB, capC)
          .worker(
              Worker.builder()
                  .name("step-a")
                  .capabilityName("step-a")
                  .function(
                      new WorkerFunction.Sync<>(
                          Map.class,
                          Map.class,
                          (input, scope) -> WorkerResult.of(Map.of("a_done", true))))
                  .build())
          .worker(
              Worker.builder()
                  .name("step-b")
                  .capabilityName("step-b")
                  .function(
                      new WorkerFunction.Sync<>(
                          Map.class,
                          Map.class,
                          (input, scope) -> WorkerResult.of(Map.of("b_done", true))))
                  .build())
          .worker(
              Worker.builder()
                  .name("step-c")
                  .capabilityName("step-c")
                  .function(
                      new WorkerFunction.Sync<>(
                          Map.class,
                          Map.class,
                          (input, scope) -> WorkerResult.of(Map.of("aggregated", true))))
                  .build())
          .binding(
              Binding.builder()
                  .name("step-a")
                  .capability(capA)
                  .on(new io.casehub.api.model.ContextChangeTrigger(".status == \"go\""))
                  .build())
          .binding(
              Binding.builder()
                  .name("step-b")
                  .capability(capB)
                  .on(new io.casehub.api.model.ContextChangeTrigger(".status == \"go\""))
                  .build())
          .binding(
              Binding.builder()
                  .name("step-c")
                  .capability(capC)
                  .on(new io.casehub.api.model.ContextChangeTrigger(".status == \"go\""))
                  .requires(
                      new BindingRequires(List.of("step-a", "step-b"), 2, OnThresholdReached.KEEP))
                  .build())
          .goals(successGoal)
          .completion(GoalExpression.allOf(successGoal))
          .build();
    }
  }
}
