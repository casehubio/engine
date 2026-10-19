# io.casehub.api.model.ai.AgentInvocation

**Package:** `io.casehub.api.model.ai`

**Kind:** `record`

A completed synchronous invocation of a streaming agent.

## Fields

### `completion` (`AgentEvent.InvocationComplete`)

### `text` (`java.lang.String`)

## Record Components

### `completion` (`AgentEvent.InvocationComplete`)

### `text` (`java.lang.String`)

## Constructors

### `public AgentInvocation(java.lang.String text, AgentEvent.InvocationComplete completion)`

#### Parameters

- `text` (`java.lang.String`)
- `completion` (`AgentEvent.InvocationComplete`)

## Methods

### `public AgentEvent.InvocationComplete completion()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public static io.casehub.api.model.ai.AgentInvocation invoke(AgentProvider provider, AgentSessionConfig config, java.time.Duration timeout)`

#### Parameters

- `provider` (`AgentProvider`)
- `config` (`AgentSessionConfig`)
- `timeout` (`java.time.Duration`)

### `public java.lang.String text()`

### `public final java.lang.String toString()`
