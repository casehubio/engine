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

import io.casehub.api.spi.routing.RetrievedExperience;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.GoalHorizon;
import io.casehub.eidos.api.GoalLifecycleState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

public class GoalAwarePlaybookSelector {

  private static final Logger LOG = Logger.getLogger(GoalAwarePlaybookSelector.class);

  private static final Map<String, Double> OUTCOME_WEIGHTS =
      Map.of(
          "COMPLETED", 1.0,
          "FAULTED", 0.0,
          "CANCELLED", 0.2);
  private static final double DEFAULT_OUTCOME_WEIGHT = 0.5;

  private static final Map<GoalHorizon, Double> HORIZON_WEIGHTS =
      Map.of(
          GoalHorizon.IMMEDIATE, 1.0,
          GoalHorizon.SHORT_TERM, 0.8,
          GoalHorizon.MEDIUM_TERM, 0.5,
          GoalHorizon.LONG_TERM, 0.3,
          GoalHorizon.ASPIRATIONAL, 0.1);

  public PlaybookSelection select(
      List<RetrievedExperience> candidates, List<AgentGoal> goals, String tenancyId) {

    List<AgentGoal> activeGoals =
        goals.stream().filter(g -> g.lifecycleState() == GoalLifecycleState.ACTIVE).toList();

    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("candidates must not be empty");
    }

    if (activeGoals.isEmpty()) {
      return new PlaybookSelection(
          candidates.get(0), 0.0, List.of(), "no active goals — selected first candidate");
    }

    RetrievedExperience best = null;
    double bestScore = -1.0;
    List<GoalAlignmentEntry> bestAlignment = List.of();

    for (RetrievedExperience candidate : candidates) {
      double qualitySignal = qualitySignal(candidate);
      double totalScore = 0.0;
      List<GoalAlignmentEntry> alignment = new ArrayList<>();

      for (AgentGoal goal : activeGoals) {
        GoalHorizon horizon = goal.horizon() != null ? goal.horizon() : GoalHorizon.MEDIUM_TERM;
        double horizonWeight = HORIZON_WEIGHTS.getOrDefault(horizon, 0.5);
        double score = candidate.similarityScore() * qualitySignal * horizonWeight;

        totalScore += score;
        alignment.add(
            new GoalAlignmentEntry(
                goal.name(),
                horizon,
                score,
                "quality=%.2f similarity=%.2f horizon=%s"
                    .formatted(qualitySignal, candidate.similarityScore(), horizon)));
      }

      if (totalScore > bestScore) {
        bestScore = totalScore;
        best = candidate;
        bestAlignment = alignment;
      }
    }

    return new PlaybookSelection(
        best,
        bestScore,
        bestAlignment,
        "selected by goal alignment: score=%.4f".formatted(bestScore));
  }

  double qualitySignal(RetrievedExperience candidate) {
    if (candidate.confidence() != null) {
      return candidate.confidence();
    }
    return OUTCOME_WEIGHTS.getOrDefault(candidate.outcome(), DEFAULT_OUTCOME_WEIGHT);
  }
}
