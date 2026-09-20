# io.casehub.api.model.stigmergy.DetectedTeam

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `avgAffinity` (`double`)

### `dominantRelations` (`java.util.Set<io.casehub.api.spi.observation.NeighborRelation>`)

### `memberAgents` (`java.util.Set<java.lang.String>`)

### `stabilityCount` (`int`)

### `teamId` (`java.lang.String`)

## Record Components

### `avgAffinity` (`double`)

### `dominantRelations` (`java.util.Set<io.casehub.api.spi.observation.NeighborRelation>`)

### `memberAgents` (`java.util.Set<java.lang.String>`)

### `stabilityCount` (`int`)

### `teamId` (`java.lang.String`)

## Constructors

### `public DetectedTeam(java.lang.String teamId, java.util.Set<java.lang.String> memberAgents, java.util.Set<io.casehub.api.spi.observation.NeighborRelation> dominantRelations, double avgAffinity, int stabilityCount)`

#### Parameters

- `teamId` (`java.lang.String`)
- `memberAgents` (`java.util.Set<java.lang.String>`)
- `dominantRelations` (`java.util.Set<io.casehub.api.spi.observation.NeighborRelation>`)
- `avgAffinity` (`double`)
- `stabilityCount` (`int`)

## Methods

### `public double avgAffinity()`

### `public java.util.Set<io.casehub.api.spi.observation.NeighborRelation> dominantRelations()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Set<java.lang.String> memberAgents()`

### `public int stabilityCount()`

### `public java.lang.String teamId()`

### `public final java.lang.String toString()`
