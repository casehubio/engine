# io.casehub.api.spi.observation.RuleContext

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `agentId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `changedKeys` (`java.util.Set<java.lang.String>`)

### `contextSnapshot` (`JsonNode`)

### `landscape` (`io.casehub.api.spi.observation.InterestLandscape`)

### `observations` (`java.util.List<io.casehub.api.spi.observation.Observation>`)

### `signals` (`java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal>`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `agentId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `changedKeys` (`java.util.Set<java.lang.String>`)

### `contextSnapshot` (`JsonNode`)

### `landscape` (`io.casehub.api.spi.observation.InterestLandscape`)

### `observations` (`java.util.List<io.casehub.api.spi.observation.Observation>`)

### `signals` (`java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal>`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public RuleContext(java.util.List<io.casehub.api.spi.observation.Observation> observations, java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal> signals, JsonNode contextSnapshot, java.util.Set<java.lang.String> changedKeys, io.casehub.api.spi.observation.InterestLandscape landscape, java.lang.String agentId, java.lang.String tenancyId, java.util.UUID caseId)`

#### Parameters

- `observations` (`java.util.List<io.casehub.api.spi.observation.Observation>`)
- `signals` (`java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal>`)
- `contextSnapshot` (`JsonNode`)
- `changedKeys` (`java.util.Set<java.lang.String>`)
- `landscape` (`io.casehub.api.spi.observation.InterestLandscape`)
- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `caseId` (`java.util.UUID`)

## Methods

### `public java.lang.String agentId()`

### `public java.util.UUID caseId()`

### `public java.util.Set<java.lang.String> changedKeys()`

### `public JsonNode contextSnapshot()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.spi.observation.InterestLandscape landscape()`

### `public java.util.List<io.casehub.api.spi.observation.Observation> observations()`

### `public java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal> signals()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`
