# io.casehub.engine.plan.goap.PlannerConfig

**Package:** `io.casehub.engine.plan.goap`

**Kind:** `record`

## Fields

### `DEFAULT_MAX_ITERATIONS` (`int`)

### `backwardPruning` (`boolean`)

### `blacklistedActions` (`java.util.Set<java.lang.String>`)

### `forwardSimulation` (`boolean`)

### `maxIterations` (`int`)

## Record Components

### `backwardPruning` (`boolean`)

### `blacklistedActions` (`java.util.Set<java.lang.String>`)

### `forwardSimulation` (`boolean`)

### `maxIterations` (`int`)

## Constructors

### `public PlannerConfig(int maxIterations, java.util.Set<java.lang.String> blacklistedActions, boolean backwardPruning, boolean forwardSimulation)`

#### Parameters

- `maxIterations` (`int`)
- `blacklistedActions` (`java.util.Set<java.lang.String>`)
- `backwardPruning` (`boolean`)
- `forwardSimulation` (`boolean`)

## Methods

### `public boolean backwardPruning()`

### `public java.util.Set<java.lang.String> blacklistedActions()`

### `public static io.casehub.engine.plan.goap.PlannerConfig defaults()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public boolean forwardSimulation()`

### `public final int hashCode()`

### `public int maxIterations()`

### `public final java.lang.String toString()`
