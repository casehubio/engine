# io.casehub.api.model.stigmergy.ProvisionBudget

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `cooldownCycles` (`java.lang.Integer`)

### `idleGraceCycles` (`java.lang.Integer`)

### `idleThreshold` (`java.lang.Double`)

### `maxConcurrent` (`java.lang.Integer`)

### `maxProvisions` (`java.lang.Integer`)

## Record Components

### `cooldownCycles` (`java.lang.Integer`)

### `idleGraceCycles` (`java.lang.Integer`)

### `idleThreshold` (`java.lang.Double`)

### `maxConcurrent` (`java.lang.Integer`)

### `maxProvisions` (`java.lang.Integer`)

## Constructors

### `public ProvisionBudget(java.lang.Integer maxProvisions, java.lang.Integer maxConcurrent, java.lang.Integer cooldownCycles, java.lang.Double idleThreshold, java.lang.Integer idleGraceCycles)`

#### Parameters

- `maxProvisions` (`java.lang.Integer`)
- `maxConcurrent` (`java.lang.Integer`)
- `cooldownCycles` (`java.lang.Integer`)
- `idleThreshold` (`java.lang.Double`)
- `idleGraceCycles` (`java.lang.Integer`)

## Methods

### `public java.lang.Integer cooldownCycles()`

### `public int effectiveCooldownCycles()`

### `public int effectiveIdleGraceCycles()`

### `public double effectiveIdleThreshold()`

### `public int effectiveMaxConcurrent()`

### `public int effectiveMaxProvisions()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.Integer idleGraceCycles()`

### `public java.lang.Double idleThreshold()`

### `public java.lang.Integer maxConcurrent()`

### `public java.lang.Integer maxProvisions()`

### `public final java.lang.String toString()`
