# io.casehub.api.model.stigmergy.ImprovementBudget

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `allowedRepos` (`java.util.List<java.lang.String>`)

### `cooldownMinutes` (`java.lang.Integer`)

### `deniedPaths` (`java.util.List<java.lang.String>`)

### `maxChangeSize` (`java.lang.Integer`)

### `maxConcurrent` (`java.lang.Integer`)

### `maxPerDay` (`java.lang.Integer`)

### `requireReview` (`java.lang.Boolean`)

## Record Components

### `allowedRepos` (`java.util.List<java.lang.String>`)

### `cooldownMinutes` (`java.lang.Integer`)

### `deniedPaths` (`java.util.List<java.lang.String>`)

### `maxChangeSize` (`java.lang.Integer`)

### `maxConcurrent` (`java.lang.Integer`)

### `maxPerDay` (`java.lang.Integer`)

### `requireReview` (`java.lang.Boolean`)

## Constructors

### `public ImprovementBudget(java.lang.Integer maxConcurrent, java.lang.Integer maxPerDay, java.lang.Integer cooldownMinutes, java.util.List<java.lang.String> allowedRepos, java.util.List<java.lang.String> deniedPaths, java.lang.Boolean requireReview, java.lang.Integer maxChangeSize)`

#### Parameters

- `maxConcurrent` (`java.lang.Integer`)
- `maxPerDay` (`java.lang.Integer`)
- `cooldownMinutes` (`java.lang.Integer`)
- `allowedRepos` (`java.util.List<java.lang.String>`)
- `deniedPaths` (`java.util.List<java.lang.String>`)
- `requireReview` (`java.lang.Boolean`)
- `maxChangeSize` (`java.lang.Integer`)

## Methods

### `public java.util.List<java.lang.String> allowedRepos()`

### `public java.lang.Integer cooldownMinutes()`

### `public java.util.List<java.lang.String> deniedPaths()`

### `public java.util.List<java.lang.String> effectiveAllowedRepos()`

### `public int effectiveCooldownMinutes()`

### `public java.util.List<java.lang.String> effectiveDeniedPaths()`

### `public int effectiveMaxChangeSize()`

### `public int effectiveMaxConcurrent()`

### `public int effectiveMaxPerDay()`

### `public boolean effectiveRequireReview()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.Integer maxChangeSize()`

### `public java.lang.Integer maxConcurrent()`

### `public java.lang.Integer maxPerDay()`

### `public java.lang.Boolean requireReview()`

### `public final java.lang.String toString()`
