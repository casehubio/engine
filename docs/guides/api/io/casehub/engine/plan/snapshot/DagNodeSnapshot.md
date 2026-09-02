# io.casehub.engine.plan.snapshot.DagNodeSnapshot

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `contingency` (`io.casehub.engine.plan.snapshot.DagPlanSnapshot`)

### `dependsOn` (`java.util.Set<java.lang.String>`)

### `executorName` (`java.lang.String`)

### `hasJudgment` (`boolean`)

### `id` (`java.lang.String`)

### `joinType` (`io.casehub.engine.plan.JoinType`)

### `taskDescription` (`java.lang.String`)

### `taskId` (`java.lang.String`)

## Record Components

### `contingency` (`io.casehub.engine.plan.snapshot.DagPlanSnapshot`)

### `dependsOn` (`java.util.Set<java.lang.String>`)

### `executorName` (`java.lang.String`)

### `hasJudgment` (`boolean`)

### `id` (`java.lang.String`)

### `joinType` (`io.casehub.engine.plan.JoinType`)

### `taskDescription` (`java.lang.String`)

### `taskId` (`java.lang.String`)

## Constructors

### `public DagNodeSnapshot(java.lang.String id, java.lang.String taskId, java.lang.String taskDescription, java.lang.String executorName, java.util.Set<java.lang.String> dependsOn, io.casehub.engine.plan.JoinType joinType)`

#### Parameters

- `id` (`java.lang.String`)
- `taskId` (`java.lang.String`)
- `taskDescription` (`java.lang.String`)
- `executorName` (`java.lang.String`)
- `dependsOn` (`java.util.Set<java.lang.String>`)
- `joinType` (`io.casehub.engine.plan.JoinType`)

### `public DagNodeSnapshot(java.lang.String id, java.lang.String taskId, java.lang.String taskDescription, java.lang.String executorName, java.util.Set<java.lang.String> dependsOn, io.casehub.engine.plan.JoinType joinType, io.casehub.engine.plan.snapshot.DagPlanSnapshot contingency)`

#### Parameters

- `id` (`java.lang.String`)
- `taskId` (`java.lang.String`)
- `taskDescription` (`java.lang.String`)
- `executorName` (`java.lang.String`)
- `dependsOn` (`java.util.Set<java.lang.String>`)
- `joinType` (`io.casehub.engine.plan.JoinType`)
- `contingency` (`io.casehub.engine.plan.snapshot.DagPlanSnapshot`)

### `public DagNodeSnapshot(java.lang.String id, java.lang.String taskId, java.lang.String taskDescription, java.lang.String executorName, java.util.Set<java.lang.String> dependsOn, io.casehub.engine.plan.JoinType joinType, io.casehub.engine.plan.snapshot.DagPlanSnapshot contingency, boolean hasJudgment)`

#### Parameters

- `id` (`java.lang.String`)
- `taskId` (`java.lang.String`)
- `taskDescription` (`java.lang.String`)
- `executorName` (`java.lang.String`)
- `dependsOn` (`java.util.Set<java.lang.String>`)
- `joinType` (`io.casehub.engine.plan.JoinType`)
- `contingency` (`io.casehub.engine.plan.snapshot.DagPlanSnapshot`)
- `hasJudgment` (`boolean`)

## Methods

### `public io.casehub.engine.plan.snapshot.DagPlanSnapshot contingency()`

### `public java.util.Set<java.lang.String> dependsOn()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String executorName()`

### `public boolean hasJudgment()`

### `public final int hashCode()`

### `public java.lang.String id()`

### `public io.casehub.engine.plan.JoinType joinType()`

### `public java.lang.String taskDescription()`

### `public java.lang.String taskId()`

### `public final java.lang.String toString()`
