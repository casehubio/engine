# io.casehub.engine.plan.monitoring.MonitoringConfig

**Package:** `io.casehub.engine.plan.monitoring`

**Kind:** `record`

## Fields

### `DEFAULT_THRESHOLD` (`double`)

### `DEFAULT_WINDOW_SIZE` (`int`)

### `enabled` (`boolean`)

### `perCompletionThreshold` (`double`)

### `windowSize` (`int`)

## Record Components

### `enabled` (`boolean`)

### `perCompletionThreshold` (`double`)

### `windowSize` (`int`)

## Constructors

### `public MonitoringConfig(boolean enabled, double perCompletionThreshold, int windowSize)`

#### Parameters

- `enabled` (`boolean`)
- `perCompletionThreshold` (`double`)
- `windowSize` (`int`)

## Methods

### `public static io.casehub.engine.plan.monitoring.MonitoringConfig defaults()`

### `public static io.casehub.engine.plan.monitoring.MonitoringConfig disabled()`

### `public boolean enabled()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public double perCompletionThreshold()`

### `public final java.lang.String toString()`

### `public int windowSize()`
