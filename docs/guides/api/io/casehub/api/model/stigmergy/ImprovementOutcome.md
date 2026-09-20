# io.casehub.api.model.stigmergy.ImprovementOutcome

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `caseId` (`java.util.UUID`)

### `category` (`java.lang.String`)

### `ciDelta` (`java.lang.Integer`)

### `completedAt` (`java.time.Instant`)

### `coverageDelta` (`java.lang.Double`)

### `improvementCaseId` (`java.util.UUID`)

### `lintDelta` (`java.lang.Integer`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.String>`)

### `prUrl` (`java.lang.String`)

### `status` (`io.casehub.api.model.stigmergy.ImprovementOutcome.OutcomeStatus`)

### `target` (`java.lang.String`)

## Record Components

### `caseId` (`java.util.UUID`)

### `category` (`java.lang.String`)

### `ciDelta` (`java.lang.Integer`)

### `completedAt` (`java.time.Instant`)

### `coverageDelta` (`java.lang.Double`)

### `improvementCaseId` (`java.util.UUID`)

### `lintDelta` (`java.lang.Integer`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.String>`)

### `prUrl` (`java.lang.String`)

### `status` (`io.casehub.api.model.stigmergy.ImprovementOutcome.OutcomeStatus`)

### `target` (`java.lang.String`)

## Constructors

### `public ImprovementOutcome(java.util.UUID caseId, java.util.UUID improvementCaseId, java.lang.String category, java.lang.String target, io.casehub.api.model.stigmergy.ImprovementOutcome.OutcomeStatus status, java.lang.String prUrl, java.lang.Integer ciDelta, java.lang.Double coverageDelta, java.lang.Integer lintDelta, java.time.Instant completedAt, java.util.Map<java.lang.String,java.lang.String> metadata)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `improvementCaseId` (`java.util.UUID`)
- `category` (`java.lang.String`)
- `target` (`java.lang.String`)
- `status` (`io.casehub.api.model.stigmergy.ImprovementOutcome.OutcomeStatus`)
- `prUrl` (`java.lang.String`)
- `ciDelta` (`java.lang.Integer`)
- `coverageDelta` (`java.lang.Double`)
- `lintDelta` (`java.lang.Integer`)
- `completedAt` (`java.time.Instant`)
- `metadata` (`java.util.Map<java.lang.String,java.lang.String>`)

## Methods

### `public java.util.UUID caseId()`

### `public java.lang.String category()`

### `public java.lang.Integer ciDelta()`

### `public java.time.Instant completedAt()`

### `public java.lang.Double coverageDelta()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.UUID improvementCaseId()`

### `public java.lang.Integer lintDelta()`

### `public java.util.Map<java.lang.String,java.lang.String> metadata()`

### `public java.lang.String prUrl()`

### `public io.casehub.api.model.stigmergy.ImprovementOutcome.OutcomeStatus status()`

### `public java.lang.String target()`

### `public final java.lang.String toString()`
