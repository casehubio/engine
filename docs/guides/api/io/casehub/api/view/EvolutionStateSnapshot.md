# io.casehub.api.view.EvolutionStateSnapshot

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `activeImprovementCount` (`int`)

### `activeStreams` (`java.util.List<io.casehub.api.view.EvolutionStateSnapshot.ImprovementStreamView>`)

### `areaComplianceLevels` (`java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.ComplianceLevel>`)

### `caseId` (`java.util.UUID`)

### `categoryStates` (`java.util.Map<java.lang.String,io.casehub.api.view.EvolutionStateSnapshot.CategoryStateView>`)

### `circuitBreakerState` (`io.casehub.api.model.stigmergy.CircuitBreakerState`)

### `complianceEvaluatedAt` (`java.time.Instant`)

### `componentScores` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `dailyImprovementCount` (`int`)

### `evolutionEnabled` (`boolean`)

### `healthDelta` (`double`)

### `healthScore` (`double`)

### `healthWindowMinutes` (`int`)

### `pendingInboxCount` (`int`)

### `projectComplianceLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)

### `recentTicks` (`java.util.List<io.casehub.api.model.stigmergy.TickTrace>`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `activeImprovementCount` (`int`)

### `activeStreams` (`java.util.List<io.casehub.api.view.EvolutionStateSnapshot.ImprovementStreamView>`)

### `areaComplianceLevels` (`java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.ComplianceLevel>`)

### `caseId` (`java.util.UUID`)

### `categoryStates` (`java.util.Map<java.lang.String,io.casehub.api.view.EvolutionStateSnapshot.CategoryStateView>`)

### `circuitBreakerState` (`io.casehub.api.model.stigmergy.CircuitBreakerState`)

### `complianceEvaluatedAt` (`java.time.Instant`)

### `componentScores` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `dailyImprovementCount` (`int`)

### `evolutionEnabled` (`boolean`)

### `healthDelta` (`double`)

### `healthScore` (`double`)

### `healthWindowMinutes` (`int`)

### `pendingInboxCount` (`int`)

### `projectComplianceLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)

### `recentTicks` (`java.util.List<io.casehub.api.model.stigmergy.TickTrace>`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public EvolutionStateSnapshot(java.util.UUID caseId, java.time.Instant timestamp, double healthScore, java.util.Map<java.lang.String,java.lang.Double> componentScores, double healthDelta, int healthWindowMinutes, io.casehub.api.model.stigmergy.CircuitBreakerState circuitBreakerState, java.util.Map<java.lang.String,io.casehub.api.view.EvolutionStateSnapshot.CategoryStateView> categoryStates, io.casehub.api.model.stigmergy.ComplianceLevel projectComplianceLevel, java.time.Instant complianceEvaluatedAt, java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.ComplianceLevel> areaComplianceLevels, java.util.List<io.casehub.api.model.stigmergy.TickTrace> recentTicks, int activeImprovementCount, int dailyImprovementCount, java.util.List<io.casehub.api.view.EvolutionStateSnapshot.ImprovementStreamView> activeStreams, boolean evolutionEnabled, int pendingInboxCount)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `timestamp` (`java.time.Instant`)
- `healthScore` (`double`)
- `componentScores` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `healthDelta` (`double`)
- `healthWindowMinutes` (`int`)
- `circuitBreakerState` (`io.casehub.api.model.stigmergy.CircuitBreakerState`)
- `categoryStates` (`java.util.Map<java.lang.String,io.casehub.api.view.EvolutionStateSnapshot.CategoryStateView>`)
- `projectComplianceLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)
- `complianceEvaluatedAt` (`java.time.Instant`)
- `areaComplianceLevels` (`java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.ComplianceLevel>`)
- `recentTicks` (`java.util.List<io.casehub.api.model.stigmergy.TickTrace>`)
- `activeImprovementCount` (`int`)
- `dailyImprovementCount` (`int`)
- `activeStreams` (`java.util.List<io.casehub.api.view.EvolutionStateSnapshot.ImprovementStreamView>`)
- `evolutionEnabled` (`boolean`)
- `pendingInboxCount` (`int`)

## Methods

### `public int activeImprovementCount()`

### `public java.util.List<io.casehub.api.view.EvolutionStateSnapshot.ImprovementStreamView> activeStreams()`

### `public java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.ComplianceLevel> areaComplianceLevels()`

### `public java.util.UUID caseId()`

### `public java.util.Map<java.lang.String,io.casehub.api.view.EvolutionStateSnapshot.CategoryStateView> categoryStates()`

### `public io.casehub.api.model.stigmergy.CircuitBreakerState circuitBreakerState()`

### `public java.time.Instant complianceEvaluatedAt()`

### `public java.util.Map<java.lang.String,java.lang.Double> componentScores()`

### `public int dailyImprovementCount()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public boolean evolutionEnabled()`

### `public final int hashCode()`

### `public double healthDelta()`

### `public double healthScore()`

### `public int healthWindowMinutes()`

### `public int pendingInboxCount()`

### `public io.casehub.api.model.stigmergy.ComplianceLevel projectComplianceLevel()`

### `public java.util.List<io.casehub.api.model.stigmergy.TickTrace> recentTicks()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`
