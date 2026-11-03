# io.casehub.api.spi.stigmergy.AgentWorkLifecycleListener

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `interface`

Receives per-agent work lifecycle events from the engine runtime.

<p>Implementations are multi-instance: multiple listeners coexist via CDI `Instance` /
Spring `ObjectProvider`. Do not use `@DefaultBean` on implementations.

## Methods

### `public default void onRoleAdopted(java.util.UUID caseId, java.lang.String agentId, java.lang.String groupId, java.lang.String role)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `groupId` (`java.lang.String`)
- `role` (`java.lang.String`)

### `public default void onRoleReleased(java.util.UUID caseId, java.lang.String agentId, java.lang.String groupId, java.lang.String role)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `groupId` (`java.lang.String`)
- `role` (`java.lang.String`)

### `public default void onTaskAdapted(java.util.UUID caseId, java.lang.String agentId, java.lang.String taskId, java.lang.String adaptationReason)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `taskId` (`java.lang.String`)
- `adaptationReason` (`java.lang.String`)

### `public default void onTaskAssigned(java.util.UUID caseId, java.lang.String agentId, java.lang.String taskId, java.util.Map<java.lang.String,java.lang.Object> context)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `taskId` (`java.lang.String`)
- `context` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `public default void onWorkStateChanged(java.util.UUID caseId, java.lang.String agentId, io.casehub.api.model.stigmergy.AgentWorkState from, io.casehub.api.model.stigmergy.AgentWorkState to)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agentId` (`java.lang.String`)
- `from` (`io.casehub.api.model.stigmergy.AgentWorkState`)
- `to` (`io.casehub.api.model.stigmergy.AgentWorkState`)
