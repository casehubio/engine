# io.casehub.api.model.FailureDiagnosis

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `category` (`io.casehub.api.model.FailureCategory`)

### `critique` (`java.lang.String`)

### `outcomeStatus` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

### `workerId` (`java.lang.String`)

## Record Components

### `category` (`io.casehub.api.model.FailureCategory`)

### `critique` (`java.lang.String`)

### `outcomeStatus` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

### `workerId` (`java.lang.String`)

## Constructors

### `public FailureDiagnosis(io.casehub.api.model.FailureCategory category, java.lang.String workerId, java.lang.String outcomeStatus, java.time.Instant timestamp, java.lang.String critique)`

#### Parameters

- `category` (`io.casehub.api.model.FailureCategory`)
- `workerId` (`java.lang.String`)
- `outcomeStatus` (`java.lang.String`)
- `timestamp` (`java.time.Instant`)
- `critique` (`java.lang.String`)

## Methods

### `public io.casehub.api.model.FailureCategory category()`

### `public java.lang.String critique()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public static io.casehub.api.model.FailureDiagnosis of(io.casehub.api.model.FailureCategory category, java.lang.String workerId, java.lang.String outcomeStatus, java.time.Instant timestamp)`

#### Parameters

- `category` (`io.casehub.api.model.FailureCategory`)
- `workerId` (`java.lang.String`)
- `outcomeStatus` (`java.lang.String`)
- `timestamp` (`java.time.Instant`)

### `public java.lang.String outcomeStatus()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`

### `public java.lang.String workerId()`
