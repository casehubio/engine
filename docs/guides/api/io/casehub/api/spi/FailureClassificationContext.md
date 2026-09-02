# io.casehub.api.spi.FailureClassificationContext

**Package:** `io.casehub.api.spi`

**Kind:** `record`

## Fields

### `attemptCount` (`int`)

### `bindingName` (`java.lang.String`)

### `capabilityName` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `maxRerouteAttempts` (`int`)

### `tenancyId` (`java.lang.String`)

### `workerId` (`java.lang.String`)

## Record Components

### `attemptCount` (`int`)

### `bindingName` (`java.lang.String`)

### `capabilityName` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `maxRerouteAttempts` (`int`)

### `tenancyId` (`java.lang.String`)

### `workerId` (`java.lang.String`)

## Constructors

### `public FailureClassificationContext(java.lang.String workerId, java.util.UUID caseId, java.lang.String tenancyId, java.lang.String bindingName, java.lang.String capabilityName, int attemptCount, int maxRerouteAttempts)`

#### Parameters

- `workerId` (`java.lang.String`)
- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `bindingName` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `attemptCount` (`int`)
- `maxRerouteAttempts` (`int`)

## Methods

### `public int attemptCount()`

### `public java.lang.String bindingName()`

### `public java.lang.String capabilityName()`

### `public java.util.UUID caseId()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int maxRerouteAttempts()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`

### `public java.lang.String workerId()`
