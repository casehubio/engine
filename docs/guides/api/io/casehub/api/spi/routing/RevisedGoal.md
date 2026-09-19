# io.casehub.api.spi.routing.GoalRevisionProposal.RevisedGoal

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

## Fields

### `action` (`io.casehub.api.spi.routing.GoalRevisionAction`)

### `goalName` (`java.lang.String`)

### `newPriority` (`java.lang.Double`)

### `revisedDescription` (`java.lang.String`)

### `revisionReason` (`java.lang.String`)

## Record Components

### `action` (`io.casehub.api.spi.routing.GoalRevisionAction`)

### `goalName` (`java.lang.String`)

### `newPriority` (`java.lang.Double`)

### `revisedDescription` (`java.lang.String`)

### `revisionReason` (`java.lang.String`)

## Constructors

### `public RevisedGoal(java.lang.String goalName, io.casehub.api.spi.routing.GoalRevisionAction action, java.lang.String revisedDescription, java.lang.String revisionReason)`

#### Parameters

- `goalName` (`java.lang.String`)
- `action` (`io.casehub.api.spi.routing.GoalRevisionAction`)
- `revisedDescription` (`java.lang.String`)
- `revisionReason` (`java.lang.String`)

### `public RevisedGoal(java.lang.String goalName, io.casehub.api.spi.routing.GoalRevisionAction action, java.lang.String revisedDescription, java.lang.String revisionReason, java.lang.Double newPriority)`

#### Parameters

- `goalName` (`java.lang.String`)
- `action` (`io.casehub.api.spi.routing.GoalRevisionAction`)
- `revisedDescription` (`java.lang.String`)
- `revisionReason` (`java.lang.String`)
- `newPriority` (`java.lang.Double`)

## Methods

### `public io.casehub.api.spi.routing.GoalRevisionAction action()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String goalName()`

### `public final int hashCode()`

### `public java.lang.Double newPriority()`

### `public java.lang.String revisedDescription()`

### `public java.lang.String revisionReason()`

### `public final java.lang.String toString()`
