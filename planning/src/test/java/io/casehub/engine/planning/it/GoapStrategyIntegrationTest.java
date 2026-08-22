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
class GoapStrategyIntegrationTest {

  @Inject CaseInstanceCache cache;
  @Inject GoapCaseBean goapBean;

  @BeforeEach
  void setUp() {
    GoapCaseBean.executionOrder.clear();
  }

  @Test
  void goapStrategy_executesInDependencyOrder() {
    UUID caseId = goapBean.startCase(Map.of("trigger", true));

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(cache.get(caseId).getCaseContext().get("analysisResult")).isEqualTo(true);
              assertThat(cache.get(caseId).getCaseContext().get("riskAssessment")).isEqualTo(true);
              assertThat(cache.get(caseId).getCaseContext().get("report")).isEqualTo(true);
              assertThat(GoapCaseBean.executionOrder)
                  .containsExactly("analyse", "assess", "report");
            });
  }

  @ApplicationScoped
  public static class GoapCaseBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capAnalyse =
          Capability.builder().name("analyse").inputSchema(".").outputSchema(".").build();
      Capability capAssess =
          Capability.builder().name("assess").inputSchema(".").outputSchema(".").build();
      Capability capReport =
          Capability.builder().name("report").inputSchema(".").outputSchema(".").build();

      Worker wReport =
          Worker.builder()
              .name("worker-report")
              .capabilityName("report")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("report");
                        return WorkerResult.of(Map.of("report", true));
                      }))
              .build();

      Worker wAssess =
          Worker.builder()
              .name("worker-assess")
              .capabilityName("assess")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("assess");
                        return WorkerResult.of(Map.of("riskAssessment", true));
                      }))
              .build();

      Worker wAnalyse =
          Worker.builder()
              .name("worker-analyse")
              .capabilityName("analyse")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("analyse");
                        return WorkerResult.of(Map.of("analysisResult", true));
                      }))
              .build();

      GoapAction actAnalyse =
          new GoapAction("analyse", Map.of(), Map.of("analysisResult", true), 1.0);
      GoapAction actAssess =
          new GoapAction(
              "assess", Map.of("analysisResult", true), Map.of("riskAssessment", true), 1.0);
      GoapAction actReport =
          new GoapAction("report", Map.of("riskAssessment", true), Map.of("report", true), 1.0);

      return CaseDefinition.builder()
          .namespace("test-goap")
          .name("GOAP Strategy Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capAnalyse, capAssess, capReport)
          .workers(wReport, wAssess, wAnalyse)
          .bindings(
              Binding.builder()
                  .name("report")
                  .capability(capReport)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("assess")
                  .capability(capAssess)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("analyse")
                  .capability(capAnalyse)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build())
          .goapActions(List.of(actAnalyse, actAssess, actReport))
          .goalToEffectKey("done", Set.of("report"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".report == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".report == true"))
                      .build()))
          .build();
    }
  }
}
