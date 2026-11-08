# io.casehub.api.model.coordination.SwarmCoordinationEvent.SwarmInitialised

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

### `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `swarmId` (`java.util.UUID`)

## Record Components

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

### `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `swarmId` (`java.util.UUID`)

## Constructors

### `public SwarmInitialised(java.util.UUID swarmId, java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences, io.casehub.api.model.stigmergy.SwarmProgress progress)`

#### Parameters

- `swarmId` (`java.util.UUID`)
- `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)
- `progress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences()`

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.SwarmProgress progress()`

### `public java.util.UUID swarmId()`

### `public final java.lang.String toString()`
