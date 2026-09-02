# io.casehub.api.spi.judgment.EscalationContext

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `record`

Context for post-verification escalation of judgment yields.

<p>Refs engine#1012, engine#999, engine#994.

## Fields

### `bindingName` (`java.lang.String`)

### `callerIdentity` (`io.casehub.api.spi.judgment.CallerIdentity`)

### `caseId` (`java.util.UUID`)

### `decision` (`java.lang.String`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `escalationCount` (`int`)

### `evidence` (`java.util.List<io.casehub.api.spi.judgment.Evidence>`)

### `maxEscalations` (`int`)

### `responseTime` (`java.time.Duration`)

### `target` (`io.casehub.api.model.JudgmentTarget`)

### `tenancyId` (`java.lang.String`)

### `verificationResult` (`io.casehub.api.spi.judgment.VerificationResult`)

## Record Components

### `bindingName` (`java.lang.String`)

### `callerIdentity` (`io.casehub.api.spi.judgment.CallerIdentity`)

### `caseId` (`java.util.UUID`)

### `decision` (`java.lang.String`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `escalationCount` (`int`)

### `evidence` (`java.util.List<io.casehub.api.spi.judgment.Evidence>`)

### `maxEscalations` (`int`)

### `responseTime` (`java.time.Duration`)

### `target` (`io.casehub.api.model.JudgmentTarget`)

### `tenancyId` (`java.lang.String`)

### `verificationResult` (`io.casehub.api.spi.judgment.VerificationResult`)

## Constructors

### `public EscalationContext(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String bindingName, io.casehub.api.model.JudgmentTarget target, java.lang.String decision, java.util.List<io.casehub.api.spi.judgment.Evidence> evidence, io.casehub.api.spi.judgment.VerificationResult verificationResult, int escalationCount, int maxEscalations, io.casehub.api.model.CaseDefinition definition, io.casehub.api.spi.judgment.CallerIdentity callerIdentity, java.time.Duration responseTime)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `bindingName` (`java.lang.String`)
- `target` (`io.casehub.api.model.JudgmentTarget`)
- `decision` (`java.lang.String`)
- `evidence` (`java.util.List<io.casehub.api.spi.judgment.Evidence>`)
- `verificationResult` (`io.casehub.api.spi.judgment.VerificationResult`)
- `escalationCount` (`int`)
- `maxEscalations` (`int`)
- `definition` (`io.casehub.api.model.CaseDefinition`)
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

### `public int escalationCount()`

### `public java.util.List<io.casehub.api.spi.judgment.Evidence> evidence()`

### `public final int hashCode()`

### `public int maxEscalations()`

### `public java.time.Duration responseTime()`

### `public io.casehub.api.model.JudgmentTarget target()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`

### `public io.casehub.api.spi.judgment.VerificationResult verificationResult()`
