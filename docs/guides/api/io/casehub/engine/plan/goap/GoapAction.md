# io.casehub.engine.plan.goap.GoapAction

**Package:** `io.casehub.engine.plan.goap`

**Kind:** `record`

## Fields

### `benefit` (`double`)

### `cost` (`double`)

### `costFunction` (`io.casehub.engine.plan.goap.CostFunction`)

### `effects` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

### `name` (`java.lang.String`)

### `preconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

### `softPreconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

## Record Components

### `benefit` (`double`)

### `cost` (`double`)

### `costFunction` (`io.casehub.engine.plan.goap.CostFunction`)

### `effects` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

### `name` (`java.lang.String`)

### `preconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

### `softPreconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

## Constructors

### `public GoapAction(java.lang.String name, java.util.Map<java.lang.String,java.lang.Boolean> preconditions, java.util.Map<java.lang.String,java.lang.Boolean> effects, double cost)`

#### Parameters

- `name` (`java.lang.String`)
- `preconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)
- `effects` (`java.util.Map<java.lang.String,java.lang.Boolean>`)
- `cost` (`double`)

### `public GoapAction(java.lang.String name, java.util.Map<java.lang.String,java.lang.Boolean> preconditions, java.util.Map<java.lang.String,java.lang.Boolean> effects, double cost, double benefit, java.util.Map<java.lang.String,java.lang.Boolean> softPreconditions)`

#### Parameters

- `name` (`java.lang.String`)
- `preconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)
- `effects` (`java.util.Map<java.lang.String,java.lang.Boolean>`)
- `cost` (`double`)
- `benefit` (`double`)
- `softPreconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

### `public GoapAction(java.lang.String name, java.util.Map<java.lang.String,java.lang.Boolean> preconditions, java.util.Map<java.lang.String,java.lang.Boolean> effects, double cost, double benefit, java.util.Map<java.lang.String,java.lang.Boolean> softPreconditions, io.casehub.engine.plan.goap.CostFunction costFunction)`

#### Parameters

- `name` (`java.lang.String`)
- `preconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)
- `effects` (`java.util.Map<java.lang.String,java.lang.Boolean>`)
- `cost` (`double`)
- `benefit` (`double`)
- `softPreconditions` (`java.util.Map<java.lang.String,java.lang.Boolean>`)
- `costFunction` (`io.casehub.engine.plan.goap.CostFunction`)

## Methods

### `public io.casehub.engine.plan.goap.GoapWorldState applyTo(io.casehub.engine.plan.goap.GoapWorldState state)`

#### Parameters

- `state` (`io.casehub.engine.plan.goap.GoapWorldState`)

### `public double benefit()`

### `public double cost()`

### `public io.casehub.engine.plan.goap.CostFunction costFunction()`

### `public double effectiveCost()`

### `public double effectiveCost(io.casehub.engine.plan.goap.GoapWorldState state)`

#### Parameters

- `state` (`io.casehub.engine.plan.goap.GoapWorldState`)

### `public java.util.Map<java.lang.String,java.lang.Boolean> effects()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public boolean isApplicable(io.casehub.engine.plan.goap.GoapWorldState state)`

#### Parameters

- `state` (`io.casehub.engine.plan.goap.GoapWorldState`)

### `public java.lang.String name()`

### `public java.util.Map<java.lang.String,java.lang.Boolean> preconditions()`

### `public java.util.Map<java.lang.String,java.lang.Boolean> softPreconditions()`

### `public final java.lang.String toString()`
