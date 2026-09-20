# io.casehub.api.model.signal.SignalConfig

**Package:** `io.casehub.api.model.signal`

**Kind:** `record`

## Fields

### `DEFAULT_EFFECTIVE_ZERO_THRESHOLD` (`double`)

### `DEFAULT_HALF_LIFE` (`java.time.Duration`)

### `DEFAULT_MAX_SIGNALS_PER_CASE` (`int`)

### `defaultHalfLife` (`java.time.Duration`)

### `effectiveZeroThreshold` (`double`)

### `maxSignalsPerCase` (`int`)

## Record Components

### `defaultHalfLife` (`java.time.Duration`)

### `effectiveZeroThreshold` (`double`)

### `maxSignalsPerCase` (`int`)

## Constructors

### `public SignalConfig(java.time.Duration defaultHalfLife, double effectiveZeroThreshold, int maxSignalsPerCase)`

#### Parameters

- `defaultHalfLife` (`java.time.Duration`)
- `effectiveZeroThreshold` (`double`)
- `maxSignalsPerCase` (`int`)

## Methods

### `public java.time.Duration defaultHalfLife()`

### `public static io.casehub.api.model.signal.SignalConfig defaults()`

### `public double effectiveZeroThreshold()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int maxSignalsPerCase()`

### `public final java.lang.String toString()`
