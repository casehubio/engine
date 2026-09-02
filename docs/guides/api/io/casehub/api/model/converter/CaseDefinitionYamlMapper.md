# io.casehub.api.model.converter.CaseDefinitionYamlMapper

**Package:** `io.casehub.api.model.converter`

**Kind:** `class`

Centralized YAML marshaller for CaseDefinition.

<p>Reads YAML CaseDefinition files and deserializes directly to API models via `CaseDefinitionModule`. Post-processing of worker functions and GOAP shorthands is handled by
`YamlCaseDefinitionConverter`.

<p>Use ObjectMapper, ExpressionEngineRegistry,
WorkerFunctionProviderRegistry) in CDI contexts. Use `.load(InputStream)` for non-CDI
contexts (tests, tooling) — JQ only.

## Fields

### `EMPTY_PROVIDERS` (`io.casehub.api.spi.WorkerFunctionProviderRegistry`)

Empty WorkerFunctionProviderRegistry for non-CDI contexts. Returns null for all worker nodes,
causing mapper to use API-local construction (agent, sync).

### `JQ_ONLY` (`io.casehub.api.engine.ExpressionEngineRegistry`)

JQ-only registry for non-CDI contexts. Does not support custom expression languages.

### `LOG` (`Logger`)

### `MAPPER` (`ObjectMapper`)

## Constructors

### `private CaseDefinitionYamlMapper()`

## Methods

### `private static io.casehub.api.model.converter.yaml.YamlCaseDefinition deserializeYaml(JsonNode node, ObjectMapper moduleMapper)`

#### Parameters

- `node` (`JsonNode`)
- `moduleMapper` (`ObjectMapper`)

### `private static void expandArray(com.fasterxml.jackson.databind.node.ObjectNode parent, java.lang.String fieldName, java.util.Map<java.lang.String,io.casehub.yaml.core.foreach.IterationGroup> groups, io.casehub.yaml.core.resolver.VariableResolver resolver, io.casehub.api.model.converter.yaml.JsonNodeForEachAdapter adapter, ObjectMapper mapper)`

#### Parameters

- `parent` (`com.fasterxml.jackson.databind.node.ObjectNode`)
- `fieldName` (`java.lang.String`)
- `groups` (`java.util.Map<java.lang.String,io.casehub.yaml.core.foreach.IterationGroup>`)
- `resolver` (`io.casehub.yaml.core.resolver.VariableResolver`)
- `adapter` (`io.casehub.api.model.converter.yaml.JsonNodeForEachAdapter`)
- `mapper` (`ObjectMapper`)

### `private static JsonNode expandForEach(JsonNode node, ObjectMapper mapper)`

#### Parameters

- `node` (`JsonNode`)
- `mapper` (`ObjectMapper`)

### `private static JsonNode expandModules(JsonNode node, ObjectMapper mapper)`

#### Parameters

- `node` (`JsonNode`)
- `mapper` (`ObjectMapper`)

### `private static JsonNode flattenExpressionOverrides(JsonNode node, ObjectMapper mapper)`

#### Parameters

- `node` (`JsonNode`)
- `mapper` (`ObjectMapper`)

### `public static io.casehub.api.model.CaseDefinition load(JsonNode mergedNode, ObjectMapper objectMapper, io.casehub.api.engine.ExpressionEngineRegistry registry, io.casehub.api.spi.WorkerFunctionProviderRegistry providerRegistry)`

Loads a CaseDefinition from a pre-merged JsonNode. For use with the YAML overlay/merge pipeline
where base and overlay documents have already been merged via YamlMerger.

#### Parameters

- `mergedNode` (`JsonNode`) — pre-merged JsonNode containing the complete case definition
- `objectMapper` (`ObjectMapper`) — ObjectMapper for type conversion
- `registry` (`io.casehub.api.engine.ExpressionEngineRegistry`) — ExpressionEngineRegistry (nullable — falls back to JQ-only)
- `providerRegistry` (`io.casehub.api.spi.WorkerFunctionProviderRegistry`) — WorkerFunctionProviderRegistry (nullable — falls back to no-op)

#### Returns

API model CaseDefinition

### `public static io.casehub.api.model.CaseDefinition load(java.io.InputStream yamlStream)`

