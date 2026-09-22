# io.casehub.api.model.converter.AgentConverter

**Package:** `io.casehub.api.model.converter`

**Kind:** `class`

## Constructors

### `public AgentConverter()`

## Methods

### `public static io.casehub.api.model.ai.Agent toApiAgent(com.fasterxml.jackson.databind.JsonNode agentNode, io.casehub.api.model.ai.ChatModelProviderResolver resolver)`

Builds an Agent directly from a raw YAML `com.fasterxml.jackson.databind.JsonNode`,
bypassing the generated schema POJOs. Supports the flat YAML format where `model:` is the
provider name string and other fields (`modelName`, `apiKey`, etc.) sit at the same
level.

#### Parameters

- `agentNode` (`com.fasterxml.jackson.databind.JsonNode`)
- `resolver` (`io.casehub.api.model.ai.ChatModelProviderResolver`)
