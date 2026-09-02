# io.casehub.api.spi.routing.GoalFormationResult

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

## Fields

### `registered` (`java.util.List<AgentGoal>`)

### `rejected` (`java.util.List<io.casehub.api.spi.routing.GoalFormationResult.RejectedGoal>`)

### `totalGoalCount` (`int`)

## Record Components

### `registered` (`java.util.List<AgentGoal>`)

### `rejected` (`java.util.List<io.casehub.api.spi.routing.GoalFormationResult.RejectedGoal>`)

### `totalGoalCount` (`int`)

## Constructors

### `public GoalFormationResult(java.util.List<AgentGoal> registered, java.util.List<io.casehub.api.spi.routing.GoalFormationResult.RejectedGoal> rejected, int totalGoalCount)`

#### Parameters

- `registered` (`java.util.List<AgentGoal>`)
- `rejected` (`java.util.List<io.casehub.api.spi.routing.GoalFormationResult.RejectedGoal>`)
- `totalGoalCount` (`int`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.List<AgentGoal> registered()`

### `public java.util.List<io.casehub.api.spi.routing.GoalFormationResult.RejectedGoal> rejected()`

### `public final java.lang.String toString()`

### `public int totalGoalCount()`
