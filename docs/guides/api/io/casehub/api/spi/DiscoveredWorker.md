# io.casehub.api.spi.DiscoveredWorker

**Package:** `io.casehub.api.spi`

**Kind:** `record`

A worker contributed by a `WorkerFunctionProvider` during tool discovery. Each discovered
worker carries a capability (derived from the external tool's metadata) and a function that
invokes the tool.

<p>Used by providers that discover multiple tools from a single YAML declaration (e.g., MCP
servers exposing multiple tools via `tools/list`).

## Fields

### `capability` (`Capability`)

### `function` (`WorkerFunction<?,?>`)

### `workerName` (`java.lang.String`)

## Record Components

### `capability` (`Capability`)

### `function` (`WorkerFunction<?,?>`)

### `workerName` (`java.lang.String`)

## Constructors

### `public DiscoveredWorker(java.lang.String workerName, Capability capability, WorkerFunction<?,?> function)`

#### Parameters

- `workerName` (`java.lang.String`)
- `capability` (`Capability`)
- `function` (`WorkerFunction<?,?>`)

## Methods

### `public Capability capability()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public WorkerFunction<?,?> function()`

### `public final int hashCode()`

### `public final java.lang.String toString()`

### `public java.lang.String workerName()`
