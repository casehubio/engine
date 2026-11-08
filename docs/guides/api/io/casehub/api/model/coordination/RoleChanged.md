# io.casehub.api.model.coordination.SwarmCoordinationEvent.RoleChanged

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `roles` (`java.util.List<io.casehub.api.model.stigmergy.DetectedRole>`)

### `swarmId` (`java.util.UUID`)

## Record Components

### `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `roles` (`java.util.List<io.casehub.api.model.stigmergy.DetectedRole>`)

### `swarmId` (`java.util.UUID`)

## Constructors

### `public RoleChanged(java.util.UUID swarmId, java.util.List<io.casehub.api.model.stigmergy.DetectedRole> roles, io.casehub.api.model.stigmergy.SwarmProgress progress)`

#### Parameters

- `swarmId` (`java.util.UUID`)
- `roles` (`java.util.List<io.casehub.api.model.stigmergy.DetectedRole>`)
- `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.SwarmProgress progress()`

### `public java.util.List<io.casehub.api.model.stigmergy.DetectedRole> roles()`

### `public java.util.UUID swarmId()`

### `public final java.lang.String toString()`
