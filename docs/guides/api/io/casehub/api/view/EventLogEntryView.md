# io.casehub.api.view.EventLogEntryView

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `eventType` (`java.lang.String`)

### `payload` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `streamType` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `eventType` (`java.lang.String`)

### `payload` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `streamType` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public EventLogEntryView(java.lang.String eventType, java.lang.String streamType, java.time.Instant timestamp, java.util.Map<java.lang.String,java.lang.Object> payload)`

#### Parameters

- `eventType` (`java.lang.String`)
- `streamType` (`java.lang.String`)
- `timestamp` (`java.time.Instant`)
- `payload` (`java.util.Map<java.lang.String,java.lang.Object>`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String eventType()`

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Object> payload()`

### `public java.lang.String streamType()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`
