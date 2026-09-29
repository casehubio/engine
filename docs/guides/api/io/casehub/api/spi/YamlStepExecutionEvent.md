# io.casehub.api.spi.YamlStepExecutionEvent

**Package:** `io.casehub.api.spi`

**Kind:** `record`

## Fields

### `actionName` (`java.lang.String`)

### `bindingType` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `caseType` (`java.lang.String`)

### `contextSnapshot` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `durationMs` (`long`)

### `executionEnvironment` (`java.lang.String`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `parentStepName` (`java.lang.String`)

### `playbookName` (`java.lang.String`)

### `playbookVersion` (`java.lang.String`)

### `resultClassification` (`java.lang.String`)

### `success` (`boolean`)

### `tenancyId` (`java.lang.String`)

### `traceId` (`java.lang.String`)

## Record Components

### `actionName` (`java.lang.String`)

### `bindingType` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `caseType` (`java.lang.String`)

### `contextSnapshot` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `durationMs` (`long`)

### `executionEnvironment` (`java.lang.String`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `parentStepName` (`java.lang.String`)

### `playbookName` (`java.lang.String`)

### `playbookVersion` (`java.lang.String`)

### `resultClassification` (`java.lang.String`)

### `success` (`boolean`)

### `tenancyId` (`java.lang.String`)

### `traceId` (`java.lang.String`)

## Constructors

### `public YamlStepExecutionEvent(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String caseType, java.lang.String actionName, long durationMs, boolean success, java.util.Map<java.lang.String,java.lang.Object> metadata, java.lang.String bindingType, java.lang.String resultClassification, java.lang.String executionEnvironment, java.lang.String parentStepName, java.lang.String playbookName, java.lang.String playbookVersion, java.lang.String traceId, java.util.Map<java.lang.String,java.lang.Object> contextSnapshot)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `caseType` (`java.lang.String`)
- `actionName` (`java.lang.String`)
- `durationMs` (`long`)
- `success` (`boolean`)
- `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `bindingType` (`java.lang.String`)
- `resultClassification` (`java.lang.String`)
- `executionEnvironment` (`java.lang.String`)
- `parentStepName` (`java.lang.String`)
- `playbookName` (`java.lang.String`)
- `playbookVersion` (`java.lang.String`)
- `traceId` (`java.lang.String`)
- `contextSnapshot` (`java.util.Map<java.lang.String,java.lang.Object>`)

## Methods

### `public java.lang.String actionName()`

### `public java.lang.String bindingType()`

### `public java.util.UUID caseId()`

### `public java.lang.String caseType()`

### `public java.util.Map<java.lang.String,java.lang.Object> contextSnapshot()`

### `public long durationMs()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String executionEnvironment()`

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Object> metadata()`

### `public java.lang.String parentStepName()`

### `public java.lang.String playbookName()`

### `public java.lang.String playbookVersion()`

### `public java.lang.String resultClassification()`

### `public boolean success()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`

### `public java.lang.String traceId()`
