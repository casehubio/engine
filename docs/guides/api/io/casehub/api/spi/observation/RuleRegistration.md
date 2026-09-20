# io.casehub.api.spi.observation.RuleRegistration

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `registeredAt` (`java.time.Instant`)

### `rule` (`io.casehub.api.spi.observation.LocalRule`)

### `ruleId` (`java.lang.String`)

## Record Components

### `registeredAt` (`java.time.Instant`)

### `rule` (`io.casehub.api.spi.observation.LocalRule`)

### `ruleId` (`java.lang.String`)

## Constructors

### `public RuleRegistration(java.lang.String ruleId, io.casehub.api.spi.observation.LocalRule rule, java.time.Instant registeredAt)`

#### Parameters

- `ruleId` (`java.lang.String`)
- `rule` (`io.casehub.api.spi.observation.LocalRule`)
- `registeredAt` (`java.time.Instant`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.time.Instant registeredAt()`

### `public io.casehub.api.spi.observation.LocalRule rule()`

### `public java.lang.String ruleId()`

### `public final java.lang.String toString()`
