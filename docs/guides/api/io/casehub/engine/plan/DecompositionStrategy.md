# io.casehub.engine.plan.DecompositionStrategy

**Package:** `io.casehub.engine.plan`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.engine.plan.DagPlan<io.casehub.engine.plan.TaskNode.LeafTask<T>> decompose(io.casehub.engine.plan.TaskNode<T> task, io.casehub.engine.plan.DecompositionContext<T> context)`

#### Parameters

- `task` (`io.casehub.engine.plan.TaskNode<T>`)
- `context` (`io.casehub.engine.plan.DecompositionContext<T>`)

### `public default java.lang.String id()`

### `public default io.casehub.engine.plan.DagPlan<io.casehub.engine.plan.TaskNode.LeafTask<T>> replan(io.casehub.engine.plan.TaskNode<T> task, io.casehub.engine.plan.DecompositionContext<T> context, io.casehub.engine.plan.ReplanContext<T> replanContext)`

#### Parameters

- `task` (`io.casehub.engine.plan.TaskNode<T>`)
- `context` (`io.casehub.engine.plan.DecompositionContext<T>`)
- `replanContext` (`io.casehub.engine.plan.ReplanContext<T>`)
