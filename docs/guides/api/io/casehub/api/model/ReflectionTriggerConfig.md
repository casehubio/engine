# io.casehub.api.model.ReflectionTriggerConfig

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `DEFAULT_IMPORTANCE_WEIGHTS` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `enabled` (`boolean`)

### `importanceThreshold` (`double`)

### `importanceWeights` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `maxSourceMemories` (`int`)

### `maxUnreflectedOutcomes` (`int`)

## Record Components

### `enabled` (`boolean`)

### `importanceThreshold` (`double`)

### `importanceWeights` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `maxSourceMemories` (`int`)

### `maxUnreflectedOutcomes` (`int`)

## Constructors

### `public ReflectionTriggerConfig(boolean enabled, double importanceThreshold, int maxUnreflectedOutcomes, int maxSourceMemories, java.util.Map<java.lang.String,java.lang.Double> importanceWeights)`

#### Parameters

- `enabled` (`boolean`)
- `importanceThreshold` (`double`)
- `maxUnreflectedOutcomes` (`int`)
- `maxSourceMemories` (`int`)
- `importanceWeights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Methods

### `public static io.casehub.api.model.ReflectionTriggerConfig defaults()`

### `public boolean enabled()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public double importanceThreshold()`

### `public java.util.Map<java.lang.String,java.lang.Double> importanceWeights()`

### `public int maxSourceMemories()`

### `public int maxUnreflectedOutcomes()`

### `public final java.lang.String toString()`
