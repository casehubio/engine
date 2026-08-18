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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.CaseDefinition;
import io.casehub.engine.plan.ReplanContext;
import io.casehub.engine.plan.TaskNode;
import io.casehub.engine.plan.goap.GoapAction;
import io.casehub.worker.api.Capability;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoapDecompositionStrategyTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private GoapDecompositionStrategy strategy;
  private CaseDefinition definition;

  @BeforeEach
  void setUp() {
    strategy = new GoapDecompositionStrategy();
    definition = mock(CaseDefinition.class);
  }

  @Test
  void id_is_goap() {
    assertThat(strategy.id()).isEqualTo("goap");
  }

  @Test
  void decompose_produces_plan_with_causal_dependencies() {
    var a1 = new GoapAction("analyse", Map.of(), Map.of("analysed", true), 0.5);
    var a2 = new GoapAction("assess", Map.of("analysed", true), Map.of("assessed", true), 0.5);
    when(definition.getGoapActions()).thenReturn(List.of(a1, a2));
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("done", Set.of("assessed")));

    ObjectNode state = MAPPER.createObjectNode();
    var capabilities =
        List.of(Capability.of("analyse", ".", "."), Capability.of("assess", ".", "."));
    var context = new GoalDecompositionContext(state, 0, capabilities, null, definition);
    var task = new TaskNode.CompoundTask<JsonNode>("goal", List.of());

    var result = strategy.decompose(task, context);

    assertThat(result.nodes()).hasSize(2);
    var sorted = result.topologicalSort();
    assertThat(sorted.get(0).task()).isInstanceOf(GoalStep.class);
    assertThat(((GoalStep) sorted.get(0).task()).capabilityName()).isEqualTo("analyse");
    assertThat(((GoalStep) sorted.get(1).task()).capabilityName()).isEqualTo("assess");
    assertThat(sorted.get(1).dependsOn()).isNotEmpty();
  }

  @Test
  void decompose_returns_empty_when_no_goap_actions() {
    when(definition.getGoapActions()).thenReturn(List.of());
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of());

    ObjectNode state = MAPPER.createObjectNode();
    var context = new GoalDecompositionContext(state, 0, List.of(), null, definition);
    var task = new TaskNode.CompoundTask<JsonNode>("goal", List.of());

    assertThatThrownBy(() -> strategy.decompose(task, context))
        .isInstanceOf(io.casehub.api.model.ai.AgentException.class);
  }

  @Test
  void decompose_returns_empty_when_goal_already_satisfied() {
    var a1 = new GoapAction("analyse", Map.of(), Map.of("done", true), 0.5);
    when(definition.getGoapActions()).thenReturn(List.of(a1));
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("goal", Set.of("done")));

    ObjectNode state = MAPPER.createObjectNode();
    state.put("done", true);
    var context =
        new GoalDecompositionContext(
            state, 0, List.of(Capability.of("analyse", ".", ".")), null, definition);
    var task = new TaskNode.CompoundTask<JsonNode>("goal", List.of());

    assertThatThrownBy(() -> strategy.decompose(task, context))
        .isInstanceOf(io.casehub.api.model.ai.AgentException.class);
  }

  @Test
  void decompose_filters_by_available_capabilities() {
    var a1 = new GoapAction("available", Map.of(), Map.of("goal", true), 0.5);
    var a2 = new GoapAction("unavailable", Map.of(), Map.of("goal", true), 0.1);
    when(definition.getGoapActions()).thenReturn(List.of(a1, a2));
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("g", Set.of("goal")));

    ObjectNode state = MAPPER.createObjectNode();
    var capabilities = List.of(Capability.of("available", ".", "."));
    var context = new GoalDecompositionContext(state, 0, capabilities, null, definition);
    var task = new TaskNode.CompoundTask<JsonNode>("goal", List.of());

    var result = strategy.decompose(task, context);
    assertThat(result.nodes()).hasSize(1);
    var step = (GoalStep) result.topologicalSort().get(0).task();
    assertThat(step.capabilityName()).isEqualTo("available");
  }

  @Test
  void replan_blacklists_failed_action() {
    var a1 = new GoapAction("step1", Map.of(), Map.of("x", true), 0.5);
    var a2 = new GoapAction("step2", Map.of("x", true), Map.of("goal", true), 0.5);
    var alt = new GoapAction("alt", Map.of(), Map.of("x", true, "goal", true), 1.5);
    when(definition.getGoapActions()).thenReturn(List.of(a1, a2, alt));
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("g", Set.of("goal")));

    ObjectNode state = MAPPER.createObjectNode();
    var capabilities =
        List.of(
            Capability.of("step1", ".", "."),
            Capability.of("step2", ".", "."),
            Capability.of("alt", ".", "."));
    var context = new GoalDecompositionContext(state, 0, capabilities, null, definition);
    var task = new TaskNode.CompoundTask<JsonNode>("goal", List.of());

    var originalPlan = strategy.decompose(task, context);

    var sortedOriginal = originalPlan.topologicalSort();
    var step1Node =
        sortedOriginal.stream()
            .filter(n -> ((GoalStep) n.task()).capabilityName().equals("step1"))
            .findFirst()
            .orElseThrow();
    var step2Node =
        sortedOriginal.stream()
            .filter(n -> ((GoalStep) n.task()).capabilityName().equals("step2"))
            .findFirst()
            .orElseThrow();

    var completedSteps =
        List.of(new ReplanContext.CompletedStep(step1Node.id(), "ok", Duration.ofSeconds(1)));
    var failedStep = new ReplanContext.FailedStep(step2Node.id(), "error", null, 0);
    var replanCtx = new ReplanContext<>(completedSteps, failedStep, originalPlan, 0);

    ObjectNode updatedState = MAPPER.createObjectNode();
    updatedState.put("x", true);
    var newContext = new GoalDecompositionContext(updatedState, 0, capabilities, null, definition);

    var result = strategy.replan(task, newContext, replanCtx);

    assertThat(result.nodes()).isNotEmpty();
    var replanSteps =
        result.topologicalSort().stream().map(n -> ((GoalStep) n.task()).capabilityName()).toList();
    assertThat(replanSteps).doesNotContain("step1");
    assertThat(replanSteps).doesNotContain("step2");
  }
}
