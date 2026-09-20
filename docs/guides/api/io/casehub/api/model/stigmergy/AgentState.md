# io.casehub.api.model.stigmergy.AgentState

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `activatedAt` (`java.time.Instant`)

### `agentId` (`java.lang.String`)

### `bindingName` (`java.lang.String`)

### `departedAt` (`java.time.Instant`)

### `joinedAt` (`java.time.Instant`)

### `lastActivity` (`java.time.Instant`)

### `state` (`io.casehub.api.model.stigmergy.AgentLifecycleState`)

## Record Components

### `activatedAt` (`java.time.Instant`)

### `agentId` (`java.lang.String`)

### `bindingName` (`java.lang.String`)

### `departedAt` (`java.time.Instant`)

### `joinedAt` (`java.time.Instant`)

### `lastActivity` (`java.time.Instant`)

### `state` (`io.casehub.api.model.stigmergy.AgentLifecycleState`)

## Constructors

### `public AgentState(java.lang.String agentId, java.lang.String bindingName, io.casehub.api.model.stigmergy.AgentLifecycleState state, java.time.Instant joinedAt, java.time.Instant activatedAt, java.time.Instant departedAt, java.time.Instant lastActivity)`

#### Parameters

- `agentId` (`java.lang.String`)
- `bindingName` (`java.lang.String`)
- `state` (`io.casehub.api.model.stigmergy.AgentLifecycleState`)
- `joinedAt` (`java.time.Instant`)
- `activatedAt` (`java.time.Instant`)
- `departedAt` (`java.time.Instant`)
- `lastActivity` (`java.time.Instant`)

## Methods

### `public java.time.Instant activatedAt()`

### `public java.lang.String agentId()`

### `public java.lang.String bindingName()`

### `public java.time.Instant departedAt()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.time.Instant joinedAt()`

### `public java.time.Instant lastActivity()`

### `public io.casehub.api.model.stigmergy.AgentLifecycleState state()`

### `public final java.lang.String toString()`
