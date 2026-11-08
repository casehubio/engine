# io.casehub.api.model.coordination.TransitionPolicy

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `from` (`io.casehub.api.model.coordination.TopologyType`)

### `reversible` (`boolean`)

### `rollbackTrigger` (`io.casehub.api.model.coordination.TransitionTrigger`)

### `strategy` (`io.casehub.api.model.coordination.TransitionStrategy`)

### `to` (`io.casehub.api.model.coordination.TopologyType`)

### `trigger` (`io.casehub.api.model.coordination.TransitionTrigger`)

## Record Components

### `from` (`io.casehub.api.model.coordination.TopologyType`)

### `reversible` (`boolean`)

### `rollbackTrigger` (`io.casehub.api.model.coordination.TransitionTrigger`)

### `strategy` (`io.casehub.api.model.coordination.TransitionStrategy`)

### `to` (`io.casehub.api.model.coordination.TopologyType`)

### `trigger` (`io.casehub.api.model.coordination.TransitionTrigger`)

## Constructors

### `public TransitionPolicy(io.casehub.api.model.coordination.TopologyType from, io.casehub.api.model.coordination.TopologyType to, io.casehub.api.model.coordination.TransitionTrigger trigger, io.casehub.api.model.coordination.TransitionStrategy strategy, boolean reversible, io.casehub.api.model.coordination.TransitionTrigger rollbackTrigger)`

#### Parameters

- `from` (`io.casehub.api.model.coordination.TopologyType`)
- `to` (`io.casehub.api.model.coordination.TopologyType`)
- `trigger` (`io.casehub.api.model.coordination.TransitionTrigger`)
- `strategy` (`io.casehub.api.model.coordination.TransitionStrategy`)
- `reversible` (`boolean`)
- `rollbackTrigger` (`io.casehub.api.model.coordination.TransitionTrigger`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public io.casehub.api.model.coordination.TopologyType from()`

### `public final int hashCode()`

### `public boolean reversible()`

### `public io.casehub.api.model.coordination.TransitionTrigger rollbackTrigger()`

### `public io.casehub.api.model.coordination.TransitionStrategy strategy()`

### `public io.casehub.api.model.coordination.TopologyType to()`

### `public final java.lang.String toString()`

### `public io.casehub.api.model.coordination.TransitionTrigger trigger()`
