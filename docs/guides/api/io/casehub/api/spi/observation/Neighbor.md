# io.casehub.api.spi.observation.Neighbor

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `agentId` (`java.lang.String`)

### `bindingName` (`java.lang.String`)

### `capabilities` (`java.util.Set<java.lang.String>`)

### `currentStatus` (`io.casehub.api.model.TaskStatus`)

### `relations` (`java.util.Set<io.casehub.api.spi.observation.NeighborRelation>`)

## Record Components

### `agentId` (`java.lang.String`)

### `bindingName` (`java.lang.String`)

### `capabilities` (`java.util.Set<java.lang.String>`)

### `currentStatus` (`io.casehub.api.model.TaskStatus`)

### `relations` (`java.util.Set<io.casehub.api.spi.observation.NeighborRelation>`)

## Constructors

### `public Neighbor(java.lang.String agentId, java.util.Set<java.lang.String> capabilities, io.casehub.api.model.TaskStatus currentStatus, java.lang.String bindingName, java.util.Set<io.casehub.api.spi.observation.NeighborRelation> relations)`

#### Parameters

- `agentId` (`java.lang.String`)
- `capabilities` (`java.util.Set<java.lang.String>`)
- `currentStatus` (`io.casehub.api.model.TaskStatus`)
- `bindingName` (`java.lang.String`)
- `relations` (`java.util.Set<io.casehub.api.spi.observation.NeighborRelation>`)

## Methods

### `public java.lang.String agentId()`

### `public java.lang.String bindingName()`

### `public java.util.Set<java.lang.String> capabilities()`

### `public io.casehub.api.model.TaskStatus currentStatus()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Set<io.casehub.api.spi.observation.NeighborRelation> relations()`

### `public final java.lang.String toString()`
