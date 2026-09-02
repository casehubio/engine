# io.casehub.api.model.converter.yaml.JsonNodeForEachAdapter

**Package:** `io.casehub.api.model.converter.yaml`

**Kind:** `class`

## Fields

### `forEachField` (`java.lang.String`)

### `mapper` (`ObjectMapper`)

### `whenField` (`java.lang.String`)

## Constructors

### `public JsonNodeForEachAdapter(ObjectMapper mapper, java.lang.String forEachField, java.lang.String whenField)`

#### Parameters

- `mapper` (`ObjectMapper`)
- `forEachField` (`java.lang.String`)
- `whenField` (`java.lang.String`)

## Methods

### `public ForEachDirective getForEach(JsonNode element)`

#### Parameters

- `element` (`JsonNode`)

### `public java.lang.String getId(JsonNode element)`

#### Parameters

- `element` (`JsonNode`)

### `public java.lang.String getWhen(JsonNode element)`

#### Parameters

- `element` (`JsonNode`)

### `public JsonNode stamp(JsonNode template, java.lang.String stampedId, VariableResolver scopedResolver)`

#### Parameters

- `template` (`JsonNode`)
- `stampedId` (`java.lang.String`)
- `scopedResolver` (`VariableResolver`)
