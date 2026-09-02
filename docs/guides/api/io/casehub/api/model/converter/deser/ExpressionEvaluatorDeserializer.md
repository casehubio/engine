# io.casehub.api.model.converter.deser.ExpressionEvaluatorDeserializer

**Package:** `io.casehub.api.model.converter.deser`

**Kind:** `class`

## Fields

### `EXPRESSION_LANG_KEY` (`java.lang.String`)

### `registry` (`io.casehub.api.engine.ExpressionEngineRegistry`)

## Constructors

### `public ExpressionEvaluatorDeserializer(io.casehub.api.engine.ExpressionEngineRegistry registry)`

#### Parameters

- `registry` (`io.casehub.api.engine.ExpressionEngineRegistry`)

## Methods

### `private ExpressionEvaluator createExpression(java.lang.String expression, java.lang.String lang)`

#### Parameters

- `expression` (`java.lang.String`)
- `lang` (`java.lang.String`)

### `public ExpressionEvaluator deserialize(JsonParser p, DeserializationContext ctxt)`

#### Parameters

- `p` (`JsonParser`)
- `ctxt` (`DeserializationContext`)

### `public ExpressionEvaluator getNullValue(DeserializationContext ctxt)`

#### Parameters

- `ctxt` (`DeserializationContext`)
