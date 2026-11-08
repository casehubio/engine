package io.casehub.api.model.coordination;

import io.casehub.api.model.stigmergy.DetectedRole;
import io.casehub.api.model.stigmergy.DetectedTeam;
import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.routing.RetrievedExperience;
import java.util.List;
import java.util.UUID;

public sealed interface SwarmCoordinationEvent {
  UUID swarmId();

  record SwarmInitialised(
      UUID swarmId, List<RetrievedExperience> experiences, SwarmProgress progress)
      implements SwarmCoordinationEvent {}

  record RoleChanged(UUID swarmId, List<DetectedRole> roles, SwarmProgress progress)
      implements SwarmCoordinationEvent {}

  record TeamChanged(UUID swarmId, List<DetectedTeam> teams, SwarmProgress progress)
      implements SwarmCoordinationEvent {}

  record ProgressUpdated(UUID swarmId, SwarmProgress progress) implements SwarmCoordinationEvent {}

  record MemberCompleted(UUID swarmId, String agentId, Object result)
      implements SwarmCoordinationEvent {}

  record ProvisioningChanged(UUID swarmId, int currentSize, int maxSize)
      implements SwarmCoordinationEvent {}
}
