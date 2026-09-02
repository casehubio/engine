# io.casehub.engine.plan.ReplanContext

**Package:** `io.casehub.engine.plan`

**Kind:** `record`

## Fields

### `completedSteps` (`java.util.List<io.casehub.engine.plan.ReplanContext.CompletedStep>`)

### `failedStep` (`io.casehub.engine.plan.ReplanContext.FailedStep`)

### `originalPlan` (`io.casehub.engine.plan.DagPlan<io.casehub.engine.plan.TaskNode.LeafTask<T>>`)

### `replanCount` (`int`)

## Record Components

### `completedSteps` (`java.util.List<io.casehub.engine.plan.ReplanContext.CompletedStep>`)

### `failedStep` (`io.casehub.engine.plan.ReplanContext.FailedStep`)

### `originalPlan` (`io.casehub.engine.plan.DagPlan<io.casehub.engine.plan.TaskNode.LeafTask<T>>`)

### `replanCount` (`int`)

## Constructors

### `public ReplanContext(java.util.List<io.casehub.engine.plan.ReplanContext.CompletedStep> completedSteps, io.casehub.engine.plan.ReplanContext.FailedStep failedStep, io.casehub.engine.plan.DagPlan<io.casehub.engine.plan.TaskNode.LeafTask<T>> originalPlan, int replanCount)`

#### Parameters

- `completedSteps` (`java.util.List<io.casehub.engine.plan.ReplanContext.CompletedStep>`)
- `failedStep` (`io.casehub.engine.plan.ReplanContext.FailedStep`)
- `originalPlan` (`io.casehub.engine.plan.DagPlan<io.casehub.engine.plan.TaskNode.LeafTask<T>>`)
- `replanCount` (`int`)

## Methods

### `public java.util.List<io.casehub.engine.plan.ReplanContext.CompletedStep> completedSteps()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public io.casehub.engine.plan.ReplanContext.FailedStep failedStep()`

### `public final int hashCode()`

### `public io.casehub.engine.plan.DagPlan<io.casehub.engine.plan.TaskNode.LeafTask<T>> originalPlan()`

### `public int replanCount()`

### `public final java.lang.String toString()`
