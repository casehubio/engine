# io.casehub.api.model.ai.WorkerAgentResponse

**Package:** `io.casehub.api.model.ai`

**Kind:** `record`

## Fields

### `invocationResult` (`AgentEvent.InvocationComplete`)

### `result` (`WorkerResult<java.util.Map<java.lang.String,java.lang.Object>>`)

## Record Components

### `invocationResult` (`AgentEvent.InvocationComplete`)

### `result` (`WorkerResult<java.util.Map<java.lang.String,java.lang.Object>>`)

## Constructors

### `public WorkerAgentResponse(WorkerResult<java.util.Map<java.lang.String,java.lang.Object>> result, AgentEvent.InvocationComplete invocationResult)`

#### Parameters

- `result` (`WorkerResult<java.util.Map<java.lang.String,java.lang.Object>>`)
- `invocationResult` (`AgentEvent.InvocationComplete`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public AgentEvent.InvocationComplete invocationResult()`

### `public WorkerResult<java.util.Map<java.lang.String,java.lang.Object>> result()`

### `public final java.lang.String toString()`
