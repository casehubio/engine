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

import java.util.List;
import java.util.Objects;

public sealed interface CoordinationResult
    permits CoordinationResult.Aggregated, CoordinationResult.Partial, CoordinationResult.Failed {

  record Aggregated(String payload, List<String> contributors) implements CoordinationResult {
    public Aggregated {
      Objects.requireNonNull(payload, "payload must not be null");
      contributors = contributors != null ? List.copyOf(contributors) : List.of();
    }
  }

  record Partial(String payload, List<String> completed, List<String> pending)
      implements CoordinationResult {
    public Partial {
      Objects.requireNonNull(payload, "payload must not be null");
      completed = completed != null ? List.copyOf(completed) : List.of();
      pending = pending != null ? List.copyOf(pending) : List.of();
    }
  }

  record Failed(String reason, List<String> failedAgents) implements CoordinationResult {
    public Failed {
      Objects.requireNonNull(reason, "reason must not be null");
      failedAgents = failedAgents != null ? List.copyOf(failedAgents) : List.of();
    }
  }
}
