# io.casehub.engine.plan.snapshot.ExecutionStateView

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `activeAgents` (`java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRefView>`)

### `completedAgents` (`java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentResultView>`)

### `completedAt` (`java.time.Instant`)

### `executionId` (`java.lang.String`)

### `iteration` (`java.lang.Integer`)

### `model` (`io.casehub.engine.plan.snapshot.ExecutionStateView.ExecutionModelView`)

### `result` (`java.lang.String`)

### `startedAt` (`java.time.Instant`)

### `state` (`java.lang.String`)

## Record Components

### `activeAgents` (`java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRefView>`)

### `completedAgents` (`java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentResultView>`)

### `completedAt` (`java.time.Instant`)

### `executionId` (`java.lang.String`)

### `iteration` (`java.lang.Integer`)

### `model` (`io.casehub.engine.plan.snapshot.ExecutionStateView.ExecutionModelView`)

### `result` (`java.lang.String`)

### `startedAt` (`java.time.Instant`)

### `state` (`java.lang.String`)

## Constructors

### `public ExecutionStateView(java.lang.String executionId, java.lang.String state, io.casehub.engine.plan.snapshot.ExecutionStateView.ExecutionModelView model, java.lang.String result, java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRefView> activeAgents, java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentResultView> completedAgents, java.lang.Integer iteration, java.time.Instant startedAt, java.time.Instant completedAt)`

#### Parameters

- `executionId` (`java.lang.String`)
- `state` (`java.lang.String`)
- `model` (`io.casehub.engine.plan.snapshot.ExecutionStateView.ExecutionModelView`)
- `result` (`java.lang.String`)
- `activeAgents` (`java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRefView>`)
- `completedAgents` (`java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentResultView>`)
- `iteration` (`java.lang.Integer`)
- `startedAt` (`java.time.Instant`)
- `completedAt` (`java.time.Instant`)

## Methods

### `public java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRefView> activeAgents()`

### `public java.util.List<io.casehub.engine.plan.snapshot.ExecutionStateView.AgentResultView> completedAgents()`

### `public java.time.Instant completedAt()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String executionId()`

### `public final int hashCode()`

### `public java.lang.Integer iteration()`

### `public io.casehub.engine.plan.snapshot.ExecutionStateView.ExecutionModelView model()`

### `public java.lang.String result()`

### `public java.time.Instant startedAt()`

### `public java.lang.String state()`

### `public final java.lang.String toString()`
