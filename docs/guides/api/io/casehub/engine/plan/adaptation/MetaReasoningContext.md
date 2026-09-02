# io.casehub.engine.plan.adaptation.MetaReasoningContext

**Package:** `io.casehub.engine.plan.adaptation`

**Kind:** `record`

## Fields

### `adaptationContext` (`io.casehub.engine.plan.adaptation.AdaptationContext`)

### `adaptationCount` (`int`)

### `completedStepCount` (`int`)

### `latestFailureCategory` (`io.casehub.api.model.FailureCategory`)

### `pendingStepCount` (`int`)

### `totalStepCount` (`int`)

## Record Components

### `adaptationContext` (`io.casehub.engine.plan.adaptation.AdaptationContext`)

### `adaptationCount` (`int`)

### `completedStepCount` (`int`)

### `latestFailureCategory` (`io.casehub.api.model.FailureCategory`)

### `pendingStepCount` (`int`)

### `totalStepCount` (`int`)

## Constructors

### `public MetaReasoningContext(io.casehub.engine.plan.adaptation.AdaptationContext adaptationContext, int adaptationCount, int completedStepCount, int pendingStepCount, int totalStepCount, io.casehub.api.model.FailureCategory latestFailureCategory)`

#### Parameters

- `adaptationContext` (`io.casehub.engine.plan.adaptation.AdaptationContext`)
- `adaptationCount` (`int`)
- `completedStepCount` (`int`)
- `pendingStepCount` (`int`)
- `totalStepCount` (`int`)
- `latestFailureCategory` (`io.casehub.api.model.FailureCategory`)

## Methods

### `public io.casehub.engine.plan.adaptation.AdaptationContext adaptationContext()`

### `public int adaptationCount()`

### `public int completedStepCount()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.FailureCategory latestFailureCategory()`

### `public int pendingStepCount()`

### `public double remainingRatio()`

### `public final java.lang.String toString()`

### `public int totalStepCount()`
