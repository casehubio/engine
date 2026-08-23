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

import io.casehub.eidos.api.AgentGoal;
import java.util.List;
import java.util.Objects;

public record GoalFormationResult(
    List<AgentGoal> registered, List<RejectedGoal> rejected, int totalGoalCount) {

  public GoalFormationResult {
    Objects.requireNonNull(registered, "registered must not be null");
    Objects.requireNonNull(rejected, "rejected must not be null");
    registered = List.copyOf(registered);
    rejected = List.copyOf(rejected);
  }

  public record RejectedGoal(String name, String reason) {
    public RejectedGoal {
      Objects.requireNonNull(name, "name must not be null");
      Objects.requireNonNull(reason, "reason must not be null");
    }
  }
}
