# io.casehub.api.view.EvolutionSummary

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `categories` (`java.util.List<io.casehub.api.view.EvolutionSummary.CategorySummary>`)

### `computedAt` (`java.time.Instant`)

### `failureCount` (`int`)

### `healthTrend` (`java.lang.Double`)

### `notableEvents` (`java.util.List<io.casehub.api.view.EvolutionSummary.NotableEvent>`)

### `regressionCount` (`int`)

### `rejectionCount` (`int`)

### `researchDirections` (`java.util.List<io.casehub.api.view.EvolutionSummary.ResearchDirectionSummary>`)

### `rollbackCount` (`int`)

### `scope` (`io.casehub.api.model.stigmergy.SummaryScope`)

### `successCount` (`int`)

### `successRateTrend` (`java.lang.Double`)

### `totalImprovements` (`int`)

## Record Components

### `categories` (`java.util.List<io.casehub.api.view.EvolutionSummary.CategorySummary>`)

### `computedAt` (`java.time.Instant`)

### `failureCount` (`int`)

### `healthTrend` (`java.lang.Double`)

### `notableEvents` (`java.util.List<io.casehub.api.view.EvolutionSummary.NotableEvent>`)

### `regressionCount` (`int`)

### `rejectionCount` (`int`)

### `researchDirections` (`java.util.List<io.casehub.api.view.EvolutionSummary.ResearchDirectionSummary>`)

### `rollbackCount` (`int`)

### `scope` (`io.casehub.api.model.stigmergy.SummaryScope`)

### `successCount` (`int`)

### `successRateTrend` (`java.lang.Double`)

### `totalImprovements` (`int`)

## Constructors

### `public EvolutionSummary(io.casehub.api.model.stigmergy.SummaryScope scope, java.time.Instant computedAt, int totalImprovements, int successCount, int failureCount, int rejectionCount, int regressionCount, int rollbackCount, java.lang.Double healthTrend, java.lang.Double successRateTrend, java.util.List<io.casehub.api.view.EvolutionSummary.CategorySummary> categories, java.util.List<io.casehub.api.view.EvolutionSummary.ResearchDirectionSummary> researchDirections, java.util.List<io.casehub.api.view.EvolutionSummary.NotableEvent> notableEvents)`

#### Parameters

- `scope` (`io.casehub.api.model.stigmergy.SummaryScope`)
- `computedAt` (`java.time.Instant`)
- `totalImprovements` (`int`)
- `successCount` (`int`)
- `failureCount` (`int`)
- `rejectionCount` (`int`)
- `regressionCount` (`int`)
- `rollbackCount` (`int`)
- `healthTrend` (`java.lang.Double`)
- `successRateTrend` (`java.lang.Double`)
- `categories` (`java.util.List<io.casehub.api.view.EvolutionSummary.CategorySummary>`)
- `researchDirections` (`java.util.List<io.casehub.api.view.EvolutionSummary.ResearchDirectionSummary>`)
- `notableEvents` (`java.util.List<io.casehub.api.view.EvolutionSummary.NotableEvent>`)

## Methods

### `public java.util.List<io.casehub.api.view.EvolutionSummary.CategorySummary> categories()`

### `public java.time.Instant computedAt()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public int failureCount()`

### `public final int hashCode()`

### `public java.lang.Double healthTrend()`

### `public java.util.List<io.casehub.api.view.EvolutionSummary.NotableEvent> notableEvents()`

### `public int regressionCount()`

### `public int rejectionCount()`

### `public java.util.List<io.casehub.api.view.EvolutionSummary.ResearchDirectionSummary> researchDirections()`

### `public int rollbackCount()`

### `public io.casehub.api.model.stigmergy.SummaryScope scope()`

### `public int successCount()`

### `public java.lang.Double successRateTrend()`

### `public final java.lang.String toString()`

### `public int totalImprovements()`
