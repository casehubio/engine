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

import java.util.Map;

public record GoapAction(
    String name,
    Map<String, Boolean> preconditions,
    Map<String, Boolean> effects,
    double cost,
    double benefit,
    Map<String, Boolean> softPreconditions,
    CostFunction costFunction) {

  public GoapAction {
    preconditions = Map.copyOf(preconditions);
    effects = Map.copyOf(effects);
    softPreconditions = Map.copyOf(softPreconditions);
    if (cost < 0.0) {
      throw new IllegalArgumentException("cost must be >= 0.0");
    }
    if (benefit < 0.0 || benefit > 1.0) {
      throw new IllegalArgumentException("benefit must be in [0.0, 1.0]");
    }
  }

  public GoapAction(
      String name, Map<String, Boolean> preconditions, Map<String, Boolean> effects, double cost) {
    this(name, preconditions, effects, cost, 0.0, Map.of(), null);
  }

  public GoapAction(
      String name,
      Map<String, Boolean> preconditions,
      Map<String, Boolean> effects,
      double cost,
      double benefit,
      Map<String, Boolean> softPreconditions) {
    this(name, preconditions, effects, cost, benefit, softPreconditions, null);
  }

  public double effectiveCost() {
    return cost * (1.0 - benefit);
  }

  public double effectiveCost(GoapWorldState state) {
    if (costFunction != null) {
      return costFunction.compute(state) * (1.0 - benefit);
    }
    return effectiveCost();
  }

  public boolean isApplicable(GoapWorldState state) {
    return preconditions.entrySet().stream()
        .allMatch(
            e -> {
              Condition c = state.get(e.getKey());
              if (c == Condition.UNKNOWN) {
                return true;
              }
              return (c == Condition.TRUE) == e.getValue();
            });
  }

  public GoapWorldState applyTo(GoapWorldState state) {
    GoapWorldState result = state;
    for (var entry : effects.entrySet()) {
      result = result.with(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
