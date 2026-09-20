# io.casehub.api.spi.observation.LocalRule

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `actions` (`java.util.List<io.casehub.api.spi.observation.RuleAction>`)

### `condition` (`io.casehub.api.spi.observation.RuleCondition`)

### `id` (`java.lang.String`)

### `priority` (`int`)

## Record Components

### `actions` (`java.util.List<io.casehub.api.spi.observation.RuleAction>`)

### `condition` (`io.casehub.api.spi.observation.RuleCondition`)

### `id` (`java.lang.String`)

### `priority` (`int`)

## Constructors

### `public LocalRule(java.lang.String id, io.casehub.api.spi.observation.RuleCondition condition, java.util.List<io.casehub.api.spi.observation.RuleAction> actions, int priority)`

#### Parameters

- `id` (`java.lang.String`)
- `condition` (`io.casehub.api.spi.observation.RuleCondition`)
- `actions` (`java.util.List<io.casehub.api.spi.observation.RuleAction>`)
- `priority` (`int`)

## Methods

### `public java.util.List<io.casehub.api.spi.observation.RuleAction> actions()`

### `public io.casehub.api.spi.observation.RuleCondition condition()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String id()`

### `public int priority()`

### `public final java.lang.String toString()`
