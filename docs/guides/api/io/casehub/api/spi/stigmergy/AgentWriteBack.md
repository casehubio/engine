# io.casehub.api.spi.stigmergy.AgentWriteBack

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `interface`

Accepts agent decision write-backs into the engine's stigmergy environment.

<p>Processes the three write types from `DecisionOutcome.WriteBack`: signal deposits,
observations, and context mutations. Named `AgentWriteBack` (not `StigmergyWriter`)
because context writes target the WORKING context layer, not the stigmergy subsystem.

<p>Implementations must be thread-safe. `.writeObservation` uses append semantics —
concurrent calls for the same agent must not overwrite each other.

## Methods

### `public abstract boolean depositSignal(java.util.UUID caseId, java.lang.String agentId, java.lang.String name, double strength, java.time.Duration halfLife)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `name` (`java.lang.String`)
- `strength` (`double`)
- `halfLife` (`java.time.Duration`)

### `public abstract void writeContext(java.util.UUID caseId, java.lang.String agentId, java.lang.String key, JsonNode value)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `key` (`java.lang.String`)
- `value` (`JsonNode`)

### `public abstract void writeObservation(java.util.UUID caseId, java.lang.String agentId, io.casehub.api.spi.observation.Observation observation)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `observation` (`io.casehub.api.spi.observation.Observation`)
