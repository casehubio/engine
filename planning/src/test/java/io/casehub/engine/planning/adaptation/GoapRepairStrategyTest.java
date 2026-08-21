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
package io.casehub.engine.planning.adaptation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.TaskStatus;
import io.casehub.api.model.ai.AgentException;
import io.casehub.engine.plan.adaptation.AdaptationCause;
import io.casehub.engine.plan.adaptation.AdaptationContext;
import io.casehub.engine.plan.adaptation.CompletedStep;
import io.casehub.engine.plan.adaptation.RepairStrategy;
import io.casehub.engine.plan.adaptation.RevisedPlan;
import io.casehub.engine.plan.adaptation.RevisionContext;
import io.casehub.engine.plan.goap.GoapAction;
import io.casehub.worker.api.Capability;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoapRepairStrategyTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void idReturnsGoapRepair() {
    var strategy = new GoapRepairStrategy();
    assertEquals("goap-repair", strategy.id());
  }

  @Test
  void implementsRepairStrategy() {
    assertTrue(RepairStrategy.class.isAssignableFrom(GoapRepairStrategy.class));
  }

  @Test
  void blacklistsFailedActionAndProducesAlternativePlan() {
    var strategy = new GoapRepairStrategy();

    var actions =
        List.of(
            new GoapAction("action-a", Map.of(), Map.of("stateA", true), 1.0),
            new GoapAction("action-b", Map.of("stateA", true), Map.of("goal", true), 1.0),
            new GoapAction("action-c", Map.of(), Map.of("stateA", true), 2.0));

    ObjectNode context = MAPPER.createObjectNode();

    var definition = mock(CaseDefinition.class);
    when(definition.getGoapActions()).thenReturn(actions);
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("reach-goal", Set.of("goal")));

    var adaptCtx =
        new AdaptationContext(
            UUID.randomUUID(),
            "tenant1",
            "compound-1",
            "compound-1",
            List.of(),
            List.of(),
            List.of(),
            context,
            definition,
            TaskStatus.FAULTED,
            "action-a",
            0);

    var cause = new AdaptationCause.StepFailed("action-a", "Knowledge failure");

    var capabilities =
        List.of(
            Capability.of("action-a", "", ""),
            Capability.of("action-b", "", ""),
            Capability.of("action-c", "", ""));

    var revisionCtx = new RevisionContext(adaptCtx, cause, capabilities, List.of());

    RevisedPlan result = strategy.revise(revisionCtx);

    assertNotNull(result);
    assertFalse(result.steps().isEmpty());
    assertTrue(
        result.steps().stream().noneMatch(s -> s.capabilityName().equals("action-a")),
        "Failed action should be blacklisted");
    assertTrue(
        result.steps().stream().anyMatch(s -> s.capabilityName().equals("action-c")),
        "Alternative action should be used");
  }

  @Test
  void throwsWhenNoPlanPossibleAfterBlacklisting() {
    var strategy = new GoapRepairStrategy();

    var actions = List.of(new GoapAction("only-action", Map.of(), Map.of("goal", true), 1.0));

    ObjectNode context = MAPPER.createObjectNode();

    var definition = mock(CaseDefinition.class);
    when(definition.getGoapActions()).thenReturn(actions);
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("reach-goal", Set.of("goal")));

    var adaptCtx =
        new AdaptationContext(
            UUID.randomUUID(),
            "tenant1",
            "compound-1",
            "compound-1",
            List.of(),
            List.of(),
            List.of(),
            context,
            definition,
            TaskStatus.FAULTED,
            "only-action",
            0);

    var cause = new AdaptationCause.StepFailed("only-action", "Failed");
    var revisionCtx =
        new RevisionContext(
            adaptCtx, cause, List.of(Capability.of("only-action", "", "")), List.of());

    assertThrows(AgentException.class, () -> strategy.revise(revisionCtx));
  }

  @Test
  void filtersCompletedActions() {
    var strategy = new GoapRepairStrategy();

    var actions =
        List.of(
            new GoapAction("step-1", Map.of(), Map.of("mid", true), 1.0),
            new GoapAction("step-2", Map.of("mid", true), Map.of("goal", true), 1.0));

    ObjectNode context = MAPPER.createObjectNode();
    context.put("mid", true);

    var completedSteps =
        List.of(new CompletedStep("step-1", "step-1", "Step 1", Map.of(), Instant.now()));

    var definition = mock(CaseDefinition.class);
    when(definition.getGoapActions()).thenReturn(actions);
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("reach-goal", Set.of("goal")));

    var adaptCtx =
        new AdaptationContext(
            UUID.randomUUID(),
            "tenant1",
            "c1",
            "c1",
            completedSteps,
            List.of(),
            List.of(),
            context,
            definition,
            TaskStatus.COMPLETED,
            "step-1",
            0);

    var cause = new AdaptationCause.StepCompleted("step-1", "step-1", Map.of());
    var capabilities = List.of(Capability.of("step-1", "", ""), Capability.of("step-2", "", ""));

    var revisionCtx = new RevisionContext(adaptCtx, cause, capabilities, List.of());
    RevisedPlan result = strategy.revise(revisionCtx);

    assertNotNull(result);
    assertTrue(
        result.steps().stream().noneMatch(s -> s.capabilityName().equals("step-1")),
        "Completed action should be filtered out");
    assertTrue(
        result.steps().stream().anyMatch(s -> s.capabilityName().equals("step-2")),
        "Remaining action should be included");
  }

  @Test
  void filtersActionsToAvailableCapabilities() {
    var strategy = new GoapRepairStrategy();

    var actions =
        List.of(
            new GoapAction("action-a", Map.of(), Map.of("mid", true), 1.0),
            new GoapAction("action-b", Map.of("mid", true), Map.of("goal", true), 1.0),
            new GoapAction("action-unavailable", Map.of(), Map.of("mid", true), 0.5));

    ObjectNode context = MAPPER.createObjectNode();

    var definition = mock(CaseDefinition.class);
    when(definition.getGoapActions()).thenReturn(actions);
    when(definition.getGoalToEffectKeys()).thenReturn(Map.of("reach", Set.of("goal")));

    var adaptCtx =
        new AdaptationContext(
            UUID.randomUUID(),
            "tenant1",
            "c1",
            "c1",
            List.of(),
            List.of(),
            List.of(),
            context,
            definition,
            TaskStatus.COMPLETED,
            "action-a",
            0);

    var cause = new AdaptationCause.StepCompleted("action-a", "action-a", Map.of());
    var capabilities =
        List.of(Capability.of("action-a", "", ""), Capability.of("action-b", "", ""));

    var revisionCtx = new RevisionContext(adaptCtx, cause, capabilities, List.of());
    RevisedPlan result = strategy.revise(revisionCtx);

    assertNotNull(result);
    assertTrue(
        result.steps().stream().noneMatch(s -> s.capabilityName().equals("action-unavailable")));
  }
}
