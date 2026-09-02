# io.casehub.engine.plan.snapshot.CompoundItemSnapshot

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `children` (`java.util.List<io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot>`)

### `completion` (`io.casehub.engine.plan.snapshot.CompletionSemanticsSnapshot`)

### `dispatchMode` (`java.lang.String`)

### `entryCondition` (`java.lang.String`)

### `exitCondition` (`java.lang.String`)

### `id` (`java.lang.String`)

### `name` (`java.lang.String`)

### `planningStrategy` (`java.lang.String`)

### `repeatable` (`boolean`)

### `scopedBindings` (`java.util.Map<java.lang.String,java.lang.String>`)

## Record Components

### `children` (`java.util.List<io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot>`)

### `completion` (`io.casehub.engine.plan.snapshot.CompletionSemanticsSnapshot`)

### `dispatchMode` (`java.lang.String`)

### `entryCondition` (`java.lang.String`)

### `exitCondition` (`java.lang.String`)

### `id` (`java.lang.String`)

### `name` (`java.lang.String`)

### `planningStrategy` (`java.lang.String`)

### `repeatable` (`boolean`)

### `scopedBindings` (`java.util.Map<java.lang.String,java.lang.String>`)

## Constructors

### `public CompoundItemSnapshot(java.lang.String id, java.lang.String name, java.util.List<io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot> children, java.lang.String planningStrategy, io.casehub.engine.plan.snapshot.CompletionSemanticsSnapshot completion, java.lang.String dispatchMode, java.lang.String entryCondition, java.lang.String exitCondition, boolean repeatable, java.util.Map<java.lang.String,java.lang.String> scopedBindings)`

#### Parameters

- `id` (`java.lang.String`)
- `name` (`java.lang.String`)
- `children` (`java.util.List<io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot>`)
- `planningStrategy` (`java.lang.String`)
- `completion` (`io.casehub.engine.plan.snapshot.CompletionSemanticsSnapshot`)
- `dispatchMode` (`java.lang.String`)
- `entryCondition` (`java.lang.String`)
- `exitCondition` (`java.lang.String`)
- `repeatable` (`boolean`)
- `scopedBindings` (`java.util.Map<java.lang.String,java.lang.String>`)

## Methods

### `public java.util.List<io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot> children()`

### `public io.casehub.engine.plan.snapshot.CompletionSemanticsSnapshot completion()`

### `public java.lang.String dispatchMode()`

### `public java.lang.String entryCondition()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String exitCondition()`

### `public final int hashCode()`

### `public java.lang.String id()`

### `public java.lang.String name()`

### `public java.lang.String planningStrategy()`

### `public boolean repeatable()`

### `public java.util.Map<java.lang.String,java.lang.String> scopedBindings()`

### `public final java.lang.String toString()`
