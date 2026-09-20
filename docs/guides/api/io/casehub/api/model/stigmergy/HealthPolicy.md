# io.casehub.api.model.stigmergy.HealthPolicy

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `halfOpenMaxImprovements` (`java.lang.Integer`)

### `healthDeltaThreshold` (`java.lang.Double`)

### `healthThreshold` (`java.lang.Double`)

### `healthWindowMinutes` (`java.lang.Integer`)

### `recoveryWindowMinutes` (`java.lang.Integer`)

### `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Record Components

### `halfOpenMaxImprovements` (`java.lang.Integer`)

### `healthDeltaThreshold` (`java.lang.Double`)

### `healthThreshold` (`java.lang.Double`)

### `healthWindowMinutes` (`java.lang.Integer`)

### `recoveryWindowMinutes` (`java.lang.Integer`)

### `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Constructors

### `public HealthPolicy(java.lang.Double healthThreshold, java.lang.Double healthDeltaThreshold, java.lang.Integer healthWindowMinutes, java.lang.Integer recoveryWindowMinutes, java.lang.Integer halfOpenMaxImprovements, java.util.Map<java.lang.String,java.lang.Double> weights)`

#### Parameters

- `healthThreshold` (`java.lang.Double`)
- `healthDeltaThreshold` (`java.lang.Double`)
- `healthWindowMinutes` (`java.lang.Integer`)
- `recoveryWindowMinutes` (`java.lang.Integer`)
- `halfOpenMaxImprovements` (`java.lang.Integer`)
- `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Methods

### `public int effectiveHalfOpenMaxImprovements()`

### `public double effectiveHealthDeltaThreshold()`

### `public double effectiveHealthThreshold()`

### `public int effectiveHealthWindowMinutes()`

### `public int effectiveRecoveryWindowMinutes()`

### `public java.util.Map<java.lang.String,java.lang.Double> effectiveWeights()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.Integer halfOpenMaxImprovements()`

### `public final int hashCode()`

### `public java.lang.Double healthDeltaThreshold()`

### `public java.lang.Double healthThreshold()`

### `public java.lang.Integer healthWindowMinutes()`

### `public java.lang.Integer recoveryWindowMinutes()`

### `public final java.lang.String toString()`

### `public java.util.Map<java.lang.String,java.lang.Double> weights()`
