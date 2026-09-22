# io.casehub.api.view.EvolutionEvent

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `caseId` (`java.util.UUID`)

### `data` (`java.util.Map<java.lang.String,java.lang.String>`)

### `timestamp` (`java.time.Instant`)

### `type` (`java.lang.String`)

## Record Components

### `caseId` (`java.util.UUID`)

### `data` (`java.util.Map<java.lang.String,java.lang.String>`)

### `timestamp` (`java.time.Instant`)

### `type` (`java.lang.String`)

## Constructors

### `public EvolutionEvent(java.util.UUID caseId, java.lang.String type, java.util.Map<java.lang.String,java.lang.String> data, java.time.Instant timestamp)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `type` (`java.lang.String`)
- `data` (`java.util.Map<java.lang.String,java.lang.String>`)
- `timestamp` (`java.time.Instant`)

## Methods

### `public java.util.UUID caseId()`

### `public java.util.Map<java.lang.String,java.lang.String> data()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`

### `public java.lang.String type()`
