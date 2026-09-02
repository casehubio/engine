# io.casehub.engine.plan.snapshot.PlanVersionDelta

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `affectedCompoundIds` (`java.util.List<java.lang.String>`)

### `materializedStepIds` (`java.util.List<java.lang.String>`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `obsoletedStepIds` (`java.util.List<java.lang.String>`)

## Record Components

### `affectedCompoundIds` (`java.util.List<java.lang.String>`)

### `materializedStepIds` (`java.util.List<java.lang.String>`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `obsoletedStepIds` (`java.util.List<java.lang.String>`)

## Constructors

### `public PlanVersionDelta(java.util.List<java.lang.String> materializedStepIds, java.util.List<java.lang.String> obsoletedStepIds, java.util.List<java.lang.String> affectedCompoundIds, java.util.Map<java.lang.String,java.lang.Object> metadata)`

#### Parameters

- `materializedStepIds` (`java.util.List<java.lang.String>`)
- `obsoletedStepIds` (`java.util.List<java.lang.String>`)
- `affectedCompoundIds` (`java.util.List<java.lang.String>`)
- `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

## Methods

### `public java.util.List<java.lang.String> affectedCompoundIds()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.List<java.lang.String> materializedStepIds()`

### `public java.util.Map<java.lang.String,java.lang.Object> metadata()`

### `public java.util.List<java.lang.String> obsoletedStepIds()`

### `public final java.lang.String toString()`
