# io.casehub.api.spi.coordination.TopologyProvider

**Package:** `io.casehub.api.spi.coordination`

**Kind:** `interface`

## Methods

### `public default java.util.OptionalDouble effectiveSelfDetermination(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public default java.util.Optional<io.casehub.api.model.coordination.TopologyType> evaluateTransition(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.model.coordination.TopologyDescriptor topologyFor(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public default java.util.Optional<io.casehub.api.model.coordination.TransitionPolicy> transitionPolicy(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
