# io.casehub.api.model.coordination.SwarmCoordinationEvent.TeamChanged

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `swarmId` (`java.util.UUID`)

### `teams` (`java.util.List<io.casehub.api.model.stigmergy.DetectedTeam>`)

## Record Components

### `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `swarmId` (`java.util.UUID`)

### `teams` (`java.util.List<io.casehub.api.model.stigmergy.DetectedTeam>`)

## Constructors

### `public TeamChanged(java.util.UUID swarmId, java.util.List<io.casehub.api.model.stigmergy.DetectedTeam> teams, io.casehub.api.model.stigmergy.SwarmProgress progress)`

#### Parameters

- `swarmId` (`java.util.UUID`)
- `teams` (`java.util.List<io.casehub.api.model.stigmergy.DetectedTeam>`)
- `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.SwarmProgress progress()`

### `public java.util.UUID swarmId()`

### `public java.util.List<io.casehub.api.model.stigmergy.DetectedTeam> teams()`

### `public final java.lang.String toString()`
