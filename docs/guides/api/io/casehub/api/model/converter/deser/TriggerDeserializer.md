# io.casehub.api.model.converter.deser.TriggerDeserializer

**Package:** `io.casehub.api.model.converter.deser`

**Kind:** `class`

## Fields

### `VALID_KEYS` (`java.lang.String[]`)

## Constructors

### `public TriggerDeserializer()`

## Methods

### `public io.casehub.api.model.Trigger deserialize(JsonParser p, DeserializationContext ctxt)`

#### Parameters

- `p` (`JsonParser`)
- `ctxt` (`DeserializationContext`)

### `private io.casehub.api.model.Trigger deserializeCloudEvent(JsonNode value, DeserializationContext ctxt)`

#### Parameters

- `value` (`JsonNode`)
- `ctxt` (`DeserializationContext`)

### `private io.casehub.api.model.Trigger deserializeContextChange(JsonNode value, DeserializationContext ctxt)`

#### Parameters

- `value` (`JsonNode`)
- `ctxt` (`DeserializationContext`)

### `private io.casehub.api.model.Trigger deserializeSchedule(JsonNode value, DeserializationContext ctxt)`

#### Parameters

- `value` (`JsonNode`)
- `ctxt` (`DeserializationContext`)

### `public io.casehub.api.model.Trigger getNullValue(DeserializationContext ctxt)`

#### Parameters

- `ctxt` (`DeserializationContext`)
