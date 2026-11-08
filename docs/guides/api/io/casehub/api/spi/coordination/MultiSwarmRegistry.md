# io.casehub.api.spi.coordination.MultiSwarmRegistry

**Package:** `io.casehub.api.spi.coordination`

**Kind:** `interface`

## Methods

### `public abstract java.util.List<io.casehub.api.model.coordination.SwarmDescriptor> activeSwarms(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract void addListener(io.casehub.api.spi.coordination.RegistryListener listener)`

#### Parameters

- `listener` (`io.casehub.api.spi.coordination.RegistryListener`)

### `public abstract java.util.List<io.casehub.api.model.coordination.SwarmDescriptor> allActive()`

### `public abstract java.util.List<io.casehub.api.model.coordination.SwarmDescriptor> children(java.util.UUID parentSwarmId)`

#### Parameters

- `parentSwarmId` (`java.util.UUID`)

### `public abstract void deregister(java.util.UUID swarmId)`

#### Parameters

- `swarmId` (`java.util.UUID`)

### `public abstract io.casehub.api.model.coordination.SwarmDescriptor get(java.util.UUID swarmId)`

#### Parameters

- `swarmId` (`java.util.UUID`)

### `public abstract void register(io.casehub.api.model.coordination.SwarmDescriptor descriptor)`

#### Parameters

- `descriptor` (`io.casehub.api.model.coordination.SwarmDescriptor`)

### `public abstract void updateInterests(java.util.UUID swarmId, java.util.Set<java.lang.String> capabilities)`

#### Parameters

- `swarmId` (`java.util.UUID`)
- `capabilities` (`java.util.Set<java.lang.String>`)
