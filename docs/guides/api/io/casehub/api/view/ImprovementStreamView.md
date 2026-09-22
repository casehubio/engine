# io.casehub.api.view.EvolutionStateSnapshot.ImprovementStreamView

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `blockedBy` (`java.util.UUID`)

### `category` (`java.lang.String`)

### `conflictBlocked` (`boolean`)

### `currentStage` (`io.casehub.api.model.stigmergy.ImprovementStage`)

### `improvementCaseId` (`java.util.UUID`)

### `stageHistory` (`java.util.List<io.casehub.api.view.EvolutionStateSnapshot.StageProgress>`)

### `startedAt` (`java.time.Instant`)

### `target` (`java.lang.String`)

## Record Components

### `blockedBy` (`java.util.UUID`)

### `category` (`java.lang.String`)

### `conflictBlocked` (`boolean`)

### `currentStage` (`io.casehub.api.model.stigmergy.ImprovementStage`)

### `improvementCaseId` (`java.util.UUID`)

### `stageHistory` (`java.util.List<io.casehub.api.view.EvolutionStateSnapshot.StageProgress>`)

### `startedAt` (`java.time.Instant`)

### `target` (`java.lang.String`)

## Constructors

### `public ImprovementStreamView(java.util.UUID improvementCaseId, java.lang.String category, java.lang.String target, io.casehub.api.model.stigmergy.ImprovementStage currentStage, java.util.List<io.casehub.api.view.EvolutionStateSnapshot.StageProgress> stageHistory, java.util.UUID blockedBy, boolean conflictBlocked, java.time.Instant startedAt)`

#### Parameters

- `improvementCaseId` (`java.util.UUID`)
- `category` (`java.lang.String`)
- `target` (`java.lang.String`)
- `currentStage` (`io.casehub.api.model.stigmergy.ImprovementStage`)
- `stageHistory` (`java.util.List<io.casehub.api.view.EvolutionStateSnapshot.StageProgress>`)
- `blockedBy` (`java.util.UUID`)
- `conflictBlocked` (`boolean`)
- `startedAt` (`java.time.Instant`)

## Methods

### `public java.util.UUID blockedBy()`

### `public java.lang.String category()`

### `public boolean conflictBlocked()`

### `public io.casehub.api.model.stigmergy.ImprovementStage currentStage()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.UUID improvementCaseId()`

### `public java.util.List<io.casehub.api.view.EvolutionStateSnapshot.StageProgress> stageHistory()`

### `public java.time.Instant startedAt()`

### `public java.lang.String target()`

### `public final java.lang.String toString()`
