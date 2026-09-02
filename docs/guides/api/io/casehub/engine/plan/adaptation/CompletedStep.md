# io.casehub.engine.plan.adaptation.CompletedStep

**Package:** `io.casehub.engine.plan.adaptation`

**Kind:** `record`

## Fields

### `capabilityName` (`java.lang.String`)

### `completedAt` (`java.time.Instant`)

### `description` (`java.lang.String`)

### `output` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `stepId` (`java.lang.String`)

## Record Components

### `capabilityName` (`java.lang.String`)

### `completedAt` (`java.time.Instant`)

### `description` (`java.lang.String`)

### `output` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `stepId` (`java.lang.String`)

## Constructors

### `public CompletedStep(java.lang.String stepId, java.lang.String capabilityName, java.lang.String description, java.util.Map<java.lang.String,java.lang.Object> output, java.time.Instant completedAt)`

#### Parameters

- `stepId` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `description` (`java.lang.String`)
- `output` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `completedAt` (`java.time.Instant`)

## Methods

### `public java.lang.String capabilityName()`

### `public java.time.Instant completedAt()`

### `public java.lang.String description()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Object> output()`

### `public java.lang.String stepId()`

### `public final java.lang.String toString()`
