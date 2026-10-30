# io.casehub.api.spi.stigmergy.SwarmCoordinationListener

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `interface`

Receives swarm lifecycle events from the engine runtime.

<p>Implementations are multi-instance: multiple listeners coexist via CDI `Instance` /
Spring `ObjectProvider`. Do not use `@DefaultBean` on implementations.

## Methods

### `public default void onMemberCompleted(java.util.UUID caseId, java.lang.String agentId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)

### `public default void onProgressUpdated(java.util.UUID caseId, io.casehub.api.model.stigmergy.SwarmProgress progress)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `public default void onProvisioningChanged(java.util.UUID caseId, int currentSize, int maxSize)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `currentSize` (`int`)
- `maxSize` (`int`)

### `public default void onRoleChanged(java.util.UUID caseId, java.util.List<io.casehub.api.model.stigmergy.DetectedRole> roles, io.casehub.api.model.stigmergy.SwarmProgress progress)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `roles` (`java.util.List<io.casehub.api.model.stigmergy.DetectedRole>`)
- `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `public default void onSwarmInitialised(java.util.UUID caseId, io.casehub.api.model.stigmergy.SwarmProgress progress)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `public default void onTeamChanged(java.util.UUID caseId, java.util.List<io.casehub.api.model.stigmergy.DetectedTeam> teams, io.casehub.api.model.stigmergy.SwarmProgress progress)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `teams` (`java.util.List<io.casehub.api.model.stigmergy.DetectedTeam>`)
- `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)
