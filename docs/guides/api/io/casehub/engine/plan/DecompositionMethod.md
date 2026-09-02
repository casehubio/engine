# io.casehub.engine.plan.DecompositionMethod

**Package:** `io.casehub.engine.plan`

**Kind:** `record`

## Fields

### `estimatedCost` (`java.util.Map<java.lang.String,java.lang.Integer>`)

### `estimatedDuration` (`java.time.Duration`)

### `guard` (`java.util.function.Predicate<T>`)

### `guardLabel` (`java.lang.String`)

### `name` (`java.lang.String`)

### `strategy` (`io.casehub.engine.plan.DecompositionStrategy<T>`)

## Record Components

### `estimatedCost` (`java.util.Map<java.lang.String,java.lang.Integer>`)

### `estimatedDuration` (`java.time.Duration`)

### `guard` (`java.util.function.Predicate<T>`)

### `guardLabel` (`java.lang.String`)

### `name` (`java.lang.String`)

### `strategy` (`io.casehub.engine.plan.DecompositionStrategy<T>`)

## Constructors

### `public DecompositionMethod(java.lang.String name, java.util.function.Predicate<T> guard, io.casehub.engine.plan.DecompositionStrategy<T> strategy, java.lang.String guardLabel)`

#### Parameters

- `name` (`java.lang.String`)
- `guard` (`java.util.function.Predicate<T>`)
- `strategy` (`io.casehub.engine.plan.DecompositionStrategy<T>`)
- `guardLabel` (`java.lang.String`)

### `public DecompositionMethod(java.lang.String name, java.util.function.Predicate<T> guard, io.casehub.engine.plan.DecompositionStrategy<T> strategy, java.lang.String guardLabel, java.util.Map<java.lang.String,java.lang.Integer> estimatedCost, java.time.Duration estimatedDuration)`

#### Parameters

- `name` (`java.lang.String`)
- `guard` (`java.util.function.Predicate<T>`)
- `strategy` (`io.casehub.engine.plan.DecompositionStrategy<T>`)
- `guardLabel` (`java.lang.String`)
- `estimatedCost` (`java.util.Map<java.lang.String,java.lang.Integer>`)
- `estimatedDuration` (`java.time.Duration`)

### `public DecompositionMethod(java.util.function.Predicate<T> guard, io.casehub.engine.plan.DecompositionStrategy<T> strategy)`

#### Parameters

- `guard` (`java.util.function.Predicate<T>`)
- `strategy` (`io.casehub.engine.plan.DecompositionStrategy<T>`)

### `public DecompositionMethod(java.util.function.Predicate<T> guard, io.casehub.engine.plan.DecompositionStrategy<T> strategy, java.lang.String guardLabel)`

#### Parameters

- `guard` (`java.util.function.Predicate<T>`)
- `strategy` (`io.casehub.engine.plan.DecompositionStrategy<T>`)
- `guardLabel` (`java.lang.String`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.Map<java.lang.String,java.lang.Integer> estimatedCost()`

### `public java.time.Duration estimatedDuration()`

### `public java.util.function.Predicate<T> guard()`

### `public java.lang.String guardLabel()`

### `public final int hashCode()`

### `public java.lang.String name()`

### `public io.casehub.engine.plan.DecompositionStrategy<T> strategy()`

### `public final java.lang.String toString()`
