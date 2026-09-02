# io.casehub.api.spi.recovery.ErrorClassificationContext

**Package:** `io.casehub.api.spi.recovery`

**Kind:** `record`

## Fields

### `attemptCount` (`int`)

### `bindingName` (`java.lang.String`)

### `capabilityName` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `outcome` (`WorkerOutcome<?>`)

### `tenancyId` (`java.lang.String`)

### `workerName` (`java.lang.String`)

## Record Components

### `attemptCount` (`int`)

### `bindingName` (`java.lang.String`)

### `capabilityName` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `outcome` (`WorkerOutcome<?>`)

### `tenancyId` (`java.lang.String`)

### `workerName` (`java.lang.String`)

## Constructors

### `public ErrorClassificationContext(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String bindingName, java.lang.String workerName, java.lang.String capabilityName, WorkerOutcome<?> outcome, int attemptCount, io.casehub.api.model.CaseDefinition definition)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `bindingName` (`java.lang.String`)
- `workerName` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `outcome` (`WorkerOutcome<?>`)
- `attemptCount` (`int`)
- `definition` (`io.casehub.api.model.CaseDefinition`)

## Methods

### `public int attemptCount()`

### `public java.lang.String bindingName()`

### `public java.lang.String capabilityName()`

### `public java.util.UUID caseId()`

### `public io.casehub.api.model.CaseDefinition definition()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public WorkerOutcome<?> outcome()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`

### `public java.lang.String workerName()`
