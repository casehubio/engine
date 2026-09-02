# io.casehub.api.model.evaluator.TypedMvelExpressionEvaluator

**Package:** `io.casehub.api.model.evaluator`

**Kind:** `record`

MVEL evaluator that carries the POJO context class for typed evaluation.

<p>When a case definition declares `contextType`, MVEL expressions are created with the
resolved class so the engine can deserialize the working layer to the actual POJO before
evaluation — enabling nested property access (`transaction.amount > 1000`).

## Fields

### `contextClass` (`java.lang.Class<?>`)

### `expression` (`java.lang.String`)

## Record Components

### `contextClass` (`java.lang.Class<?>`)

### `expression` (`java.lang.String`)

## Constructors

### `public TypedMvelExpressionEvaluator(java.lang.String expression, java.lang.Class<?> contextClass)`

#### Parameters

- `expression` (`java.lang.String`)
- `contextClass` (`java.lang.Class<?>`)

## Methods

### `public java.lang.Class<?> contextClass()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String expression()`

### `public final int hashCode()`

### `public final java.lang.String toString()`

### `public java.lang.String type()`
