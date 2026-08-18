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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class GoapPlanner {

  public List<GoapAction> plan(
      GoapWorldState initial, String goalCondition, List<GoapAction> actions) {
    return plan(initial, Set.of(goalCondition), actions, PlannerConfig.defaults());
  }

  public List<GoapAction> plan(
      GoapWorldState initial, Set<String> goalConditions, List<GoapAction> actions) {
    return plan(initial, goalConditions, actions, PlannerConfig.defaults());
  }

  public List<GoapAction> plan(
      GoapWorldState initial,
      Set<String> goalConditions,
      List<GoapAction> actions,
      PlannerConfig config) {

    if (goalConditions.isEmpty() || initial.satisfiesAll(goalConditions)) {
      return List.of();
    }

    List<GoapAction> filtered =
        actions.stream().filter(a -> !config.blacklistedActions().contains(a.name())).toList();

    if (config.backwardPruning()) {
      filtered = backwardPrune(filtered, goalConditions);
    }

    if (filtered.isEmpty()) {
      return List.of();
    }

    double minCost =
        filtered.stream()
            .mapToDouble(GoapAction::effectiveCost)
            .filter(c -> c > 0)
            .min()
            .orElse(1.0);

    record Node(GoapWorldState state, List<GoapAction> plan, double cost) {}

    PriorityQueue<Node> open =
        new PriorityQueue<>(
            Comparator.comparingDouble(
                n -> n.cost() + heuristic(n.state(), goalConditions, minCost)));
    open.add(new Node(initial, List.of(), 0.0));

    Set<Map<String, Condition>> visited = new HashSet<>();
    int iterations = 0;

    while (!open.isEmpty()) {
      if (iterations++ >= config.maxIterations()) {
        return List.of();
      }

      Node current = open.poll();
      if (current.state().satisfiesAll(goalConditions)) {
        List<GoapAction> plan = current.plan();
        if (config.forwardSimulation()) {
          plan = forwardSimulate(plan, initial);
        }
        return plan;
      }
      if (!visited.add(current.state().conditions())) {
        continue;
      }

      for (GoapAction action : filtered) {
        if (action.isApplicable(current.state())) {
          GoapWorldState next = action.applyTo(current.state());
          List<GoapAction> newPlan = new ArrayList<>(current.plan());
          newPlan.add(action);
          double softPenalty = softPenalty(action, current.state());
          double actionCost = action.effectiveCost(current.state());
          open.add(new Node(next, newPlan, current.cost() + actionCost + softPenalty));
        }
      }
    }
    return List.of();
  }

  private List<GoapAction> backwardPrune(List<GoapAction> actions, Set<String> goalConditions) {
    Set<String> relevant = new HashSet<>(goalConditions);
    boolean changed = true;
    while (changed) {
      changed = false;
      for (GoapAction action : actions) {
        boolean contributes = action.effects().keySet().stream().anyMatch(relevant::contains);
        if (contributes) {
          for (String pre : action.preconditions().keySet()) {
            if (relevant.add(pre)) {
              changed = true;
            }
          }
          for (String soft : action.softPreconditions().keySet()) {
            if (relevant.add(soft)) {
              changed = true;
            }
          }
        }
      }
    }
    return actions.stream()
        .filter(a -> a.effects().keySet().stream().anyMatch(relevant::contains))
        .toList();
  }

  private List<GoapAction> forwardSimulate(List<GoapAction> plan, GoapWorldState initial) {
    List<GoapAction> result = new ArrayList<>();
    GoapWorldState current = initial;
    for (GoapAction action : plan) {
      GoapWorldState s = current;
      boolean allEffectsSatisfied =
          action.effects().entrySet().stream()
              .allMatch(e -> s.get(e.getKey()) == Condition.fromBoolean(e.getValue()));
      if (!allEffectsSatisfied) {
        result.add(action);
        current = action.applyTo(current);
      }
    }
    return result;
  }

  private double softPenalty(GoapAction action, GoapWorldState state) {
    long unsatisfied =
        action.softPreconditions().entrySet().stream()
            .filter(
                e -> {
                  Condition c = state.get(e.getKey());
                  if (c == Condition.UNKNOWN) {
                    return true;
                  }
                  return (c == Condition.TRUE) != e.getValue();
                })
            .count();
    if (unsatisfied == 0) {
      return 0.0;
    }
    return Math.max(0.5 * action.effectiveCost(), 0.1);
  }

  private double heuristic(GoapWorldState state, Set<String> goalConditions, double minCost) {
    return goalConditions.stream().filter(c -> !state.satisfies(c)).count() * minCost;
  }
}
