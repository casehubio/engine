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
import io.casehub.api.model.AgentWorkerFunction;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.StandardGoalKind;
import io.casehub.api.model.ai.DeferredAgentConfig;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.AgentSelection;
import io.casehub.eidos.api.AgentSelector;
import io.casehub.eidos.api.SelectionContext;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Integration test verifying CDI wiring of the slot-based agent worker dispatch path:
 * TestSlotAgentRegistry → AgentWorkerFunctionHandler.executeBySlot → AgentProvider invocation.
 *
 * <p>Refs casehubio/engine#1340.
 */
@QuarkusTest
public class SlotAgentWorkerCdiWiringTest {

  private static final String TEST_SLOT = "sentiment-analyst";
  private static final String TENANCY_ID =
      io.casehub.platform.api.identity.TenancyConstants.DEFAULT_TENANT_ID;

  @Inject TestSlotAgentRegistry agentRegistry;

  @InjectMock AgentProvider agentProvider;

  @Inject SlotTestCaseHub caseHub;
  @Inject CaseInstanceCache caseInstanceCache;

  @BeforeEach
  void setUp() {
    AgentDescriptor descriptor =
        AgentDescriptor.builder()
            .agentId("agent-sentiment")
            .name("Sentiment Analyst")
            .slot(TEST_SLOT)
            .tenancyId(TENANCY_ID)
            .build();
    agentRegistry.register(descriptor);

    Mockito.when(agentProvider.invoke(Mockito.any()))
        .thenReturn(
            Multi.createFrom()
                .items(
                    new AgentEvent.TextDelta("{\"sentiment\": \"POSITIVE\"}"),
                    new AgentEvent.InvocationComplete(
                        0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null)));
  }

  @Test
  void slotWorkerResolvesAgentAndExecutes() {
    Map<String, Object> initialContext = Map.of("text", "Great product!", "status", "pending");
    UUID caseId = caseHub.startCase(initialContext);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(caseId);
              assertThat(instance).isNotNull();
              assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(instance.getCaseContext().getData()).containsEntry("status", "analyzed");
            });
  }

  @Test
  void workerFunctionIsDeferredSlotMode() {
    CaseDefinition definition = caseHub.getDefinition();
    Worker worker =
        definition.getWorkers().stream()
            .filter(w -> w.name().equals("slot-sentiment-worker"))
            .findFirst()
            .orElseThrow();
    assertThat(worker.function()).isInstanceOf(AgentWorkerFunction.class);
    AgentWorkerFunction agentFn = (AgentWorkerFunction) worker.function();
    assertThat(agentFn.isDeferred()).isTrue();
    assertThat(agentFn.deferredConfig()).isInstanceOf(DeferredAgentConfig.SlotConfig.class);
    DeferredAgentConfig.SlotConfig slotConfig =
        (DeferredAgentConfig.SlotConfig) agentFn.deferredConfig();
    assertThat(slotConfig.slot()).isEqualTo(TEST_SLOT);
  }

  // ── CDI beans required for AgentWorkerFunctionHandler slot dispatch ──────

  @Alternative
  @Priority(2)
  @ApplicationScoped
  public static class TestSlotAgentRegistry implements AgentRegistry {
    private final ConcurrentHashMap<String, AgentDescriptor> descriptors =
        new ConcurrentHashMap<>();

    @Override
    public void register(AgentDescriptor descriptor) {
      descriptors.put(descriptor.agentId() + "|" + descriptor.tenancyId(), descriptor);
    }

    @Override
    public Optional<AgentDescriptor> findById(String agentId, String tenancyId) {
      return Optional.ofNullable(descriptors.get(agentId + "|" + tenancyId));
    }

    @Override
    public List<AgentMatch> find(AgentQuery query) {
      return descriptors.values().stream()
          .filter(d -> query.tenancyId() == null || query.tenancyId().equals(d.tenancyId()))
          .filter(d -> query.slot() == null || query.slot().equals(d.slot()))
          .map(d -> new AgentMatch(d, null))
          .toList();
    }
  }

  @ApplicationScoped
  public static class SlotTestCaseHub extends CaseHub {

    private static final CaseDefinition DEFINITION = createDefinition();

    @Override
    public CaseDefinition getDefinition() {
      return DEFINITION;
    }

    private static CaseDefinition createDefinition() {
      Capability sentimentCapability =
          Capability.of(
              "analyzeSentiment",
              "{ text: .text }",
              "{ sentiment: .sentiment, status: \"analyzed\" }");

      Worker slotWorker =
          Worker.builder()
              .name("slot-sentiment-worker")
              .capabilityName("analyzeSentiment")
              .function(
                  AgentWorkerFunction.ofSlot(
                      new DeferredAgentConfig.SlotConfig(TEST_SLOT, null, null, null, null)))
              .description("Slot-based deferred agent worker")
              .build();

      Binding binding =
          Binding.builder()
              .name("trigger-analysis")
              .capability(sentimentCapability)
              .on(new ContextChangeTrigger(new JQExpressionEvaluator(".status == \"pending\"")))
              .build();

      Goal analysisComplete =
          new Goal(
              "analysisComplete", new JQExpressionEvaluator(".status == \"analyzed\""), "success");

      CaseDefinition definition =
          new CaseDefinition("test", "Slot Agent Worker CDI Wiring Test", "1.0.0");
      definition.setDsl("0.1");
      definition.getCapabilities().add(sentimentCapability);
      definition.getWorkers().add(slotWorker);
      definition.getBindings().add(binding);
      definition.getGoals().add(analysisComplete);
      definition.setCompletion(
          GoalBasedCompletion.<StandardGoalKind>builder()
              .goal(StandardGoalKind.SUCCESS, GoalExpression.allOf(analysisComplete))
              .build());
      return definition;
    }
  }

  @ApplicationScoped
  public static class StubSystemPromptRenderer implements SystemPromptRenderer {

    @Override
    public RenderedPrompt render(AgentDescriptor descriptor, AgentPromptContext context) {
      return new RenderedPrompt(
          "You are " + descriptor.name() + ". Analyze sentiment.",
          RenderFormat.MARKDOWN,
          "",
          "",
          false);
    }
  }

  @ApplicationScoped
  public static class StubAgentSelector implements AgentSelector {

    @Override
    public AgentSelection select(List<AgentMatch> candidates, SelectionContext context) {
      if (candidates.isEmpty()) {
        return new AgentSelection.NoneQualified("No candidates");
      }
      return new AgentSelection.Selected(
          candidates.get(0).descriptor(), candidates.get(0).resolvedCapability(), 1.0, "stub");
    }
  }
}
