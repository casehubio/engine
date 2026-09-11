# io.casehub.api.model.PathologyAlertEvent

**Package:** `io.casehub.api.model`

**Kind:** `record`

Subscribable event for agent-reported pathology conditions detected during case execution.
Enables the notification pipeline to route pathology alerts to operators and agents.

## Fields

### `EVENT_TYPE` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `conditionType` (`java.lang.String`)

### `detail` (`java.lang.String`)

### `from` (`java.lang.String`)

### `tenancyId` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `caseId` (`java.util.UUID`)

### `conditionType` (`java.lang.String`)

### `detail` (`java.lang.String`)

### `from` (`java.lang.String`)

### `tenancyId` (`java.lang.String`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public PathologyAlertEvent(java.util.UUID caseId, java.lang.String conditionType, java.lang.String detail, java.lang.String from, java.time.Instant timestamp, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `conditionType` (`java.lang.String`)
- `detail` (`java.lang.String`)
- `from` (`java.lang.String`)
- `timestamp` (`java.time.Instant`)
- `tenancyId` (`java.lang.String`)

## Methods

### `public java.util.UUID caseId()`

### `public java.lang.String conditionType()`

### `public java.lang.String detail()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String from()`

### `public final int hashCode()`

### `public java.lang.String tenancyId()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`

### `public java.lang.String type()`
