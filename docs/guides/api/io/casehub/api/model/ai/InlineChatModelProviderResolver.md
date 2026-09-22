# io.casehub.api.model.ai.InlineChatModelProviderResolver

**Package:** `io.casehub.api.model.ai`

**Kind:** `class`

Default `ChatModelProviderResolver` that constructs LangChain4j provider instances inline
from YAML configuration. This is the extraction of the original 5-way switch from `AgentConverter.toChatModelProviderFromNode()`.

## Fields

### `INSTANCE` (`io.casehub.api.model.ai.InlineChatModelProviderResolver`)

## Constructors

### `private InlineChatModelProviderResolver()`

## Methods

### `public io.casehub.api.model.ai.ChatModelProvider resolve(java.lang.String providerType, JsonNode config)`

#### Parameters

- `providerType` (`java.lang.String`)
- `config` (`JsonNode`)
