# io.casehub.api.model.ai.WorkerAgentInvoker

**Package:** `io.casehub.api.model.ai`

**Kind:** `class`

## Fields

### `DEFAULT_TIMEOUT` (`java.time.Duration`)

### `MAPPER` (`ObjectMapper`)

### `MAP_TYPE` (`TypeReference<java.util.Map<java.lang.String,java.lang.Object>>`)

### `TEMPLATE_PATTERN` (`java.util.regex.Pattern`)

### `agentProvider` (`AgentProvider`)

### `inputTransformer` (`java.util.function.UnaryOperator<JsonNode>`)

### `model` (`java.lang.String`)

### `outputTransformer` (`java.util.function.UnaryOperator<JsonNode>`)

### `plannedActionExtractor` (`java.util.function.Function<java.util.Map<java.lang.String,java.lang.Object>,PlannedAction>`)

### `responseFormat` (`io.casehub.platform.agent.ResponseFormat`)

### `systemPrompt` (`java.lang.String`)

### `timeout` (`java.time.Duration`)

### `userMessageTemplate` (`java.lang.String`)

## Constructors

### `public WorkerAgentInvoker(AgentProvider agentProvider, java.lang.String systemPrompt, java.lang.String userMessageTemplate, java.util.function.UnaryOperator<JsonNode> inputTransformer, java.util.function.UnaryOperator<JsonNode> outputTransformer, io.casehub.platform.agent.ResponseFormat responseFormat, java.util.function.Function<java.util.Map<java.lang.String,java.lang.Object>,PlannedAction> plannedActionExtractor, java.lang.String model, java.time.Duration timeout)`

#### Parameters

- `agentProvider` (`AgentProvider`)
- `systemPrompt` (`java.lang.String`)
- `userMessageTemplate` (`java.lang.String`)
- `inputTransformer` (`java.util.function.UnaryOperator<JsonNode>`)
- `outputTransformer` (`java.util.function.UnaryOperator<JsonNode>`)
- `responseFormat` (`io.casehub.platform.agent.ResponseFormat`)
- `plannedActionExtractor` (`java.util.function.Function<java.util.Map<java.lang.String,java.lang.Object>,PlannedAction>`)
- `model` (`java.lang.String`)
- `timeout` (`java.time.Duration`)

## Methods

### `static java.lang.String applyTemplate(java.lang.String template, java.util.Map<java.lang.String,java.lang.Object> variables)`

#### Parameters

- `template` (`java.lang.String`)
- `variables` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `public WorkerResult<java.util.Map<java.lang.String,java.lang.Object>> execute(java.util.Map<java.lang.String,java.lang.Object> input)`

#### Parameters

- `input` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `public io.casehub.api.model.ai.WorkerAgentResponse executeDetailed(java.util.Map<java.lang.String,java.lang.Object> input)`

#### Parameters

- `input` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `public java.lang.String model()`
