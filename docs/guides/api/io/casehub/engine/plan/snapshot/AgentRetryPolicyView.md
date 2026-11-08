# io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRetryPolicyView

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `backoffStrategy` (`java.lang.String`)

### `initialDelayMs` (`java.lang.Integer`)

### `maxRetries` (`int`)

## Record Components

### `backoffStrategy` (`java.lang.String`)

### `initialDelayMs` (`java.lang.Integer`)

### `maxRetries` (`int`)

## Constructors

### `public AgentRetryPolicyView(int maxRetries, java.lang.String backoffStrategy, java.lang.Integer initialDelayMs)`

#### Parameters

- `maxRetries` (`int`)
- `backoffStrategy` (`java.lang.String`)
- `initialDelayMs` (`java.lang.Integer`)

## Methods

### `public java.lang.String backoffStrategy()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.Integer initialDelayMs()`

### `public int maxRetries()`

### `public final java.lang.String toString()`
