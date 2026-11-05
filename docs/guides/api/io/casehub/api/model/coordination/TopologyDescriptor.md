# io.casehub.api.model.coordination.TopologyDescriptor

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `agentRoles` (`java.util.Map<java.lang.String,io.casehub.api.model.coordination.AgentRole>`)

### `coordinationPattern` (`io.casehub.api.model.coordination.CoordinationPattern`)

### `groupId` (`java.lang.String`)

### `topologyType` (`io.casehub.api.model.coordination.TopologyType`)

## Record Components

### `agentRoles` (`java.util.Map<java.lang.String,io.casehub.api.model.coordination.AgentRole>`)

### `coordinationPattern` (`io.casehub.api.model.coordination.CoordinationPattern`)

### `groupId` (`java.lang.String`)

### `topologyType` (`io.casehub.api.model.coordination.TopologyType`)

## Constructors

### `public TopologyDescriptor(io.casehub.api.model.coordination.TopologyType topologyType, io.casehub.api.model.coordination.CoordinationPattern coordinationPattern, java.lang.String groupId, java.util.Map<java.lang.String,io.casehub.api.model.coordination.AgentRole> agentRoles)`

#### Parameters

- `topologyType` (`io.casehub.api.model.coordination.TopologyType`)
- `coordinationPattern` (`io.casehub.api.model.coordination.CoordinationPattern`)
- `groupId` (`java.lang.String`)
- `agentRoles` (`java.util.Map<java.lang.String,io.casehub.api.model.coordination.AgentRole>`)

### `public TopologyDescriptor(io.casehub.api.model.coordination.TopologyType topologyType, java.util.Map<java.lang.String,io.casehub.api.model.coordination.AgentRole> agentRoles)`

#### Parameters

- `topologyType` (`io.casehub.api.model.coordination.TopologyType`)
- `agentRoles` (`java.util.Map<java.lang.String,io.casehub.api.model.coordination.AgentRole>`)

## Methods

### `public java.util.Map<java.lang.String,io.casehub.api.model.coordination.AgentRole> agentRoles()`

### `public io.casehub.api.model.coordination.DecisionAuthority authorityFor(java.lang.String agentId)`

#### Parameters

- `agentId` (`java.lang.String`)

### `public io.casehub.api.model.coordination.CoordinationPattern coordinationPattern()`

### `public java.util.Optional<java.lang.String> coordinatorAgentId()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String groupId()`

### `public final int hashCode()`

### `public final java.lang.String toString()`

### `public io.casehub.api.model.coordination.TopologyType topologyType()`
