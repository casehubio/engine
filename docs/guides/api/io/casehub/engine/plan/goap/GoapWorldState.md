# io.casehub.engine.plan.goap.GoapWorldState

**Package:** `io.casehub.engine.plan.goap`

**Kind:** `record`

## Fields

### `conditions` (`java.util.Map<java.lang.String,io.casehub.engine.plan.goap.Condition>`)

## Record Components

### `conditions` (`java.util.Map<java.lang.String,io.casehub.engine.plan.goap.Condition>`)

## Constructors

### `public GoapWorldState(java.util.Map<java.lang.String,io.casehub.engine.plan.goap.Condition> conditions)`

#### Parameters

- `conditions` (`java.util.Map<java.lang.String,io.casehub.engine.plan.goap.Condition>`)

## Methods

### `public static io.casehub.engine.plan.goap.GoapWorldState closedWorld(java.util.Map<java.lang.String,java.lang.Boolean> known)`

#### Parameters

- `known` (`java.util.Map<java.lang.String,java.lang.Boolean>`)

### `public java.util.Map<java.lang.String,io.casehub.engine.plan.goap.Condition> conditions()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public io.casehub.engine.plan.goap.Condition get(java.lang.String key)`

#### Parameters

- `key` (`java.lang.String`)

### `public final int hashCode()`

### `public static io.casehub.engine.plan.goap.GoapWorldState openWorld(com.fasterxml.jackson.databind.JsonNode workingLayer)`

#### Parameters

- `workingLayer` (`com.fasterxml.jackson.databind.JsonNode`)

### `public boolean satisfies(java.lang.String goalCondition)`

#### Parameters

- `goalCondition` (`java.lang.String`)

### `public boolean satisfiesAll(java.util.Set<java.lang.String> goalConditions)`

#### Parameters

- `goalConditions` (`java.util.Set<java.lang.String>`)

### `public final java.lang.String toString()`

### `public io.casehub.engine.plan.goap.GoapWorldState with(java.lang.String key, boolean value)`

#### Parameters

- `key` (`java.lang.String`)
- `value` (`boolean`)

### `public io.casehub.engine.plan.goap.GoapWorldState with(java.lang.String key, io.casehub.engine.plan.goap.Condition value)`

#### Parameters

- `key` (`java.lang.String`)
- `value` (`io.casehub.engine.plan.goap.Condition`)
