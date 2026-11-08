# io.casehub.api.spi.coordination.CoordinationDriver

**Package:** `io.casehub.api.spi.coordination`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.model.coordination.CoordinationResult aggregate(java.util.List<io.casehub.api.model.coordination.DelegationOutcome> outcomes, io.casehub.api.model.coordination.CoordinationTask originalTask)`

#### Parameters

- `outcomes` (`java.util.List<io.casehub.api.model.coordination.DelegationOutcome>`)
- `originalTask` (`io.casehub.api.model.coordination.CoordinationTask`)

### `public abstract java.util.List<java.lang.String> allocate(io.casehub.api.model.coordination.CoordinationTask task, io.casehub.api.model.coordination.TopologyDescriptor topology)`

#### Parameters

- `task` (`io.casehub.api.model.coordination.CoordinationTask`)
- `topology` (`io.casehub.api.model.coordination.TopologyDescriptor`)

### `public abstract java.util.List<io.casehub.api.model.coordination.DelegationAssignment> delegate(io.casehub.api.model.coordination.CoordinationTask task, io.casehub.api.model.coordination.TopologyDescriptor topology)`

#### Parameters

- `task` (`io.casehub.api.model.coordination.CoordinationTask`)
- `topology` (`io.casehub.api.model.coordination.TopologyDescriptor`)

### `public abstract void onLifecycleEvent(io.casehub.api.model.coordination.CoordinationLifecycleEvent event)`

#### Parameters

- `event` (`io.casehub.api.model.coordination.CoordinationLifecycleEvent`)
