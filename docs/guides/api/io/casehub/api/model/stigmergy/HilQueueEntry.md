# io.casehub.api.model.stigmergy.HilQueueEntry

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `blockingHypotheses` (`java.util.List<java.lang.String>`)

### `capabilityArea` (`java.lang.String`)

### `citation` (`java.lang.String`)

### `priority` (`int`)

### `queuedAt` (`java.time.Instant`)

### `reason` (`java.lang.String`)

### `sourceUrl` (`java.lang.String`)

## Record Components

### `blockingHypotheses` (`java.util.List<java.lang.String>`)

### `capabilityArea` (`java.lang.String`)

### `citation` (`java.lang.String`)

### `priority` (`int`)

### `queuedAt` (`java.time.Instant`)

### `reason` (`java.lang.String`)

### `sourceUrl` (`java.lang.String`)

## Constructors

### `public HilQueueEntry(java.lang.String sourceUrl, java.lang.String citation, java.lang.String reason, java.lang.String capabilityArea, int priority, java.util.List<java.lang.String> blockingHypotheses, java.time.Instant queuedAt)`

#### Parameters

- `sourceUrl` (`java.lang.String`)
- `citation` (`java.lang.String`)
- `reason` (`java.lang.String`)
- `capabilityArea` (`java.lang.String`)
- `priority` (`int`)
- `blockingHypotheses` (`java.util.List<java.lang.String>`)
- `queuedAt` (`java.time.Instant`)

## Methods

### `public java.util.List<java.lang.String> blockingHypotheses()`

### `public java.lang.String capabilityArea()`

### `public java.lang.String citation()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int priority()`

### `public java.time.Instant queuedAt()`

### `public java.lang.String reason()`

### `public java.lang.String sourceUrl()`

### `public final java.lang.String toString()`
