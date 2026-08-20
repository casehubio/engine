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
package io.casehub.engine.planning.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.casehub.api.spi.routing.ExperiencePlanStep;
import io.casehub.api.spi.routing.RetrievedExperience;
import io.casehub.api.spi.routing.RoutingOutcome;
import io.casehub.engine.plan.goap.CostFunction;
import io.casehub.engine.plan.goap.GoapAction;
import io.casehub.engine.plan.goap.GoapWorldState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoapCostEnricherTest {

  private static final double TOLERANCE = 0.001;

  private static ExperiencePlanStep step(String capName, RoutingOutcome outcome) {
    return new ExperiencePlanStep("b-" + capName, capName, "w1", outcome, 0, Map.of());
  }

  private static RetrievedExperience exp(double sim, ExperiencePlanStep... steps) {
    return new RetrievedExperience(
        "p", "s", "COMPLETED", 1.0, sim, Map.of(), List.of(steps), Map.of());
  }

  @Test
  void empty_experiences_returns_actions_unchanged() {
    var action = new GoapAction("a", Map.of("x", true), Map.of("y", true), 1.0);
    var result = GoapCostEnricher.enrichWithLearnedCosts(List.of(action), List.of(), 1);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).costFunction()).isNull();
  }

  @Test
  void all_actions_below_threshold_returns_unchanged() {
    var action = new GoapAction("a", Map.of("x", true), Map.of("y", true), 1.0);
    var experiences = List.of(exp(1.0, step("a", RoutingOutcome.SUCCESS)));
    var result = GoapCostEnricher.enrichWithLearnedCosts(List.of(action), experiences, 10);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).costFunction()).isNull();
  }

  @Test
  void partial_enrichment_only_matching_actions() {
    var a1 = new GoapAction("a", Map.of(), Map.of("x", true), 1.0);
    var a2 = new GoapAction("b", Map.of("x", true), Map.of("y", true), 1.0);
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.FAILURE)),
            exp(1.0, step("a", RoutingOutcome.FAILURE)));
    var result = GoapCostEnricher.enrichWithLearnedCosts(List.of(a1, a2), experiences, 1);
    assertThat(result.get(0).costFunction()).isNotNull();
    assertThat(result.get(1).costFunction()).isNull();
  }

  @Test
  void wraps_existing_cost_function() {
    CostFunction dynamic = state -> 2.0;
    var action = new GoapAction("a", Map.of(), Map.of("x", true), 1.0, 0.0, Map.of(), dynamic);
    // 3 success + 1 failure → rate = 0.5 → factor = 2.0
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.FAILURE)));
    var result = GoapCostEnricher.enrichWithLearnedCosts(List.of(action), experiences, 1);
    var enriched = result.get(0);
    assertThat(enriched.costFunction()).isNotNull();
    var ws = GoapWorldState.closedWorld(Map.of("x", true));
    // dynamic returns 2.0, factor = 2.0 → enriched = 4.0
    assertThat(enriched.costFunction().compute(ws)).isCloseTo(4.0, within(TOLERANCE));
  }

  @Test
  void wraps_static_cost_when_no_cost_function() {
    var action = new GoapAction("a", Map.of(), Map.of("x", true), 3.0);
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.FAILURE)));
    var result = GoapCostEnricher.enrichWithLearnedCosts(List.of(action), experiences, 1);
    var enriched = result.get(0);
    var ws = GoapWorldState.closedWorld(Map.of("x", true));
    // static cost 3.0 × factor 2.0 = 6.0
    assertThat(enriched.costFunction().compute(ws)).isCloseTo(6.0, within(TOLERANCE));
  }

  @Test
  void benefit_preserved_in_effective_cost() {
    var action = new GoapAction("a", Map.of(), Map.of("x", true), 2.0, 0.5, Map.of());
    var experiences =
        List.of(
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.SUCCESS)),
            exp(1.0, step("a", RoutingOutcome.FAILURE)));
    var result = GoapCostEnricher.enrichWithLearnedCosts(List.of(action), experiences, 1);
    var enriched = result.get(0);
    var ws = GoapWorldState.closedWorld(Map.of("x", true));
    // static 2.0 × factor 2.0 = 4.0 base, then effectiveCost applies × (1-0.5) = 2.0
    assertThat(enriched.effectiveCost(ws)).isCloseTo(2.0, within(TOLERANCE));
  }
}
