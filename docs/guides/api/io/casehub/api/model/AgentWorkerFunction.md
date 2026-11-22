# io.casehub.api.model.AgentWorkerFunction

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)

### `slotConfig` (`io.casehub.api.model.ai.AgentSlotConfig`)

## Record Components

### `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)

### `slotConfig` (`io.casehub.api.model.ai.AgentSlotConfig`)

## Constructors

### `public AgentWorkerFunction(io.casehub.api.model.ai.WorkerAgentInvoker invoker)`

#### Parameters

- `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)

### `public AgentWorkerFunction(io.casehub.api.model.ai.WorkerAgentInvoker invoker, io.casehub.api.model.ai.AgentSlotConfig slotConfig)`

#### Parameters

- `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)
- `slotConfig` (`io.casehub.api.model.ai.AgentSlotConfig`)

## Methods

### `public static io.casehub.api.model.AgentWorkerFunction eager(io.casehub.api.model.ai.WorkerAgentInvoker invoker)`

#### Parameters

- `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.Class<java.util.Map<java.lang.String,java.lang.Object>> inputType()`

### `public io.casehub.api.model.ai.WorkerAgentInvoker invoker()`

### `public boolean isDeferred()`

### `public static io.casehub.api.model.AgentWorkerFunction ofSlot(io.casehub.api.model.ai.AgentSlotConfig slotConfig)`

#### Parameters

- `slotConfig` (`io.casehub.api.model.ai.AgentSlotConfig`)

### `public java.lang.Class<java.util.Map<java.lang.String,java.lang.Object>> outputType()`

### `public io.casehub.api.model.ai.AgentSlotConfig slotConfig()`

### `public final java.lang.String toString()`
