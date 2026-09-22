# io.casehub.api.view.EvolutionSummary.NotableEvent

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `summary` (`java.util.Map<java.lang.String,java.lang.String>`)

### `timestamp` (`java.time.Instant`)

### `type` (`io.casehub.api.model.event.CaseHubEventType`)

## Record Components

### `summary` (`java.util.Map<java.lang.String,java.lang.String>`)

### `timestamp` (`java.time.Instant`)

### `type` (`io.casehub.api.model.event.CaseHubEventType`)

## Constructors

### `public NotableEvent(io.casehub.api.model.event.CaseHubEventType type, java.time.Instant timestamp, java.util.Map<java.lang.String,java.lang.String> summary)`

#### Parameters

- `type` (`io.casehub.api.model.event.CaseHubEventType`)
- `timestamp` (`java.time.Instant`)
- `summary` (`java.util.Map<java.lang.String,java.lang.String>`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.String> summary()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`

### `public io.casehub.api.model.event.CaseHubEventType type()`
