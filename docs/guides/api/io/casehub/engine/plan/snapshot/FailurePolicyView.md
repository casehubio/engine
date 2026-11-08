# io.casehub.engine.plan.snapshot.ExecutionStateView.FailurePolicyView

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `agentRetryPolicy` (`io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRetryPolicyView`)

### `aggregationFailureAction` (`java.lang.String`)

### `routingFailureAction` (`java.lang.String`)

## Record Components

### `agentRetryPolicy` (`io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRetryPolicyView`)

### `aggregationFailureAction` (`java.lang.String`)

### `routingFailureAction` (`java.lang.String`)

## Constructors

### `public FailurePolicyView(java.lang.String routingFailureAction, java.lang.String aggregationFailureAction, io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRetryPolicyView agentRetryPolicy)`

#### Parameters

- `routingFailureAction` (`java.lang.String`)
- `aggregationFailureAction` (`java.lang.String`)
- `agentRetryPolicy` (`io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRetryPolicyView`)

## Methods

### `public io.casehub.engine.plan.snapshot.ExecutionStateView.AgentRetryPolicyView agentRetryPolicy()`

### `public java.lang.String aggregationFailureAction()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String routingFailureAction()`

### `public final java.lang.String toString()`
