# io.casehub.api.spi.observation.Observation

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `confidence` (`double`)

### `details` (`java.util.Map<java.lang.String,JsonNode>`)

### `patternId` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `confidence` (`double`)

### `details` (`java.util.Map<java.lang.String,JsonNode>`)

### `patternId` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public Observation(java.lang.String patternId, double confidence, java.util.Map<java.lang.String,JsonNode> details, java.time.Instant timestamp)`

#### Parameters

- `patternId` (`java.lang.String`)
- `confidence` (`double`)
- `details` (`java.util.Map<java.lang.String,JsonNode>`)
- `timestamp` (`java.time.Instant`)

## Methods

### `public double confidence()`

### `public java.util.Map<java.lang.String,JsonNode> details()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String patternId()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`
