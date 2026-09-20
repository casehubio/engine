# io.casehub.api.spi.observation.RuleFiring

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `executedActions` (`java.util.List<io.casehub.api.spi.observation.RuleAction>`)

### `firedAt` (`java.time.Instant`)

### `ruleId` (`java.lang.String`)

## Record Components

### `executedActions` (`java.util.List<io.casehub.api.spi.observation.RuleAction>`)

### `firedAt` (`java.time.Instant`)

### `ruleId` (`java.lang.String`)

## Constructors

### `public RuleFiring(java.lang.String ruleId, java.util.List<io.casehub.api.spi.observation.RuleAction> executedActions, java.time.Instant firedAt)`

#### Parameters

- `ruleId` (`java.lang.String`)
- `executedActions` (`java.util.List<io.casehub.api.spi.observation.RuleAction>`)
- `firedAt` (`java.time.Instant`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.spi.observation.RuleAction> executedActions()`

### `public java.time.Instant firedAt()`

### `public final int hashCode()`

### `public java.lang.String ruleId()`

### `public final java.lang.String toString()`
