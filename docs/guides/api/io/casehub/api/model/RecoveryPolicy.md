# io.casehub.api.model.RecoveryPolicy

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `DEFAULT` (`io.casehub.api.model.RecoveryPolicy`)

### `DISABLED` (`io.casehub.api.model.RecoveryPolicy`)

### `classifierId` (`java.lang.String`)

### `enabled` (`boolean`)

### `maxRerouteAttempts` (`int`)

### `maxRetries` (`int`)

### `replanStrategyId` (`java.lang.String`)

### `revisionStrategyId` (`java.lang.String`)

## Record Components

### `classifierId` (`java.lang.String`)

### `enabled` (`boolean`)

### `maxRerouteAttempts` (`int`)

### `maxRetries` (`int`)

### `replanStrategyId` (`java.lang.String`)

### `revisionStrategyId` (`java.lang.String`)

## Constructors

### `public RecoveryPolicy(int maxRetries, int maxRerouteAttempts, java.lang.String classifierId, java.lang.String revisionStrategyId, java.lang.String replanStrategyId, boolean enabled)`

#### Parameters

- `maxRetries` (`int`)
- `maxRerouteAttempts` (`int`)
- `classifierId` (`java.lang.String`)
- `revisionStrategyId` (`java.lang.String`)
- `replanStrategyId` (`java.lang.String`)
- `enabled` (`boolean`)

## Methods

### `public java.lang.String classifierId()`

### `public boolean enabled()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int maxRerouteAttempts()`

### `public int maxRetries()`

### `public java.lang.String replanStrategyId()`

### `public java.lang.String revisionStrategyId()`

### `public final java.lang.String toString()`
