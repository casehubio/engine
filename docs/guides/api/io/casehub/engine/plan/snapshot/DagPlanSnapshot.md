# io.casehub.engine.plan.snapshot.DagPlanSnapshot

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `nodes` (`java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagNodeSnapshot>`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `nodes` (`java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagNodeSnapshot>`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public DagPlanSnapshot(java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagNodeSnapshot> nodes, java.time.Instant timestamp)`

#### Parameters

- `nodes` (`java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagNodeSnapshot>`)
- `timestamp` (`java.time.Instant`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public static io.casehub.engine.plan.snapshot.DagPlanSnapshot from(io.casehub.engine.plan.DagPlan<?> plan, java.time.Instant timestamp)`

#### Parameters

- `plan` (`io.casehub.engine.plan.DagPlan<?>`)
- `timestamp` (`java.time.Instant`)

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,io.casehub.engine.plan.snapshot.DagNodeSnapshot> nodes()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`
