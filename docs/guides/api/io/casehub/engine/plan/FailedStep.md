# io.casehub.engine.plan.ReplanContext.FailedStep

**Package:** `io.casehub.engine.plan`

**Kind:** `record`

## Fields

### `cause` (`java.lang.Throwable`)

### `errorMessage` (`java.lang.String`)

### `retryAttempts` (`int`)

### `stepId` (`java.lang.String`)

## Record Components

### `cause` (`java.lang.Throwable`)

### `errorMessage` (`java.lang.String`)

### `retryAttempts` (`int`)

### `stepId` (`java.lang.String`)

## Constructors

### `public FailedStep(java.lang.String stepId, java.lang.String errorMessage, java.lang.Throwable cause, int retryAttempts)`

#### Parameters

- `stepId` (`java.lang.String`)
- `errorMessage` (`java.lang.String`)
- `cause` (`java.lang.Throwable`)
- `retryAttempts` (`int`)

## Methods

### `public java.lang.Throwable cause()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String errorMessage()`

### `public final int hashCode()`

### `public int retryAttempts()`

### `public java.lang.String stepId()`

### `public final java.lang.String toString()`
