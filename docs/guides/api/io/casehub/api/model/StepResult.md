# io.casehub.api.model.StepResult

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `elapsed` (`java.time.Duration`)

### `error` (`java.lang.String`)

### `status` (`io.casehub.api.model.StepStatus`)

### `stepId` (`java.lang.String`)

### `stepName` (`java.lang.String`)

## Record Components

### `elapsed` (`java.time.Duration`)

### `error` (`java.lang.String`)

### `status` (`io.casehub.api.model.StepStatus`)

### `stepId` (`java.lang.String`)

### `stepName` (`java.lang.String`)

## Constructors

### `public StepResult(java.lang.String stepId, java.lang.String stepName, io.casehub.api.model.StepStatus status, java.time.Duration elapsed, java.lang.String error)`

#### Parameters

- `stepId` (`java.lang.String`)
- `stepName` (`java.lang.String`)
- `status` (`io.casehub.api.model.StepStatus`)
- `elapsed` (`java.time.Duration`)
- `error` (`java.lang.String`)

## Methods

### `public java.time.Duration elapsed()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String error()`

### `public final int hashCode()`

### `public io.casehub.api.model.StepStatus status()`

### `public java.lang.String stepId()`

### `public java.lang.String stepName()`

### `public final java.lang.String toString()`
