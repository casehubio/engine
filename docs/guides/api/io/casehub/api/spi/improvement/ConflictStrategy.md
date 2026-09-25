# io.casehub.api.spi.improvement.ConflictStrategy

**Package:** `io.casehub.api.spi.improvement`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.spi.improvement.ConflictStrategy.ConflictResult check(io.casehub.api.model.stigmergy.ImprovementRequest request, java.util.Map<java.util.UUID,io.casehub.api.model.stigmergy.ImprovementRequest> activeImprovements, int trivialThreshold)`

#### Parameters

- `request` (`io.casehub.api.model.stigmergy.ImprovementRequest`)
- `activeImprovements` (`java.util.Map<java.util.UUID,io.casehub.api.model.stigmergy.ImprovementRequest>`)
- `trivialThreshold` (`int`)

### `public abstract java.lang.String domainId()`
