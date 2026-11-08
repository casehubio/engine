# io.casehub.api.model.coordination.SwarmDescriptor

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `agentIds` (`java.util.Set<java.lang.String>`)

### `capabilities` (`java.util.Set<java.lang.String>`)

### `caseId` (`java.util.UUID`)

### `parentSwarmId` (`java.util.UUID`)

### `swarmId` (`java.util.UUID`)

### `topologyType` (`io.casehub.api.model.coordination.TopologyType`)

## Record Components

### `agentIds` (`java.util.Set<java.lang.String>`)

### `capabilities` (`java.util.Set<java.lang.String>`)

### `caseId` (`java.util.UUID`)

### `parentSwarmId` (`java.util.UUID`)

### `swarmId` (`java.util.UUID`)

### `topologyType` (`io.casehub.api.model.coordination.TopologyType`)

## Constructors

### `public SwarmDescriptor(java.util.UUID swarmId, java.util.UUID caseId, io.casehub.api.model.coordination.TopologyType topologyType, java.util.Set<java.lang.String> capabilities, java.util.Set<java.lang.String> agentIds, java.util.UUID parentSwarmId)`

#### Parameters

- `swarmId` (`java.util.UUID`)
- `caseId` (`java.util.UUID`)
- `topologyType` (`io.casehub.api.model.coordination.TopologyType`)
- `capabilities` (`java.util.Set<java.lang.String>`)
- `agentIds` (`java.util.Set<java.lang.String>`)
- `parentSwarmId` (`java.util.UUID`)

## Methods

### `public java.util.Set<java.lang.String> agentIds()`

### `public java.util.Set<java.lang.String> capabilities()`

### `public java.util.UUID caseId()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.UUID parentSwarmId()`

### `public java.util.UUID swarmId()`

### `public final java.lang.String toString()`

### `public io.casehub.api.model.coordination.TopologyType topologyType()`
