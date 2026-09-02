# io.casehub.engine.plan.PortfolioConfig

**Package:** `io.casehub.engine.plan`

**Kind:** `record`

## Fields

### `DEFAULT_DELEGATES` (`java.util.List<java.lang.String>`)

### `DEFAULT_TIMEOUTS` (`java.util.Map<java.lang.String,java.lang.Long>`)

### `DEFAULT_TIMEOUT_MS` (`long`)

### `delegates` (`java.util.List<java.lang.String>`)

### `timeouts` (`java.util.Map<java.lang.String,java.lang.Long>`)

## Record Components

### `delegates` (`java.util.List<java.lang.String>`)

### `timeouts` (`java.util.Map<java.lang.String,java.lang.Long>`)

## Constructors

### `public PortfolioConfig(java.util.List<java.lang.String> delegates, java.util.Map<java.lang.String,java.lang.Long> timeouts)`

#### Parameters

- `delegates` (`java.util.List<java.lang.String>`)
- `timeouts` (`java.util.Map<java.lang.String,java.lang.Long>`)

## Methods

### `public static io.casehub.engine.plan.PortfolioConfig defaults()`

### `public java.util.List<java.lang.String> delegates()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public long timeoutFor(java.lang.String strategyId)`

#### Parameters

- `strategyId` (`java.lang.String`)

### `public java.util.Map<java.lang.String,java.lang.Long> timeouts()`

### `public final java.lang.String toString()`
