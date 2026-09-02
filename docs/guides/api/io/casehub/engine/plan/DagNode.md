# io.casehub.engine.plan.DagNode

**Package:** `io.casehub.engine.plan`

**Kind:** `record`

## Fields

### `contingency` (`io.casehub.engine.plan.DagPlan<T>`)

### `dependsOn` (`java.util.Set<java.lang.String>`)

### `id` (`java.lang.String`)

### `joinType` (`io.casehub.engine.plan.JoinType`)

### `judgment` (`io.casehub.api.model.JudgmentTarget`)

### `task` (`T`)

## Record Components

### `contingency` (`io.casehub.engine.plan.DagPlan<T>`)

### `dependsOn` (`java.util.Set<java.lang.String>`)

### `id` (`java.lang.String`)

### `joinType` (`io.casehub.engine.plan.JoinType`)

### `judgment` (`io.casehub.api.model.JudgmentTarget`)

### `task` (`T`)

## Constructors

### `public DagNode(java.lang.String id, T task, java.util.Set<java.lang.String> dependsOn, io.casehub.engine.plan.JoinType joinType)`

#### Parameters

- `id` (`java.lang.String`)
- `task` (`T`)
- `dependsOn` (`java.util.Set<java.lang.String>`)
- `joinType` (`io.casehub.engine.plan.JoinType`)

### `public DagNode(java.lang.String id, T task, java.util.Set<java.lang.String> dependsOn, io.casehub.engine.plan.JoinType joinType, io.casehub.engine.plan.DagPlan<T> contingency)`

#### Parameters

- `id` (`java.lang.String`)
- `task` (`T`)
- `dependsOn` (`java.util.Set<java.lang.String>`)
- `joinType` (`io.casehub.engine.plan.JoinType`)
- `contingency` (`io.casehub.engine.plan.DagPlan<T>`)

### `public DagNode(java.lang.String id, T task, java.util.Set<java.lang.String> dependsOn, io.casehub.engine.plan.JoinType joinType, io.casehub.engine.plan.DagPlan<T> contingency, io.casehub.api.model.JudgmentTarget judgment)`

#### Parameters

- `id` (`java.lang.String`)
- `task` (`T`)
- `dependsOn` (`java.util.Set<java.lang.String>`)
- `joinType` (`io.casehub.engine.plan.JoinType`)
- `contingency` (`io.casehub.engine.plan.DagPlan<T>`)
- `judgment` (`io.casehub.api.model.JudgmentTarget`)

## Methods

### `public io.casehub.engine.plan.DagPlan<T> contingency()`

### `public java.util.Set<java.lang.String> dependsOn()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String id()`

### `public io.casehub.engine.plan.JoinType joinType()`

### `public io.casehub.api.model.JudgmentTarget judgment()`

### `public T task()`

### `public final java.lang.String toString()`
