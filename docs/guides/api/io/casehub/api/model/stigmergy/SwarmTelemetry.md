# io.casehub.api.model.stigmergy.SwarmTelemetry

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `activeTime` (`java.time.Duration`)

### `agentId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `invocationCount` (`int`)

### `messagesSent` (`int`)

### `signalsDeposited` (`int`)

### `tasksCompleted` (`int`)

### `tokensConsumed` (`long`)

### `totalLatency` (`java.time.Duration`)

### `workState` (`io.casehub.api.model.stigmergy.AgentWorkState`)

## Record Components

### `activeTime` (`java.time.Duration`)

### `agentId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `invocationCount` (`int`)

### `messagesSent` (`int`)

### `signalsDeposited` (`int`)

### `tasksCompleted` (`int`)

### `tokensConsumed` (`long`)

### `totalLatency` (`java.time.Duration`)

### `workState` (`io.casehub.api.model.stigmergy.AgentWorkState`)

## Constructors

### `public SwarmTelemetry(java.util.UUID caseId, java.lang.String agentId, io.casehub.api.model.stigmergy.AgentWorkState workState, int signalsDeposited, int messagesSent, int tasksCompleted, java.time.Duration activeTime, int invocationCount, long tokensConsumed, java.time.Duration totalLatency)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `workState` (`io.casehub.api.model.stigmergy.AgentWorkState`)
- `signalsDeposited` (`int`)
- `messagesSent` (`int`)
- `tasksCompleted` (`int`)
- `activeTime` (`java.time.Duration`)
- `invocationCount` (`int`)
- `tokensConsumed` (`long`)
- `totalLatency` (`java.time.Duration`)

## Methods

### `public java.time.Duration activeTime()`

### `public java.lang.String agentId()`

### `public java.util.UUID caseId()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int invocationCount()`

### `public int messagesSent()`

### `public int signalsDeposited()`

### `public int tasksCompleted()`

### `public final java.lang.String toString()`

### `public long tokensConsumed()`

### `public java.time.Duration totalLatency()`

### `public io.casehub.api.model.stigmergy.AgentWorkState workState()`
