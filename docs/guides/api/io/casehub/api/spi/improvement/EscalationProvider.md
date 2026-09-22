# io.casehub.api.spi.improvement.EscalationProvider

**Package:** `io.casehub.api.spi.improvement`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.model.stigmergy.EscalationResult evaluate(java.util.UUID caseId, java.lang.String tenancyId, io.casehub.api.model.stigmergy.ImprovementStage stage, io.casehub.api.model.stigmergy.EscalationContext context, io.casehub.api.model.stigmergy.EscalationPolicy policy, java.util.List<io.casehub.api.model.stigmergy.WatchPattern> activeWatchPatterns)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `stage` (`io.casehub.api.model.stigmergy.ImprovementStage`)
- `context` (`io.casehub.api.model.stigmergy.EscalationContext`)
- `policy` (`io.casehub.api.model.stigmergy.EscalationPolicy`)
- `activeWatchPatterns` (`java.util.List<io.casehub.api.model.stigmergy.WatchPattern>`)
