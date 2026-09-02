# io.casehub.api.spi.routing.GoalRevisionContext

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

## Fields

### `agentId` (`java.lang.String`)

### `counts` (`java.util.Map<java.lang.String,GoalOutcomeCounts>`)

### `goals` (`java.util.List<AgentGoal>`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `agentId` (`java.lang.String`)

### `counts` (`java.util.Map<java.lang.String,GoalOutcomeCounts>`)

### `goals` (`java.util.List<AgentGoal>`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public GoalRevisionContext(java.lang.String agentId, java.lang.String tenancyId, java.util.List<AgentGoal> goals, java.util.Map<java.lang.String,GoalOutcomeCounts> counts)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `goals` (`java.util.List<AgentGoal>`)
- `counts` (`java.util.Map<java.lang.String,GoalOutcomeCounts>`)

## Methods

### `public java.lang.String agentId()`

### `public java.util.Map<java.lang.String,GoalOutcomeCounts> counts()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<AgentGoal> goals()`

### `public final int hashCode()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`
