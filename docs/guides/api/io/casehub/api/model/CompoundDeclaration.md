# io.casehub.api.model.CompoundDeclaration

**Package:** `io.casehub.api.model`

**Kind:** `record`

YAML-declared compound task definition. Captures the structural declaration from the YAML DSL;
the engine converts these to runtime PlanItemDefinition.Compound instances.

## Fields

### `completionSemantics` (`java.lang.String`)

### `dispatchMode` (`java.lang.String`)

### `entryCondition` (`ExpressionEvaluator`)

### `exitCondition` (`ExpressionEvaluator`)

### `name` (`java.lang.String`)

### `planningStrategy` (`java.lang.String`)

### `repeatable` (`boolean`)

### `scopedBindings` (`java.util.Map<java.lang.String,io.casehub.api.model.Participation>`)

## Record Components

### `completionSemantics` (`java.lang.String`)

how children complete: "all" (default), "firstWins", or integer for
    M-of-N

### `dispatchMode` (`java.lang.String`)

"ORCHESTRATED" (sequential) or "CHOREOGRAPHED" (parallel, default)

### `entryCondition` (`ExpressionEvaluator`)

optional expression that must be true before this compound activates

### `exitCondition` (`ExpressionEvaluator`)

optional expression that forces completion when true

### `name` (`java.lang.String`)

compound name — unique within the definition

### `planningStrategy` (`java.lang.String`)

optional planning strategy override for this compound's children

### `repeatable` (`boolean`)

whether this compound can be re-entered after completion

### `scopedBindings` (`java.util.Map<java.lang.String,io.casehub.api.model.Participation>`)

bindings scoped to this compound, with participation level

## Constructors

### `public CompoundDeclaration(java.lang.String name, java.lang.String completionSemantics, java.lang.String dispatchMode, java.util.Map<java.lang.String,io.casehub.api.model.Participation> scopedBindings, ExpressionEvaluator entryCondition, ExpressionEvaluator exitCondition, boolean repeatable, java.lang.String planningStrategy)`

#### Parameters

- `name` (`java.lang.String`)
- `completionSemantics` (`java.lang.String`)
- `dispatchMode` (`java.lang.String`)
- `scopedBindings` (`java.util.Map<java.lang.String,io.casehub.api.model.Participation>`)
- `entryCondition` (`ExpressionEvaluator`)
- `exitCondition` (`ExpressionEvaluator`)
- `repeatable` (`boolean`)
- `planningStrategy` (`java.lang.String`)

## Methods

### `public static io.casehub.api.model.CompoundDeclaration.Builder builder(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public java.lang.String completionSemantics()`

### `public java.lang.String dispatchMode()`

### `public ExpressionEvaluator entryCondition()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public ExpressionEvaluator exitCondition()`

### `public final int hashCode()`

### `public java.lang.String name()`

### `public java.lang.String planningStrategy()`

### `public boolean repeatable()`

### `public java.util.Map<java.lang.String,io.casehub.api.model.Participation> scopedBindings()`

### `public final java.lang.String toString()`
