# io.casehub.api.view.EvolutionStateSnapshot.CategoryStateView

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `failureCount` (`int`)

### `paused` (`boolean`)

### `pausedUntil` (`java.time.Instant`)

### `rejectionCount` (`int`)

### `successCount` (`int`)

### `suppressed` (`boolean`)

## Record Components

### `failureCount` (`int`)

### `paused` (`boolean`)

### `pausedUntil` (`java.time.Instant`)

### `rejectionCount` (`int`)

### `successCount` (`int`)

### `suppressed` (`boolean`)

## Constructors

### `public CategoryStateView(int successCount, int failureCount, int rejectionCount, boolean paused, java.time.Instant pausedUntil, boolean suppressed)`

#### Parameters

- `successCount` (`int`)
- `failureCount` (`int`)
- `rejectionCount` (`int`)
- `paused` (`boolean`)
- `pausedUntil` (`java.time.Instant`)
- `suppressed` (`boolean`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public int failureCount()`

### `public final int hashCode()`

### `public boolean paused()`

### `public java.time.Instant pausedUntil()`

### `public int rejectionCount()`

### `public int successCount()`

### `public boolean suppressed()`

### `public final java.lang.String toString()`
