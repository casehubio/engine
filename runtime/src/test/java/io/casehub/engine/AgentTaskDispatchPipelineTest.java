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

import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.model.AgentTaskTarget;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.StandardGoalKind;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.api.identity.TenancyConstants;
import io.casehub.worker.api.Capability;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AgentTaskDispatchPipelineTest {

  @InjectMock AgentProvider agentProvider;

  @Inject AgentRegistry agentRegistry;

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject CaseHubRuntime caseHubRuntime;

  @Inject CaseInstanceCache caseInstanceCache;

  @BeforeEach
  void setUp() {
    Mockito.when(agentProvider.invoke(Mockito.any()))
        .thenReturn(
            Multi.createFrom()
                .items(
                    new AgentEvent.TextDelta("{\"analysis\": \"done\"}"),
                    new AgentEvent.InvocationComplete(
                        0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null)));
  }

  @Test
  void agentTaskTarget_dispatches_through_full_pipeline() {
    String capabilityName = "analyzeText-" + UUID.randomUUID().toString().substring(0, 8);

    agentRegistry.register(
        AgentDescriptor.builder()
            .agentId("test-analyzer-" + UUID.randomUUID())
            .name("Test Analyzer")
            .slot("test-analyzer-slot")
            .capabilities(List.of(AgentCapability.builder().name(capabilityName).build()))
            .tenancyId(TenancyConstants.DEFAULT_TENANT_ID)
            .briefing("Analyze the input text and return JSON with an analysis field.")
            .build());

    AgentTaskTarget target = AgentTaskTarget.builder().capability(capabilityName).build();

    Binding binding =
        Binding.builder()
            .name("analyze-binding")
            .agentTask(target)
            .on(new ContextChangeTrigger(new JQExpressionEvaluator(".status == \"pending\"")))
            .build();

    Goal goal = new Goal("done", new JQExpressionEvaluator(".analysis == \"done\""), "success");

    CaseDefinition definition =
        new CaseDefinition("agent-task-pipeline-test", "Agent Task Pipeline Test", "1.0.0");
    definition.setDsl("0.1");
    definition.getCapabilities().add(Capability.of(capabilityName, ".", "."));
    definition.getBindings().add(binding);
    definition.getGoals().add(goal);
    definition.setCompletion(
        GoalBasedCompletion.<StandardGoalKind>builder()
            .goal(StandardGoalKind.SUCCESS, GoalExpression.allOf(goal))
            .build());

    caseDefinitionRegistry.registerCaseDefinition(definition);

    UUID caseId =
        caseHubRuntime.startCase(definition, Map.of("text", "Hello world", "status", "pending"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(caseId);
              assertThat(instance).isNotNull();
              assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);

              Map<String, Object> ctx = instance.getCaseContext().getData();
              assertThat(ctx).containsEntry("analysis", "done");
            });
  }
}
