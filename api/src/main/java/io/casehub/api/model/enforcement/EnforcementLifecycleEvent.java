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

import io.casehub.api.model.coordination.CommunicationMode;
import java.util.List;
import java.util.Objects;

public sealed interface EnforcementLifecycleEvent
    permits EnforcementLifecycleEvent.Activated,
        EnforcementLifecycleEvent.Deactivated,
        EnforcementLifecycleEvent.RulesUpdated {

  record Activated(String moderatorId, CommunicationMode mode, List<InteractionRule> rules)
      implements EnforcementLifecycleEvent {
    public Activated {
      Objects.requireNonNull(moderatorId, "moderatorId must not be null");
      Objects.requireNonNull(mode, "mode must not be null");
      rules = rules != null ? List.copyOf(rules) : List.of();
    }
  }

  record Deactivated(String moderatorId, String reason) implements EnforcementLifecycleEvent {
    public Deactivated {
      Objects.requireNonNull(moderatorId, "moderatorId must not be null");
      Objects.requireNonNull(reason, "reason must not be null");
    }
  }

  record RulesUpdated(
      String moderatorId, List<InteractionRule> previousRules, List<InteractionRule> newRules)
      implements EnforcementLifecycleEvent {
    public RulesUpdated {
      Objects.requireNonNull(moderatorId, "moderatorId must not be null");
      previousRules = previousRules != null ? List.copyOf(previousRules) : List.of();
      newRules = newRules != null ? List.copyOf(newRules) : List.of();
    }
  }
}
