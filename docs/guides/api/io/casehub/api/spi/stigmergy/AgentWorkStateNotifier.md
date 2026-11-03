# io.casehub.api.spi.stigmergy.AgentWorkStateNotifier

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `interface`

Cross-module bridge for work state transitions. `WorkerExecutionOrchestrator` (common-core)
calls this interface; `StigmergyCoordinator` (runtime-core) implements it.

## Methods

### `public abstract void taskAssigned(java.util.UUID caseId, java.lang.String agentId, java.lang.String taskId, java.util.Map<java.lang.String,java.lang.Object> context)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `taskId` (`java.lang.String`)
- `context` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `public abstract void transitionWorkState(java.util.UUID caseId, java.lang.String agentId, io.casehub.api.model.stigmergy.AgentWorkState targetState)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `targetState` (`io.casehub.api.model.stigmergy.AgentWorkState`)
