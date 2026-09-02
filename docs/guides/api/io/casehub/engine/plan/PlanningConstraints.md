# io.casehub.engine.plan.PlanningConstraints

**Package:** `io.casehub.engine.plan`

**Kind:** `record`

## Fields

### `costBudgets` (`java.util.Map<java.lang.String,java.lang.Integer>`)

### `resourceLimit` (`java.lang.Integer`)

### `timeBudget` (`java.time.Duration`)

### `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Record Components

### `costBudgets` (`java.util.Map<java.lang.String,java.lang.Integer>`)

### `resourceLimit` (`java.lang.Integer`)

### `timeBudget` (`java.time.Duration`)

### `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Constructors

### `public PlanningConstraints(java.time.Duration timeBudget, java.lang.Integer resourceLimit, java.util.Map<java.lang.String,java.lang.Double> weights, java.util.Map<java.lang.String,java.lang.Integer> costBudgets)`

#### Parameters

- `timeBudget` (`java.time.Duration`)
- `resourceLimit` (`java.lang.Integer`)
- `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `costBudgets` (`java.util.Map<java.lang.String,java.lang.Integer>`)

## Methods

### `public java.util.Map<java.lang.String,java.lang.Integer> costBudgets()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public boolean hasHardConstraints()`

### `public final int hashCode()`

### `public static io.casehub.engine.plan.PlanningConstraints of(java.time.Duration timeBudget, java.lang.Integer resourceLimit)`

#### Parameters

- `timeBudget` (`java.time.Duration`)
- `resourceLimit` (`java.lang.Integer`)

### `public java.lang.Integer resourceLimit()`

### `public java.time.Duration timeBudget()`

### `public final java.lang.String toString()`

### `public static io.casehub.engine.plan.PlanningConstraints unconstrained()`

### `public java.util.Map<java.lang.String,java.lang.Double> weights()`
