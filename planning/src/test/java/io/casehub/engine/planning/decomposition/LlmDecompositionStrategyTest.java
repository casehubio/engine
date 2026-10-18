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
package io.casehub.engine.planning.decomposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.ai.AgentException;
import io.casehub.engine.plan.TaskNode;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.worker.api.Capability;
import io.smallrye.mutiny.Multi;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LlmDecompositionStrategyTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void producesSequentialDagPlanFromLlmResponse() {
    var strategy = buildWithMockProvider(sequentialResponse());
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(
                new Capability("data-gathering", "", "", null),
                new Capability("analysis", "", "", null),
                new Capability("reporting", "", "", null)));
    var task =
        new TaskNode.CompoundTask<JsonNode>(
            "comprehensive-analysis", "comprehensive-analysis", List.of());

    var plan = strategy.decompose(task, context);

    assertThat(plan.nodes()).hasSize(3);
    var sorted = plan.topologicalSort();
    assertThat(((GoalStep) sorted.get(0).task()).capabilityName()).isEqualTo("data-gathering");
    assertThat(((GoalStep) sorted.get(1).task()).capabilityName()).isEqualTo("analysis");
    assertThat(((GoalStep) sorted.get(2).task()).capabilityName()).isEqualTo("reporting");
  }

  @Test
  void idIsLlm() {
    var strategy = buildWithMockProvider(sequentialResponse());
    assertThat(strategy.id()).isEqualTo("llm");
  }

  @Test
  void failsWhenAgentProviderAbsent() {
    var strategy = new LlmDecompositionStrategy(Optional.empty());
    var context = new GoalDecompositionContext(MAPPER.createObjectNode(), 0, List.of());
    var task = new TaskNode.CompoundTask<JsonNode>("goal-1", "goal-1", List.of());

    assertThatThrownBy(() -> strategy.decompose(task, context))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void failsOnEmptySteps() {
    var strategy = buildWithMockProvider("{\"steps\": []}");
    var context = new GoalDecompositionContext(MAPPER.createObjectNode(), 0, List.of());
    var task = new TaskNode.CompoundTask<JsonNode>("goal-1", "goal-1", List.of());

    assertThatThrownBy(() -> strategy.decompose(task, context))
        .isInstanceOf(AgentException.class)
        .hasMessageContaining("no steps");
  }

  @Test
  void failsOnMissingStepsField() {
    var strategy = buildWithMockProvider("{\"result\": \"ok\"}");
    var context = new GoalDecompositionContext(MAPPER.createObjectNode(), 0, List.of());
    var task = new TaskNode.CompoundTask<JsonNode>("goal-1", "goal-1", List.of());

    assertThatThrownBy(() -> strategy.decompose(task, context)).isInstanceOf(AgentException.class);
  }

  @Test
  void includesConstraintsInPromptWhenPresent() {
    var capturedPrompt = new java.util.concurrent.atomic.AtomicReference<String>();

    AgentProvider capturingProvider =
        new AgentProvider() {
          @Override
          public Multi<AgentEvent> invoke(AgentSessionConfig config) {
            capturedPrompt.set(config.userPrompt());
            return Multi.createFrom()
                .items(
                    new AgentEvent.TextDelta(sequentialResponse()),
                    new AgentEvent.InvocationComplete(
                        0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null));
          }

          @Override
          public AgentSession openSession(AgentSessionInit init) {
            throw new UnsupportedOperationException();
          }
        };

    var strategy = new LlmDecompositionStrategy(Optional.of(capturingProvider));

    var constraints =
        io.casehub.engine.plan.PlanningConstraints.of(java.time.Duration.ofMinutes(30), 3);
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(new Capability("analysis", "", "", null)),
            constraints);
    var task = new TaskNode.CompoundTask<JsonNode>("research", "research", List.of());

    strategy.decompose(task, context);

    assertThat(capturedPrompt.get()).contains("30 minutes").contains("3");
  }

  @Test
  void includesCostBudgetsInPromptWhenPresent() {
    var capturedPrompt = new java.util.concurrent.atomic.AtomicReference<String>();

    var strategy =
        new LlmDecompositionStrategy(
            Optional.of(capturingProvider(sequentialResponse(), capturedPrompt)));

    var constraints =
        new io.casehub.engine.plan.PlanningConstraints(
            null, null, java.util.Map.of(), java.util.Map.of("tokens", 5000, "apiCalls", 10));
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(new Capability("analysis", "", "", null)),
            constraints);
    var task = new TaskNode.CompoundTask<JsonNode>("research", "research", List.of());

    strategy.decompose(task, context);

    assertThat(capturedPrompt.get()).contains("5000").contains("apiCalls");
  }

  @Test
  void includesWeightsInPromptWhenPresent() {
    var capturedPrompt = new java.util.concurrent.atomic.AtomicReference<String>();

    var strategy =
        new LlmDecompositionStrategy(
            Optional.of(capturingProvider(sequentialResponse(), capturedPrompt)));

    var constraints =
        new io.casehub.engine.plan.PlanningConstraints(
            null, null, java.util.Map.of("speed", 0.8, "quality", 0.2), java.util.Map.of());
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(new Capability("analysis", "", "", null)),
            constraints);
    var task = new TaskNode.CompoundTask<JsonNode>("research", "research", List.of());

    strategy.decompose(task, context);

    assertThat(capturedPrompt.get()).contains("speed").contains("0.8").contains("quality");
  }

  @Test
  void rendersCostBudgetsOnlyWithNoTimeBudgetOrResourceLimit() {
    var capturedPrompt = new java.util.concurrent.atomic.AtomicReference<String>();

    var strategy =
        new LlmDecompositionStrategy(
            Optional.of(capturingProvider(sequentialResponse(), capturedPrompt)));

    var constraints =
        new io.casehub.engine.plan.PlanningConstraints(
            null, null, java.util.Map.of(), java.util.Map.of("tokens", 3000));
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(new Capability("analysis", "", "", null)),
            constraints);
    var task = new TaskNode.CompoundTask<JsonNode>("research", "research", List.of());

    strategy.decompose(task, context);

    assertThat(capturedPrompt.get()).contains("3000").contains("Constraints:");
  }

  @Test
  void omitsConstraintTextWhenUnconstrained() {
    var capturedPrompt = new java.util.concurrent.atomic.AtomicReference<String>();

    var strategy =
        new LlmDecompositionStrategy(
            Optional.of(capturingProvider(sequentialResponse(), capturedPrompt)));

    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(), 0, List.of(new Capability("analysis", "", "", null)));
    var task = new TaskNode.CompoundTask<JsonNode>("research", "research", List.of());

    strategy.decompose(task, context);

    assertThat(capturedPrompt.get()).doesNotContain("Constraints:");
  }

  @Test
  void replanIncludesFailureContextInPrompt() {
    var capturedPrompt = new java.util.concurrent.atomic.AtomicReference<String>();

    var replanResponse =
        "{\"steps\": [{\"id\": \"r1\", \"description\": \"recovery step\", \"capabilityName\": \"analysis\"}]}";
    var strategy =
        new LlmDecompositionStrategy(
            Optional.of(capturingProvider(replanResponse, capturedPrompt)));

    var completed =
        List.of(
            new io.casehub.engine.plan.ReplanContext.CompletedStep(
                "s1", java.util.Map.of("result", "ok"), java.time.Duration.ofSeconds(2)));
    var failed =
        new io.casehub.engine.plan.ReplanContext.FailedStep("s2", "service unavailable", null, 3);
    var replanCtx = new io.casehub.engine.plan.ReplanContext<JsonNode>(completed, failed, null, 0);

    var task = new TaskNode.CompoundTask<JsonNode>("ct-1", "analyse-data", List.of());
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(new Capability("analysis", "Analyse data", "", null)));

    var result = strategy.replan(task, context, replanCtx);

    assertThat(result.nodes()).hasSize(1);
    var prompt = capturedPrompt.get();
    assertThat(prompt).contains("analyse-data");
    assertThat(prompt).contains("service unavailable");
    assertThat(prompt).contains("Completed steps");
    assertThat(prompt).contains("3 retries");
  }

  @Test
  void replanFailsWhenChatModelAbsent() {
    var strategy = new LlmDecompositionStrategy(Optional.empty());

    var failed = new io.casehub.engine.plan.ReplanContext.FailedStep("s1", "err", null, 0);
    var replanCtx = new io.casehub.engine.plan.ReplanContext<JsonNode>(List.of(), failed, null, 0);
    var task = new TaskNode.CompoundTask<JsonNode>("ct-1", "goal-1", List.of());
    var context = new GoalDecompositionContext(MAPPER.createObjectNode(), 0, List.of());

    assertThatThrownBy(() -> strategy.replan(task, context, replanCtx))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void replanFailsOnEmptySteps() {
    var strategy = buildWithMockProvider("{\"steps\": []}");

    var failed = new io.casehub.engine.plan.ReplanContext.FailedStep("s1", "err", null, 0);
    var replanCtx = new io.casehub.engine.plan.ReplanContext<JsonNode>(List.of(), failed, null, 0);
    var task = new TaskNode.CompoundTask<JsonNode>("ct-1", "goal-1", List.of());
    var context = new GoalDecompositionContext(MAPPER.createObjectNode(), 0, List.of());

    assertThatThrownBy(() -> strategy.replan(task, context, replanCtx))
        .isInstanceOf(AgentException.class)
        .hasMessageContaining("no steps");
  }

  @Test
  void parsesContingencyFromLlmResponse() {
    String response =
        "{\"steps\": ["
            + "{\"id\": \"s1\", \"description\": \"Primary\", \"capabilityName\": \"primary-cap\","
            + " \"contingency\": [\"alt-a\", \"alt-b\"]},"
            + "{\"id\": \"s2\", \"description\": \"Second\", \"capabilityName\": \"second-cap\","
            + " \"dependsOn\": [\"s1\"]}"
            + "]}";
    var strategy = buildWithMockProvider(response);
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(
                new Capability("primary-cap", "", "", null),
                new Capability("second-cap", "", "", null)));
    var task = new TaskNode.CompoundTask<JsonNode>("goal", "goal", List.of());

    var plan = strategy.decompose(task, context);

    var s1 = plan.nodes().get("s1");
    assertThat(s1.contingency()).isNotNull();
    assertThat(s1.contingency().nodes()).hasSize(2);
    var contingencySteps = s1.contingency().topologicalSort();
    assertThat(((GoalStep) contingencySteps.get(0).task()).capabilityName()).isEqualTo("alt-a");
    assertThat(((GoalStep) contingencySteps.get(1).task()).capabilityName()).isEqualTo("alt-b");

    var s2 = plan.nodes().get("s2");
    assertThat(s2.contingency()).isNull();
  }

  @Test
  void noContingencyField_nodeHasNullContingency() {
    var strategy = buildWithMockProvider(sequentialResponse());
    var context =
        new GoalDecompositionContext(
            MAPPER.createObjectNode(),
            0,
            List.of(
                new Capability("data-gathering", "", "", null),
                new Capability("analysis", "", "", null),
                new Capability("reporting", "", "", null)));
    var task = new TaskNode.CompoundTask<JsonNode>("goal", "goal", List.of());

    var plan = strategy.decompose(task, context);

    plan.nodes().values().forEach(n -> assertThat(n.contingency()).isNull());
  }

  private static String sequentialResponse() {
    return "{\"steps\": ["
        + "{\"id\": \"s1\", \"description\": \"Gather data\", \"capabilityName\": \"data-gathering\"},"
        + "{\"id\": \"s2\", \"description\": \"Analyse\", \"capabilityName\": \"analysis\", \"dependsOn\": [\"s1\"]},"
        + "{\"id\": \"s3\", \"description\": \"Report\", \"capabilityName\": \"reporting\", \"dependsOn\": [\"s2\"]}"
        + "]}";
  }

  private static AgentProvider stubProvider(String cannedResponse) {
    return new AgentProvider() {
      @Override
      public Multi<AgentEvent> invoke(AgentSessionConfig config) {
        return Multi.createFrom()
            .items(
                new AgentEvent.TextDelta(cannedResponse),
                new AgentEvent.InvocationComplete(
                    0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null));
      }

      @Override
      public AgentSession openSession(AgentSessionInit init) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static LlmDecompositionStrategy buildWithMockProvider(String cannedResponse) {
    return new LlmDecompositionStrategy(Optional.of(stubProvider(cannedResponse)));
  }

  private static AgentProvider capturingProvider(
      String cannedResponse, java.util.concurrent.atomic.AtomicReference<String> capturedPrompt) {
    return new AgentProvider() {
      @Override
      public Multi<AgentEvent> invoke(AgentSessionConfig config) {
        capturedPrompt.set(config.userPrompt());
        return Multi.createFrom()
            .items(
                new AgentEvent.TextDelta(cannedResponse),
                new AgentEvent.InvocationComplete(
                    0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null));
      }

      @Override
      public AgentSession openSession(AgentSessionInit init) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
