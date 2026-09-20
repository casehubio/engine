# io.casehub.api.spi.observation.ObservationContext

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `agentId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `changedKeys` (`java.util.Set<java.lang.String>`)

### `history` (`java.util.List<io.casehub.api.spi.observation.ContextSnapshot>`)

### `interestLandscape` (`io.casehub.api.spi.observation.InterestLandscape`)

### `signals` (`java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal>`)

### `snapshot` (`JsonNode`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `agentId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `changedKeys` (`java.util.Set<java.lang.String>`)

### `history` (`java.util.List<io.casehub.api.spi.observation.ContextSnapshot>`)

### `interestLandscape` (`io.casehub.api.spi.observation.InterestLandscape`)

### `signals` (`java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal>`)

### `snapshot` (`JsonNode`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public ObservationContext(JsonNode snapshot, java.util.Set<java.lang.String> changedKeys, java.util.List<io.casehub.api.spi.observation.ContextSnapshot> history, java.lang.String agentId, java.lang.String tenancyId, java.util.UUID caseId)`

#### Parameters

- `snapshot` (`JsonNode`)
- `changedKeys` (`java.util.Set<java.lang.String>`)
- `history` (`java.util.List<io.casehub.api.spi.observation.ContextSnapshot>`)
- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `caseId` (`java.util.UUID`)

### `public ObservationContext(JsonNode snapshot, java.util.Set<java.lang.String> changedKeys, java.util.List<io.casehub.api.spi.observation.ContextSnapshot> history, java.lang.String agentId, java.lang.String tenancyId, java.util.UUID caseId, java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal> signals)`

#### Parameters

- `snapshot` (`JsonNode`)
- `changedKeys` (`java.util.Set<java.lang.String>`)
- `history` (`java.util.List<io.casehub.api.spi.observation.ContextSnapshot>`)
- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `caseId` (`java.util.UUID`)
- `signals` (`java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal>`)

### `public ObservationContext(JsonNode snapshot, java.util.Set<java.lang.String> changedKeys, java.util.List<io.casehub.api.spi.observation.ContextSnapshot> history, java.lang.String agentId, java.lang.String tenancyId, java.util.UUID caseId, java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal> signals, io.casehub.api.spi.observation.InterestLandscape interestLandscape)`

#### Parameters

- `snapshot` (`JsonNode`)
- `changedKeys` (`java.util.Set<java.lang.String>`)
- `history` (`java.util.List<io.casehub.api.spi.observation.ContextSnapshot>`)
- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `caseId` (`java.util.UUID`)
- `signals` (`java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal>`)
- `interestLandscape` (`io.casehub.api.spi.observation.InterestLandscape`)

## Methods

### `public java.lang.String agentId()`

### `public java.util.UUID caseId()`

### `public java.util.Set<java.lang.String> changedKeys()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.List<io.casehub.api.spi.observation.ContextSnapshot> history()`

### `public io.casehub.api.spi.observation.InterestLandscape interestLandscape()`

### `public java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal> signals()`

### `public JsonNode snapshot()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`
