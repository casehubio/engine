# io.casehub.api.spi.observation.RuleConfig

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `DEFAULT_MAX_ACTIONS` (`int`)

### `DEFAULT_MAX_RULES` (`int`)

### `DEFAULT_TIMEOUT_MS` (`int`)

### `maxActionsPerCycle` (`int`)

### `maxRulesPerCase` (`int`)

### `ruleEvaluationTimeoutMs` (`int`)

## Record Components

### `maxActionsPerCycle` (`int`)

### `maxRulesPerCase` (`int`)

### `ruleEvaluationTimeoutMs` (`int`)

## Constructors

### `public RuleConfig(int maxRulesPerCase, int maxActionsPerCycle, int ruleEvaluationTimeoutMs)`

#### Parameters

- `maxRulesPerCase` (`int`)
- `maxActionsPerCycle` (`int`)
- `ruleEvaluationTimeoutMs` (`int`)

## Methods

### `public static io.casehub.api.spi.observation.RuleConfig defaults()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int maxActionsPerCycle()`

### `public int maxRulesPerCase()`

### `public int ruleEvaluationTimeoutMs()`

### `public final java.lang.String toString()`
