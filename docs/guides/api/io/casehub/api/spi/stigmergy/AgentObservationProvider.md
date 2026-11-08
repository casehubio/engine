# io.casehub.api.spi.stigmergy.AgentObservationProvider

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `interface`

Pull-based query for stigmergy state, scoped for per-agent LLM reasoning.

<p>Complements `io.casehub.api.spi.observation.EnvironmentObserver` (push-based,
rule-driven). Both coexist and read from the same underlying registries.

<p>Implementations must be thread-safe for concurrent access from multiple agent threads.

## Methods

### `public abstract java.util.List<io.casehub.api.spi.observation.Observation> observations(java.util.UUID caseId, java.lang.String agentId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)

### `public abstract java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal> perceiveSignals(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)
