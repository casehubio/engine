# io.casehub.api.view.EvolutionStateSnapshot.StageProgress

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `completedAt` (`java.time.Instant`)

### `enteredAt` (`java.time.Instant`)

### `stage` (`io.casehub.api.model.stigmergy.ImprovementStage`)

### `status` (`io.casehub.api.view.EvolutionStateSnapshot.StageProgress.StageStatus`)

## Record Components

### `completedAt` (`java.time.Instant`)

### `enteredAt` (`java.time.Instant`)

### `stage` (`io.casehub.api.model.stigmergy.ImprovementStage`)

### `status` (`io.casehub.api.view.EvolutionStateSnapshot.StageProgress.StageStatus`)

## Constructors

### `public StageProgress(io.casehub.api.model.stigmergy.ImprovementStage stage, io.casehub.api.view.EvolutionStateSnapshot.StageProgress.StageStatus status, java.time.Instant enteredAt, java.time.Instant completedAt)`

#### Parameters

- `stage` (`io.casehub.api.model.stigmergy.ImprovementStage`)
- `status` (`io.casehub.api.view.EvolutionStateSnapshot.StageProgress.StageStatus`)
- `enteredAt` (`java.time.Instant`)
- `completedAt` (`java.time.Instant`)

## Methods

### `public java.time.Instant completedAt()`

### `public java.time.Instant enteredAt()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.ImprovementStage stage()`

### `public io.casehub.api.view.EvolutionStateSnapshot.StageProgress.StageStatus status()`

### `public final java.lang.String toString()`
