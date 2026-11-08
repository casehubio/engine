# io.casehub.engine.plan.snapshot.DagResultView

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `allSucceeded` (`boolean`)

### `completedResults` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `elapsed` (`java.time.Duration`)

### `nodeDurationsMs` (`java.util.Map<java.lang.String,java.lang.Long>`)

### `nodeStates` (`java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagResultView.NodeStateView>`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `allSucceeded` (`boolean`)

### `completedResults` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `elapsed` (`java.time.Duration`)

### `nodeDurationsMs` (`java.util.Map<java.lang.String,java.lang.Long>`)

### `nodeStates` (`java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagResultView.NodeStateView>`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public DagResultView(java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagResultView.NodeStateView> nodeStates, java.util.Map<java.lang.String,java.lang.Object> completedResults, boolean allSucceeded, java.time.Duration elapsed, java.time.Instant timestamp, java.util.Map<java.lang.String,java.lang.Long> nodeDurationsMs)`

#### Parameters

- `nodeStates` (`java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagResultView.NodeStateView>`)
- `completedResults` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `allSucceeded` (`boolean`)
- `elapsed` (`java.time.Duration`)
- `timestamp` (`java.time.Instant`)
- `nodeDurationsMs` (`java.util.Map<java.lang.String,java.lang.Long>`)

## Methods

### `public boolean allSucceeded()`

### `public java.util.Map<java.lang.String,java.lang.Object> completedResults()`

### `public java.time.Duration elapsed()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Long> nodeDurationsMs()`

### `public java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagResultView.NodeStateView> nodeStates()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`
