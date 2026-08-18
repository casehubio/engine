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
package io.casehub.engine.plan.goap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GoapPlannerTest {

  private final GoapPlanner planner = new GoapPlanner();

  @Test
  void plan_compound_goals() {
    var a1 = new GoapAction("a1", Map.of(), Map.of("x", true), 0.3);
    var a2 = new GoapAction("a2", Map.of(), Map.of("y", true), 0.5);
    var initial = GoapWorldState.closedWorld(Map.of());

    List<GoapAction> plan = planner.plan(initial, Set.of("x", "y"), List.of(a1, a2));
    assertThat(plan).extracting(GoapAction::name).containsExactlyInAnyOrder("a1", "a2");
  }

  @Test
  void plan_compound_goals_already_satisfied() {
    var a1 = new GoapAction("a1", Map.of(), Map.of("x", true), 0.3);
    var initial = GoapWorldState.closedWorld(Map.of("x", true, "y", true));

    List<GoapAction> plan = planner.plan(initial, Set.of("x", "y"), List.of(a1));
    assertThat(plan).isEmpty();
  }

  @Test
  void plan_prefers_lower_effective_cost() {
    var cheap = new GoapAction("cheap", Map.of(), Map.of("x", true), 0.3, 0.5, Map.of());
    var expensive = new GoapAction("expensive", Map.of(), Map.of("x", true), 0.8, 0.0, Map.of());
    var initial = GoapWorldState.closedWorld(Map.of());

    List<GoapAction> plan = planner.plan(initial, Set.of("x"), List.of(cheap, expensive));
    assertThat(plan).extracting(GoapAction::name).containsExactly("cheap");
  }

  @Test
  void plan_chains_dependencies() {
    var a1 = new GoapAction("a1", Map.of(), Map.of("x", true), 0.3);
    var a2 = new GoapAction("a2", Map.of("x", true), Map.of("y", true), 0.5);
    var initial = GoapWorldState.closedWorld(Map.of("x", false));

    List<GoapAction> plan = planner.plan(initial, Set.of("y"), List.of(a1, a2));
    assertThat(plan).extracting(GoapAction::name).containsExactly("a1", "a2");
  }

  @Test
  void plan_soft_precondition_penalty() {
    var withSoft =
        new GoapAction("withSoft", Map.of(), Map.of("x", true), 0.5, 0.0, Map.of("optional", true));
    var withoutSoft =
        new GoapAction("withoutSoft", Map.of(), Map.of("x", true), 0.5, 0.0, Map.of());
    var initial = GoapWorldState.closedWorld(Map.of());

    List<GoapAction> plan = planner.plan(initial, Set.of("x"), List.of(withSoft, withoutSoft));
    assertThat(plan).extracting(GoapAction::name).containsExactly("withoutSoft");
  }

  @Test
  void plan_single_goal_backward_compat() {
    var a1 = new GoapAction("a1", Map.of(), Map.of("x", true), 0.3);
    var initial = GoapWorldState.closedWorld(Map.of());

    List<GoapAction> plan = planner.plan(initial, "x", List.of(a1));
    assertThat(plan).extracting(GoapAction::name).containsExactly("a1");
  }

  @Test
  void plan_unreachable_returns_empty() {
    var a1 = new GoapAction("a1", Map.of("missing", true), Map.of("x", true), 0.3);
    var initial = GoapWorldState.closedWorld(Map.of("missing", false));

    List<GoapAction> plan = planner.plan(initial, Set.of("x"), List.of(a1));
    assertThat(plan).isEmpty();
  }

  @Test
  void plan_respects_iteration_ceiling() {
    var a1 = new GoapAction("a1", Map.of(), Map.of("step1", true), 0.5);
    var a2 = new GoapAction("a2", Map.of("step1", true), Map.of("goal", true), 0.5);
    var initial = GoapWorldState.closedWorld(Map.of("step1", false));
    var config = new PlannerConfig(1, Set.of(), false, false);

    var result = planner.plan(initial, Set.of("goal"), List.of(a1, a2), config);
    assertThat(result).isEmpty();
  }

  @Test
  void plan_excludes_blacklisted_actions() {
    var good = new GoapAction("good", Map.of(), Map.of("goal", true), 0.5);
    var bad = new GoapAction("bad", Map.of(), Map.of("goal", true), 0.1);
    var initial = GoapWorldState.closedWorld(Map.of());
    var config = new PlannerConfig(10_000, Set.of("bad"), false, false);

    var result = planner.plan(initial, Set.of("goal"), List.of(good, bad), config);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("good");
  }

  @Test
  void plan_backward_pruning_removes_irrelevant_actions() {
    var relevant = new GoapAction("relevant", Map.of(), Map.of("goal", true), 0.5);
    var irrelevant = new GoapAction("irrelevant", Map.of(), Map.of("other", true), 0.1);
    var initial = GoapWorldState.closedWorld(Map.of());
    var config = new PlannerConfig(10_000, Set.of(), true, false);

    var result = planner.plan(initial, Set.of("goal"), List.of(relevant, irrelevant), config);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("relevant");
  }

  @Test
  void plan_backward_pruning_keeps_transitive_dependencies() {
    var a1 = new GoapAction("step1", Map.of(), Map.of("intermediate", true), 0.5);
    var a2 = new GoapAction("step2", Map.of("intermediate", true), Map.of("goal", true), 0.5);
    var noise = new GoapAction("noise", Map.of(), Map.of("unrelated", true), 0.1);
    var initial = GoapWorldState.closedWorld(Map.of("intermediate", false));
    var config = new PlannerConfig(10_000, Set.of(), true, false);

    var result = planner.plan(initial, Set.of("goal"), List.of(a1, a2, noise), config);
    assertThat(result).extracting(GoapAction::name).containsExactly("step1", "step2");
  }

  @Test
  void plan_forward_simulation_strips_redundant_actions() {
    var a1 = new GoapAction("setup_x", Map.of(), Map.of("x", true), 0.5);
    var a2 = new GoapAction("setup_xy", Map.of(), Map.of("x", true, "y", true), 0.6);
    var a3 = new GoapAction("finish", Map.of("x", true, "y", true), Map.of("goal", true), 0.3);
    var initial = GoapWorldState.closedWorld(Map.of("x", false, "y", false));
    var config = new PlannerConfig(10_000, Set.of(), false, true);

    var result = planner.plan(initial, Set.of("goal"), List.of(a1, a2, a3), config);
    assertThat(result).extracting(GoapAction::name).doesNotContain("setup_x");
  }

  @Test
  void plan_with_high_cost_actions_still_finds_plan() {
    var a1 = new GoapAction("expensive", Map.of(), Map.of("goal", true), 50.0);
    var initial = GoapWorldState.closedWorld(Map.of());
    var config = PlannerConfig.defaults();

    var result = planner.plan(initial, Set.of("goal"), List.of(a1), config);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("expensive");
  }

  @Test
  void plan_uses_dynamic_cost() {
    CostFunction cheapWhenSmall = state -> state.get("large") == Condition.TRUE ? 100.0 : 1.0;
    var dynamic =
        new GoapAction(
            "dynamic", Map.of(), Map.of("goal", true), 1.0, 0.0, Map.of(), cheapWhenSmall);
    var fixed = new GoapAction("fixed", Map.of(), Map.of("goal", true), 5.0);
    var initial = GoapWorldState.closedWorld(Map.of("large", false));
    var config = PlannerConfig.defaults();

    var result = planner.plan(initial, Set.of("goal"), List.of(dynamic, fixed), config);
    assertThat(result.get(0).name()).isEqualTo("dynamic");
  }
}
