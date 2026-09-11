# io.casehub.api.model.evaluator.LambdaExpressionEvaluator

**Package:** `io.casehub.api.model.evaluator`

**Kind:** `class`

An `ExpressionEvaluator` backed by a Java lambda. Thin subclass of platform's `LambdaExpression` that preserves the `Predicate`-based constructor for Java DSL users.

<p>The lambda itself is not serialisable, but the optional `description` field provides a
human-readable expression string for REST serialization and UI display. Use the two-arg
constructor to attach a description. Without a description, the evaluator serializes with `"expression": null`.

## Fields

### `TYPE` (`java.lang.String`)

### `description` (`java.lang.String`)

## Constructors

### `public LambdaExpressionEvaluator(java.util.function.Predicate<io.casehub.api.context.CaseContext> predicate)`

#### Parameters

- `predicate` (`java.util.function.Predicate<io.casehub.api.context.CaseContext>`)

### `public LambdaExpressionEvaluator(java.util.function.Predicate<io.casehub.api.context.CaseContext> predicate, java.lang.String description)`

#### Parameters

- `predicate` (`java.util.function.Predicate<io.casehub.api.context.CaseContext>`)
- `description` (`java.lang.String`)

## Methods

### `public java.lang.String expression()`

### `public boolean test(io.casehub.api.context.CaseContext context)`

#### Parameters

- `context` (`io.casehub.api.context.CaseContext`)

### `public java.lang.String type()`
