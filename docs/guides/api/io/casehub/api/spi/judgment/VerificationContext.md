# io.casehub.api.spi.judgment.VerificationContext

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `record`

Context for post-response verification of judgment yields.

<p>Refs engine#1012, engine#997, engine#994.

## Fields

### `bindingName` (`java.lang.String`)

### `callerIdentity` (`io.casehub.api.spi.judgment.CallerIdentity`)

### `caseId` (`java.util.UUID`)

### `decision` (`java.lang.String`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `evidence` (`java.util.List<io.casehub.api.spi.judgment.Evidence>`)

### `inputData` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `responseTime` (`java.time.Duration`)

### `target` (`io.casehub.api.model.JudgmentTarget`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `bindingName` (`java.lang.String`)

### `callerIdentity` (`io.casehub.api.spi.judgment.CallerIdentity`)

### `caseId` (`java.util.UUID`)

### `decision` (`java.lang.String`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `evidence` (`java.util.List<io.casehub.api.spi.judgment.Evidence>`)

### `inputData` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `responseTime` (`java.time.Duration`)

### `target` (`io.casehub.api.model.JudgmentTarget`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public VerificationContext(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String bindingName, io.casehub.api.model.JudgmentTarget target, java.util.Map<java.lang.String,java.lang.Object> inputData, io.casehub.api.model.CaseDefinition definition, java.lang.String decision, java.util.List<io.casehub.api.spi.judgment.Evidence> evidence, io.casehub.api.spi.judgment.CallerIdentity callerIdentity, java.time.Duration responseTime)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `bindingName` (`java.lang.String`)
- `target` (`io.casehub.api.model.JudgmentTarget`)
- `inputData` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `definition` (`io.casehub.api.model.CaseDefinition`)
- `decision` (`java.lang.String`)
- `evidence` (`java.util.List<io.casehub.api.spi.judgment.Evidence>`)
- `callerIdentity` (`io.casehub.api.spi.judgment.CallerIdentity`)
- `responseTime` (`java.time.Duration`)

## Methods

### `public java.lang.String bindingName()`

### `public io.casehub.api.spi.judgment.CallerIdentity callerIdentity()`

### `public java.util.UUID caseId()`

### `public java.lang.String decision()`

### `public io.casehub.api.model.CaseDefinition definition()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.spi.judgment.Evidence> evidence()`

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Object> inputData()`

### `public java.time.Duration responseTime()`

### `public io.casehub.api.model.JudgmentTarget target()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`
