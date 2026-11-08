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

import java.util.Objects;

public sealed interface CoordinationLifecycleEvent
    permits CoordinationLifecycleEvent.Started,
        CoordinationLifecycleEvent.Stopped,
        CoordinationLifecycleEvent.Reconfigured {

  record Started(String coordinatorId, TopologyDescriptor topology)
      implements CoordinationLifecycleEvent {
    public Started {
      Objects.requireNonNull(coordinatorId, "coordinatorId must not be null");
      Objects.requireNonNull(topology, "topology must not be null");
    }
  }

  record Stopped(String coordinatorId, String reason) implements CoordinationLifecycleEvent {
    public Stopped {
      Objects.requireNonNull(coordinatorId, "coordinatorId must not be null");
      Objects.requireNonNull(reason, "reason must not be null");
    }
  }

  record Reconfigured(
      String coordinatorId, TopologyDescriptor previousTopology, TopologyDescriptor newTopology)
      implements CoordinationLifecycleEvent {
    public Reconfigured {
      Objects.requireNonNull(coordinatorId, "coordinatorId must not be null");
      Objects.requireNonNull(previousTopology, "previousTopology must not be null");
      Objects.requireNonNull(newTopology, "newTopology must not be null");
    }
  }
}
