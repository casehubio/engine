# io.casehub.api.engine.RuleSpace

**Package:** `io.casehub.api.engine`

**Kind:** `interface`

## Fields

### `NOOP` (`io.casehub.api.engine.RuleSpace`)

## Methods

### `public abstract void deregister(java.lang.String ruleId)`

#### Parameters

- `ruleId` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.api.spi.observation.RuleFiring> lastFired()`

### `public abstract java.util.List<io.casehub.api.spi.observation.RuleRegistration> mine()`

### `public abstract io.casehub.api.spi.observation.RuleRegistration register(io.casehub.api.spi.observation.LocalRule rule)`

#### Parameters

- `rule` (`io.casehub.api.spi.observation.LocalRule`)
