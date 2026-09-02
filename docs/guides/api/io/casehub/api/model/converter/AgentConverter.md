# io.casehub.api.model.converter.AgentConverter

**Package:** `io.casehub.api.model.converter`

**Kind:** `class`

## Constructors

### `public AgentConverter()`

## Methods

### `public static io.casehub.api.model.ai.Agent toApiAgent(com.fasterxml.jackson.databind.JsonNode agentNode)`

Builds an Agent directly from a raw YAML `com.fasterxml.jackson.databind.JsonNode`,
bypassing the generated schema POJOs. Supports the flat YAML format where `model:` is the
provider name string and other fields (`modelName`, `apiKey`, etc.) sit at the same
level.

#### Parameters

- `agentNode` (`com.fasterxml.jackson.databind.JsonNode`)

### `private static io.casehub.api.model.ai.ChatModelProvider toChatModelProviderFromNode(com.fasterxml.jackson.databind.JsonNode node, java.lang.String providerType)`

#### Parameters

- `node` (`com.fasterxml.jackson.databind.JsonNode`)
- `providerType` (`java.lang.String`)
