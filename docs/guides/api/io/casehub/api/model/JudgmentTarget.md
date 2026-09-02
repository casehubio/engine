# io.casehub.api.model.JudgmentTarget

**Package:** `io.casehub.api.model`

**Kind:** `class`

Binding target for caller-agnostic judgment yields.

<p>The engine publishes a judgment request via `JudgmentScheduler`; any caller type (human,
LLM, webhook, A2A agent) can respond. Unlike `HumanTaskTarget`, this target carries no
human-specific fields (candidateGroups, outcomes, templateRef).

<p>Refs engine#996, engine#994.

## Fields

### `escalatorStrategy` (`java.lang.String`)

### `evidenceRequirements` (`java.util.List<java.lang.String>`)

### `expiresAtExpression` (`ExpressionEvaluator`)

### `expiresIn` (`java.time.Duration`)

### `expiresInExpression` (`ExpressionEvaluator`)

### `inputMapping` (`ExpressionEvaluator`)

### `maxEscalationAttempts` (`int`)

### `outcomes` (`java.util.Set<java.lang.String>`)

### `outputMapping` (`ExpressionEvaluator`)

### `priority` (`java.lang.String`)

### `prompt` (`java.lang.String`)

### `promptExpression` (`ExpressionEvaluator`)

### `resolutionType` (`java.lang.Class<?>`)

### `routingConfig` (`io.casehub.api.model.RoutingConfig`)

### `scope` (`java.lang.String`)

### `scopeExpression` (`ExpressionEvaluator`)

### `title` (`java.lang.String`)

### `titleExpression` (`ExpressionEvaluator`)

### `trustThreshold` (`java.lang.String`)

### `verifierStrategy` (`java.lang.String`)

## Constructors

### `private JudgmentTarget(io.casehub.api.model.JudgmentTarget.Builder builder)`

#### Parameters

- `builder` (`io.casehub.api.model.JudgmentTarget.Builder`)

## Methods

### `public static io.casehub.api.model.JudgmentTarget.Builder builder()`

### `public java.lang.String escalatorStrategy()`

### `public java.util.List<java.lang.String> evidenceRequirements()`

### `public ExpressionEvaluator expiresAtExpression()`

### `public java.time.Duration expiresIn()`

### `public ExpressionEvaluator expiresInExpression()`

### `public ExpressionEvaluator inputMapping()`

### `public int maxEscalationAttempts()`

### `public java.util.Set<java.lang.String> outcomes()`

### `public ExpressionEvaluator outputMapping()`

### `public java.lang.String priority()`

### `public java.lang.String prompt()`

### `public ExpressionEvaluator promptExpression()`

### `public java.lang.Class<?> resolutionType()`

### `public io.casehub.api.model.RoutingConfig routingConfig()`

### `public java.lang.String scope()`

### `public ExpressionEvaluator scopeExpression()`

### `public java.lang.String title()`

### `public ExpressionEvaluator titleExpression()`

### `public java.lang.String trustThreshold()`

### `public java.lang.String verifierStrategy()`
