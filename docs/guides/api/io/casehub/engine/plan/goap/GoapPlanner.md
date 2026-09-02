# io.casehub.engine.plan.goap.GoapPlanner

**Package:** `io.casehub.engine.plan.goap`

**Kind:** `class`

## Constructors

### `public GoapPlanner()`

## Methods

### `private java.util.List<io.casehub.engine.plan.goap.GoapAction> backwardPrune(java.util.List<io.casehub.engine.plan.goap.GoapAction> actions, java.util.Set<java.lang.String> goalConditions)`

#### Parameters

- `actions` (`java.util.List<io.casehub.engine.plan.goap.GoapAction>`)
- `goalConditions` (`java.util.Set<java.lang.String>`)

### `private java.util.List<io.casehub.engine.plan.goap.GoapAction> forwardSimulate(java.util.List<io.casehub.engine.plan.goap.GoapAction> plan, io.casehub.engine.plan.goap.GoapWorldState initial)`

#### Parameters

- `plan` (`java.util.List<io.casehub.engine.plan.goap.GoapAction>`)
- `initial` (`io.casehub.engine.plan.goap.GoapWorldState`)

### `private double heuristic(io.casehub.engine.plan.goap.GoapWorldState state, java.util.Set<java.lang.String> goalConditions, double minCost)`

#### Parameters

- `state` (`io.casehub.engine.plan.goap.GoapWorldState`)
- `goalConditions` (`java.util.Set<java.lang.String>`)
- `minCost` (`double`)

### `public java.util.List<io.casehub.engine.plan.goap.GoapAction> plan(io.casehub.engine.plan.goap.GoapWorldState initial, java.lang.String goalCondition, java.util.List<io.casehub.engine.plan.goap.GoapAction> actions)`

#### Parameters

- `initial` (`io.casehub.engine.plan.goap.GoapWorldState`)
- `goalCondition` (`java.lang.String`)
- `actions` (`java.util.List<io.casehub.engine.plan.goap.GoapAction>`)

### `public java.util.List<io.casehub.engine.plan.goap.GoapAction> plan(io.casehub.engine.plan.goap.GoapWorldState initial, java.util.Set<java.lang.String> goalConditions, java.util.List<io.casehub.engine.plan.goap.GoapAction> actions)`

#### Parameters

- `initial` (`io.casehub.engine.plan.goap.GoapWorldState`)
- `goalConditions` (`java.util.Set<java.lang.String>`)
- `actions` (`java.util.List<io.casehub.engine.plan.goap.GoapAction>`)

### `public java.util.List<io.casehub.engine.plan.goap.GoapAction> plan(io.casehub.engine.plan.goap.GoapWorldState initial, java.util.Set<java.lang.String> goalConditions, java.util.List<io.casehub.engine.plan.goap.GoapAction> actions, io.casehub.engine.plan.goap.PlannerConfig config)`

#### Parameters

- `initial` (`io.casehub.engine.plan.goap.GoapWorldState`)
- `goalConditions` (`java.util.Set<java.lang.String>`)
- `actions` (`java.util.List<io.casehub.engine.plan.goap.GoapAction>`)
- `config` (`io.casehub.engine.plan.goap.PlannerConfig`)

### `private double softPenalty(io.casehub.engine.plan.goap.GoapAction action, io.casehub.engine.plan.goap.GoapWorldState state)`

#### Parameters

- `action` (`io.casehub.engine.plan.goap.GoapAction`)
- `state` (`io.casehub.engine.plan.goap.GoapWorldState`)
