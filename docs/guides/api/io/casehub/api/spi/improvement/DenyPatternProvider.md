# io.casehub.api.spi.improvement.DenyPatternProvider

**Package:** `io.casehub.api.spi.improvement`

**Kind:** `interface`

## Methods

### `public abstract java.lang.String domainId()`

### `public abstract boolean isDenied(java.util.UUID caseId, java.lang.String tenancyId, io.casehub.api.model.improvement.ImprovementRequest request, io.casehub.api.model.improvement.ImprovementConfig config)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `request` (`io.casehub.api.model.improvement.ImprovementRequest`)
- `config` (`io.casehub.api.model.improvement.ImprovementConfig`)
