# io.casehub.api.spi.stigmergy.AgentLlmStatsProvider.AgentLlmStats

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `record`

## Fields

### `ZERO` (`io.casehub.api.spi.stigmergy.AgentLlmStatsProvider.AgentLlmStats`)

### `invocationCount` (`int`)

### `tokensConsumed` (`long`)

### `totalLatency` (`java.time.Duration`)

## Record Components

### `invocationCount` (`int`)

### `tokensConsumed` (`long`)

### `totalLatency` (`java.time.Duration`)

## Constructors

### `public AgentLlmStats(int invocationCount, long tokensConsumed, java.time.Duration totalLatency)`

#### Parameters

- `invocationCount` (`int`)
- `tokensConsumed` (`long`)
- `totalLatency` (`java.time.Duration`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int invocationCount()`

### `public final java.lang.String toString()`

### `public long tokensConsumed()`

### `public java.time.Duration totalLatency()`
