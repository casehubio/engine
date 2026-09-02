# io.casehub.api.model.CapabilityTarget

**Package:** `io.casehub.api.model`

**Kind:** `record`

Binding target that routes to an available worker via capability matching.

## Fields

### `capability` (`Capability`)

### `inputProjection` (`ExpressionEvaluator`)

### `outputProjection` (`ExpressionEvaluator`)

## Record Components

### `capability` (`Capability`)

### `inputProjection` (`ExpressionEvaluator`)

### `outputProjection` (`ExpressionEvaluator`)

## Constructors

### `public CapabilityTarget(Capability capability)`

#### Parameters

- `capability` (`Capability`)

### `public CapabilityTarget(Capability capability, ExpressionEvaluator inputProjection, ExpressionEvaluator outputProjection)`

#### Parameters

- `capability` (`Capability`)
- `inputProjection` (`ExpressionEvaluator`)
- `outputProjection` (`ExpressionEvaluator`)

## Methods

### `public Capability capability()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public ExpressionEvaluator inputProjection()`

### `public ExpressionEvaluator outputProjection()`

### `public final java.lang.String toString()`
