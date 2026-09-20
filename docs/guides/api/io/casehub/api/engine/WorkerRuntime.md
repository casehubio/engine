# io.casehub.api.engine.WorkerRuntime

**Package:** `io.casehub.api.engine`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.context.CaseContext awaitCase(java.util.UUID childCaseId, java.time.Duration timeout)`

#### Parameters

- `childCaseId` (`java.util.UUID`)
- `timeout` (`java.time.Duration`)

### `public abstract io.casehub.api.model.WorkerContext context()`

### `public default io.casehub.api.engine.InterestSpace interests()`

### `public default void leave()`

### `public default io.casehub.api.engine.MetricsSpace metrics()`

### `public default io.casehub.api.engine.NeighborSpace neighbors()`

### `public default io.casehub.api.engine.RuleSpace rules()`

### `public default io.casehub.api.engine.SignalSpace signals()`

### `public abstract io.casehub.api.context.CaseContext spawnAndAwaitCase(java.lang.String caseType, java.util.Map<java.lang.String,java.lang.Object> input, java.time.Duration timeout)`

#### Parameters

- `caseType` (`java.lang.String`)
- `input` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `timeout` (`java.time.Duration`)

### `public abstract java.util.UUID spawnCase(java.lang.String caseType, java.util.Map<java.lang.String,java.lang.Object> input)`

#### Parameters

- `caseType` (`java.lang.String`)
- `input` (`java.util.Map<java.lang.String,java.lang.Object>`)
