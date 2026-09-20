# io.casehub.api.spi.observation.ContextSnapshot

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `changedKeys` (`java.util.Set<java.lang.String>`)

### `changedValues` (`java.util.Map<java.lang.String,JsonNode>`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `changedKeys` (`java.util.Set<java.lang.String>`)

### `changedValues` (`java.util.Map<java.lang.String,JsonNode>`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public ContextSnapshot(java.util.Set<java.lang.String> changedKeys, java.util.Map<java.lang.String,JsonNode> changedValues, java.time.Instant timestamp)`

#### Parameters

- `changedKeys` (`java.util.Set<java.lang.String>`)
- `changedValues` (`java.util.Map<java.lang.String,JsonNode>`)
- `timestamp` (`java.time.Instant`)

## Methods

### `public java.util.Set<java.lang.String> changedKeys()`

### `public java.util.Map<java.lang.String,JsonNode> changedValues()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`
