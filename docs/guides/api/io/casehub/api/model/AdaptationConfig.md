# io.casehub.api.model.AdaptationConfig

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `DEFAULT_CONTINGENCY_THRESHOLD` (`double`)

### `DEFAULT_PROGRESS_THRESHOLD` (`double`)

### `contingencyThreshold` (`java.lang.Double`)

### `metaReasoner` (`java.lang.String`)

### `optimization` (`java.lang.String`)

### `repair` (`java.lang.String`)

### `threshold` (`java.lang.Double`)

### `trigger` (`java.lang.String`)

## Record Components

### `contingencyThreshold` (`java.lang.Double`)

### `metaReasoner` (`java.lang.String`)

### `optimization` (`java.lang.String`)

### `repair` (`java.lang.String`)

### `threshold` (`java.lang.Double`)

### `trigger` (`java.lang.String`)

## Constructors

### `public AdaptationConfig(java.lang.String trigger, java.lang.String optimization, java.lang.Double threshold, java.lang.String metaReasoner, java.lang.String repair)`

#### Parameters

- `trigger` (`java.lang.String`)
- `optimization` (`java.lang.String`)
- `threshold` (`java.lang.Double`)
- `metaReasoner` (`java.lang.String`)
- `repair` (`java.lang.String`)

### `public AdaptationConfig(java.lang.String trigger, java.lang.String optimization, java.lang.Double threshold, java.lang.String metaReasoner, java.lang.String repair, java.lang.Double contingencyThreshold)`

#### Parameters

- `trigger` (`java.lang.String`)
- `optimization` (`java.lang.String`)
- `threshold` (`java.lang.Double`)
- `metaReasoner` (`java.lang.String`)
- `repair` (`java.lang.String`)
- `contingencyThreshold` (`java.lang.Double`)

## Methods

### `public java.lang.Double contingencyThreshold()`

### `public double effectiveContingencyThreshold()`

### `public java.lang.String effectiveMetaReasoner()`

### `public java.lang.String effectiveRepair(io.casehub.api.model.CaseDefinition definition)`

#### Parameters

- `definition` (`io.casehub.api.model.CaseDefinition`)

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String metaReasoner()`

### `public static io.casehub.api.model.AdaptationConfig of(java.lang.String trigger, java.lang.String optimization)`

#### Parameters

- `trigger` (`java.lang.String`)
- `optimization` (`java.lang.String`)

### `public java.lang.String optimization()`

### `public java.lang.String repair()`

### `public java.lang.Double threshold()`

### `public final java.lang.String toString()`

### `public java.lang.String trigger()`
