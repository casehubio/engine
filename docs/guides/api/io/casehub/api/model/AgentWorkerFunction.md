# io.casehub.api.model.AgentWorkerFunction

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `deferredConfig` (`io.casehub.api.model.ai.DeferredAgentConfig`)

### `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)

## Record Components

### `deferredConfig` (`io.casehub.api.model.ai.DeferredAgentConfig`)

### `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)

## Constructors

### `public AgentWorkerFunction(io.casehub.api.model.ai.WorkerAgentInvoker invoker)`

#### Parameters

- `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)

### `public AgentWorkerFunction(io.casehub.api.model.ai.WorkerAgentInvoker invoker, io.casehub.api.model.ai.DeferredAgentConfig deferredConfig)`

#### Parameters

- `invoker` (`io.casehub.api.model.ai.WorkerAgentInvoker`)
- `deferredConfig` (`io.casehub.api.model.ai.DeferredAgentConfig`)

## Methods

### `public io.casehub.api.model.ai.DeferredAgentConfig deferredConfig()`

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

### `public static io.casehub.api.model.AgentWorkerFunction ofCapability(io.casehub.api.model.ai.DeferredAgentConfig.CapabilityConfig config)`

#### Parameters

- `config` (`io.casehub.api.model.ai.DeferredAgentConfig.CapabilityConfig`)

### `public static io.casehub.api.model.AgentWorkerFunction ofSlot(io.casehub.api.model.ai.DeferredAgentConfig.SlotConfig config)`

#### Parameters

- `config` (`io.casehub.api.model.ai.DeferredAgentConfig.SlotConfig`)

### `public java.lang.Class<java.util.Map<java.lang.String,java.lang.Object>> outputType()`

### `public final java.lang.String toString()`
