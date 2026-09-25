# io.casehub.api.model.stigmergy.ConductorInboxEntry

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `areaId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `category` (`java.lang.String`)

### `confidence` (`double`)

### `decision` (`io.casehub.api.model.stigmergy.ConductorDecision`)

### `escalationTriggers` (`java.util.List<io.casehub.api.model.stigmergy.EscalationTrigger>`)

### `id` (`java.lang.String`)

### `improvementCaseId` (`java.util.UUID`)

### `queuedAt` (`java.time.Instant`)

### `resolvedAt` (`java.time.Instant`)

### `stage` (`java.lang.String`)

### `status` (`io.casehub.api.model.stigmergy.ConductorInboxEntry.Status`)

### `summary` (`java.lang.String`)

### `timeoutMinutes` (`java.lang.Integer`)

## Record Components

### `areaId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `category` (`java.lang.String`)

### `confidence` (`double`)

### `decision` (`io.casehub.api.model.stigmergy.ConductorDecision`)

### `escalationTriggers` (`java.util.List<io.casehub.api.model.stigmergy.EscalationTrigger>`)

### `id` (`java.lang.String`)

### `improvementCaseId` (`java.util.UUID`)

### `queuedAt` (`java.time.Instant`)

### `resolvedAt` (`java.time.Instant`)

### `stage` (`java.lang.String`)

### `status` (`io.casehub.api.model.stigmergy.ConductorInboxEntry.Status`)

### `summary` (`java.lang.String`)

### `timeoutMinutes` (`java.lang.Integer`)

## Constructors

### `public ConductorInboxEntry(java.util.UUID caseId, java.lang.String id, java.lang.String stage, io.casehub.api.model.stigmergy.ConductorInboxEntry.Status status, java.lang.String category, java.lang.String areaId, java.util.UUID improvementCaseId, java.lang.String summary, java.util.List<io.casehub.api.model.stigmergy.EscalationTrigger> escalationTriggers, double confidence, java.time.Instant queuedAt, java.time.Instant resolvedAt, java.lang.Integer timeoutMinutes, io.casehub.api.model.stigmergy.ConductorDecision decision)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `id` (`java.lang.String`)
- `stage` (`java.lang.String`)
- `status` (`io.casehub.api.model.stigmergy.ConductorInboxEntry.Status`)
- `category` (`java.lang.String`)
- `areaId` (`java.lang.String`)
- `improvementCaseId` (`java.util.UUID`)
- `summary` (`java.lang.String`)
- `escalationTriggers` (`java.util.List<io.casehub.api.model.stigmergy.EscalationTrigger>`)
- `confidence` (`double`)
- `queuedAt` (`java.time.Instant`)
- `resolvedAt` (`java.time.Instant`)
- `timeoutMinutes` (`java.lang.Integer`)
- `decision` (`io.casehub.api.model.stigmergy.ConductorDecision`)

## Methods

### `public java.lang.String areaId()`

### `public java.util.UUID caseId()`

### `public java.lang.String category()`

### `public double confidence()`

### `public io.casehub.api.model.stigmergy.ConductorDecision decision()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.model.stigmergy.EscalationTrigger> escalationTriggers()`

### `public final int hashCode()`

### `public java.lang.String id()`

### `public java.util.UUID improvementCaseId()`

### `public java.time.Instant queuedAt()`

### `public java.time.Instant resolvedAt()`

### `public java.lang.String stage()`

### `public io.casehub.api.model.stigmergy.ConductorInboxEntry.Status status()`

### `public java.lang.String summary()`

### `public java.lang.Integer timeoutMinutes()`

### `public final java.lang.String toString()`
