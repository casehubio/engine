# io.casehub.api.model.CloudEventTrigger

**Package:** `io.casehub.api.model`

**Kind:** `class`

CloudEvent-based trigger. Fires when a matching CloudEvent is received.

<p>Supports exact match on `type` (required), and optional exact match on `source`
and `subject`. An optional `filter` expression provides predicate-based filtering
over the event and context.

## Fields

### `filter` (`ExpressionEvaluator`)

### `source` (`java.lang.String`)

### `subject` (`java.lang.String`)

### `type` (`java.lang.String`)

## Constructors

### `public CloudEventTrigger(java.lang.String type)`

#### Parameters

- `type` (`java.lang.String`)

### `public CloudEventTrigger(java.lang.String type, java.lang.String source, java.lang.String subject, ExpressionEvaluator filter)`

#### Parameters

- `type` (`java.lang.String`)
- `source` (`java.lang.String`)
- `subject` (`java.lang.String`)
- `filter` (`ExpressionEvaluator`)

## Methods

### `public ExpressionEvaluator getFilter()`

### `public java.lang.String getSource()`

### `public java.lang.String getSubject()`

### `public java.lang.String getType()`

### `public java.lang.String toString()`
