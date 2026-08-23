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

/**
 * Verifies that the GOAP A* planner selects the cheapest path when multiple independent actions
 * produce the same goal effect. Refs engine#924.
 */
@QuarkusTest
class GoapCostOrderingIntegrationTest {

  @Inject CaseInstanceCache cache;
  @Inject CostOrderingBean costBean;

  @BeforeEach
  void setUp() {
    CostOrderingBean.executionOrder.clear();
  }

  @Test
  void goapStrategy_selectsCheapestPathToGoal() {
    UUID caseId = costBean.startCase(Map.of("trigger", true));

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(CostOrderingBean.executionOrder)
                  .as("A* should select cheapPath (cost 0.5) over expensivePath (cost 5.0)")
                  .containsExactly("cheapPath");
            });
  }

  @ApplicationScoped
  public static class CostOrderingBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capCheap =
          Capability.builder().name("cheapPath").inputSchema(".").outputSchema(".").build();
      Capability capExpensive =
          Capability.builder().name("expensivePath").inputSchema(".").outputSchema(".").build();

      Worker wCheap =
          Worker.builder()
              .name("worker-cheapPath")
              .capabilityName("cheapPath")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("cheapPath");
                        return WorkerResult.of(Map.of("resolved", true));
                      }))
              .build();

      Worker wExpensive =
          Worker.builder()
              .name("worker-expensivePath")
              .capabilityName("expensivePath")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("expensivePath");
                        return WorkerResult.of(Map.of("resolved", true));
                      }))
              .build();

      GoapAction actCheap = new GoapAction("cheapPath", Map.of(), Map.of("resolved", true), 0.5);
      GoapAction actExpensive =
          new GoapAction("expensivePath", Map.of(), Map.of("resolved", true), 5.0);

      return CaseDefinition.builder()
          .namespace("test-goap-cost")
          .name("GOAP Cost Ordering Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capCheap, capExpensive)
          .workers(wCheap, wExpensive)
          .bindings(
              Binding.builder()
                  .name("cheapPath")
                  .capability(capCheap)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("expensivePath")
                  .capability(capExpensive)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build())
          .goapActions(List.of(actCheap, actExpensive))
          .goalToEffectKey("done", Set.of("resolved"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".resolved == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".resolved == true"))
                      .build()))
          .build();
    }
  }
}
