# io.casehub.api.model.coordination.DecisionScope

**Package:** `io.casehub.api.model.coordination`

**Kind:** `enum`

## Enum Constants

### `FULL_AUTONOMY` (`io.casehub.api.model.coordination.DecisionScope`)

### `STIMULUS_DRIVEN` (`io.casehub.api.model.coordination.DecisionScope`)

### `TASK_ADAPTATION` (`io.casehub.api.model.coordination.DecisionScope`)

## Constructors

### `private DecisionScope()`

## Methods

### `public abstract boolean isPermitted(io.casehub.api.model.coordination.DecisionType type)`

#### Parameters

- `type` (`io.casehub.api.model.coordination.DecisionType`)

### `public static io.casehub.api.model.coordination.DecisionScope valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.DecisionScope[] values()`
