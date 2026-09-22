# io.casehub.api.model.ai.ChatModelProviderResolver

**Package:** `io.casehub.api.model.ai`

**Kind:** `interface`

SPI for constructing `ChatModelProvider` instances from a provider type and YAML
configuration. The default `InlineChatModelProviderResolver` builds LangChain4j providers
inline. Alternative implementations (e.g. in blocks engine-adapter) can route through the
platform's `RoutingAgentProvider` for pool management and rate limiting.

## Methods

### `public abstract io.casehub.api.model.ai.ChatModelProvider resolve(java.lang.String providerType, JsonNode config)`

#### Parameters

- `providerType` (`java.lang.String`)
- `config` (`JsonNode`)
