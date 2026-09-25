# io.casehub.api.spi.improvement.RegressionEvaluator

**Package:** `io.casehub.api.spi.improvement`

**Kind:** `interface`

## Methods

### `public abstract java.lang.String domainId()`

### `public abstract io.casehub.api.model.stigmergy.RegressionVerdict evaluate(java.util.UUID caseId, io.casehub.api.model.stigmergy.HealthScoreSnapshot baseline, io.casehub.api.model.stigmergy.HealthScoreSnapshot current, java.lang.String category)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `baseline` (`io.casehub.api.model.stigmergy.HealthScoreSnapshot`)
- `current` (`io.casehub.api.model.stigmergy.HealthScoreSnapshot`)
- `category` (`java.lang.String`)

### `public abstract java.lang.String evaluatorId()`
