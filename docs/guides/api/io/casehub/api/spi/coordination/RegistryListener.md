# io.casehub.api.spi.coordination.RegistryListener

**Package:** `io.casehub.api.spi.coordination`

**Kind:** `interface`

## Methods

### `public abstract void onDeregistered(java.util.UUID swarmId)`

#### Parameters

- `swarmId` (`java.util.UUID`)

### `public abstract void onInterestsUpdated(java.util.UUID swarmId, java.util.Set<java.lang.String> capabilities)`

#### Parameters

- `swarmId` (`java.util.UUID`)
- `capabilities` (`java.util.Set<java.lang.String>`)

### `public abstract void onRegistered(io.casehub.api.model.coordination.SwarmDescriptor descriptor)`

#### Parameters

- `descriptor` (`io.casehub.api.model.coordination.SwarmDescriptor`)