Loads a CaseDefinition from a YAML InputStream using a plain ObjectMapper and JQ-only
expression support.

<p>For non-CDI contexts (tests, tooling). Does not support custom expression languages — use
ObjectMapper, ExpressionEngineRegistry,
WorkerFunctionProviderRegistry) in CDI deployments.

#### Parameters

- `yamlStream` (`java.io.InputStream`) — InputStream containing YAML CaseDefinition

#### Returns

API model CaseDefinition

#### Throws

- `IOException` — if reading or parsing fails

### `public static io.casehub.api.model.CaseDefinition load(java.io.InputStream yamlStream, ObjectMapper objectMapper, io.casehub.api.engine.ExpressionEngineRegistry registry, io.casehub.api.spi.WorkerFunctionProviderRegistry providerRegistry)`

Loads a CaseDefinition from a YAML InputStream using the CDI-managed ObjectMapper and
ExpressionEngineRegistry. Supports all registered expression languages.

#### Parameters

- `yamlStream` (`java.io.InputStream`) — InputStream containing YAML CaseDefinition
- `objectMapper` (`ObjectMapper`) — ObjectMapper configured for YAML (with config/secret placeholder support)
- `registry` (`io.casehub.api.engine.ExpressionEngineRegistry`) — ExpressionEngineRegistry for creating evaluators from YAML expression strings
- `providerRegistry` (`io.casehub.api.spi.WorkerFunctionProviderRegistry`) — WorkerFunctionProviderRegistry for SDK-dependent worker construction

#### Returns

API model CaseDefinition

#### Throws

- `IOException` — if reading or parsing fails

### `public static io.casehub.api.model.CaseDefinition load(java.io.InputStream yamlStream, ObjectMapper objectMapper, io.casehub.api.engine.ExpressionEngineRegistry registry, io.casehub.api.spi.WorkerFunctionProviderRegistry providerRegistry, java.util.Map<java.lang.String,io.casehub.yaml.core.resolver.VariableSource> variableSources)`

Loads a CaseDefinition with variable resolution. Variables like `${env.X`} and `${config.X`} are resolved before Jackson deserialization. The `each` prefix is deferred —
it is resolved during forEach expansion.

#### Parameters

- `yamlStream` (`java.io.InputStream`) — InputStream containing YAML CaseDefinition
- `objectMapper` (`ObjectMapper`) — ObjectMapper configured for YAML
- `registry` (`io.casehub.api.engine.ExpressionEngineRegistry`) — ExpressionEngineRegistry for creating evaluators
- `providerRegistry` (`io.casehub.api.spi.WorkerFunctionProviderRegistry`) — WorkerFunctionProviderRegistry for SDK-dependent worker construction
- `variableSources` (`java.util.Map<java.lang.String,io.casehub.yaml.core.resolver.VariableSource>`) — prefix-keyed variable sources (e.g., "env" → System::getenv)

#### Returns

API model CaseDefinition

#### Throws

- `IOException` — if reading or parsing fails

### `public static ExpressionEvaluator resolveExpression(JsonNode node, io.casehub.api.engine.ExpressionEngineRegistry registry, java.lang.String defaultLang)`

Resolves a YAML expression node to an `ExpressionEvaluator`.

<p>Accepts two forms:

<ul>
  <li>String: `".amount > 1000"` — uses `defaultLang`
  <li>Single-key map: `{mvel: "transaction.amount > 1000"`} — language is the map key
</ul>

#### Parameters

- `node` (`JsonNode`) — raw YAML node (null or NullNode returns null)
- `registry` (`io.casehub.api.engine.ExpressionEngineRegistry`) — registry for creating evaluators
- `defaultLang` (`java.lang.String`) — language to use when `node` is a plain string

#### Returns

ExpressionEvaluator, or null if node is absent/null

### `private static java.lang.String resolveExpressionLang(JsonNode node)`

#### Parameters

- `node` (`JsonNode`)

### `private static JsonNode resolveVariables(JsonNode node, ObjectMapper mapper, java.util.Map<java.lang.String,io.casehub.yaml.core.resolver.VariableSource> sources)`

#### Parameters

- `node` (`JsonNode`)
- `mapper` (`ObjectMapper`)
- `sources` (`java.util.Map<java.lang.String,io.casehub.yaml.core.resolver.VariableSource>`)
