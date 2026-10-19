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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AgentWorkerExecutionTest {

  @Inject AgentCaseHub agentCaseHub;

  @Inject CaseInstanceCache caseInstanceCache;

  @Test
  public void agentWorkerExecutesAndCompletesCase() {
    Map<String, Object> initialContext =
        Map.of(
            "text", "This is a great product!",
            "status", "pending");

    UUID caseId = agentCaseHub.startCase(initialContext);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(caseId);
              assertNotNull(instance);
              assertEquals(CaseStatus.COMPLETED, instance.getState());

              // Verify agent processed the sentiment
              Map<String, Object> context = instance.getCaseContext().getData();
              assertNotNull(context.get("sentiment"));
              assertEquals("POSITIVE", context.get("sentiment"));
            });
  }

  @ApplicationScoped
  public static class AgentCaseHub extends CaseHub {

    private static final CaseDefinition DEFINITION = createDefinition();

    @Override
    public CaseDefinition getDefinition() {
      return DEFINITION;
    }

    private static CaseDefinition createDefinition() {
      // Stub AgentProvider that returns fixed sentiment
      AgentProvider stubProvider =
          new AgentProvider() {
            @Override
            public Multi<AgentEvent> invoke(AgentSessionConfig config) {
              return Multi.createFrom()
                  .items(
                      new AgentEvent.TextDelta("{\"sentiment\": \"POSITIVE\"}"),
                      new AgentEvent.InvocationComplete(
                          0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null));
            }

            @Override
            public AgentSession openSession(AgentSessionInit init) {
              throw new UnsupportedOperationException();
            }
          };

      // Create WorkerAgentInvoker
      WorkerAgentInvoker sentimentInvoker =
          new WorkerAgentInvoker(
              stubProvider,
              "You are a sentiment analyzer. Analyze the text and return POSITIVE, NEGATIVE, or NEUTRAL.",
              null,
              new io.casehub.api.model.ai.JqTransformer("{ text: .text }")::apply,
              new io.casehub.api.model.ai.JqTransformer("{ sentiment: .sentiment }")::apply,
              null,
              null,
              Duration.ofSeconds(30));

      // Create capability
      Capability sentimentCapability =
          Capability.of(
              "analyzeSentiment",
              "{ text: .text }",
              "{ sentiment: .sentiment, status: \"analyzed\" }");

      // Create worker with Agent
      Worker aiWorker =
          Worker.builder()
              .name("sentiment-worker")
              .capabilityName("analyzeSentiment")
              .function(new AgentWorkerFunction(sentimentInvoker))
              .description("AI-powered sentiment analysis worker")
              .build();

      // Create binding to trigger on pending status
      Binding binding =
          Binding.builder()
              .name("trigger-sentiment-analysis")
              .capability(sentimentCapability)
              .on(new ContextChangeTrigger(new JQExpressionEvaluator(".status == \"pending\"")))
              .build();

      // Create goal
      Goal analysisComplete =
          new Goal(
              "analysisComplete", new JQExpressionEvaluator(".status == \"analyzed\""), "success");

      // Create case definition
      CaseDefinition definition = new CaseDefinition("test", "AI Sentiment Analysis", "1.0.0");
      definition.setDsl("0.1");
      definition.setTitle("AI Agent Worker Test Case");

      definition.getCapabilities().add(sentimentCapability);
      definition.getWorkers().add(aiWorker);
      definition.getBindings().add(binding);
      definition.getGoals().add(analysisComplete);

      // Set completion criteria
      definition.setCompletion(
          GoalBasedCompletion.<StandardGoalKind>builder()
              .goal(StandardGoalKind.SUCCESS, GoalExpression.allOf(analysisComplete))
              .build());

      return definition;
    }
  }
}
