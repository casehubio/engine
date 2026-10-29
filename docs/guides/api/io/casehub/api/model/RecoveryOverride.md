# io.casehub.api.model.RecoveryOverride

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `maxLevel` (`io.casehub.api.model.RecoveryLevel`)

### `maxRerouteAttempts` (`java.lang.Integer`)

### `maxRetries` (`java.lang.Integer`)

### `maxTriageAttempts` (`java.lang.Integer`)

### `skipRecovery` (`boolean`)

### `skipRecoveryFor` (`java.util.Set<io.casehub.api.model.OutcomeType>`)

### `skipTriage` (`boolean`)

## Record Components

### `maxLevel` (`io.casehub.api.model.RecoveryLevel`)

### `maxRerouteAttempts` (`java.lang.Integer`)

### `maxRetries` (`java.lang.Integer`)

### `maxTriageAttempts` (`java.lang.Integer`)

### `skipRecovery` (`boolean`)

### `skipRecoveryFor` (`java.util.Set<io.casehub.api.model.OutcomeType>`)

### `skipTriage` (`boolean`)

## Constructors

### `public RecoveryOverride(java.lang.Integer maxRetries, java.lang.Integer maxRerouteAttempts, io.casehub.api.model.RecoveryLevel maxLevel, boolean skipRecovery, java.util.Set<io.casehub.api.model.OutcomeType> skipRecoveryFor, boolean skipTriage, java.lang.Integer maxTriageAttempts)`

#### Parameters

- `maxRetries` (`java.lang.Integer`)
- `maxRerouteAttempts` (`java.lang.Integer`)
- `maxLevel` (`io.casehub.api.model.RecoveryLevel`)
- `skipRecovery` (`boolean`)
- `skipRecoveryFor` (`java.util.Set<io.casehub.api.model.OutcomeType>`)
- `skipTriage` (`boolean`)
- `maxTriageAttempts` (`java.lang.Integer`)

## Methods

### `public io.casehub.api.model.RecoveryLevel effectiveMaxLevel()`

### `public int effectiveMaxRerouteAttempts(io.casehub.api.model.RecoveryPolicy policy)`

#### Parameters

- `policy` (`io.casehub.api.model.RecoveryPolicy`)

### `public int effectiveMaxRetries(io.casehub.api.model.RecoveryPolicy policy)`

#### Parameters

- `policy` (`io.casehub.api.model.RecoveryPolicy`)

### `public int effectiveMaxTriageAttempts(io.casehub.api.model.RecoveryPolicy policy)`

#### Parameters

- `policy` (`io.casehub.api.model.RecoveryPolicy`)

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.RecoveryLevel maxLevel()`

### `public java.lang.Integer maxRerouteAttempts()`

### `public java.lang.Integer maxRetries()`

### `public java.lang.Integer maxTriageAttempts()`

### `public static io.casehub.api.model.RecoveryOverride skip()`

### `public boolean skipRecovery()`

### `public java.util.Set<io.casehub.api.model.OutcomeType> skipRecoveryFor()`

### `public boolean skipTriage()`

### `public final java.lang.String toString()`
