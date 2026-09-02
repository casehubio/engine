# io.casehub.api.model.converter.deser.CaseCompletionDeserializer

**Package:** `io.casehub.api.model.converter.deser`

**Kind:** `class`

## Constructors

### `public CaseCompletionDeserializer()`

## Methods

### `public io.casehub.api.model.CaseCompletion deserialize(JsonParser p, DeserializationContext ctxt)`

#### Parameters

- `p` (`JsonParser`)
- `ctxt` (`DeserializationContext`)

### `public io.casehub.api.model.CaseCompletion getNullValue(DeserializationContext ctxt)`

#### Parameters

- `ctxt` (`DeserializationContext`)

### `private ExpressionEvaluator resolveExpression(JsonNode node, DeserializationContext ctxt)`

#### Parameters

- `node` (`JsonNode`)
- `ctxt` (`DeserializationContext`)

### `private io.casehub.api.model.GoalKind resolveGoalKind(java.lang.String kindValue, JsonNode exprNode, DeserializationContext ctxt)`

#### Parameters

- `kindValue` (`java.lang.String`)
- `exprNode` (`JsonNode`)
- `ctxt` (`DeserializationContext`)
