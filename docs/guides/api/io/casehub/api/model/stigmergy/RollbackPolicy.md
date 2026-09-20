# io.casehub.api.model.stigmergy.RollbackPolicy

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `autoRevertThreshold` (`java.lang.Double`)

### `pauseCategoryOnRegression` (`java.lang.Boolean`)

### `pauseThreshold` (`java.lang.Double`)

### `regressionWindowMinutes` (`java.lang.Integer`)

### `requireReviewForRevert` (`java.lang.Boolean`)

### `sustainedFailureCount` (`java.lang.Integer`)

## Record Components

### `autoRevertThreshold` (`java.lang.Double`)

### `pauseCategoryOnRegression` (`java.lang.Boolean`)

### `pauseThreshold` (`java.lang.Double`)

### `regressionWindowMinutes` (`java.lang.Integer`)

### `requireReviewForRevert` (`java.lang.Boolean`)

### `sustainedFailureCount` (`java.lang.Integer`)

## Constructors

### `public RollbackPolicy(java.lang.Double autoRevertThreshold, java.lang.Double pauseThreshold, java.lang.Boolean requireReviewForRevert, java.lang.Boolean pauseCategoryOnRegression, java.lang.Integer regressionWindowMinutes, java.lang.Integer sustainedFailureCount)`

#### Parameters

- `autoRevertThreshold` (`java.lang.Double`)
- `pauseThreshold` (`java.lang.Double`)
- `requireReviewForRevert` (`java.lang.Boolean`)
- `pauseCategoryOnRegression` (`java.lang.Boolean`)
- `regressionWindowMinutes` (`java.lang.Integer`)
- `sustainedFailureCount` (`java.lang.Integer`)

## Methods

### `public java.lang.Double autoRevertThreshold()`

### `public double effectiveAutoRevertThreshold()`

### `public boolean effectivePauseCategoryOnRegression()`

### `public double effectivePauseThreshold()`

### `public int effectiveRegressionWindowMinutes()`

### `public boolean effectiveRequireReviewForRevert()`

### `public int effectiveSustainedFailureCount()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.Boolean pauseCategoryOnRegression()`

### `public java.lang.Double pauseThreshold()`

### `public java.lang.Integer regressionWindowMinutes()`

### `public java.lang.Boolean requireReviewForRevert()`

### `public java.lang.Integer sustainedFailureCount()`

### `public final java.lang.String toString()`
