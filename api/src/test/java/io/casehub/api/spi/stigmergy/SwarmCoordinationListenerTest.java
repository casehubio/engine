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
package io.casehub.api.spi.stigmergy;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.stigmergy.SwarmProgress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SwarmCoordinationListenerTest {

  @Test
  void new_swarmId_overloads_compile() {
    var caseId = UUID.randomUUID();
    var swarmId = UUID.randomUUID();
    var progress = new SwarmProgress(0.5, 0.5, 0.5, Instant.now());

    SwarmCoordinationListener listener = new SwarmCoordinationListener() {};
    listener.onSwarmInitialised(caseId, swarmId, progress);
    listener.onProgressUpdated(caseId, swarmId, progress);
    listener.onRoleChanged(caseId, swarmId, List.of(), progress);
    listener.onTeamChanged(caseId, swarmId, List.of(), progress);
    listener.onMemberCompleted(caseId, swarmId, "agent-a");
    listener.onProvisioningChanged(caseId, swarmId, 3, 5);
    listener.onTelemetrySnapshot(caseId, swarmId, List.of());
    assertThat(listener).isNotNull();
  }

  @Test
  void old_methods_still_compile_for_backward_compat() {
    var caseId = UUID.randomUUID();
    var progress = new SwarmProgress(0.5, 0.5, 0.5, Instant.now());

    SwarmCoordinationListener listener = new SwarmCoordinationListener() {};
    listener.onSwarmInitialised(caseId, progress);
    listener.onProgressUpdated(caseId, progress);
    assertThat(listener).isNotNull();
  }
}
