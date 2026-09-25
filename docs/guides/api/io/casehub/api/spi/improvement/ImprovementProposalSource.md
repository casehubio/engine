# io.casehub.api.spi.improvement.ImprovementProposalSource

**Package:** `io.casehub.api.spi.improvement`

**Kind:** `interface`

## Methods

### `public abstract java.lang.String domainId()`

### `public abstract java.util.List<io.casehub.api.model.stigmergy.ImprovementRequest> propose(java.util.UUID caseId, java.lang.String tenancyId, io.casehub.api.model.stigmergy.ImprovementConfig config)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `config` (`io.casehub.api.model.stigmergy.ImprovementConfig`)

### `public abstract java.lang.String sourceId()`
