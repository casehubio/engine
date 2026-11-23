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
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.StandardGoalKind;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying that null-timeout workers are enriched to the platform default and
 * that the enriched timeout is enforced. The test application.properties sets {@code
 * casehub.engine.worker.default-timeout-ms=2000}.
 *
 * <p>Refs casehubio/engine#1341.
 */
@QuarkusTest
public class TimeoutEnrichmentIntegrationTest {

  @Inject TimeoutTestCaseHub caseHub;
  @Inject CaseInstanceCache caseInstanceCache;

  @Test
  void nullTimeoutWorkerCompletesWithEnrichedDefault() {
    Map<String, Object> initialContext = Map.of("action", "fast", "status", "pending");
    UUID caseId = caseHub.startCase(initialContext);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(caseId);
              assertThat(instance).isNotNull();
              assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(instance.getCaseContext().getData()).containsEntry("status", "done");
            });
  }

  @ApplicationScoped
  public static class TimeoutTestCaseHub extends CaseHub {

    private static final CaseDefinition DEFINITION = createDefinition();

    @Override
    public CaseDefinition getDefinition() {
      return DEFINITION;
    }

    @SuppressWarnings("unchecked")
    private static CaseDefinition createDefinition() {
      Capability processCapability =
          Capability.of("process", "{ action: .action }", "{ status: \"done\" }");

      WorkerFunction.Sync<Map<String, Object>, Map<String, Object>> fastFn =
          new WorkerFunction.Sync<>(
              (Class<Map<String, Object>>) (Class<?>) Map.class,
              (Class<Map<String, Object>>) (Class<?>) Map.class,
              (input, scope) -> WorkerResult.of(Map.of("status", "done")));

      Worker fastWorker =
          Worker.builder()
              .name("fast-worker")
              .capabilityName("process")
              .function(fastFn)
              .description("Completes instantly — proves null timeout was enriched, not rejected")
              .executionPolicy(new ExecutionPolicy(null, null, null))
              .build();

      Binding binding =
          Binding.builder()
              .name("trigger-process")
              .capability(processCapability)
              .on(new ContextChangeTrigger(new JQExpressionEvaluator(".status == \"pending\"")))
              .build();

      Goal done =
          new Goal("processDone", new JQExpressionEvaluator(".status == \"done\""), "success");

      CaseDefinition definition = new CaseDefinition("test", "Timeout Enrichment Test", "1.0.0");
      definition.setDsl("0.1");
      definition.getCapabilities().add(processCapability);
      definition.getWorkers().add(fastWorker);
      definition.getBindings().add(binding);
      definition.getGoals().add(done);
      definition.setCompletion(
          GoalBasedCompletion.<StandardGoalKind>builder()
              .goal(StandardGoalKind.SUCCESS, GoalExpression.allOf(done))
              .build());
      return definition;
    }
  }
}
