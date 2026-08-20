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
package io.casehub.api.spi.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExperienceAnalyserActionCostTest {

  private static final double TOLERANCE = 0.001;

  private static ExperiencePlanStep step(String capabilityName, RoutingOutcome outcome) {
    return new ExperiencePlanStep(
        "b-" + capabilityName, capabilityName, "worker1", outcome, 0, Map.of());
  }

  private static ExperiencePlanStep adaptedStep(
      String capabilityName, RoutingOutcome outcome, String adaptationAction) {
    return new ExperiencePlanStep(
        "b-" + capabilityName,
        capabilityName,
        "worker1",
        outcome,
        0,
        Map.of(),
        adaptationAction,
        null);
  }

  private static RetrievedExperience exp(double similarity, ExperiencePlanStep... steps) {
    return new RetrievedExperience(
        "problem", "solution", "COMPLETED", 1.0, similarity, Map.of(), List.of(steps), Map.of());
  }

  @Test
  void single_action_all_success_returns_factor_1() {
    var experiences =
        List.of(
            exp(1.0, step("analyse", RoutingOutcome.SUCCESS)),
            exp(1.0, step("analyse", RoutingOutcome.SUCCESS)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("analyse"), 1);
    assertThat(result).containsKey("analyse");
    assertThat(result.get("analyse")).isCloseTo(1.0, within(TOLERANCE));
  }

  @Test
  void partial_success_returns_proportional_factor() {
    // 3 success + 1 failure: weighted sum = (3*1.0 + 1*-1.0) / 4 = 0.5 → factor = 2.0
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.FAILURE)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("a"), 1);
    assertThat(result.get("a")).isCloseTo(2.0, within(TOLERANCE));
  }

  @Test
  void all_failures_capped_at_max_cost_factor() {
    var experiences =
        List.of(
            exp(1.0, step("analyse", RoutingOutcome.FAILURE)),
            exp(1.0, step("analyse", RoutingOutcome.FAILURE)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("analyse"), 1);
    assertThat(result.get("analyse")).isCloseTo(10.0, within(TOLERANCE));
  }

  @Test
  void below_min_samples_excluded() {
    var experiences = List.of(exp(1.0, step("analyse", RoutingOutcome.SUCCESS)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("analyse"), 5);
    assertThat(result).isEmpty();
  }

  @Test
  void exactly_min_samples_included() {
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("a"), 5);
    assertThat(result).containsKey("a");
  }

  @Test
  void multiple_actions_independent_factors() {
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.SUCCESS), step("b", RoutingOutcome.FAILURE)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS), step("b", RoutingOutcome.FAILURE)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("a", "b"), 1);
    assertThat(result.get("a")).isCloseTo(1.0, within(TOLERANCE));
    assertThat(result.get("b")).isCloseTo(10.0, within(TOLERANCE));
  }

  @Test
  void similarity_weighting_high_similarity_contributes_more() {
    var experiences =
        List.of(
            exp(0.9, step("a", RoutingOutcome.SUCCESS)),
            exp(0.1, step("a", RoutingOutcome.FAILURE)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("a"), 1);
    // weighted: (1.0*0.9 + -1.0*0.1) / (0.9+0.1) = 0.8/1.0 = 0.8 → factor = 1.25
    assertThat(result.get("a")).isCloseTo(1.0 / 0.8, within(TOLERANCE));
  }

  @Test
  void added_substituted_steps_skipped() {
    var experiences =
        List.of(
            exp(
                1.0,
                step("a", RoutingOutcome.SUCCESS),
                adaptedStep("a", RoutingOutcome.FAILURE, "ADDED")),
            exp(
                1.0,
                step("a", RoutingOutcome.SUCCESS),
                adaptedStep("a", RoutingOutcome.FAILURE, "SUBSTITUTED")));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("a"), 1);
    assertThat(result.get("a")).isCloseTo(1.0, within(TOLERANCE));
  }

  @Test
  void empty_experiences_returns_empty_map() {
    var result = ExperienceAnalyser.actionCostFactors(List.of(), Set.of("a"), 1);
    assertThat(result).isEmpty();
  }

  @Test
  void no_matching_action_names_returns_empty_map() {
    var experiences = List.of(exp(1.0, step("b", RoutingOutcome.SUCCESS)));
    var result = ExperienceAnalyser.actionCostFactors(experiences, Set.of("a"), 1);
    assertThat(result).isEmpty();
  }

  @Test
  void clamping_boundary_low_success_rate_capped() {
    // 21 successes + 19 failures: rate = (21-19)/40 = 0.05, below 1/10=0.1 → capped at 10.0
    var exps = new ArrayList<RetrievedExperience>();
    for (int i = 0; i < 21; i++) exps.add(exp(1.0, step("a", RoutingOutcome.SUCCESS)));
    for (int i = 0; i < 19; i++) exps.add(exp(1.0, step("a", RoutingOutcome.FAILURE)));
    var result = ExperienceAnalyser.actionCostFactors(exps, Set.of("a"), 1);
    assertThat(result.get("a")).isCloseTo(10.0, within(TOLERANCE));
  }

  @Test
  void custom_max_cost_factor() {
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.FAILURE)),
            exp(1.0, step("a", RoutingOutcome.FAILURE)));
    var result =
        ExperienceAnalyser.actionCostFactors(
            experiences, Set.of("a"), 1, 5.0, ExperienceAnalyser.DEFAULT_OUTCOME_WEIGHTS);
    assertThat(result.get("a")).isCloseTo(5.0, within(TOLERANCE));
  }
}
