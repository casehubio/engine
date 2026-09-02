# io.casehub.engine.plan.snapshot.DecompositionSnapshot

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `root` (`io.casehub.engine.plan.snapshot.TaskNodeSnapshot`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `root` (`io.casehub.engine.plan.snapshot.TaskNodeSnapshot`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public DecompositionSnapshot(io.casehub.engine.plan.snapshot.TaskNodeSnapshot root, java.time.Instant timestamp)`

#### Parameters

- `root` (`io.casehub.engine.plan.snapshot.TaskNodeSnapshot`)
- `timestamp` (`java.time.Instant`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public static io.casehub.engine.plan.snapshot.DecompositionSnapshot from(io.casehub.engine.plan.TaskNode<?> root, java.time.Instant timestamp)`

#### Parameters

- `root` (`io.casehub.engine.plan.TaskNode<?>`)
- `timestamp` (`java.time.Instant`)

### `public final int hashCode()`

### `public io.casehub.engine.plan.snapshot.TaskNodeSnapshot root()`

### `public java.time.Instant timestamp()`

### `private static io.casehub.engine.plan.snapshot.DecompositionMethodSnapshot toMethodSnapshot(io.casehub.engine.plan.DecompositionMethod<?> method)`

#### Parameters

- `method` (`io.casehub.engine.plan.DecompositionMethod<?>`)

### `private static io.casehub.engine.plan.snapshot.TaskNodeSnapshot toSnapshot(io.casehub.engine.plan.TaskNode<?> node)`

#### Parameters

- `node` (`io.casehub.engine.plan.TaskNode<?>`)

### `public final java.lang.String toString()`
