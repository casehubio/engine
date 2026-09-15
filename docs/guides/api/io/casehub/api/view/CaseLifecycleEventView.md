# io.casehub.api.view.CaseLifecycleEventView

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `actorId` (`java.lang.String`)

### `actorRole` (`java.lang.String`)

### `caseDefinitionName` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `caseStatus` (`java.lang.String`)

### `commandType` (`java.lang.String`)

### `eventType` (`java.lang.String`)

### `namespace` (`java.lang.String`)

### `satisfiedGoalKind` (`java.lang.String`)

### `satisfiedGoalName` (`java.lang.String`)

## Record Components

### `actorId` (`java.lang.String`)

### `actorRole` (`java.lang.String`)

### `caseDefinitionName` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `caseStatus` (`java.lang.String`)

### `commandType` (`java.lang.String`)

### `eventType` (`java.lang.String`)

### `namespace` (`java.lang.String`)

### `satisfiedGoalKind` (`java.lang.String`)

### `satisfiedGoalName` (`java.lang.String`)

## Constructors

### `public CaseLifecycleEventView(java.util.UUID caseId, java.lang.String eventType, java.lang.String commandType, java.lang.String caseStatus, java.lang.String actorId, java.lang.String actorRole, java.lang.String caseDefinitionName, java.lang.String namespace, java.lang.String satisfiedGoalName, java.lang.String satisfiedGoalKind)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `eventType` (`java.lang.String`)
- `commandType` (`java.lang.String`)
- `caseStatus` (`java.lang.String`)
- `actorId` (`java.lang.String`)
- `actorRole` (`java.lang.String`)
- `caseDefinitionName` (`java.lang.String`)
- `namespace` (`java.lang.String`)
- `satisfiedGoalName` (`java.lang.String`)
- `satisfiedGoalKind` (`java.lang.String`)

## Methods

### `public java.lang.String actorId()`

### `public java.lang.String actorRole()`

### `public java.lang.String caseDefinitionName()`

### `public java.util.UUID caseId()`

### `public java.lang.String caseStatus()`

### `public java.lang.String commandType()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String eventType()`

### `public final int hashCode()`

### `public java.lang.String namespace()`

### `public java.lang.String satisfiedGoalKind()`

### `public java.lang.String satisfiedGoalName()`

### `public final java.lang.String toString()`
