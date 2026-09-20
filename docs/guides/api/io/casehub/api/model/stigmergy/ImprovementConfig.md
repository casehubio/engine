# io.casehub.api.model.stigmergy.ImprovementConfig

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `budget` (`io.casehub.api.model.stigmergy.ImprovementBudget`)

### `caseTemplateId` (`java.lang.String`)

### `conflictTrivialThreshold` (`java.lang.Integer`)

### `consensusMinSources` (`java.lang.Integer`)

### `enabledCategories` (`java.util.List<java.lang.String>`)

### `evolutionEnabled` (`java.lang.Boolean`)

### `evolutionTickIntervalMinutes` (`java.lang.Integer`)

### `healthPolicy` (`io.casehub.api.model.stigmergy.HealthPolicy`)

### `researchMethodology` (`io.casehub.api.model.stigmergy.ResearchMethodology`)

### `rollbackPolicy` (`io.casehub.api.model.stigmergy.RollbackPolicy`)

### `signalNamespace` (`java.lang.String`)

## Record Components

### `budget` (`io.casehub.api.model.stigmergy.ImprovementBudget`)

### `caseTemplateId` (`java.lang.String`)

### `conflictTrivialThreshold` (`java.lang.Integer`)

### `consensusMinSources` (`java.lang.Integer`)

### `enabledCategories` (`java.util.List<java.lang.String>`)

### `evolutionEnabled` (`java.lang.Boolean`)

### `evolutionTickIntervalMinutes` (`java.lang.Integer`)

### `healthPolicy` (`io.casehub.api.model.stigmergy.HealthPolicy`)

### `researchMethodology` (`io.casehub.api.model.stigmergy.ResearchMethodology`)

### `rollbackPolicy` (`io.casehub.api.model.stigmergy.RollbackPolicy`)

### `signalNamespace` (`java.lang.String`)

## Constructors

### `public ImprovementConfig(java.lang.String signalNamespace, java.lang.Integer consensusMinSources, java.util.List<java.lang.String> enabledCategories, io.casehub.api.model.stigmergy.ImprovementBudget budget, java.lang.String caseTemplateId)`

#### Parameters

- `signalNamespace` (`java.lang.String`)
- `consensusMinSources` (`java.lang.Integer`)
- `enabledCategories` (`java.util.List<java.lang.String>`)
- `budget` (`io.casehub.api.model.stigmergy.ImprovementBudget`)
- `caseTemplateId` (`java.lang.String`)

### `public ImprovementConfig(java.lang.String signalNamespace, java.lang.Integer consensusMinSources, java.util.List<java.lang.String> enabledCategories, io.casehub.api.model.stigmergy.ImprovementBudget budget, java.lang.String caseTemplateId, java.lang.Boolean evolutionEnabled, java.lang.Integer evolutionTickIntervalMinutes, io.casehub.api.model.stigmergy.RollbackPolicy rollbackPolicy, io.casehub.api.model.stigmergy.HealthPolicy healthPolicy, io.casehub.api.model.stigmergy.ResearchMethodology researchMethodology, java.lang.Integer conflictTrivialThreshold)`

#### Parameters

- `signalNamespace` (`java.lang.String`)
- `consensusMinSources` (`java.lang.Integer`)
- `enabledCategories` (`java.util.List<java.lang.String>`)
- `budget` (`io.casehub.api.model.stigmergy.ImprovementBudget`)
- `caseTemplateId` (`java.lang.String`)
- `evolutionEnabled` (`java.lang.Boolean`)
- `evolutionTickIntervalMinutes` (`java.lang.Integer`)
- `rollbackPolicy` (`io.casehub.api.model.stigmergy.RollbackPolicy`)
- `healthPolicy` (`io.casehub.api.model.stigmergy.HealthPolicy`)
- `researchMethodology` (`io.casehub.api.model.stigmergy.ResearchMethodology`)
- `conflictTrivialThreshold` (`java.lang.Integer`)

## Methods

### `public io.casehub.api.model.stigmergy.ImprovementBudget budget()`

### `public java.lang.String caseTemplateId()`

### `public java.lang.Integer conflictTrivialThreshold()`

### `public java.lang.Integer consensusMinSources()`

### `public io.casehub.api.model.stigmergy.ImprovementBudget effectiveBudget()`

### `public java.lang.String effectiveCaseTemplateId()`

### `public int effectiveConflictTrivialThreshold()`

### `public int effectiveConsensusMinSources()`

### `public java.util.List<java.lang.String> effectiveEnabledCategories()`

### `public boolean effectiveEvolutionEnabled()`

### `public int effectiveEvolutionTickIntervalMinutes()`

### `public io.casehub.api.model.stigmergy.HealthPolicy effectiveHealthPolicy()`

### `public io.casehub.api.model.stigmergy.ResearchMethodology effectiveResearchMethodology()`

### `public io.casehub.api.model.stigmergy.RollbackPolicy effectiveRollbackPolicy()`

### `public java.lang.String effectiveSignalNamespace()`

### `public java.util.List<java.lang.String> enabledCategories()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.Boolean evolutionEnabled()`

### `public java.lang.Integer evolutionTickIntervalMinutes()`

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.HealthPolicy healthPolicy()`

### `public io.casehub.api.model.stigmergy.ResearchMethodology researchMethodology()`

### `public io.casehub.api.model.stigmergy.RollbackPolicy rollbackPolicy()`

### `public java.lang.String signalNamespace()`

### `public final java.lang.String toString()`
