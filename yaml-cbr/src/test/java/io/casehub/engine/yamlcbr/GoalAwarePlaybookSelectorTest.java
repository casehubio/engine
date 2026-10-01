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
package io.casehub.engine.yamlcbr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.spi.routing.RetrievedExperience;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.GoalHorizon;
import io.casehub.eidos.api.GoalLifecycleState;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoalAwarePlaybookSelectorTest {

  private GoalAwarePlaybookSelector selector;

  @BeforeEach
  void setUp() {
    selector = new GoalAwarePlaybookSelector();
  }

  @Test
  void selectsHighestScoringCandidate() {
    var completed = experience("plan-a", "COMPLETED", 0.9, 0.95);
    var faulted = experience("plan-b", "FAULTED", 0.9, null);
    var goals = List.of(goal("resolve-incident", GoalHorizon.IMMEDIATE, GoalLifecycleState.ACTIVE));

    var result = selector.select(List.of(completed, faulted), goals, "tenant-1");

    assertNotNull(result);
    assertEquals(completed, result.selectedExperience());
    assertTrue(result.score() > 0.0);
  }

  @Test
  void confidenceOverridesOutcomeClassification() {
    var highConf = experience("plan-a", "COMPLETED", 0.8, 0.95);
    var lowConf = experience("plan-b", "COMPLETED", 0.8, 0.3);
    var goals = List.of(goal("g1", GoalHorizon.IMMEDIATE, GoalLifecycleState.ACTIVE));

    var result = selector.select(List.of(highConf, lowConf), goals, "tenant-1");

    assertEquals(highConf, result.selectedExperience());
  }

  @Test
  void horizonWeightsAffectScoring() {
    var candidate = experience("plan-a", "COMPLETED", 0.9, null);

    var immGoal = goal("urgent", GoalHorizon.IMMEDIATE, GoalLifecycleState.ACTIVE);
    var aspGoal = goal("dream", GoalHorizon.ASPIRATIONAL, GoalLifecycleState.ACTIVE);

    var resultImm = selector.select(List.of(candidate), List.of(immGoal), "t");
    var resultAsp = selector.select(List.of(candidate), List.of(aspGoal), "t");

    assertTrue(resultImm.score() > resultAsp.score());
  }

  @Test
  void excludesNonActiveGoals() {
    var candidate = experience("plan-a", "COMPLETED", 0.9, null);
    var blocked = goal("blocked-goal", GoalHorizon.IMMEDIATE, GoalLifecycleState.BLOCKED);
    var dormant = goal("dormant-goal", GoalHorizon.IMMEDIATE, GoalLifecycleState.DORMANT);

    var result = selector.select(List.of(candidate), List.of(blocked, dormant), "t");

    assertEquals(0.0, result.score());
  }

  @Test
  void returnsFirstCandidateWhenNoActiveGoals() {
    var candidate = experience("plan-a", "COMPLETED", 0.9, null);

    var result = selector.select(List.of(candidate), List.of(), "t");

    assertNotNull(result);
    assertEquals(candidate, result.selectedExperience());
    assertEquals(0.0, result.score());
  }

  @Test
  void outcomeClassificationForUnknownOutcome() {
    var unknown = experience("plan-a", "WEIRD_STATUS", 0.9, null);
    var goals = List.of(goal("g1", GoalHorizon.IMMEDIATE, GoalLifecycleState.ACTIVE));

    var result = selector.select(List.of(unknown), goals, "t");

    assertTrue(result.score() > 0.0);
    assertTrue(result.score() < 0.9);
  }

  @Test
  void cancelledOutcomeGetsLowWeight() {
    var cancelled = experience("plan-a", "CANCELLED", 0.9, null);
    var completed = experience("plan-b", "COMPLETED", 0.9, null);
    var goals = List.of(goal("g1", GoalHorizon.IMMEDIATE, GoalLifecycleState.ACTIVE));

    var result = selector.select(List.of(cancelled, completed), goals, "t");

    assertEquals(completed, result.selectedExperience());
  }

  @Test
  void goalAlignmentEntriesPopulated() {
    var candidate = experience("plan-a", "COMPLETED", 0.9, 0.8);
    var goals =
        List.of(
            goal("g1", GoalHorizon.IMMEDIATE, GoalLifecycleState.ACTIVE),
            goal("g2", GoalHorizon.LONG_TERM, GoalLifecycleState.ACTIVE));

    var result = selector.select(List.of(candidate), goals, "t");

    assertEquals(2, result.goalAlignment().size());
    assertEquals("g1", result.goalAlignment().get(0).goalName());
    assertEquals("g2", result.goalAlignment().get(1).goalName());
    assertTrue(
        result.goalAlignment().get(0).alignmentScore()
            > result.goalAlignment().get(1).alignmentScore());
  }

  // --- Helpers ---

  private static RetrievedExperience experience(
      String problem, String outcome, double similarity, Double confidence) {
    return new RetrievedExperience(
        problem, "solution", outcome, confidence, similarity, Map.of(), List.of(), Map.of());
  }

  private static AgentGoal goal(String name, GoalHorizon horizon, GoalLifecycleState state) {
    return new AgentGoal(
        name,
        "test goal",
        GoalPriority.PRIMARY,
        Visibility.PUBLIC,
        List.of(),
        Map.of(),
        state,
        horizon);
  }
}
