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
package io.casehub.api.model.enforcement;

import jakarta.annotation.Nullable;
import java.util.Objects;

public sealed interface TurnDecision
    permits TurnDecision.Permitted, TurnDecision.Denied, TurnDecision.Deferred {

  record Permitted(String agentId) implements TurnDecision {
    public Permitted {
      Objects.requireNonNull(agentId, "agentId must not be null");
    }
  }

  record Denied(String agentId, String reason) implements TurnDecision {
    public Denied {
      Objects.requireNonNull(agentId, "agentId must not be null");
      Objects.requireNonNull(reason, "reason must not be null");
    }
  }

  record Deferred(String agentId, @Nullable String reason) implements TurnDecision {
    public Deferred {
      Objects.requireNonNull(agentId, "agentId must not be null");
    }
  }
}
