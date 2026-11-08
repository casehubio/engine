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
package io.casehub.api.model.coordination;

import jakarta.annotation.Nullable;
import java.util.Objects;

public record TransitionPolicy(
    TopologyType from,
    TopologyType to,
    TransitionTrigger trigger,
    TransitionStrategy strategy,
    boolean reversible,
    @Nullable TransitionTrigger rollbackTrigger) {

  public TransitionPolicy {
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    Objects.requireNonNull(trigger, "trigger must not be null");
    Objects.requireNonNull(strategy, "strategy must not be null");
    if (from == to)
      throw new IllegalArgumentException(
          "from and to must differ — a transition to the same topology is a no-op");
    if (!reversible && rollbackTrigger != null)
      throw new IllegalArgumentException("rollbackTrigger requires reversible=true");
  }
}
