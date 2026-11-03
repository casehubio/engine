# io.casehub.api.model.coordination.AgentRole

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `COORDINATOR` (`io.casehub.api.model.coordination.AgentRole`)

### `JUDGE` (`io.casehub.api.model.coordination.AgentRole`)

### `MODERATOR` (`io.casehub.api.model.coordination.AgentRole`)

### `SUPERVISOR` (`io.casehub.api.model.coordination.AgentRole`)

### `name` (`Path`)

### `platformRole` (`io.casehub.api.model.coordination.PlatformRole`)

### `structuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

## Record Components

### `name` (`Path`)

### `platformRole` (`io.casehub.api.model.coordination.PlatformRole`)

### `structuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

## Constructors

### `public AgentRole(Path name, io.casehub.api.model.coordination.StructuralRole structuralRole, io.casehub.api.model.coordination.PlatformRole platformRole)`

#### Parameters

- `name` (`Path`)
- `structuralRole` (`io.casehub.api.model.coordination.StructuralRole`)
- `platformRole` (`io.casehub.api.model.coordination.PlatformRole`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public boolean isKindOf(io.casehub.api.model.coordination.AgentRole other)`

#### Parameters

- `other` (`io.casehub.api.model.coordination.AgentRole`)

### `public Path name()`

### `public static io.casehub.api.model.coordination.AgentRole of(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.AgentRole of(java.lang.String name, io.casehub.api.model.coordination.StructuralRole structuralRole)`

#### Parameters

- `name` (`java.lang.String`)
- `structuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

### `public io.casehub.api.model.coordination.PlatformRole platformRole()`

### `public io.casehub.api.model.coordination.StructuralRole structuralRole()`

### `public final java.lang.String toString()`
