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

import io.casehub.api.model.stigmergy.DetectedRole;
import io.casehub.api.model.stigmergy.DetectedTeam;
import io.casehub.api.model.stigmergy.SwarmProgress;
import java.util.List;
import java.util.UUID;

/**
 * Receives swarm lifecycle events from the engine runtime.
 *
 * <p>Implementations are multi-instance: multiple listeners coexist via CDI {@code Instance} /
 * Spring {@code ObjectProvider}. Do not use {@code @DefaultBean} on implementations.
 */
public interface SwarmCoordinationListener {

  default void onSwarmInitialised(UUID caseId, SwarmProgress progress) {}

  default void onRoleChanged(UUID caseId, List<DetectedRole> roles, SwarmProgress progress) {}

  default void onTeamChanged(UUID caseId, List<DetectedTeam> teams, SwarmProgress progress) {}

  default void onProgressUpdated(UUID caseId, SwarmProgress progress) {}

  default void onMemberCompleted(UUID caseId, String agentId) {}

  default void onProvisioningChanged(UUID caseId, int currentSize, int maxSize) {}
}
