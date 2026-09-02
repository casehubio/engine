# io.casehub.api.spi.routing.GoalFormationContext

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

## Fields

### `agentId` (`java.lang.String`)

### `existingGoals` (`java.util.List<AgentGoal>`)

### `recentMemories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

### `reflectionInsights` (`java.util.List<java.lang.String>`)

### `remainingCapacity` (`int`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `agentId` (`java.lang.String`)

### `existingGoals` (`java.util.List<AgentGoal>`)

### `recentMemories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

### `reflectionInsights` (`java.util.List<java.lang.String>`)

### `remainingCapacity` (`int`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public GoalFormationContext(java.lang.String agentId, java.lang.String tenancyId, java.util.List<java.lang.String> reflectionInsights, java.util.List<AgentGoal> existingGoals, java.util.List<io.casehub.api.model.RetrievedMemory> recentMemories, int remainingCapacity)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `reflectionInsights` (`java.util.List<java.lang.String>`)
- `existingGoals` (`java.util.List<AgentGoal>`)
- `recentMemories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)
- `remainingCapacity` (`int`)

## Methods

### `public java.lang.String agentId()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<AgentGoal> existingGoals()`

### `public final int hashCode()`

### `public java.util.List<io.casehub.api.model.RetrievedMemory> recentMemories()`

### `public java.util.List<java.lang.String> reflectionInsights()`

### `public int remainingCapacity()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`
