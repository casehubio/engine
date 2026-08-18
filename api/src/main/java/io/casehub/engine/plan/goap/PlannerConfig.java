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

import java.util.Set;

public record PlannerConfig(
    int maxIterations,
    Set<String> blacklistedActions,
    boolean backwardPruning,
    boolean forwardSimulation) {

  public static final int DEFAULT_MAX_ITERATIONS = 10_000;

  public static PlannerConfig defaults() {
    return new PlannerConfig(DEFAULT_MAX_ITERATIONS, Set.of(), true, true);
  }

  public PlannerConfig {
    blacklistedActions = Set.copyOf(blacklistedActions);
    if (maxIterations < 1) throw new IllegalArgumentException("maxIterations must be >= 1");
  }
}
