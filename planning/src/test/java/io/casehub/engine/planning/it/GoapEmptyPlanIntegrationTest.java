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
package io.casehub.engine.planning.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.engine.plan.goap.GoapAction;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GoapEmptyPlanIntegrationTest {

  @Inject CaseInstanceCache cache;
  @Inject EmptyPlanBean emptyPlanBean;

  @BeforeEach
  void setUp() {
    EmptyPlanBean.executionOrder.clear();
  }

  @Test
  void goapStrategy_waitsForContextWhenNoPlanViable_thenExecutesAfterSignal() {
    UUID caseId = emptyPlanBean.startCase(Map.of("trigger", true));

    await()
        .during(2, TimeUnit.SECONDS)
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.RUNNING);
              assertThat(EmptyPlanBean.executionOrder).isEmpty();
            });

    emptyPlanBean.signal(caseId, "dataReady", true);

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(EmptyPlanBean.executionOrder).containsExactly("process", "deliver");
            });
  }

  @ApplicationScoped
  public static class EmptyPlanBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capProcess =
          Capability.builder().name("process").inputSchema(".").outputSchema(".").build();
      Capability capDeliver =
          Capability.builder().name("deliver").inputSchema(".").outputSchema(".").build();

      Worker wProcess =
          Worker.builder()
              .name("worker-process")
              .capabilityName("process")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("process");
                        return WorkerResult.of(Map.of("processed", true));
                      }))
              .build();

      Worker wDeliver =
          Worker.builder()
              .name("worker-deliver")
              .capabilityName("deliver")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("deliver");
                        return WorkerResult.of(Map.of("delivered", true));
                      }))
              .build();

      GoapAction actProcess =
          new GoapAction("process", Map.of("dataReady", true), Map.of("processed", true), 1.0);
      GoapAction actDeliver =
          new GoapAction("deliver", Map.of("processed", true), Map.of("delivered", true), 1.0);

      return CaseDefinition.builder()
          .namespace("test-goap-empty")
          .name("GOAP Empty Plan Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capProcess, capDeliver)
          .workers(wProcess, wDeliver)
          .bindings(
              Binding.builder()
                  .name("process")
                  .capability(capProcess)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("deliver")
                  .capability(capDeliver)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build())
          .goapActions(List.of(actProcess, actDeliver))
          .goalToEffectKey("done", Set.of("delivered"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".delivered == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".delivered == true"))
                      .build()))
          .build();
    }
  }
}
